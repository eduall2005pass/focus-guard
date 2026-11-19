package com.focusguard.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Lock
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
import com.focusguard.data.model.AppItem
import com.focusguard.data.model.DeviceOwnerStatus
import com.focusguard.data.model.PolicyEnforcementResult
import com.focusguard.data.repository.AllowlistRepository
import com.focusguard.dpc.DevicePolicyController
import com.focusguard.ui.screens.AdminProvisioningScreen
import com.focusguard.ui.screens.AllowedAppsScreen
import com.focusguard.ui.screens.StatusScreen
import com.focusguard.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {

    private lateinit var controller: DevicePolicyController
    private lateinit var repository: AllowlistRepository

    private var currentStatus by mutableStateOf(DeviceOwnerStatus())
    private var isEnforcing by mutableStateOf(false)
    private var lastResult by mutableStateOf<PolicyEnforcementResult?>(null)
    private var installedAppsList by mutableStateOf<List<AppItem>>(emptyList())
    private var customAllowedSet by mutableStateOf<Set<String>>(emptySet())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        controller = DevicePolicyController(this)
        repository = AllowlistRepository(this)

        // Observe custom allowed packages
        lifecycleScope.launch {
            repository.customAllowedPackagesFlow.collect { customSet ->
                customAllowedSet = customSet
                refreshInstalledApps(customSet)
            }
        }

        setContent {
            FocusGuardTheme {
                MainContent()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (controller.checkAndRemediateSafeMode()) return
        refreshStatus()
        refreshInstalledApps(customAllowedSet)
    }

    private fun refreshStatus() {
        currentStatus = controller.getDeviceOwnerStatus()
    }

    private fun refreshInstalledApps(customSet: Set<String>) {
        lifecycleScope.launch(Dispatchers.Default) {
            val installedApps = repository.getAllInstalledAppsWithStatus(customSet).associateBy { it.packageName }.toMutableMap()
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
        }
    }

    private fun triggerEnforceAll() {
        isEnforcing = true
        lifecycleScope.launch(Dispatchers.Default) {
            val result = controller.enforceAllPolicies(customAllowedSet)
            withContext(Dispatchers.Main) {
                lastResult = result
                isEnforcing = false
                refreshStatus()
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
    private fun MainContent() {
        var selectedTabIndex by remember { mutableStateOf(0) }
        val tabs = listOf("Status", "Allowed Apps", "Admin & Setup")

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
                    NavigationBarItem(
                        selected = selectedTabIndex == 0,
                        onClick = { selectedTabIndex = 0 },
                        icon = { Icon(Icons.Default.Shield, contentDescription = "Status") },
                        label = { Text("Status") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GuardGreenText,
                            selectedTextColor = GuardGreenText,
                            indicatorColor = GuardGreenContainer,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTabIndex == 1,
                        onClick = { selectedTabIndex = 1 },
                        icon = { Icon(Icons.Default.Apps, contentDescription = "Allowed Apps") },
                        label = { Text("Allowed Apps") },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GuardGreenText,
                            selectedTextColor = GuardGreenText,
                            indicatorColor = GuardGreenContainer,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted
                        )
                    )
                    NavigationBarItem(
                        selected = selectedTabIndex == 2,
                        onClick = { selectedTabIndex = 2 },
                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin & Setup") },
                        label = { Text("Admin & Setup") },
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
                    1 -> AllowedAppsScreen(
                        installedApps = installedAppsList,
                        customAllowed = customAllowedSet,
                        onAddCustomPackage = { addCustomPackage(it) },
                        onRemoveCustomPackage = { removeCustomPackage(it) },
                        onReinstallPackage = { launchAppStoreReinstall(it) }
                    )
                    2 -> AdminProvisioningScreen()
                }
            }
        }
    }
}
