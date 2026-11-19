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

    companion object {
        private const val TAG = "FocusGuard_DPC"
        const val KAHF_GUARD_DNS_HOST = "high.kahfguard.com"

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
            "com.android.server.telecom"
        )
    }

    fun isDeviceOwner(): Boolean {
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun isDeviceAdmin(): Boolean {
        return dpm.isAdminActive(adminComponent)
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
            activeRestrictions = activeRestrictions
        )
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

        // 2. Enforce System User Restrictions (Including disabling ADB and uninstallation)
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

        // 3. Configure Private DNS to high.kahfguard.com (DNS-over-TLS)
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

        // 4. Enforce Application Allowlist / Blocklist on all installed packages
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
     */
    fun evaluatePackage(packageName: String, customAllowed: Set<String>) {
        if (!isDeviceOwner()) return
        if (packageName == context.packageName || CRITICAL_SYSTEM_PACKAGES.contains(packageName)) return

        val isAllowed = repository.isExplicitlyAllowed(packageName, customAllowed)
        val isBlocked = repository.isExplicitlyBlocked(packageName)

        if (!isAllowed || isBlocked) {
            Log.i(TAG, "Newly installed package $packageName is unapproved. Hiding immediately.")
            try {
                dpm.setApplicationHidden(adminComponent, packageName, true)
            } catch (e: Exception) {
                try {
                    dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), true)
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to restrict new package $packageName", e2)
                }
            }
        }
    }
}
