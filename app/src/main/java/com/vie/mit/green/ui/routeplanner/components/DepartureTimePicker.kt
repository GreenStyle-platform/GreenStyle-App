package com.vie.mit.green.ui.routeplanner.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vie.mit.common.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Thanh chọn thời điểm xuất phát hiển thị trong luồng RoutePlanner.
 * - `selectedDepartureTimeMillis == null`: hiển thị "Khởi hành ngay".
 * - `selectedDepartureTimeMillis != null`: hiển thị ngày & giờ xuất phát kèm nút đặt lại nhanh.
 */
@Composable
fun DepartureTimeSelectorBar(
    selectedDepartureTimeMillis: Long?,
    onClick: () -> Unit,
    onResetToNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val label = remember(selectedDepartureTimeMillis) {
        formatDepartureTimeLabel(selectedDepartureTimeMillis)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selectedDepartureTimeMillis != null) {
            colors.white
        } else {
            colors.white.copy(alpha = 0.18f)
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            val contentColor = if (selectedDepartureTimeMillis != null) colors.navyBlue else colors.white
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = "Thời điểm xuất phát",
                tint = contentColor,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selectedDepartureTimeMillis != null) {
                IconButton(
                    onClick = onResetToNow,
                    modifier = Modifier.size(24.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đặt lại về khởi hành ngay",
                        tint = colors.navyBlue.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private data class DepartureQuickPreset(
    val label: String,
    val offsetMinutes: Int?,
)

private val QuickPresets = listOf(
    DepartureQuickPreset(label = "Ngay bây giờ", offsetMinutes = null),
    DepartureQuickPreset(label = "+15 phút", offsetMinutes = 15),
    DepartureQuickPreset(label = "+30 phút", offsetMinutes = 30),
    DepartureQuickPreset(label = "+1 giờ", offsetMinutes = 60),
)

/**
 * Dialog cho phép người dùng chọn nhanh hoặc tuỳ chỉnh Ngày & Giờ xuất phát (định dạng 24h).
 * Tự động ngăn chọn thời điểm trong quá khứ để khớp ràng buộc API backend.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepartureTimePickerDialog(
    initialDepartureTimeMillis: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long?) -> Unit,
) {
    val colors = AppTheme.colors
    val nowMillis = remember { System.currentTimeMillis() }
    val defaultTargetMillis = remember(initialDepartureTimeMillis, nowMillis) {
        val candidate = initialDepartureTimeMillis ?: (nowMillis + 5 * 60_000L)
        candidate.coerceAtLeast(nowMillis + 60_000L)
    }

    val initialCalendar = remember(defaultTargetMillis) {
        Calendar.getInstance().apply { timeInMillis = defaultTargetMillis }
    }

    var selectedUtcDateMillis by remember(defaultTargetMillis) {
        mutableStateOf(toUtcDatePickerMillis(defaultTargetMillis))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val timePickerState = rememberTimePickerState(
        initialHour = initialCalendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = initialCalendar.get(Calendar.MINUTE),
        is24Hour = true,
    )

    val combinedMillis = combineLocalDateAndTimeMillis(
        utcDateMillis = selectedUtcDateMillis,
        hourOfDay = timePickerState.hour,
        minute = timePickerState.minute,
    )
    val isInPast = combinedMillis < System.currentTimeMillis()
    val formattedDate = remember(selectedUtcDateMillis) {
        formatDateLabelFromUtcMillis(selectedUtcDateMillis)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Text(
                    text = "Chọn thời điểm xuất phát",
                    style = AppTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1F2937),
                    ),
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Các nút chọn nhanh
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(QuickPresets, key = { it.label }) { preset ->
                        FilterChip(
                            selected = preset.offsetMinutes == null && initialDepartureTimeMillis == null,
                            onClick = {
                                if (preset.offsetMinutes == null) {
                                    onConfirm(null)
                                } else {
                                    onConfirm(System.currentTimeMillis() + preset.offsetMinutes * 60_000L)
                                }
                            },
                            label = {
                                Text(
                                    text = preset.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.navyBlue.copy(alpha = 0.12f),
                                selectedLabelColor = colors.navyBlue,
                                containerColor = Color(0xFFF3F4F6),
                                labelColor = Color(0xFF374151),
                            ),
                            border = null,
                            shape = RoundedCornerShape(16.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chọn ngày
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF9FAFB),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Chọn ngày",
                            tint = colors.navyBlue,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Ngày: $formattedDate",
                            style = AppTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1F2937),
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bộ chọn giờ 24h
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        selectorColor = colors.navyBlue,
                        timeSelectorSelectedContainerColor = colors.navyBlue.copy(alpha = 0.14f),
                        timeSelectorSelectedContentColor = colors.navyBlue,
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                if (isInPast) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Thời điểm khởi hành phải là hiện tại hoặc trong tương lai.",
                        style = AppTheme.typography.bodySmall.copy(
                            color = Color(0xFFDC2626),
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Hủy",
                            color = Color(0xFF6B7280),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(combinedMillis) },
                        enabled = !isInPast,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.navyBlue,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            text = "Xác nhận",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val todayUtcStart = remember { toUtcDatePickerMillis(System.currentTimeMillis()) }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedUtcDateMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis >= todayUtcStart
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { picked ->
                            selectedUtcDateMillis = picked
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(text = "Chọn", color = colors.navyBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(text = "Hủy", color = Color(0xFF6B7280))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Converts a local epoch millisecond timestamp to the UTC midnight millisecond format
 * expected by Material 3 [DatePicker], avoiding off-by-one-day timezone shifts (e.g. UTC+7).
 */
internal fun toUtcDatePickerMillis(
    localEpochMillis: Long,
    localTimeZone: TimeZone = TimeZone.getDefault(),
): Long {
    val localCal = Calendar.getInstance(localTimeZone).apply {
        timeInMillis = localEpochMillis
    }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            localCal.get(Calendar.YEAR),
            localCal.get(Calendar.MONTH),
            localCal.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0,
        )
    }.timeInMillis
}

/**
 * Combines a UTC midnight date (from Material 3 [DatePicker]) with local [hourOfDay] and [minute]
 * into an epoch millisecond timestamp in [localTimeZone].
 */
internal fun combineLocalDateAndTimeMillis(
    utcDateMillis: Long,
    hourOfDay: Int,
    minute: Int,
    localTimeZone: TimeZone = TimeZone.getDefault(),
): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcDateMillis
    }
    return Calendar.getInstance(localTimeZone).apply {
        clear()
        set(
            utcCal.get(Calendar.YEAR),
            utcCal.get(Calendar.MONTH),
            utcCal.get(Calendar.DAY_OF_MONTH),
            hourOfDay,
            minute,
            0,
        )
    }.timeInMillis
}

internal fun formatDepartureTimeLabel(
    departureTimeMillis: Long?,
    nowMillis: Long = System.currentTimeMillis(),
    localTimeZone: TimeZone = TimeZone.getDefault(),
): String {
    if (departureTimeMillis == null) return "Khởi hành ngay"

    val targetCal = Calendar.getInstance(localTimeZone).apply { timeInMillis = departureTimeMillis }
    val todayCal = Calendar.getInstance(localTimeZone).apply { timeInMillis = nowMillis }
    val tomorrowCal = Calendar.getInstance(localTimeZone).apply {
        timeInMillis = nowMillis
        add(Calendar.DAY_OF_YEAR, 1)
    }

    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault()).apply {
        timeZone = localTimeZone
    }
    val dateFormatter = SimpleDateFormat("dd/MM", Locale.getDefault()).apply {
        timeZone = localTimeZone
    }
    val timeText = timeFormatter.format(Date(departureTimeMillis))

    val dayText = when {
        isSameDay(targetCal, todayCal) -> "Hôm nay"
        isSameDay(targetCal, tomorrowCal) -> "Ngày mai"
        else -> dateFormatter.format(Date(departureTimeMillis))
    }
    return "Khởi hành: $dayText, $timeText"
}

private fun formatDateLabelFromUtcMillis(
    utcDateMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
    localTimeZone: TimeZone = TimeZone.getDefault(),
): String {
    val combinedNoon = combineLocalDateAndTimeMillis(utcDateMillis, 12, 0, localTimeZone)
    val targetCal = Calendar.getInstance(localTimeZone).apply { timeInMillis = combinedNoon }
    val todayCal = Calendar.getInstance(localTimeZone).apply { timeInMillis = nowMillis }
    val tomorrowCal = Calendar.getInstance(localTimeZone).apply {
        timeInMillis = nowMillis
        add(Calendar.DAY_OF_YEAR, 1)
    }
    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply {
        timeZone = localTimeZone
    }.format(Date(combinedNoon))

    return when {
        isSameDay(targetCal, todayCal) -> "Hôm nay ($dateStr)"
        isSameDay(targetCal, tomorrowCal) -> "Ngày mai ($dateStr)"
        else -> dateStr
    }
}

private fun isSameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
