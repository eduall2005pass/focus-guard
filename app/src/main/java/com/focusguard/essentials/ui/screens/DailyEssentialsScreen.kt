package com.focusguard.essentials.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.R
import com.focusguard.essentials.data.model.*
import com.focusguard.essentials.data.repository.DailyEssentialsRepository
import com.focusguard.essentials.ui.components.*
import com.focusguard.essentials.ui.theme.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DailyEssentialsScreen(
    repository: DailyEssentialsRepository,
    selectedDate: String,
    onDateChanged: (String) -> Unit,
    onNavigateToMonthlyReport: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // State for the selected day's data
    var dayData by remember {
        mutableStateOf(
            DailyEssentialsDayData(
                date = selectedDate,
                salah = DailySalahRecord(date = selectedDate),
                readings = emptyList(),
                quranHadith = QuranHadithRecord(date = selectedDate),
                qaza = DailyQazaRecord(date = selectedDate),
                booksCatalog = emptyList()
            )
        )
    }

    var qazaSettings by remember { mutableStateOf(QazaSettings()) }
    var monthlyQazaSummary by remember { mutableStateOf<MonthlyQazaSummary?>(null) }
    var showQazaDialog by remember { mutableStateOf(false) }

    fun loadDateData(targetDate: String) {
        coroutineScope.launch {
            val data = repository.getDayData(targetDate)
            dayData = data
            val currentYearMonth = targetDate.take(7)
            val settings = repository.getQazaSettings()
            qazaSettings = settings
            monthlyQazaSummary = repository.getMonthlyQazaSummary(currentYearMonth)
        }
    }

    LaunchedEffect(selectedDate) {
        loadDateData(selectedDate)
    }

    val parsedDate = remember(selectedDate) {
        try {
            LocalDate.parse(selectedDate)
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    val formattedDateText = remember(parsedDate) {
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH)
        parsedDate.format(formatter)
    }

    val currentMonthYearText = remember(parsedDate) {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
        parsedDate.format(formatter)
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
        // A. HEADER: Logo + "Focus Guard" + "Plan · Track · Grow" + Mosque watermark + Settings
        // ========================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 2.dp)
        ) {
            // Faint Mosque Watermark aligned to right background
            Icon(
                painter = painterResource(id = R.drawable.ic_mosque_silhouette),
                contentDescription = null,
                tint = DeepEmerald.copy(alpha = 0.18f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(54.dp)
                    .width(150.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Focus Guard Leaf Logo
                Icon(
                    painter = painterResource(id = R.drawable.ic_focus_guard_leaf),
                    contentDescription = "Focus Guard Logo",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(34.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Title & Subtitle
                Column {
                    Text(
                        text = "Focus Guard",
                        fontFamily = IslamicSerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = DarkForestText
                    )
                    Text(
                        text = "Plan · Track · Grow",
                        fontFamily = IslamicSansFont,
                        fontSize = 11.sp,
                        color = MutedSlateText,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Settings icon button in refined rounded container
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WarmIvorySubtle)
                        .border(1.dp, WarmCardBorder, RoundedCornerShape(12.dp))
                        .clickable { onOpenSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = Color(0xFF334B3E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ========================================================
        // SEGMENTED TAB SWITCHER: [ Today ] [ Monthly Report ]
        // ========================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(23.dp))
                .background(WarmIvorySubtle)
                .border(1.dp, WarmCardBorder, RoundedCornerShape(23.dp))
                .padding(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Today Tab (Active)
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
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Today",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Today",
                            fontFamily = IslamicSansFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }

                // Monthly Report Tab (Inactive)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigateToMonthlyReport() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_chart_bars),
                            contentDescription = "Monthly Report",
                            tint = MutedSlateText,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Monthly Report",
                            fontFamily = IslamicSansFont,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MutedSlateText
                        )
                    }
                }
            }
        }

        // ========================================================
        // DATE ROW: Calendar Icon + Date Text + Day Steppers
        // ========================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.DateRange,
                contentDescription = "Calendar",
                tint = DarkForestText,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = formattedDateText,
                fontFamily = IslamicSansFont,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = DarkForestText,
                modifier = Modifier.clickable {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val picked = LocalDate.of(year, month + 1, dayOfMonth)
                            onDateChanged(picked.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        },
                        parsedDate.year,
                        parsedDate.monthValue - 1,
                        parsedDate.dayOfMonth
                    ).show()
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = {
                    val prevDay = parsedDate.minusDays(1)
                    onDateChanged(prevDay.format(DateTimeFormatter.ISO_LOCAL_DATE))
                },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Day",
                    tint = MutedSlateText
                )
            }

            IconButton(
                onClick = {
                    val nextDay = parsedDate.plusDays(1)
                    onDateChanged(nextDay.format(DateTimeFormatter.ISO_LOCAL_DATE))
                },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Day",
                    tint = MutedSlateText
                )
            }
        }

        // ========================================================
        // B. DAILY SALAH CARD
        // ========================================================
        SalahCard(
            record = dayData.salah,
            onToggleSalah = { type ->
                coroutineScope.launch {
                    val updated = repository.toggleSalah(selectedDate, type)
                    dayData = dayData.copy(salah = updated)
                }
            }
        )

        // ========================================================
        // C. BOOK READING CARD (Multiple Books)
        // ========================================================
        ReadingCard(
            readingsToday = dayData.readings,
            booksCatalog = dayData.booksCatalog,
            onAddReading = { title, pages, totalPages ->
                coroutineScope.launch {
                    repository.saveReading(selectedDate, title, pages, totalPages)
                    loadDateData(selectedDate)
                }
            },
            onDeleteReading = { readingId ->
                coroutineScope.launch {
                    repository.deleteReading(readingId)
                    loadDateData(selectedDate)
                }
            }
        )

        // ========================================================
        // D. QURAN & HADITH CARD
        // ========================================================
        QuranHadithCard(
            record = dayData.quranHadith,
            onUpdateRecord = { updated ->
                dayData = dayData.copy(quranHadith = updated)
                coroutineScope.launch {
                    repository.saveQuranHadithRecord(updated)
                    loadDateData(selectedDate)
                }
            }
        )

        // ========================================================
        // E. QAZA SALAH CARD
        // ========================================================
        QazaSalahCard(
            record = dayData.qaza,
            onIncrementQaza = { type, delta ->
                coroutineScope.launch {
                    val updated = repository.incrementDailyQaza(selectedDate, type, delta)
                    dayData = dayData.copy(qaza = updated)
                    val ym = selectedDate.substring(0, 7)
                    monthlyQazaSummary = repository.getMonthlyQazaSummary(ym)
                }
            },
            onSetQazaCount = { type, count ->
                coroutineScope.launch {
                    val updated = repository.setDailyQazaCount(selectedDate, type, count)
                    dayData = dayData.copy(qaza = updated)
                    val ym = selectedDate.substring(0, 7)
                    monthlyQazaSummary = repository.getMonthlyQazaSummary(ym)
                }
            },
            onOpenMonthlySummary = {
                showQazaDialog = true
            }
        )

        // ========================================================
        // MONTHLY REPORT BANNER CARD: [ Bar chart | November 2026 | > ]
        // ========================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(WarmIvorySurface)
                .border(1.dp, WarmCardBorder, RoundedCornerShape(18.dp))
                .clickable { onNavigateToMonthlyReport() }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_chart_bars),
                    contentDescription = "Monthly",
                    tint = DeepEmerald,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = currentMonthYearText,
                        fontFamily = IslamicSerifFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DarkForestText
                    )
                    Text(
                        text = "View your monthly report",
                        fontFamily = IslamicSansFont,
                        fontSize = 12.sp,
                        color = MutedSlateText
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View",
                    tint = DarkCharcoalText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Qaza Starting Balance & Monthly Carry-Forward Dialog
    if (showQazaDialog) {
        QazaBalanceDialog(
            settings = qazaSettings,
            monthlySummary = monthlyQazaSummary,
            onSaveSettings = { updatedSettings ->
                coroutineScope.launch {
                    repository.saveQazaSettings(updatedSettings)
                    qazaSettings = updatedSettings
                    val ym = selectedDate.substring(0, 7)
                    monthlyQazaSummary = repository.getMonthlyQazaSummary(ym)
                }
            },
            onDismiss = { showQazaDialog = false }
        )
    }
}
