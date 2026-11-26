package com.vie.mit.data.repository.geocode

import com.vie.mit.data.model.remote.response.map.HereReverseGeocodeItemResponse
import com.vie.mit.data.repository.geocode.ReverseGeocodeResultSelector.MIN_QUERY_LENGTH
import java.text.Normalizer
import java.util.Locale

/**
 * Selects the best address label from a list of HERE reverse-geocode candidates.
 *
 * When [queryHint] has at least [MIN_QUERY_LENGTH] trimmed characters, candidates are ranked
 * against `title`, `address.label`, and `categories[].name` using Vietnamese-friendly
 * normalization (case-insensitive and diacritic-insensitive, including `đ`/`Đ` -> `d`) with
 * exact-diacritic and shortest-distance tie-breakers.
 *
 * Otherwise (or when no candidate matches the query), the closest valid candidate is returned.
 */
object ReverseGeocodeResultSelector {
    const val MIN_QUERY_LENGTH: Int = 3
    private val WHITESPACE_REGEX = Regex("\\s+")
    private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")

    private data class RankedCandidate(
        val label: String,
        val matchTier: Int,
        val exactAccentPenalty: Int,
        val distance: Long,
        val index: Int,
    )

    fun hasEffectiveQuery(queryHint: String?): Boolean =
        collapseWhitespace(queryHint).length >= MIN_QUERY_LENGTH

    fun selectBestLabel(
        items: List<HereReverseGeocodeItemResponse>,
        queryHint: String? = null,
    ): String? {
        val validCandidates = items.mapIndexedNotNull { index, item ->
            val label = item.displayLabel() ?: return@mapIndexedNotNull null
            Triple(index, item, label)
        }
        if (validCandidates.isEmpty()) return null
        val collapsedQuery = collapseWhitespace(queryHint)
        if (collapsedQuery.length < MIN_QUERY_LENGTH) {
            return pickClosest(validCandidates)
        }
        val rawLowerQuery = collapsedQuery.lowercase(Locale.ROOT)
        val normalizedQuery = normalizeVietnamese(collapsedQuery)
        if (normalizedQuery.length < MIN_QUERY_LENGTH) {
            return pickClosest(validCandidates)
        }
        val queryTokens = normalizedQuery.split(" ").filter { it.isNotBlank() }
        val bestMatch = validCandidates.mapNotNull { (index, item, label) ->
            scoreCandidate(
                index = index,
                item = item,
                displayLabel = label,
                rawLowerQuery = rawLowerQuery,
                normalizedQuery = normalizedQuery,
                queryTokens = queryTokens,
            )
        }.minWithOrNull(
            compareBy<RankedCandidate>(
                { it.matchTier },
                { it.exactAccentPenalty },
                { it.distance },
                { it.index },
            ),
        )

        return bestMatch?.label ?: pickClosest(validCandidates)
    }

    internal fun normalizeVietnamese(input: String): String {
        val collapsed = collapseWhitespace(input).lowercase(Locale.ROOT).replace('đ', 'd')
        val decomposed = Normalizer.normalize(collapsed, Normalizer.Form.NFD)
        return DIACRITICS_REGEX.replace(decomposed, "")
    }

    private fun scoreCandidate(
        index: Int,
        item: HereReverseGeocodeItemResponse,
        displayLabel: String,
        rawLowerQuery: String,
        normalizedQuery: String,
        queryTokens: List<String>,
    ): RankedCandidate? {
        val titleRaw = collapseWhitespace(item.title).lowercase(Locale.ROOT)
        val titleNorm = normalizeVietnamese(titleRaw)
        val labelRaw = collapseWhitespace(item.address?.label).lowercase(Locale.ROOT)
        val labelNorm = normalizeVietnamese(labelRaw)
        val categories = item.categories.orEmpty().mapNotNull { category ->
            val rawName = collapseWhitespace(category.name).lowercase(Locale.ROOT)
            if (rawName.isBlank()) {
                null
            } else {
                Triple(rawName, normalizeVietnamese(rawName), category.primary == true)
            }
        }
        val titleMatched = titleNorm.contains(normalizedQuery)
        val titleExactAccent = titleRaw.contains(rawLowerQuery)
        val labelMatched = labelNorm.contains(normalizedQuery)
        val labelExactAccent = labelRaw.contains(rawLowerQuery)
        val primaryCategoryMatched = categories.any { (_, normName, isPrimary) ->
            isPrimary && normName.contains(normalizedQuery)
        }
        val primaryCategoryExactAccent = categories.any { (rawName, _, isPrimary) ->
            isPrimary && rawName.contains(rawLowerQuery)
        }
        val anyCategoryMatched = categories.any { (_, normName, _) ->
            normName.contains(normalizedQuery)
        }
        val anyCategoryExactAccent = categories.any { (rawName, _, _) ->
            rawName.contains(rawLowerQuery)
        }

        val (tier, exactAccent) = when {
            titleMatched && anyCategoryMatched -> 0 to (titleExactAccent && anyCategoryExactAccent)
            titleMatched -> 1 to titleExactAccent
            labelMatched -> 2 to labelExactAccent
            primaryCategoryMatched -> 3 to primaryCategoryExactAccent
            anyCategoryMatched -> 4 to anyCategoryExactAccent
            queryTokens.size >= 2 -> {
                val combinedNorm = buildString {
                    append(titleNorm)
                    append(' ')
                    append(labelNorm)
                    categories.forEach { (_, normName, _) ->
                        append(' ')
                        append(normName)
                    }
                }
                if (queryTokens.all { combinedNorm.contains(it) }) {
                    val combinedRaw = buildString {
                        append(titleRaw)
                        append(' ')
                        append(labelRaw)
                        categories.forEach { (rawName, _, _) ->
                            append(' ')
                            append(rawName)
                        }
                    }
                    val rawTokens = rawLowerQuery.split(" ").filter { it.isNotBlank() }
                    5 to rawTokens.all { combinedRaw.contains(it) }
                } else {
                    return null
                }
            }

            else -> return null
        }

        return RankedCandidate(
            label = displayLabel,
            matchTier = tier,
            exactAccentPenalty = if (exactAccent) 0 else 1,
            distance = item.distance ?: Long.MAX_VALUE,
            index = index,
        )
    }

    private fun pickClosest(
        candidates: List<Triple<Int, HereReverseGeocodeItemResponse, String>>,
    ): String = candidates.minWith(
        compareBy<Triple<Int, HereReverseGeocodeItemResponse, String>>(
            { it.second.distance ?: Long.MAX_VALUE },
            { it.first },
        ),
    ).third

    private fun HereReverseGeocodeItemResponse.displayLabel(): String? =
        address?.label?.trim()?.takeIf { it.isNotBlank() } ?: title?.trim()
            ?.takeIf { it.isNotBlank() }

    private fun collapseWhitespace(value: String?): String =
        value?.trim()?.replace(WHITESPACE_REGEX, " ").orEmpty()
}
