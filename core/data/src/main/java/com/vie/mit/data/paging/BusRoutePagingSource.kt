package com.vie.mit.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.vie.mit.data.model.domain.map.BusRoute
import com.vie.mit.data.model.mapper.toDomain
import com.vie.mit.data.network.api.transit.TransitRouteApi

class BusRoutePagingSource(
    private val transitRouteApi: TransitRouteApi,
    private val query: String,
) : PagingSource<Int, BusRoute>() {

    override fun getRefreshKey(state: PagingState<Int, BusRoute>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, BusRoute> {
        val page = params.key ?: 1
        return try {
            val response = transitRouteApi.searchBusRoutes(query = query, page = page)
            val routes = response.content.map { it.toDomain() }
            val nextKey = if (response.last || response.currentPage >= response.totalPages || routes.isEmpty()) {
                null
            } else {
                response.currentPage + 1
            }
            val prevKey = if (response.first || response.currentPage <= 1) {
                null
            } else {
                response.currentPage - 1
            }
            LoadResult.Page(
                data = routes,
                prevKey = prevKey,
                nextKey = nextKey,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
