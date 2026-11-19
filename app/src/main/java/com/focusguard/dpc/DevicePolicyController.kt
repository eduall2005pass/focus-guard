package com.focusguard.dpc

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import android.util.Log
import com.focusguard.data.model.DeviceOwnerStatus
import com.focusguard.data.model.PolicyEnforcementResult
import com.focusguard.data.repository.AllowlistRepository
import com.focusguard.receiver.FocusGuardDeviceAdminReceiver

class DevicePolicyController(private val context: Context) {

    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    private val adminComponent = ComponentName(context, FocusGuardDeviceAdminReceiver::class.java)
    private val repository = AllowlistRepository(context)
    val baselineManager = com.focusguard.data.repository.BaselineInventoryManager(context)
    val approvedAppRegistry = com.focusguard.data.repository.ApprovedAppRegistry(context)

    companion object {
        private const val TAG = "FocusGuard_DPC"
        const val KAHF_GUARD_DNS_HOST = "high.kahfguard.com"
        const val GALAXY_STORE_PACKAGE = "com.sec.android.app.samsungapps"
        const val KNOX_SERVICE_PLUGIN_PACKAGE = "com.samsung.android.knox.ksp"

        // Critical safety set: packages that MUST NEVER be hidden under any condition
        val CRITICAL_SYSTEM_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.google.android.webview",
            "com.android.webview",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.vending",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.android.providers.settings",
            "com.android.keychain",
            "com.android.phone",
            "com.android.server.telecom",
            // Samsung essential update & firmware maintenance components
            "com.sec.android.app.samsungapps.una2", // Samsung In-App Update Agent
            "com.wssyncmldm",                      // Samsung FOTA / OS Firmware Update
            "com.sec.android.soagent",                   // Samsung System Update Agent
            "com.samsung.android.app.omcagent",    // Samsung OMC / Carrier Update Agent
            "com.samsung.android.samsungpass",     // Samsung Pass Framework
            "com.samsung.android.samsungpassautofill", // Samsung Pass Autofill
            // Google Play & System Integration Services
            "com.google.android.modulemetadata",
            "com.google.android.googlequicksearchbox" // Preserved for Circle to Search & OCR text copy
        )
    }

    fun isDeviceOwner(): Boolean {
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun isDeviceAdmin(): Boolean {
        return dpm.isAdminActive(adminComponent)
    }

    /**
     * Checks if the device was booted into Safe Mode.
     * If Safe Mode is detected, automatically forces an immediate reboot back to normal mode.
     */
    fun checkAndRemediateSafeMode(): Boolean {
        if (context.packageManager.isSafeMode) {
            Log.e(TAG, "CRITICAL: Safe Mode detected on device! Forcing reboot back to normal mode.")
            rebootToNormalMode()
            return true
        }
        return false
    }

    /**
     * Forces immediate reboot of the device back to normal mode using Device Owner privilege.
     */
    fun rebootToNormalMode() {
        if (!isDeviceOwner()) return
        try {
            Log.w(TAG, "Rebooting device via Device Policy Controller...")
            dpm.reboot(adminComponent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute device reboot: ${e.message}", e)
        }
    }

    fun getDeviceOwnerStatus(): DeviceOwnerStatus {
        val isDO = isDeviceOwner()
        val isDA = isDeviceAdmin()
        val activeRestrictions = mutableListOf<String>()

        var privateDnsMode = 0
        var privateDnsHost: String? = null
        var isPrivateDnsLocked = false
        var isVpnRestricted = false
        var isSafeBootDisallowed = false
        var isUnknownSourcesBlocked = false
        var isUninstallBlocked = false

        if (isDO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    privateDnsMode = dpm.getGlobalPrivateDnsMode(adminComponent)
                    privateDnsHost = dpm.getGlobalPrivateDnsHost(adminComponent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read Private DNS mode", e)
            }

            try {
                val bundle = dpm.getUserRestrictions(adminComponent)
                for (key in bundle.keySet()) {
                    if (bundle.getBoolean(key)) {
                        activeRestrictions.add(key)
                    }
                }
                isPrivateDnsLocked = activeRestrictions.contains(UserManager.DISALLOW_CONFIG_PRIVATE_DNS)
                isVpnRestricted = activeRestrictions.contains(UserManager.DISALLOW_CONFIG_VPN)
                isSafeBootDisallowed = activeRestrictions.contains(UserManager.DISALLOW_SAFE_BOOT)
                isUnknownSourcesBlocked = activeRestrictions.contains(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read user restrictions", e)
            }

            try {
                isUninstallBlocked = dpm.isUninstallBlocked(adminComponent, context.packageName)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read uninstall blocked status", e)
            }
        }

        val isAdbDebuggingBlocked = activeRestrictions.contains(UserManager.DISALLOW_DEBUGGING_FEATURES)
        val isPolicyPermanentlyLocked = isDO && isAdbDebuggingBlocked && isUninstallBlocked
        val baselineCount = baselineManager.loadBaseline().size

        return DeviceOwnerStatus(
            isDeviceOwner = isDO,
            isDeviceAdmin = isDA,
            adminComponent = adminComponent.flattenToShortString(),
            privateDnsMode = privateDnsMode,
            privateDnsHost = privateDnsHost,
            isPrivateDnsLocked = isPrivateDnsLocked,
            isVpnRestricted = isVpnRestricted,
            isUninstallBlocked = isUninstallBlocked,
            isSafeBootDisallowed = isSafeBootDisallowed,
            isUnknownSourcesBlocked = isUnknownSourcesBlocked,
            isAdbDebuggingBlocked = isAdbDebuggingBlocked,
            isPolicyPermanentlyLocked = isPolicyPermanentlyLocked,
            baselinePackageCount = baselineCount,
            activeRestrictions = activeRestrictions
        )
    }

    /**
     * Configures Samsung Knox Service Plugin (KSP) via Android Enterprise Managed Configurations (OEMConfig)
     * if KSP is present on the device.
     */
    fun configureKnoxServicePlugin(): Boolean {
        if (!isDeviceOwner()) return false
        return try {
            // Check if Knox Service Plugin is installed
            context.packageManager.getPackageInfo(KNOX_SERVICE_PLUGIN_PACKAGE, 0)

            val approvedPkgsString = approvedAppRegistry.getApprovedPackages().joinToString(",")
            val kspRestrictions = android.os.Bundle().apply {
                val appPolicies = android.os.Bundle().apply {
                    putBoolean("enableApplicationManagementPolicies", true)
                    // On One UI 8.0/Knox 3.13, setting showGalaxyStoreApp to "Hide"
                    // conceals the Galaxy Store UI while preserving underlying Samsung update daemons
                    putString("showGalaxyStoreApp", "Hide")
                    putString("installWhiteList", approvedPkgsString)
                }
                putBundle("applicationManagementPolicies", appPolicies)
            }

            dpm.setApplicationRestrictions(adminComponent, KNOX_SERVICE_PLUGIN_PACKAGE, kspRestrictions)
            Log.i(TAG, "Successfully applied Knox Service Plugin OEMConfig for Galaxy Store isolation.")
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "Knox Service Plugin not installed. Scoped presentation interceptor will guard Galaxy Store.")
            false
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply KSP restrictions: ${e.message}", e)
            false
        }
    }

    /**
     * Enforces the complete Focus Guard policy suite.
     * Must be run when DO is active.
     */
    fun enforceAllPolicies(
        customAllowed: Set<String>,
        blockNewAppInstalls: Boolean = true
    ): PolicyEnforcementResult {
        if (!isDeviceOwner()) {
            return PolicyEnforcementResult(
                errors = listOf("Application is not provisioned as Device Owner. Run provisioning commands first.")
            )
        }

        val errors = mutableListOf<String>()
        val appliedRestrictions = mutableListOf<String>()
        var hiddenCount = 0
        var suspendedCount = 0
        var protectedCount = 0

        // 1. Enforce Focus Guard Self-Protection (Prevent Uninstall)
        try {
            dpm.setUninstallBlocked(adminComponent, context.packageName, true)
            protectedCount++
        } catch (e: Exception) {
            errors.add("Failed to lock uninstall for Focus Guard: ${e.message}")
        }

        // 2. Initialize Permanent Approved App Registry & Baseline Inventory
        try {
            val registryCount = approvedAppRegistry.initializeRegistryIfNotExists()
            val baselineCount = baselineManager.createBaselineIfNotExists()
            Log.i(TAG, "Approved registry active ($registryCount packages). Baseline active ($baselineCount packages).")
        } catch (e: Exception) {
            errors.add("Failed to initialize app registry/baseline: ${e.message}")
        }

        // 3. Configure Automatic Samsung OTA Firmware Updates
        try {
            dpm.setSystemUpdatePolicy(
                adminComponent,
                android.app.admin.SystemUpdatePolicy.createAutomaticInstallPolicy()
            )
            Log.i(TAG, "Automatic Samsung OTA update policy enforced.")
        } catch (e: Exception) {
            Log.w(TAG, "Could not set automatic system update policy: ${e.message}")
        }

        // 4. Configure Knox Service Plugin (OEMConfig) for Galaxy Store Isolation
        configureKnoxServicePlugin()

        // 5. Enforce System User Restrictions (Including disabling ADB and uninstallation)
        val standardRestrictions = mutableListOf(
            UserManager.DISALLOW_CONFIG_VPN,
            UserManager.DISALLOW_NETWORK_RESET,
            UserManager.DISALLOW_SAFE_BOOT,
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
            UserManager.DISALLOW_ADD_USER,
            UserManager.DISALLOW_CONFIG_DATE_TIME,
            UserManager.DISALLOW_DEBUGGING_FEATURES, // Disables USB Debugging (ADB) completely
            UserManager.DISALLOW_UNINSTALL_APPS     // Disallows uninstalling applications system-wide
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            standardRestrictions.add(UserManager.DISALLOW_CONFIG_PRIVATE_DNS)
        }

        if (blockNewAppInstalls) {
            standardRestrictions.add(UserManager.DISALLOW_INSTALL_APPS)
        }

        for (restriction in standardRestrictions) {
            try {
                dpm.addUserRestriction(adminComponent, restriction)
                appliedRestrictions.add(restriction)
            } catch (e: Exception) {
                errors.add("Failed to apply restriction $restriction: ${e.message}")
            }
        }

        // 6. Configure Private DNS to high.kahfguard.com (DNS-over-TLS)
        var dnsStatus = "Unsupported on this Android version"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val dnsResult = dpm.setGlobalPrivateDnsModeSpecifiedHost(adminComponent, KAHF_GUARD_DNS_HOST)
                dnsStatus = when (dnsResult) {
                    DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR -> "Enforced ($KAHF_GUARD_DNS_HOST)"
                    DevicePolicyManager.PRIVATE_DNS_SET_ERROR_HOST_NOT_SERVING -> "Warning: Host not serving TLS on 853 ($KAHF_GUARD_DNS_HOST)"
                    DevicePolicyManager.PRIVATE_DNS_SET_ERROR_FAILURE_SETTING -> "Error: Failure applying Private DNS setting"
                    else -> "Status code: $dnsResult"
                }
            } catch (e: Exception) {
                dnsStatus = "Failed: ${e.message}"
                errors.add("Private DNS error: ${e.message}")
            }
        }

        // 7. Enforce Application Allowlist / Blocklist on all installed packages
        val pm = context.packageManager
        val installedPackages = pm.getInstalledPackages(PackageManager.GET_META_DATA)

        for (pkgInfo in installedPackages) {
            val pkgName = pkgInfo.packageName

            // Safety guard: Never touch Focus Guard itself or critical system services
            if (pkgName == context.packageName || CRITICAL_SYSTEM_PACKAGES.contains(pkgName)) {
                try {
                    // Ensure critical packages are unhidden and unsuspended
                    dpm.setApplicationHidden(adminComponent, pkgName, false)
                    dpm.setPackagesSuspended(adminComponent, arrayOf(pkgName), false)
                    dpm.setUninstallBlocked(adminComponent, pkgName, true)
                } catch (ignored: Exception) {
                }
                continue
            }

            // Special handling for Samsung Galaxy Store on One UI 8.0:
            // Do NOT hide the package, because doing so halts background Samsung app and service updates.
            // The UI is isolated via Knox KSP OEMConfig and the Focus Guard foreground presentation interceptor.
            if (pkgName == GALAXY_STORE_PACKAGE) {
                try {
                    dpm.setApplicationHidden(adminComponent, pkgName, false)
                    dpm.setPackagesSuspended(adminComponent, arrayOf(pkgName), false)
                } catch (ignored: Exception) {
                }
                continue
            }

            val isAllowed = repository.isExplicitlyAllowed(pkgName, customAllowed)
            val isExplicitlyBlocked = repository.isExplicitlyBlocked(pkgName)
            val isEssential = repository.isEssential(pkgName)

            // Determine if app should be accessible
            val shouldAllow = (isAllowed || isEssential) && !isExplicitlyBlocked

            if (shouldAllow) {
                // Unhide and unsuspend if previously restricted
                try {
                    val isHidden = dpm.isApplicationHidden(adminComponent, pkgName)
                    if (isHidden) {
                        dpm.setApplicationHidden(adminComponent, pkgName, false)
                    }
                    dpm.setPackagesSuspended(adminComponent, arrayOf(pkgName), false)
                    dpm.setUninstallBlocked(adminComponent, pkgName, true)
                    protectedCount++
                } catch (e: Exception) {
                    Log.w(TAG, "Could not unhide/protect allowed app $pkgName: ${e.message}")
                }
            } else {
                // App is not allowed: hide and suspend
                try {
                    val isSystemApp = (pkgInfo.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
                    // For third-party apps and unapproved system apps (like Chrome/YouTube/Browsers)
                    dpm.setApplicationHidden(adminComponent, pkgName, true)
                    hiddenCount++
                } catch (e: Exception) {
                    // If hiding fails on some OEM system app, suspend it
                    try {
                        val suspendResult = dpm.setPackagesSuspended(adminComponent, arrayOf(pkgName), true)
                        if (suspendResult.isEmpty()) {
                            suspendedCount++
                        } else {
                            errors.add("Could not hide/suspend $pkgName: ${e.message}")
                        }
                    } catch (e2: Exception) {
                        errors.add("Failed to restrict $pkgName: ${e2.message}")
                    }
                }
            }
        }

        return PolicyEnforcementResult(
            totalPackagesScanned = installedPackages.size,
            packagesHidden = hiddenCount,
            packagesSuspended = suspendedCount,
            packagesProtected = protectedCount,
            restrictionsApplied = appliedRestrictions,
            privateDnsStatus = dnsStatus,
            errors = errors
        )
    }

    /**
     * Event-driven evaluation when a package is added or replaced.
     * Uses cryptographic baseline validation to permit updates to pre-existing apps
     * while intercepting unauthorized new apps.
     */
    fun evaluatePackage(packageName: String, customAllowed: Set<String>) {
        if (!isDeviceOwner()) return
        if (packageName == context.packageName || CRITICAL_SYSTEM_PACKAGES.contains(packageName) || packageName == GALAXY_STORE_PACKAGE) return

        // 1. Check if the package is present in the Permanent Approved App Registry
        // (Allows initial install as well as reinstallation after being uninstalled)
        if (approvedAppRegistry.isApproved(packageName)) {
            val certSha256 = baselineManager.getInstalledCertificateSha256(packageName)
            approvedAppRegistry.markInstalledState(packageName, isInstalled = true, certSha256 = certSha256)
            Log.i(TAG, "Approved application $packageName verified in permanent registry (install/reinstall permitted).")
            try {
                dpm.setApplicationHidden(adminComponent, packageName, false)
                dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), false)
                dpm.setUninstallBlocked(adminComponent, packageName, true)
            } catch (e: Exception) {
                Log.w(TAG, "Error ensuring unhidden state for approved package $packageName: ${e.message}")
            }
            return
        }

        // 2. Check if this is a legitimate update to an existing baseline package
        if (baselineManager.isLegitimateBaselineUpdate(packageName)) {
            Log.i(TAG, "Legitimate update to baseline package $packageName verified with matching signature. Permitting.")
            try {
                dpm.setApplicationHidden(adminComponent, packageName, false)
                dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), false)
                dpm.setUninstallBlocked(adminComponent, packageName, true)
            } catch (e: Exception) {
                Log.w(TAG, "Error ensuring unhidden state for updated package $packageName: ${e.message}")
            }
            return
        }

        // 3. Unauthorized new application: Enforce immediate lockdown and remediation
        Log.w(TAG, "UNAUTHORIZED APPLICATION DETECTED: $packageName! Immediate remediation initiated.")
        try {
            dpm.setApplicationHidden(adminComponent, packageName, true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide unauthorized package $packageName", e)
        }

        try {
            dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to suspend unauthorized package $packageName", e)
        }

        // Attempt silent uninstallation via PackageInstaller if permitted
        try {
            val packageInstaller = context.packageManager.packageInstaller
            val callbackIntent = android.content.Intent("com.focusguard.ACTION_UNINSTALL_COMPLETE")
            val pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                0,
                callbackIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            packageInstaller.uninstall(packageName, pendingIntent.intentSender)
            Log.i(TAG, "Triggered uninstallation session for unauthorized package: $packageName")
        } catch (e: Exception) {
            Log.w(TAG, "Could not trigger silent uninstall for $packageName (app remains hidden/suspended): ${e.message}")
        }
    }

    /**
     * Called when an application is uninstalled by the user.
     * Preserves the approved record in the registry so the user can reinstall it at any time.
     */
    fun onPackageUninstalled(packageName: String) {
        if (!isDeviceOwner()) return
        if (approvedAppRegistry.isApproved(packageName)) {
            Log.i(TAG, "Approved package $packageName uninstalled. Record preserved in registry for future reinstallation.")
            approvedAppRegistry.markInstalledState(packageName, isInstalled = false)
        }
    }
}
