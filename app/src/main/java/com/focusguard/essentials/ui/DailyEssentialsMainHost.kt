package com.focusguard.essentials.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.R
import com.focusguard.data.security.SecurityPinManager
import com.focusguard.essentials.data.model.QazaSettings
import com.focusguard.essentials.data.repository.DailyEssentialsRepository
import com.focusguard.essentials.ui.components.ExplicitFilterCard
import com.focusguard.essentials.ui.components.QazaBalanceDialog
import com.focusguard.essentials.ui.components.StudyResourcesCard
import com.focusguard.essentials.ui.screens.DailyEssentialsScreen
import com.focusguard.essentials.ui.screens.MonthlyReportScreen
import com.focusguard.essentials.ui.screens.ReadingLibraryScreen
import com.focusguard.essentials.ui.theme.*
import com.focusguard.study.model.ApprovedWebsite
import com.focusguard.ui.components.SecurityThreeDotMenu
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class EssentialsNavigationTab {
    HOME,
    REPORT,
    LIBRARY,
    PROFILE
}

@Composable
fun DailyEssentialsMainHost(
    repository: DailyEssentialsRepository,
    onOpenFocusGuardStatus: () -> Unit,
    onOpenStudyWebsite: (ApprovedWebsite) -> Unit = {},
    onOpenStudySitesTab: () -> Unit = {},
    pinManager: SecurityPinManager? = null,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(EssentialsNavigationTab.HOME) }
    var selectedDate by remember {
        mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
    }
    var selectedYearMonth by remember {
        mutableStateOf(YearMonth.now().toString())
    }

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showQazaSetupDialog by remember { mutableStateOf(false) }
    var qazaSettings by remember { mutableStateOf(QazaSettings()) }

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        qazaSettings = repository.getQazaSettings()
    }

    androidx.activity.compose.BackHandler(enabled = currentTab != EssentialsNavigationTab.HOME) {
        currentTab = EssentialsNavigationTab.HOME
    }

    Scaffold(
        bottomBar = {
            // Sleek, compact bottom navigation bar matching the reference screenshot exactly
            EssentialsBottomNavBar(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    currentTab = tab
                }
            )
        },
        containerColor = WarmIvoryBackground,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                EssentialsNavigationTab.HOME -> {
                    DailyEssentialsScreen(
                        repository = repository,
                        selectedDate = selectedDate,
                        onDateChanged = { newDate ->
                            selectedDate = newDate
                            selectedYearMonth = newDate.take(7)
                        },
                        onNavigateToMonthlyReport = {
                            selectedYearMonth = selectedDate.take(7)
                            currentTab = EssentialsNavigationTab.REPORT
                        },
                        onOpenSettings = {
                            showSettingsSheet = true
                        }
                    )
                }

                EssentialsNavigationTab.REPORT -> {
                    MonthlyReportScreen(
                        repository = repository,
                        selectedYearMonth = selectedYearMonth,
                        onYearMonthChanged = { newYm ->
                            selectedYearMonth = newYm
                        },
                        onNavigateToToday = {
                            selectedDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                            selectedYearMonth = selectedDate.take(7)
                            currentTab = EssentialsNavigationTab.HOME
                        },
                        onSelectDay = { targetDate ->
                            selectedDate = targetDate
                            selectedYearMonth = targetDate.take(7)
                            currentTab = EssentialsNavigationTab.HOME
                        }
                    )
                }

                EssentialsNavigationTab.LIBRARY -> {
                    ReadingLibraryScreen(
                        repository = repository
                    )
                }

                EssentialsNavigationTab.PROFILE -> {
                    EssentialsProfileScreen(
                        repository = repository,
                        onOpenFocusGuardAdmin = onOpenFocusGuardStatus,
                        onOpenQazaSetup = { showQazaSetupDialog = true },
                        onOpenStudyWebsite = onOpenStudyWebsite,
                        onOpenAllStudySites = onOpenStudySitesTab,
                        pinManager = pinManager
                    )
                }
            }
        }
    }

    // Settings Quick Menu Dialog
    if (showSettingsSheet) {
        AlertDialog(
            onDismissRequest = { showSettingsSheet = false },
            title = {
                Text(
                    text = "Daily Essentials Settings",
                    fontFamily = IslamicSerifFont,
                    fontWeight = FontWeight.Bold,
                    color = DarkForestText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showSettingsSheet = false
                            selectedDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                            selectedYearMonth = selectedDate.substring(0, 7)
                            currentTab = EssentialsNavigationTab.HOME
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Jump to Today", color = DeepEmerald, fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(
                        onClick = {
                            showSettingsSheet = false
                            showQazaSetupDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Adjust Starting Qaza Balances", color = DeepEmerald, fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(
                        onClick = {
                            showSettingsSheet = false
                            currentTab = EssentialsNavigationTab.PROFILE
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Study Resources & Sites", color = DeepEmerald, fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(
                        onClick = {
                            showSettingsSheet = false
                            currentTab = EssentialsNavigationTab.PROFILE
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Explicit Filter & Blocklist", color = DeepEmerald, fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(
                        onClick = {
                            showSettingsSheet = false
                            onOpenFocusGuardStatus()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Open Focus Guard Status", color = DeepEmerald, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettingsSheet = false },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                ) {
                    Text("Close", color = WarmIvorySurface)
                }
            },
            containerColor = WarmIvorySurface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showQazaSetupDialog) {
        QazaBalanceDialog(
            settings = qazaSettings,
            monthlySummary = null,
            onSaveSettings = { updated ->
                coroutineScope.launch {
                    repository.saveQazaSettings(updated)
                    qazaSettings = updated
                    showQazaSetupDialog = false
                }
            },
            onDismiss = { showQazaSetupDialog = false }
        )
    }
}

@Composable
private fun EssentialsBottomNavBar(
    currentTab: EssentialsNavigationTab,
    onTabSelected: (EssentialsNavigationTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = WarmIvorySubtle,
        tonalElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(width = 0.8.dp, color = WarmCardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(54.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // 1. Home
            EssentialsNavItem(
                icon = { tint ->
                    Icon(
                        imageVector = if (currentTab == EssentialsNavigationTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = "Home",
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = "Home",
                isSelected = currentTab == EssentialsNavigationTab.HOME,
                onClick = { onTabSelected(EssentialsNavigationTab.HOME) }
            )

            // 2. Report
            EssentialsNavItem(
                icon = { tint ->
                    Icon(
                        painter = painterResource(id = R.drawable.ic_chart_bars),
                        contentDescription = "Report",
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = "Report",
                isSelected = currentTab == EssentialsNavigationTab.REPORT,
                onClick = { onTabSelected(EssentialsNavigationTab.REPORT) }
            )

            // 3. Library
            EssentialsNavItem(
                icon = { tint ->
                    Icon(
                        painter = painterResource(id = R.drawable.ic_book_open),
                        contentDescription = "Library",
                        tint = tint,
                        modifier = Modifier.size(20.dp)
                    )
                },
                label = "Library",
                isSelected = currentTab == EssentialsNavigationTab.LIBRARY,
                onClick = { onTabSelected(EssentialsNavigationTab.LIBRARY) }
            )

            // 4. Profile
            EssentialsNavItem(
                icon = { tint ->
                    Icon(
                        imageVector = if (currentTab == EssentialsNavigationTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                        contentDescription = "Profile",
                        tint = tint,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = "Profile",
                isSelected = currentTab == EssentialsNavigationTab.PROFILE,
                onClick = { onTabSelected(EssentialsNavigationTab.PROFILE) }
            )
        }
    }
}

@Composable
private fun RowScope.EssentialsNavItem(
    icon: @Composable (Color) -> Unit,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (isSelected) DeepEmerald else MutedSlateText
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        icon(tint)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontFamily = IslamicSansFont,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 11.sp,
            color = tint
        )
    }
}

@Composable
private fun EssentialsProfileScreen(
    repository: DailyEssentialsRepository,
    onOpenFocusGuardAdmin: () -> Unit,
    onOpenQazaSetup: () -> Unit,
    onOpenStudyWebsite: (ApprovedWebsite) -> Unit,
    onOpenAllStudySites: () -> Unit,
    pinManager: SecurityPinManager?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WarmIvoryBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profile & Settings",
                fontFamily = IslamicSerifFont,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = DarkForestText
            )
            SecurityThreeDotMenu(
                pinManager = pinManager,
                tint = DarkForestText
            )
        }

        // 1. Study Resources Card (BoiMate, Medical Study, GTC, Approved Sites)
        StudyResourcesCard(
            onOpenWebsite = onOpenStudyWebsite,
            onOpenAllSites = onOpenAllStudySites
        )

        // 2. Explicit Filter & Blocklist Card (Zero PIN required!)
        ExplicitFilterCard()

        // 3. Focus Guard Protection Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = WarmIvorySurface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(WarmCardBorder)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(SageGreenLight, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = DeepEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Focus Guard Protection",
                            fontFamily = IslamicSerifFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DarkForestText
                        )
                        Text(
                            text = "ডিভাইস ওনার সুরক্ষা, প্রাইভেট ডিএনএস ও এক্সেসিবিলিটি সক্রিয়",
                            fontFamily = IslamicSansFont,
                            fontSize = 11.sp,
                            color = MutedSlateText
                        )
                    }
                }
                Button(
                    onClick = onOpenFocusGuardAdmin,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Focus Guard Status", color = WarmIvorySurface, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 4. Qaza Salah Balances Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = WarmIvorySurface),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(WarmCardBorder)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Qaza Salah Initial Balances",
                    fontFamily = IslamicSerifFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = DarkForestText
                )
                Text(
                    text = "Configure your baseline outstanding Qaza count to calculate carry-forward accurately each month.",
                    fontFamily = IslamicSansFont,
                    fontSize = 12.sp,
                    color = MutedSlateText
                )
                Button(
                    onClick = onOpenQazaSetup,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Configure Qaza Balances", color = WarmIvorySurface)
                }
            }
        }
    }
}
