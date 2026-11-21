package com.focusguard.essentials.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.R
import com.focusguard.essentials.data.model.*
import com.focusguard.essentials.data.repository.DailyEssentialsRepository
import com.focusguard.essentials.ui.components.MonthlyActivityCalendar
import com.focusguard.essentials.ui.components.QazaBalanceDialog
import com.focusguard.essentials.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthlyReportScreen(
    repository: DailyEssentialsRepository,
    selectedYearMonth: String,
    onYearMonthChanged: (String) -> Unit,
    onNavigateToToday: () -> Unit,
    onSelectDay: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var reportData by remember { mutableStateOf<MonthlyReportData?>(null) }
    var qazaSettings by remember { mutableStateOf(QazaSettings()) }
    var showQazaDialog by remember { mutableStateOf(false) }

    fun loadMonthData(ym: String) {
        coroutineScope.launch {
            val data = repository.getMonthlyReport(ym)
            reportData = data
            qazaSettings = repository.getQazaSettings()
        }
    }

    LaunchedEffect(selectedYearMonth) {
        loadMonthData(selectedYearMonth)
    }

    val parsedYm = remember(selectedYearMonth) {
        try {
            YearMonth.parse(selectedYearMonth)
        } catch (e: Exception) {
            YearMonth.now()
        }
    }

    val monthNameText = remember(parsedYm) {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
        parsedYm.format(formatter)
    }

    val isCurrentMonth = remember(parsedYm) {
        parsedYm == YearMonth.now()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmIvoryBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ========================================================
        // TOP SWITCHER: [ Today ] [ Monthly Report (Active) ]
        // ========================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(Color(0xFFECEAE3))
                .padding(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Today Tab (Inactive)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigateToToday() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Today",
                            tint = Color(0xFF3E5247),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Today",
                            fontFamily = IslamicSansFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color(0xFF3E5247)
                        )
                    }
                }

                // Monthly Report Tab (Active)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(DeepEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chart_bars),
                            contentDescription = "Monthly Report",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Monthly Report",
                            fontFamily = IslamicSansFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // ========================================================
        // MONTH NAVIGATION BAR: < [ MMMM yyyy ] > [ Today ]
        // ========================================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(WarmIvorySurface)
                .border(1.dp, WarmCardBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = {
                    val prev = parsedYm.minusMonths(1).toString()
                    onYearMonthChanged(prev)
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = DarkForestText
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = monthNameText,
                    fontFamily = IslamicSerifFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkForestText
                )
                Text(
                    text = "Summary & Trends",
                    fontFamily = IslamicSansFont,
                    fontSize = 11.sp,
                    color = MutedSlateText
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isCurrentMonth) {
                    IconButton(
                        onClick = {
                            val curr = YearMonth.now().toString()
                            onYearMonthChanged(curr)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "Current Month",
                            tint = DeepEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val next = parsedYm.plusMonths(1).toString()
                        onYearMonthChanged(next)
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Month",
                        tint = DarkForestText
                    )
                }
            }
        }

        reportData?.let { data ->
            // ========================================================
            // 1. MONTHLY OVERVIEW
            // ========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(WarmIvorySurface)
                    .border(1.dp, WarmCardBorder, RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Monthly Overview",
                        fontFamily = IslamicSerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkForestText
                    )

                    // 2x3 Metric Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricPill(
                            title = "Salah Completed",
                            value = "${data.totalSalahCompleted} / ${data.totalSalahPossible}",
                            subtitle = if (data.totalSalahPossible > 0) "${((data.totalSalahCompleted.toFloat() / data.totalSalahPossible) * 100).toInt()}%" else "0%",
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            title = "Book Reading",
                            value = "${data.totalReadingPages} pgs",
                            subtitle = "${data.bookSummaries.size} books active",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricPill(
                            title = "Quran Study",
                            value = "${data.quranStudyPages} pgs",
                            subtitle = "${data.quranStudyAyahs} ayahs",
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            title = "Quran Memorization",
                            value = "${data.quranMemoPages} pgs",
                            subtitle = "${data.quranMemoAyahs} ayahs",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricPill(
                            title = "Hadith Study",
                            value = "${data.hadithStudyCount}",
                            subtitle = "hadith studied",
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            title = "Hadith Memorization",
                            value = "${data.hadithMemoCount}",
                            subtitle = "hadith memorized",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ========================================================
            // 2. SALAH CONSISTENCY
            // ========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(WarmIvorySurface)
                    .border(1.dp, WarmCardBorder, RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Salah Consistency",
                        fontFamily = IslamicSerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = DarkForestText
                    )

                    data.salahConsistency.forEach { stat ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stat.type.displayName,
                                    fontFamily = IslamicSansFont,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = DarkForestText
                                )
                                Text(
                                    text = "${stat.completedDays} / ${stat.totalDays} days (${stat.percentage.toInt()}%)",
                                    fontFamily = IslamicSansFont,
                                    fontSize = 11.5.sp,
                                    color = MutedSlateText
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (stat.percentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = DeepEmerald,
                                trackColor = InputPillBackground
                            )
                        }
                    }
                }
            }

            // ========================================================
            // 3. READING HISTORY
            // ========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(WarmIvorySurface)
                    .border(1.dp, WarmCardBorder, RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reading History",
                            fontFamily = IslamicSerifFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = DarkForestText
                        )
                        Text(
                            text = "Total: ${data.totalReadingPages} pgs",
                            fontFamily = IslamicSansFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = DeepEmerald
                        )
                    }

                    if (data.bookSummaries.isEmpty()) {
                        Text(
                            text = "No book reading recorded in this month yet.",
                            fontFamily = IslamicSansFont,
                            fontSize = 12.sp,
                            color = MutedSlateText,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        data.bookSummaries.forEach { book ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(WarmIvorySubtle)
                                    .border(1.dp, WarmCardBorder, RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.bookTitle,
                                        fontFamily = IslamicSansFont,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = DarkForestText
                                    )
                                    Text(
                                        text = "Lifetime total: ${book.lifetimePages} pages",
                                        fontFamily = IslamicSansFont,
                                        fontSize = 11.sp,
                                        color = MutedSlateText
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SageGreenLight)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "+${book.pagesThisMonth} pgs",
                                        fontFamily = IslamicSansFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = DeepEmerald
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ========================================================
            // 4. QAZA SALAH SUMMARY & CARRY FORWARD
            // ========================================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(WarmIvorySurface)
                    .border(1.dp, WarmCardBorder, RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Qaza Salah Summary",
                                fontFamily = IslamicSerifFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = DarkForestText
                            )
                            Text(
                                text = "Remaining = Prior + Added - Completed",
                                fontFamily = IslamicSansFont,
                                fontSize = 11.sp,
                                color = MutedSlateText
                            )
                        }

                        TextButton(onClick = { showQazaDialog = true }) {
                            Text("Adjust", color = DeepEmerald, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Table
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarmIvorySubtle)
                            .border(1.dp, WarmCardBorder, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Prayer", fontFamily = IslamicSansFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MutedSlateText)
                            Text("Prior", fontFamily = IslamicSansFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MutedSlateText)
                            Text("Added", fontFamily = IslamicSansFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MutedSlateText)
                            Text("Done", fontFamily = IslamicSansFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MutedSlateText)
                            Text("Remaining", fontFamily = IslamicSansFont, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = DeepEmerald)
                        }
                        HorizontalDivider(color = WarmDivider)

                        data.qazaSummary.prayers.forEach { (type, stat) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(type.displayName, fontFamily = IslamicSansFont, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = DarkForestText)
                                Text("${stat.priorOutstanding}", fontFamily = IslamicSansFont, fontSize = 12.sp, color = DarkCharcoalText)
                                Text("+${stat.addedThisMonth}", fontFamily = IslamicSansFont, fontSize = 12.sp, color = MutedSlateText)
                                Text("-${stat.completedThisMonth}", fontFamily = IslamicSansFont, fontSize = 12.sp, color = AccentGreenHeart)
                                Text("${stat.remaining}", fontFamily = IslamicSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DeepEmerald)
                            }
                        }

                        HorizontalDivider(color = WarmDivider)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Outstanding", fontFamily = IslamicSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkForestText)
                            Text("", fontSize = 12.sp)
                            Text("", fontSize = 12.sp)
                            Text("-${data.qazaSummary.totalCompletedThisMonth}", fontFamily = IslamicSansFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentGreenHeart)
                            Text("${data.qazaSummary.totalRemaining}", fontFamily = IslamicSansFont, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DeepEmerald)
                        }
                    }
                }
            }

            // ========================================================
            // 5. MONTHLY ACTIVITY CALENDAR (Heatmap)
            // ========================================================
            MonthlyActivityCalendar(
                yearMonth = selectedYearMonth,
                activityIntensityMap = data.dailyActivityIntensity,
                selectedDate = "",
                onSelectDate = { tappedDate ->
                    onSelectDay(tappedDate)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showQazaDialog) {
        QazaBalanceDialog(
            settings = qazaSettings,
            monthlySummary = reportData?.qazaSummary,
            onSaveSettings = { updated ->
                coroutineScope.launch {
                    repository.saveQazaSettings(updated)
                    loadMonthData(selectedYearMonth)
                }
            },
            onDismiss = { showQazaDialog = false }
        )
    }
}

@Composable
private fun MetricPill(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(WarmIvorySubtle)
            .border(1.dp, WarmCardBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(
            text = title,
            fontFamily = IslamicSansFont,
            fontSize = 11.sp,
            color = MutedSlateText
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            fontFamily = IslamicSansFont,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = DarkForestText
        )
        Text(
            text = subtitle,
            fontFamily = IslamicSansFont,
            fontSize = 11.sp,
            color = DeepEmerald
        )
    }
}
