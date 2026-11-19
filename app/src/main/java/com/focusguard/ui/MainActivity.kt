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
        refreshStatus()
        refreshInstalledApps(customAllowedSet)
    }

    private fun refreshStatus() {
        currentStatus = controller.getDeviceOwnerStatus()
    }

    private fun refreshInstalledApps(customSet: Set<String>) {
        lifecycleScope.launch(Dispatchers.Default) {
            val apps = repository.getAllInstalledAppsWithStatus(customSet)
            withContext(Dispatchers.Main) {
                installedAppsList = apps
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
            repository.addCustomAllowedPackage(packageName)
            Toast.makeText(this@MainActivity, "Added $packageName to allowlist", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeCustomPackage(packageName: String) {
        lifecycleScope.launch {
            repository.removeCustomAllowedPackage(packageName)
            Toast.makeText(this@MainActivity, "Removed $packageName from allowlist", Toast.LENGTH_SHORT).show()
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
                        onRemoveCustomPackage = { removeCustomPackage(it) }
                    )
                    2 -> AdminProvisioningScreen()
                }
            }
        }
    }
}
