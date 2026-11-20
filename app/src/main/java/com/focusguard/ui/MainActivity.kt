package com.focusguard.ui

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.focusguard.R
import com.focusguard.essentials.data.repository.DailyEssentialsRepository
import com.focusguard.essentials.ui.DailyEssentialsMainHost
import com.focusguard.study.filter.StudyCoolingOffManager
import com.focusguard.data.model.AppItem
import com.focusguard.data.model.DeviceOwnerStatus
import com.focusguard.data.model.PolicyEnforcementResult
import com.focusguard.data.repository.AllowlistRepository
import com.focusguard.dpc.DevicePolicyController
import com.focusguard.study.model.ApprovedWebsite
import com.focusguard.study.repository.StudyWebsiteRegistry
import com.focusguard.study.ui.FullscreenStudyBrowser
import com.focusguard.study.ui.StudyPortalScreen
import com.focusguard.study.ui.StudySitesScreen
import com.focusguard.ui.screens.AdminProvisioningScreen
import com.focusguard.ui.screens.AllowedAppsScreen
import com.focusguard.ui.screens.StatusScreen
import com.focusguard.ui.theme.*
import com.focusguard.data.security.SecurityPinManager
import com.focusguard.ui.components.SetSecurityPinDialog
import com.focusguard.ui.components.EnterSecurityPinDialog
import com.focusguard.ui.components.SecurityThreeDotMenu
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "FocusGuard_Main"
        private const val APP_LIST_REFRESH_COOLDOWN_MS = 5_000L
    }

    private lateinit var controller: DevicePolicyController
    private lateinit var repository: AllowlistRepository
    private lateinit var dailyEssentialsRepo: DailyEssentialsRepository

    enum class AppScreenMode {
        DAILY_ESSENTIALS,
        DEVICE_SECURITY
    }

    private var currentScreenMode by mutableStateOf(AppScreenMode.DAILY_ESSENTIALS)
    private var initialSecurityTabIndex by mutableIntStateOf(0)
    private var activeFullscreenWebsite by mutableStateOf<ApprovedWebsite?>(null)
    private var currentStatus by mutableStateOf(DeviceOwnerStatus())
    private var isEnforcing by mutableStateOf(false)
    private var lastResult by mutableStateOf<PolicyEnforcementResult?>(null)
    private var installedAppsList by mutableStateOf<List<AppItem>>(emptyList())
    private var customAllowedSet by mutableStateOf<Set<String>>(emptySet())
    private var installedAppsRefreshJob: Job? = null
    private var lastInstalledAppsRefreshAt = 0L
    private var lastInstalledAppsRefreshSet: Set<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        controller = DevicePolicyController(this)
        repository = AllowlistRepository(this)
        dailyEssentialsRepo = DailyEssentialsRepository(this)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                com.focusguard.study.repository.StudyWebsiteRegistry(this@MainActivity).loadWebsites()
            } catch (_: Exception) {}
        }
        handleScreenModeIntent(intent)

        try {
            com.focusguard.accessibility.AccessibilityServiceWatcher(
                this,
                android.os.Handler(android.os.Looper.getMainLooper())
            ).startWatching()
        } catch (_: Exception) {}

        // Observe custom allowed packages and automatically enforce policies on launch
        lifecycleScope.launch {
            repository.customAllowedPackagesFlow
                .catch { error ->
                    Log.e(TAG, "Could not read saved app permissions; continuing with defaults", error)
                    emit(emptySet())
                }
                .collect { customSet ->
                    customAllowedSet = customSet
                    refreshInstalledApps(customSet)
                    if (controller.isDeviceOwner()) {
                        withContext(Dispatchers.IO) {
                            try {
                                controller.enforceAllPolicies(customSet)
                            } catch (e: Exception) {
                                Log.e(TAG, "Automatic policy enforcement on launch failed", e)
                            }
                        }
                    }
                }
        }

        setContent {
            val pinManager = remember { SecurityPinManager(this@MainActivity) }
            val studyWebsiteRegistry = remember { StudyWebsiteRegistry(this@MainActivity) }
            var isPinSet by remember { mutableStateOf(pinManager.isPinSet()) }

            if (!isPinSet) {
                SetSecurityPinDialog(
                    pinManager = pinManager,
                    onPinSetSuccess = {
                        isPinSet = true
                    },
                    onDismiss = {}
                )
            }

            // When a website is open, show True Fullscreen Browser
            if (activeFullscreenWebsite != null) {
                FullscreenStudyBrowser(
                    website = activeFullscreenWebsite!!,
                    approvedHosts = studyWebsiteRegistry.getAllowedHosts(),
                    onClose = { activeFullscreenWebsite = null }
                )
            } else {
                when (currentScreenMode) {
                    AppScreenMode.DAILY_ESSENTIALS -> {
                        DailyEssentialsMainHost(
                            repository = dailyEssentialsRepo,
                            onOpenFocusGuardStatus = {
                                initialSecurityTabIndex = 0
                                currentScreenMode = AppScreenMode.DEVICE_SECURITY
                            },
                            onOpenStudyWebsite = { website ->
                                activeFullscreenWebsite = website
                            },
                            onOpenStudySitesTab = {
                                initialSecurityTabIndex = 1
                                currentScreenMode = AppScreenMode.DEVICE_SECURITY
                            },
                            pinManager = pinManager
                        )
                    }
                    AppScreenMode.DEVICE_SECURITY -> {
                        FocusGuardTheme {
                            androidx.activity.compose.BackHandler {
                                currentScreenMode = AppScreenMode.DAILY_ESSENTIALS
                            }

                            MainContent(
                                initialTabIndex = initialSecurityTabIndex,
                                pinManager = pinManager,
                                onBackToEssentials = {
                                    currentScreenMode = AppScreenMode.DAILY_ESSENTIALS
                                },
                                onOpenWebsite = { website ->
                                    activeFullscreenWebsite = website
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            controller.ensureAccessibilityServiceEnabled()
        } catch (_: Exception) {}
        try {
            if (controller.checkAndRemediateSafeMode()) return
        } catch (error: Exception) {
            Log.e(TAG, "Safe-mode check failed; keeping the main screen available", error)
        }
        try {
            if (StudyCoolingOffManager(this).isCoolingOffActive()) {
                val allowedPackages = customAllowedSet.toSet()
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        controller.enforceEmergencyRestrictedPolicy(true, allowedPackages)
                    } catch (error: Exception) {
                        Log.e(TAG, "Could not restore emergency app restrictions", error)
                    }
                }
            }
        } catch (error: Exception) {
            Log.e(TAG, "Could not read the saved cooling-off state", error)
        }
        refreshStatusSafely()
        refreshInstalledApps(customAllowedSet)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleScreenModeIntent(intent)
    }

    private fun handleScreenModeIntent(intent: android.content.Intent?) {
        activeFullscreenWebsite = null
        when (intent?.getStringExtra("screen_mode")) {
            "security" -> {
                initialSecurityTabIndex = intent.getIntExtra("tab_index", 0)
                currentScreenMode = AppScreenMode.DEVICE_SECURITY
            }
            "study_sites", "study_resources" -> {
                initialSecurityTabIndex = 1
                currentScreenMode = AppScreenMode.DEVICE_SECURITY
            }
            "essentials" -> {
                currentScreenMode = AppScreenMode.DAILY_ESSENTIALS
            }
        }
    }

    override fun onStop() {
        super.onStop()
        SecurityPinManager.lockSession()
        currentScreenMode = AppScreenMode.DAILY_ESSENTIALS
    }

    private fun refreshStatusSafely() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val status = controller.getDeviceOwnerStatus()
                withContext(Dispatchers.Main) {
                    currentStatus = status
                }
            } catch (error: Exception) {
                Log.e(TAG, "Could not read device-owner status", error)
            }
        }
    }

    private fun refreshInstalledApps(customSet: Set<String>) {
        val now = SystemClock.elapsedRealtime()
        if (installedAppsRefreshJob?.isActive == true) return
        if (lastInstalledAppsRefreshSet == customSet &&
            now - lastInstalledAppsRefreshAt < APP_LIST_REFRESH_COOLDOWN_MS
        ) return

        lastInstalledAppsRefreshAt = now
        lastInstalledAppsRefreshSet = customSet.toSet()
        installedAppsRefreshJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val installedApps = repository.getAllInstalledAppsWithStatus(customSet)
                    .associateBy { it.packageName }
                    .toMutableMap()
                val approvedRegistry = controller.approvedAppRegistry.loadRegistry()

                // Merge registry entries: Ensure uninstalled approved apps appear in the list
                for ((pkgName, entry) in approvedRegistry) {
                    if (!installedApps.containsKey(pkgName)) {
                        installedApps[pkgName] = AppItem(
                            packageName = pkgName,
                            displayName = entry.displayName,
                            category = entry.category,
                            isEssential = entry.isEssential,
                            isAllowed = true,
                            isInstalled = false,
                            note = "Approved in Registry (Reinstallable)"
                        )
                    }
                }

                val mergedList = installedApps.values.sortedWith(
                    compareBy({ !it.isAllowed }, { it.category.ordinal }, { it.displayName })
                )

                withContext(Dispatchers.Main) {
                    installedAppsList = mergedList
                }
            } catch (error: Exception) {
                Log.e(TAG, "Could not load installed apps; leaving the app usable", error)
            } finally {
                installedAppsRefreshJob = null
            }
        }
    }

    private fun triggerEnforceAll() {
        isEnforcing = true
        lifecycleScope.launch(Dispatchers.Default) {
            val result = controller.enforceAllPolicies(customAllowedSet)
            withContext(Dispatchers.Main) {
                lastResult = result
                isEnforcing = false
                refreshStatusSafely()
                refreshInstalledApps(customAllowedSet)
                Toast.makeText(
                    this@MainActivity,
                    "Enforcement complete: ${result.packagesProtected} protected, ${result.packagesHidden} hidden",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun addCustomPackage(packageName: String) {
        lifecycleScope.launch {
            controller.approvedAppRegistry.addApprovedPackage(packageName)
            repository.addCustomAllowedPackage(packageName)
            refreshInstalledApps(customAllowedSet)
            Toast.makeText(this@MainActivity, "Permanently approved $packageName in registry", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeCustomPackage(packageName: String) {
        lifecycleScope.launch {
            controller.approvedAppRegistry.removeApprovedPackage(packageName)
            repository.removeCustomAllowedPackage(packageName)
            refreshInstalledApps(customAllowedSet)
            Toast.makeText(this@MainActivity, "Removed $packageName from approved registry", Toast.LENGTH_SHORT).show()
        }
    }

    private fun launchAppStoreReinstall(packageName: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=$packageName")).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(webIntent)
            } catch (e2: Exception) {
                Toast.makeText(this@MainActivity, "Cannot open store for $packageName: ${e2.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainContent(
        initialTabIndex: Int = 0,
        pinManager: SecurityPinManager,
        onBackToEssentials: () -> Unit,
        onOpenWebsite: (ApprovedWebsite) -> Unit
    ) {
        var selectedTabIndex by remember(initialTabIndex) { mutableStateOf(initialTabIndex) }
        val studyWebsiteRegistry = remember { StudyWebsiteRegistry(this@MainActivity) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = GuardGreenText,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Focus Guard",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "Device Restriction System",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                )
                            }
                        }
                    },
                    actions = {
                        SecurityThreeDotMenu(
                            pinManager = pinManager,
                            tint = GuardGreenText
                        )
                        IconButton(onClick = onBackToEssentials) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_focus_guard_leaf),
                                contentDescription = "Daily Essentials",
                                tint = GuardGreenText,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkBackground,
                        titleContentColor = TextPrimary
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = TextPrimary
                ) {
                    // 1. Daily Essentials
                    NavigationBarItem(
                        selected = false,
                        onClick = onBackToEssentials,
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_focus_guard_leaf),
                                contentDescription = "Daily",
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = { Text("Daily", maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GuardGreenText,
                            selectedTextColor = GuardGreenText,
                            indicatorColor = GuardGreenContainer,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )

                    // 2. Status
                    NavigationBarItem(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        icon = { Icon(Icons.Default.Shield, contentDescription = "Status") },
                        label = { Text("Status", maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GuardGreenText,
                            selectedTextColor = GuardGreenText,
                            indicatorColor = GuardGreenContainer,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )

                    // 3. Study Sites (BoiMate, Medical Contents, GTC...)
                    NavigationBarItem(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        icon = { Icon(Icons.Default.School, contentDescription = "Study Sites") },
                        label = { Text("Study Sites", maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GuardGreenText,
                            selectedTextColor = GuardGreenText,
                            indicatorColor = GuardGreenContainer,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )

                    // 4. Dashboard (Telegram, Explicit Filters & App Guard)
                    NavigationBarItem(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard", maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GuardGreenText,
                            selectedTextColor = GuardGreenText,
                            indicatorColor = GuardGreenContainer,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                }
            },
            containerColor = DarkBackground
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTabIndex) {
                    0 -> StatusScreen(
                        status = currentStatus,
                        isEnforcing = isEnforcing,
                        lastResult = lastResult,
                        onEnforceClicked = { triggerEnforceAll() }
                    )
                    1 -> StudySitesScreen(
                        websiteRegistry = studyWebsiteRegistry,
                        onOpenWebsite = onOpenWebsite
                    )
                    2 -> StudyPortalScreen()
                }
            }
        }
    }
}
