package com.oryno.piggy_ledger.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oryno.piggy_ledger.R
import com.oryno.piggy_ledger.ui.theme.NavyDark
import com.oryno.piggy_ledger.ui.theme.PinkPrimary
import com.oryno.piggy_ledger.ui.theme.TextLight
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BirthdayWheelPickerSheet(
    initialDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val currentCalendar = remember { Calendar.getInstance() }
    val currentYear = remember { currentCalendar.get(Calendar.YEAR) }
    val minYear = 1920

    val initialParts = remember(initialDate) {
        parseBirthDate(initialDate, minYear, currentYear)
    }

    var selectedYear by remember { mutableIntStateOf(initialParts.first) }
    var selectedMonth by remember { mutableIntStateOf(initialParts.second) } // 1..12
    var selectedDay by remember { mutableIntStateOf(initialParts.third) }

    val years = remember(minYear, currentYear) { (minYear..currentYear).toList() }
    val months = remember { (1..12).toList() }
    val shortMonthNames = remember {
        val symbols = DateFormatSymbols.getInstance(Locale.getDefault()).shortMonths
        (0 until 12).map { idx ->
            symbols.getOrNull(idx)?.takeIf { it.isNotBlank() } ?: (idx + 1).toString()
        }
    }

    val maxDaysInMonth = remember(selectedYear, selectedMonth) {
        daysInMonth(selectedYear, selectedMonth)
    }
    LaunchedEffect(maxDaysInMonth) {
        if (selectedDay > maxDaysInMonth) {
            selectedDay = maxDaysInMonth
        }
    }
    val days = remember(maxDaysInMonth) { (1..maxDaysInMonth).toList() }

    val formattedPreview = remember(selectedYear, selectedMonth, selectedDay) {
        String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay.coerceAtMost(maxDaysInMonth))
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = NavyDark.copy(alpha = 0.2f)) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        modifier = Modifier.testTag("birthday_wheel_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 4.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.profile_select_birthday),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = PinkPrimary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = formattedPreview,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = PinkPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close_btn),
                        tint = TextLight,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3-Column Wheel Area (Year | Month | Day) with unified center selection highlight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                // Highlight pill across all 3 wheels for the centered row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(PinkPrimary.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                        .border(1.dp, PinkPrimary.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
                )

                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DateWheelColumn(
                        items = years,
                        selectedItem = selectedYear,
                        onItemSelected = { selectedYear = it },
                        labelProvider = { it.toString() },
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("birthday_wheel_year")
                    )

                    DateWheelColumn(
                        items = months,
                        selectedItem = selectedMonth,
                        onItemSelected = { selectedMonth = it },
                        labelProvider = { m -> shortMonthNames[m - 1] },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("birthday_wheel_month")
                    )

                    DateWheelColumn(
                        items = days,
                        selectedItem = selectedDay.coerceIn(1, maxDaysInMonth),
                        onItemSelected = { selectedDay = it },
                        labelProvider = { d -> String.format(Locale.US, "%02d", d) },
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("birthday_wheel_day")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("birthday_wheel_cancel_button")
                ) {
                    Text(
                        text = stringResource(R.string.cancel_btn),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextLight
                    )
                }

                Button(
                    onClick = { onConfirm(formattedPreview) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PinkPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("birthday_wheel_save_button")
                ) {
                    Text(
                        text = stringResource(R.string.save_btn),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> DateWheelColumn(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier
) {
    val itemHeight = 44.dp
    val initialIndex = remember(items.size) {
        items.indexOf(selectedItem).coerceAtLeast(0)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()

    val centeredIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                listState.firstVisibleItemIndex
            } else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visibleItems.minByOrNull { item ->
                    abs((item.offset + item.size / 2) - viewportCenter)
                }?.index ?: listState.firstVisibleItemIndex
            }
        }
    }

    LaunchedEffect(centeredIndex, items.size) {
        if (centeredIndex in items.indices) {
            val item = items[centeredIndex]
            if (item != selectedItem) {
                onItemSelected(item)
            }
        }
    }

    LaunchedEffect(selectedItem, items.size) {
        val targetIndex = items.indexOf(selectedItem)
        if (targetIndex in items.indices && !listState.isScrollInProgress && targetIndex != centeredIndex) {
            listState.scrollToItem(targetIndex)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = snapFlingBehavior,
        contentPadding = PaddingValues(vertical = 88.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.height(220.dp)
    ) {
        items(count = items.size) { index ->
            val distance = abs(index - centeredIndex)
            val textColor = when (distance) {
                0 -> NavyDark
                1 -> NavyDark.copy(alpha = 0.55f)
                else -> Color(0xFF94A3B8).copy(alpha = 0.65f)
            }
            val textSize = when (distance) {
                0 -> 18.sp
                1 -> 15.sp
                else -> 13.sp
            }
            val weight = if (distance == 0) FontWeight.Bold else FontWeight.Medium

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelProvider(items[index]),
                    color = textColor,
                    fontSize = textSize,
                    fontWeight = weight,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun parseBirthDate(raw: String, minYear: Int, maxYear: Int): Triple<Int, Int, Int> {
    val defaultYear = (maxYear - 25).coerceIn(minYear, maxYear)
    val parts = raw.trim().split("-")
    if (parts.size == 3) {
        val y = parts[0].toIntOrNull()?.coerceIn(minYear, maxYear)
        val m = parts[1].toIntOrNull()?.coerceIn(1, 12)
        val d = parts[2].toIntOrNull()
        if (y != null && m != null && d != null) {
            return Triple(y, m, d.coerceIn(1, daysInMonth(y, m)))
        }
    }
    return Triple(defaultYear, 1, 1)
}

private fun daysInMonth(year: Int, month: Int): Int {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
}
