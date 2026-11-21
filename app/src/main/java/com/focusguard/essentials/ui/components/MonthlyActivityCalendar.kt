package com.focusguard.essentials.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.essentials.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthlyActivityCalendar(
    yearMonth: String,
    activityIntensityMap: Map<String, Float>,
    selectedDate: String,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ym = try {
        YearMonth.parse(yearMonth)
    } catch (e: Exception) {
        YearMonth.now()
    }

    val firstDayOfWeek = ym.atDay(1).dayOfWeek.value % 7 // 0 = Sunday, 1 = Monday, etc.
    val daysInMonth = ym.lengthOfMonth()
    val dayLabels = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(WarmIvorySurface)
            .border(1.dp, WarmCardBorder, RoundedCornerShape(22.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Activity Heatmap",
                    fontFamily = IslamicSerifFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = DarkForestText
                )

                // Heatmap legend: 4 small squares
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Less", fontFamily = IslamicSansFont, fontSize = 10.sp, color = MutedSlateText)
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(InputPillBackground))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFD3E2D7)))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFF6B9F7D)))
                    Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(DeepEmerald))
                    Text("More", fontFamily = IslamicSansFont, fontSize = 10.sp, color = MutedSlateText)
                }
            }

            // Weekday labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayLabels.forEach { label ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontFamily = IslamicSansFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = MutedSlateText
                        )
                    }
                }
            }

            // Days Grid
            val totalCells = firstDayOfWeek + daysInMonth
            val totalRows = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until totalRows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - firstDayOfWeek + 1

                            if (dayNumber in 1..daysInMonth) {
                                val dateStr = String.format("%s-%02d", yearMonth, dayNumber)
                                val intensity = activityIntensityMap[dateStr] ?: 0f
                                val isSelected = dateStr == selectedDate

                                val cellBg = when {
                                    intensity <= 0f -> InputPillBackground
                                    intensity <= 0.3f -> Color(0xFFD3E2D7)
                                    intensity <= 0.65f -> Color(0xFF6B9F7D)
                                    else -> DeepEmerald
                                }

                                val textCol = when {
                                    intensity > 0.65f -> Color.White
                                    else -> DarkForestText
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(cellBg)
                                        .border(
                                            width = if (isSelected) 2.dp else 0.dp,
                                            color = if (isSelected) MutedGold else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onSelectDate(dateStr) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        fontFamily = IslamicSansFont,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = textCol
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
