package com.focusguard.dpc

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager
import android.provider.Settings
import android.util.Log
import android.view.inputmethod.InputMethodManager
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
    private val parentalControlPrefs = context.getSharedPreferences(PREFS_PARENTAL_CONTROL, Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "FocusGuard_DPC"
        const val KAHF_GUARD_DNS_HOST = "high.kahfguard.com"
        const val GALAXY_STORE_PACKAGE = "com.sec.android.app.samsungapps"
        const val KNOX_SERVICE_PLUGIN_PACKAGE = "com.samsung.android.knox.ksp"
        const val PARENTAL_CARE_PACKAGE = "com.samsung.android.app.parentalcare"
        const val PREFS_PARENTAL_CONTROL = "focusguard_parental_control_prefs"
        const val KEY_PARENTAL_CONTROL_BLOCKED = "key_parental_control_blocked"
        const val KEY_FACTORY_RESET_BLOCKED = "key_factory_reset_blocked"
        const val KEY_ADB_DEBUGGING_BLOCKED = "key_adb_debugging_blocked"

        // Protected set: packages that MUST NEVER be hidden under any condition
        val CRITICAL_SYSTEM_PACKAGES = setOf(
            // Core Android OS & System Services
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.providers.settings",
            "com.android.keychain",

            // Webview & Google Play Services Framework
            "com.google.android.webview",
            "com.android.webview",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.vending",
            "com.google.android.modulemetadata",

            // Package Installers & Permission Controllers
            "com.google.android.packageinstaller",
            "com.android.packageinstaller",
            "com.samsung.android.packageinstaller",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
            "com.samsung.android.permissioncontroller",

            // Telephony, In-Call UI & Emergency Services (Never lock out emergency/calling)
            "com.android.phone",
            "com.android.server.telecom",
            "com.samsung.android.incallui",
            "com.android.emergency",
            "com.samsung.android.emergency",
            "com.sec.android.emergencymode.service",
            "com.samsung.android.app.telephonyui",

            // Home Screen Launchers (Samsung One UI, Pixel, AOSP) - Critical: NEVER hide Home Screen
            "com.sec.android.app.launcher",
            "com.google.android.apps.nexuslauncher",
            "com.android.launcher3",

            // Keyboards / Input Methods (Must remain available to unlock & type)
            "com.samsung.android.honeyboard",
            "com.google.android.inputmethod.latin",
            "com.touchtype.swiftkey",
            "ridmik.keyboard",
            "net.ridmik.keyboard",

            // Samsung Pass, Autofill, Biometrics, Credentials & Account components
            "com.samsung.android.samsungpass",            // Samsung Pass Framework
            "com.samsung.android.samsungpassautofill",    // Samsung Pass Autofill
            "com.samsung.android.authfw",                 // Samsung Authentication Framework
            "com.samsung.android.biometrics.app.setting", // Samsung Biometrics & Passkeys Setting
            "com.samsung.android.spay",                   // Samsung Wallet / Samsung Pay
            "com.samsung.android.spaymini",               // Samsung Pay Mini
            "com.samsung.android.mobileservice",          // Samsung Experience Service / Account Login
            "com.osp.app.signin",                         // Samsung Account Sign-in

            // Samsung essential update & firmware maintenance components
            "com.sec.android.app.samsungapps.una2",       // Samsung In-App Update Agent
            "com.wssyncmldm",                             // Samsung FOTA / OS Firmware Update
            "com.sec.android.soagent",                    // Samsung System Update Agent
            "com.samsung.android.app.omcagent",           // Samsung OMC / Carrier Update Agent
            "com.sec.android.provider.badge",             // Samsung Badge Provider
            "com.samsung.android.app.smartcapture",       // Samsung Screenshot / Smart Capture

            // Samsung Cloud, Smart Switch & Backup Services (Must NEVER be restricted)
            "com.samsung.android.scloud",                 // Samsung Cloud Core
            "com.samsung.android.scloud.backup",          // Samsung Cloud Backup
            "com.samsung.android.scloud.sync",            // Samsung Cloud Sync
            "com.samsung.android.scloud.developer",       // Samsung Cloud Dev
            "com.samsung.android.scloud.auth",            // Samsung Cloud Auth
            "com.samsung.android.shortcutbackupservice",  // Samsung Shortcut Backup Service
            "com.samsung.android.brightnessbackupservice",// Samsung Brightness Backup Service
            "com.samsung.android.visual.cloudcore",       // Samsung Cloud Core Visual
            "com.samsung.android.messageguardsync",       // Samsung Message Guard Sync
            "com.samsung.android.smartswitchassistant",   // Samsung Smart Switch Assistant
            "com.sec.android.easyMover",                  // Samsung Smart Switch
            "com.sec.android.easyMover.Agent",            // Smart Switch Agent
            "com.samsung.android.rubin.app",              // Samsung Customization Service
            "com.samsung.android.fmm",                    // Samsung Find My Mobile / SmartThings Find
            "com.android.calllogbackup",                  // Android Call Log Backup
            "com.android.wallpaperbackup",                // Android Wallpaper Backup
            "com.google.android.health.connect.backuprestore", // Health Connect Backup & Restore
            "com.google.android.syncadapters.calendar",   // Calendar Sync Adapter
            "com.google.android.apps.restore",            // Google Cloud Restore
            "com.wssyncmldm",                             // Device Sync ML DM
            "com.google.android.backuptransport",         // Google Backup Transport
            "com.android.backupconfirm",                  // Android Backup Confirm
            "com.android.sharedstoragebackup",            // Android Shared Storage Backup
            "com.samsung.android.knox.ksp",               // Knox Service Plugin

            // Accessibility / Screen Reader Services
            "com.samsung.android.accessibility.talkback",
            "com.google.android.marvin.talkback",

            // Gmail: keep available through standard and emergency enforcement
            "com.google.android.gm",

            // Storage, Media, Documents & Core System Providers (CRITICAL: NEVER HIDE)
            "com.android.externalstorage",
            "com.android.providers.media",
            "com.android.providers.media.module",
            "com.google.android.providers.media.module",
            "com.android.providers.downloads",
            "com.android.providers.downloads.ui",
            "com.android.providers.contacts",
            "com.android.providers.calendar",
            "com.android.providers.telephony",
            "com.android.providers.userdictionary",
            "com.android.storagemanager",
            "com.google.android.storagemanager",
            "com.android.documentsui",
            "com.sec.android.app.myfiles",
            "com.android.mtp",
            "com.android.bluetooth",
            "com.android.nfc",
            "com.android.networkstack",
            "com.android.networkstack.tethering",
            "com.google.android.networkstack",
            "com.google.android.networkstack.tethering",
            "com.android.cellbroadcastreceiver",
            "com.android.carrierconfig",
            "com.samsung.android.lool",
            "com.samsung.android.sm.policy",
            "com.samsung.android.sdms",
            "com.sec.android.app.soundalive",
            "com.sec.android.app.camera"
        )

        // System and preinstalled packages that MUST be hidden/suspended if found on the device
        val RESTRICTED_SYSTEM_PACKAGES = setOf(
            // Browsers
            "com.android.chrome",
            "com.sec.android.app.sbrowser",
            "com.sec.android.app.sbrowser.lite",
            "org.mozilla.firefox",
            "com.opera.browser",
            "com.opera.mini.native",
            "com.microsoft.emmx",
            "com.brave.browser",
            "com.duckduckgo.mobile.android",
            "com.google.android.browser",
            "com.mi.globalbrowser",
            "com.coloros.browser",
            "com.vivo.browser",
            "com.transsion.phoenix",

            // Google Apps & Assistants
            "com.google.android.googlequicksearchbox",
            "com.google.android.apps.googleassistant",
            "com.google.android.apps.tachyon",

            // Video & Entertainment
            "com.google.android.youtube",
            "com.google.android.apps.youtube.music",
            "com.google.android.apps.youtube.kids",
            "com.netflix.mediaclient",
            "com.amazon.avod.thirdpartyclient",

            // Social & Feeds
            "com.facebook.katana",
            "com.facebook.lite",
            "com.facebook.orca",
            "com.facebook.services",
            "com.facebook.appmanager",
            "com.facebook.system",
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.twitter.android",
            "com.snapchat.android",
            "com.reddit.frontpage",
            "com.pinterest",

            // Third-party App Store / Update Managers (non-essential) & Cloud
            "com.microsoft.appmanager",
            "com.microsoft.skydrive",
            "com.amazon.appmanager",

            // Telegram Native Apps (Study Telegram is Web K only inside Focus Guard)
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "org.thunderdog.challegram",

            // Game Hub / Launchers & Bloatware
            "com.samsung.android.game.gamehome",
            "com.samsung.android.game.gametools",
            "com.samsung.android.game.gos",
            "com.samsung.android.kidsinstaller",
            "com.samsung.android.aremoji",
            "com.samsung.android.aremojieditor",
            "com.sec.android.app.chromecustomizations"
        )

        // Emergency Allowed Packages during 1-Hour Cooling-Off (Phone, SMS, Maps, Notes, File Manager)
        val EMERGENCY_ALLOWED_PACKAGES = setOf(
            // 1. Phone
            "com.samsung.android.dialer",
            "com.samsung.android.incallui",
            "com.android.phone",
            "com.android.server.telecom",
            "com.google.android.dialer",
            "com.android.dialer",

            // 2. SMS / Messaging
            "com.samsung.android.messaging",
            "com.google.android.apps.messaging",
            "com.android.mms",

            // 3. Maps
            "com.google.android.apps.maps",

            // 4. Notes
            "com.samsung.android.app.notes",
            "com.google.android.keep",

            // 5. File Manager
            "com.sec.android.app.myfiles",
            "com.google.android.apps.nbu.files",
            "com.android.documentsui",

            // 6. Local Audio & Media Player
            "com.sec.android.app.music",

            // 7. Islamic & Quran/Hadith Spiritual Apps
            "com.tos.namajtime",                     // Muslim Bangla
            "com.muslimbangla",                      // Muslim Bangla (Alt)
            "com.ihadis.ihadis",                     // iHadis (Authentic Hadith)
            "com.hadithbd",                          // Hadith BD
            "com.greentech.quran",                   // Al Quran (Tafsir & by Word)
            "com.quran.labs.androidquran"            // Quran for Android
        )
    }

    fun isDeviceOwner(): Boolean {
        return dpm.isDeviceOwnerApp(context.packageName)
    }

    fun isDeviceAdmin(): Boolean {
        return dpm.isAdminActive(adminComponent)
    }

    fun isParentalControlAccessBlocked(): Boolean {
        return parentalControlPrefs.getBoolean(KEY_PARENTAL_CONTROL_BLOCKED, false)
    }

    fun setParentalControlAccessBlocked(blocked: Boolean): Boolean {
        if (!isDeviceOwner()) return false
        return try {
            val success = if (blocked) {
                dpm.setApplicationHidden(adminComponent, PARENTAL_CARE_PACKAGE, false)
                val failed = dpm.setPackagesSuspended(adminComponent, arrayOf(PARENTAL_CARE_PACKAGE), true)
                failed.isEmpty()
            } else {
                val failed = dpm.setPackagesSuspended(adminComponent, arrayOf(PARENTAL_CARE_PACKAGE), false)
                dpm.setApplicationHidden(adminComponent, PARENTAL_CARE_PACKAGE, false)
                failed.isEmpty()
            }
            if (success) {
                parentalControlPrefs.edit().putBoolean(KEY_PARENTAL_CONTROL_BLOCKED, blocked).apply()
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error updating parental control restriction", e)
            false
        }
    }

    /**
     * Checks if Factory Reset is blocked via DPC restriction.
     * When blocked, Android Settings displays native "Action not allowed by your admin".
     */
    fun isFactoryResetBlocked(): Boolean {
        return parentalControlPrefs.getBoolean(KEY_FACTORY_RESET_BLOCKED, false)
    }

    /**
     * Toggles Factory Reset restriction directly via DevicePolicyManager.
     * Does not use accessibility service; enforces UserManager.DISALLOW_FACTORY_RESET natively.
     */
    fun setFactoryResetBlocked(blocked: Boolean): Boolean {
        parentalControlPrefs.edit().putBoolean(KEY_FACTORY_RESET_BLOCKED, blocked).apply()
        if (!isDeviceOwner()) return false
        return try {
            if (blocked) {
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            } else {
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating factory reset restriction", e)
            false
        }
    }

    /**
     * Checks if ADB USB debugging is blocked via DPC restriction.
     */
    fun isAdbDebuggingBlocked(): Boolean {
        return parentalControlPrefs.getBoolean(KEY_ADB_DEBUGGING_BLOCKED, false)
    }

    /**
     * Toggles ADB USB debugging restriction directly via DevicePolicyManager.
     * By default false so debugging/testing on device is not interrupted.
     */
    fun setAdbDebuggingBlocked(blocked: Boolean): Boolean {
        parentalControlPrefs.edit().putBoolean(KEY_ADB_DEBUGGING_BLOCKED, blocked).apply()
        if (!isDeviceOwner()) return false
        return try {
            if (blocked) {
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
            } else {
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating ADB debugging restriction", e)
            false
        }
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

    /**
     * Immediately turns off and locks the device screen via Device Admin / Device Owner.
     * Used for instant physical cutoff when intense explicit attempt spikes are detected.
     */
    fun lockDeviceNow() {
        if (!isDeviceAdmin() && !isDeviceOwner()) return
        try {
            Log.w(TAG, "Locking device screen immediately via DevicePolicyManager...")
            dpm.lockNow()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to lock device: ${e.message}", e)
        }
    }

    /**
     * Temporarily clears DISALLOW_INSTALL_APPS and DISALLOW_INSTALL_UNKNOWN_SOURCES
     * to allow development/maintenance updates via ADB without factory resetting.
     */
    fun clearInstallRestrictionForUpdate(): Boolean {
        if (!isDeviceOwner()) return false
        return try {
            Log.i(TAG, "Temporarily clearing install restrictions for maintenance/update...")
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_APPS)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_APPS_CONTROL)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_UNINSTALL_APPS)
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear install restrictions: ${e.message}", e)
            false
        }
    }

    /**
     * Dynamically queries all installed home launcher packages.
     * Guarantees that whatever launcher the device uses (Samsung One UI Home,
     * Pixel Launcher, AOSP Launcher, etc.), it is NEVER hidden or suspended.
     */
    fun getHomeLauncherPackages(): Set<String> {
        return try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolvers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.queryIntentActivities(homeIntent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.queryIntentActivities(homeIntent, 0)
            }
            resolvers.mapNotNull { it.activityInfo?.packageName }.toSet()
        } catch (e: Exception) {
            Log.w(TAG, "Error querying home launcher packages: ${e.message}")
            emptySet()
        }
    }

    /**
     * Dynamically queries all enabled keyboard and input method packages.
     * Guarantees that Samsung Keyboard, Gboard, SwiftKey, or any other user-enabled
     * IME will NEVER be hidden or suspended, keeping typing and password input fully functional.
     */
    fun getInputMethodPackages(): Set<String> {
        return try {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            val enabledImes = imm?.enabledInputMethodList ?: emptyList()
            enabledImes.map { it.packageName }.toSet()
        } catch (e: Exception) {
            Log.w(TAG, "Error querying enabled input method packages: ${e.message}")
            emptySet()
        }
    }

    /**
     * Returns true if the package is Focus Guard itself, an essential critical system package,
     * the active/default home launcher, or an enabled keyboard input method.
     */
    fun isCriticalOrProtectedPackage(pkgName: String): Boolean {
        if (pkgName == PARENTAL_CARE_PACKAGE) {
            return false
        }
        if (pkgName == context.packageName || CRITICAL_SYSTEM_PACKAGES.contains(pkgName)) {
            return true
        }
        if (getHomeLauncherPackages().contains(pkgName)) {
            return true
        }
        if (getInputMethodPackages().contains(pkgName)) {
            return true
        }

        // Essential system services, HALs, drivers, and framework providers
        try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageManager.MATCH_UNINSTALLED_PACKAGES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_UNINSTALLED_PACKAGES
            }
            val appInfo = pm.getApplicationInfo(pkgName, flags)
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            if (isSystem && !RESTRICTED_SYSTEM_PACKAGES.contains(pkgName)) {
                return true
            }
        } catch (_: Exception) {}

        return false
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
        var isAppsControlDisallowed = false

        if (isDO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    privateDnsMode = dpm.getGlobalPrivateDnsMode(adminComponent)
                    privateDnsHost = dpm.getGlobalPrivateDnsHost(adminComponent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read Private DNS mode via DPM", e)
            }

            try {
                val sysHost = Settings.Global.getString(context.contentResolver, "private_dns_specifier")
                val sysMode = Settings.Global.getString(context.contentResolver, "private_dns_mode")
                if (privateDnsHost.isNullOrBlank() && !sysHost.isNullOrBlank()) {
                    privateDnsHost = sysHost
                }
                if (privateDnsMode == 0 && sysMode == "hostname") {
                    privateDnsMode = 3
                }
            } catch (_: Exception) {}

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
                isAppsControlDisallowed = activeRestrictions.contains(UserManager.DISALLOW_APPS_CONTROL)
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
        val isPolicyPermanentlyLocked = isDO && isAdbDebuggingBlocked &&
            isUninstallBlocked && isAppsControlDisallowed
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
            isAppsControlDisallowed = isAppsControlDisallowed,
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

            val kspRestrictions = android.os.Bundle().apply {
                val appPolicies = android.os.Bundle().apply {
                    putBoolean("enableApplicationManagementPolicies", true)
                    // On One UI 8.0/Knox 3.13, setting showGalaxyStoreApp to "Hide"
                    // conceals the Galaxy Store UI while preserving underlying Samsung update daemons
                    putString("showGalaxyStoreApp", "Hide")
                }
                putBundle("applicationManagementPolicies", appPolicies)

                // Explicitly allow backup and reset in KSP
                val deviceSettings = android.os.Bundle().apply {
                    putBoolean("enableDeviceSettingsPolicy", true)
                    putBoolean("hideSettingsBackupAndReset", false)
                }
                putBundle("deviceSettingsPolicy", deviceSettings)
            }

            dpm.setApplicationRestrictions(adminComponent, KNOX_SERVICE_PLUGIN_PACKAGE, kspRestrictions)
            Log.i(TAG, "Successfully applied Knox Service Plugin OEMConfig for Galaxy Store isolation without restricting Samsung Cloud.")
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
     * Ensures Focus Guard's Accessibility Service is enabled in Secure Settings.
     */
    fun ensureAccessibilityServiceEnabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && isDeviceOwner()) {
            try {
                dpm.setPermissionGrantState(
                    adminComponent,
                    context.packageName,
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                )
            } catch (_: Exception) {}
        }

        try {
            // Disable Circle to Search (touch_and_hold_to_search = 0)
            android.provider.Settings.Secure.putInt(
                context.contentResolver,
                "touch_and_hold_to_search",
                0
            )
            // Remove floating onscreen accessibility shortcut buttons
            android.provider.Settings.Secure.putString(
                context.contentResolver,
                "accessibility_button_targets",
                ""
            )
        } catch (_: Exception) {}

        try {
            val serviceComponent = "${context.packageName}/com.focusguard.accessibility.WhatsAppCommunityAccessibilityService"
            val shortServiceComponent = "${context.packageName}/.accessibility.WhatsAppCommunityAccessibilityService"
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""

            // Remove TextSpotlightService, AssistantMenuService, SelectToSpeakService (floating buttons)
            val filteredServices = enabledServices.split(":")
                .filter {
                    it.isNotBlank() &&
                            !it.contains("TextSpotlightService") &&
                            !it.contains("AssistantMenuService") &&
                            !it.contains("SelectToSpeakService")
                }
            val hasOurService = filteredServices.any { it == serviceComponent || it == shortServiceComponent }
            val finalServices = if (hasOurService) {
                filteredServices
            } else {
                filteredServices + serviceComponent
            }
            val updated = finalServices.joinToString(":")
            if (updated != enabledServices) {
                android.provider.Settings.Secure.putString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    updated
                )
            }
            android.provider.Settings.Secure.putInt(
                context.contentResolver,
                android.provider.Settings.Secure.ACCESSIBILITY_ENABLED,
                1
            )
            Log.i(TAG, "Focus Guard Accessibility Service enabled & floating buttons disabled via Secure Settings.")
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting accessibility service: ${e.message}")
        }
    }

    /**
     * Enforces Private DNS (DoT) to high.kahfguard.com and locks the setting.
     * 1. Temporarily clears DISALLOW_CONFIG_PRIVATE_DNS so modifications are accepted by the OS.
     * 2. Sets hostname & specifier via ContentResolver Settings.Global and DPM global settings.
     * 3. On Android 10+ (Q), calls dpm.setGlobalPrivateDnsModeSpecifiedHost.
     * 4. Re-applies DISALLOW_CONFIG_PRIVATE_DNS restriction to lock Private DNS.
     */
    fun enforcePrivateDns(): String {
        if (!isDeviceOwner()) return "Requires Device Owner"

        // 1. Temporarily clear restriction so modification is permitted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_PRIVATE_DNS)
            } catch (_: Exception) {}
        }

        // 2. Set via DPM Global Settings
        try {
            dpm.setGlobalSetting(adminComponent, "private_dns_mode", "hostname")
            dpm.setGlobalSetting(adminComponent, "private_dns_specifier", KAHF_GUARD_DNS_HOST)
        } catch (e: Exception) {
            Log.w(TAG, "Failed setGlobalSetting for Private DNS: ${e.message}")
        }

        // 3. Set via ContentResolver Settings.Global
        try {
            Settings.Global.putString(context.contentResolver, "private_dns_mode", "hostname")
            Settings.Global.putString(context.contentResolver, "private_dns_specifier", KAHF_GUARD_DNS_HOST)
        } catch (e: Exception) {
            Log.w(TAG, "Failed Settings.Global.putString for Private DNS: ${e.message}")
        }

        // 4. Set via official DevicePolicyManager API (Android 10+)
        var status = "Enforced via Global Settings ($KAHF_GUARD_DNS_HOST)"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val dnsResult = dpm.setGlobalPrivateDnsModeSpecifiedHost(adminComponent, KAHF_GUARD_DNS_HOST)
                status = when (dnsResult) {
                    DevicePolicyManager.PRIVATE_DNS_SET_NO_ERROR -> "Enforced ($KAHF_GUARD_DNS_HOST)"
                    DevicePolicyManager.PRIVATE_DNS_SET_ERROR_HOST_NOT_SERVING -> "Warning: Host not serving TLS on 853 ($KAHF_GUARD_DNS_HOST) - Enforced via Global Settings"
                    DevicePolicyManager.PRIVATE_DNS_SET_ERROR_FAILURE_SETTING -> "Error: Failure applying Private DNS setting - Enforced via Global Settings"
                    else -> "Status code: $dnsResult (Enforced via Global Settings)"
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed setGlobalPrivateDnsModeSpecifiedHost: ${e.message}")
            }

            // 5. Re-lock Private DNS configuration
            try {
                dpm.addUserRestriction(adminComponent, UserManager.DISALLOW_CONFIG_PRIVATE_DNS)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to lock private DNS restriction: ${e.message}")
            }
        }

        return status
    }

    /**
     * Enforces the complete Focus Guard policy suite.
     * Must be run when DO is active.
     */
    fun enforceAllPolicies(
        customAllowed: Set<String>,
        blockNewAppInstalls: Boolean = false
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

        // 4.5. Ensure Device Input & Safety Accessibility Service is active
        ensureAccessibilityServiceEnabled()

        // 5. Enforce System User Restrictions
        val standardRestrictions = mutableListOf(
            UserManager.DISALLOW_CONFIG_VPN,
            UserManager.DISALLOW_NETWORK_RESET,
            UserManager.DISALLOW_SAFE_BOOT,
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
            UserManager.DISALLOW_ADD_USER,
            UserManager.DISALLOW_CONFIG_DATE_TIME
        )

        // Enforce DISALLOW_FACTORY_RESET if factory reset is blocked via DPC
        if (isFactoryResetBlocked()) {
            standardRestrictions.add(UserManager.DISALLOW_FACTORY_RESET)
        } else {
            try {
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_FACTORY_RESET)
            } catch (_: Exception) {}
        }

        // Enforce DISALLOW_INSTALL_APPS when blockNewAppInstalls is enabled
        if (blockNewAppInstalls) {
            standardRestrictions.add(UserManager.DISALLOW_INSTALL_APPS)
        } else {
            try {
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_INSTALL_APPS)
            } catch (_: Exception) {}
        }

        // CRITICAL: Ensure DISALLOW_APPS_CONTROL is NOT set system-wide!
        // Clearing DISALLOW_APPS_CONTROL allows user to disable or force-stop unwanted bloatware apps in Settings without "Not allowed by admin".
        try {
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_APPS_CONTROL)
        } catch (_: Exception) {}

        // CRITICAL: Ensure DISALLOW_UNINSTALL_APPS is NOT set system-wide!
        // Focus Guard self-protection is enforced specifically via setUninstallBlocked(context.packageName, true).
        // Clearing DISALLOW_UNINSTALL_APPS ensures that unapproved or unwanted apps can be uninstalled.
        try {
            dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_UNINSTALL_APPS)
        } catch (_: Exception) {}

        // Enforce DISALLOW_DEBUGGING_FEATURES only when ADB debugging block is explicitly enabled
        // Default is false so developer/user can continue debugging/testing on device safely!
        if (isAdbDebuggingBlocked()) {
            standardRestrictions.add(UserManager.DISALLOW_DEBUGGING_FEATURES)
        } else {
            try {
                dpm.clearUserRestriction(adminComponent, UserManager.DISALLOW_DEBUGGING_FEATURES)
            } catch (_: Exception) {}
        }

        for (restriction in standardRestrictions) {
            try {
                dpm.addUserRestriction(adminComponent, restriction)
                appliedRestrictions.add(restriction)
            } catch (e: Exception) {
                errors.add("Failed to apply restriction $restriction: ${e.message}")
            }
        }

        // 6. Configure Private DNS to high.kahfguard.com (DNS-over-TLS) and lock configuration
        val dnsStatus = enforcePrivateDns()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appliedRestrictions.add(UserManager.DISALLOW_CONFIG_PRIVATE_DNS)
        }

        // 6.5. Controlled Google App Policy (Discover Feed Off, Search Widget Off, Circle to Search & Lens Allowed)
        disableGoogleDiscoverFeed()
        disableGoogleSearchWidgetComponents()

        // 6.6. Register RestrictedAuthWebViewActivity as Persistent Preferred Web Handler for In-App Logins / OAuth
        try {
            val filter = android.content.IntentFilter(android.content.Intent.ACTION_VIEW).apply {
                addCategory(android.content.Intent.CATEGORY_DEFAULT)
                addCategory(android.content.Intent.CATEGORY_BROWSABLE)
                addDataScheme("http")
                addDataScheme("https")
            }
            val targetActivity = ComponentName(context, "com.focusguard.study.ui.RestrictedAuthWebViewActivity")
            dpm.addPersistentPreferredActivity(adminComponent, filter, targetActivity)
            Log.i(TAG, "Registered RestrictedAuthWebViewActivity as persistent preferred web handler for auth/logins.")
        } catch (e: Exception) {
            Log.w(TAG, "Notice setting persistent preferred activity: ${e.message}")
        }

        // 7. Enforce Application Allowlist / Blocklist on all installed packages
        val pm = context.packageManager
        val packageFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            PackageManager.GET_META_DATA or PackageManager.MATCH_UNINSTALLED_PACKAGES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_META_DATA or PackageManager.GET_UNINSTALLED_PACKAGES
        }
        val installedPackages = pm.getInstalledPackages(packageFlags)
        val homeLaunchers = getHomeLauncherPackages()
        val inputMethods = getInputMethodPackages()

        for (pkgInfo in installedPackages) {
            val pkgName = pkgInfo.packageName

            // Special handling for Samsung Parental Care (Parental Controls in Settings)
            if (pkgName == PARENTAL_CARE_PACKAGE) {
                if (isParentalControlAccessBlocked()) {
                    try {
                        dpm.setApplicationHidden(adminComponent, pkgName, false)
                        val failed = dpm.setPackagesSuspended(adminComponent, arrayOf(pkgName), true)
                        if (failed.isEmpty()) suspendedCount++
                    } catch (_: Exception) {}
                } else {
                    try {
                        dpm.setPackagesSuspended(adminComponent, arrayOf(pkgName), false)
                        dpm.setApplicationHidden(adminComponent, pkgName, false)
                    } catch (_: Exception) {}
                }
                continue
            }

            // Safety guard: Never touch Focus Guard itself, critical system services, launchers, keyboards, or protected system infrastructure
            if (isCriticalOrProtectedPackage(pkgName)) {
                try {
                    // Ensure critical packages are unhidden and unsuspended
                    dpm.setApplicationHidden(adminComponent, pkgName, false)
                    dpm.setPackagesSuspended(adminComponent, arrayOf(pkgName), false)
                    dpm.setUninstallBlocked(adminComponent, pkgName, pkgName == context.packageName)
                    if (pkgName == context.packageName) protectedCount++
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

            // If it is an explicitly restricted system app (Chrome, YouTube, etc.)
            if (RESTRICTED_SYSTEM_PACKAGES.contains(pkgName)) {
                try {
                    dpm.setUninstallBlocked(adminComponent, pkgName, false)
                    dpm.setApplicationHidden(adminComponent, pkgName, true)
                    dpm.setPackagesSuspended(adminComponent, arrayOf(pkgName), true)
                    hiddenCount++
                } catch (e: Exception) {
                    try {
                        dpm.setPackagesSuspended(adminComponent, arrayOf(pkgName), true)
                        suspendedCount++
                    } catch (e2: Exception) {
                        errors.add("Failed to restrict system package $pkgName: ${e2.message}")
                    }
                }
                continue
            }

            // Third-party (user-installed) applications
            val isApproved = approvedAppRegistry.isApproved(pkgName)
            val isAllowed = repository.isExplicitlyAllowed(pkgName, customAllowed)
            val isExplicitlyBlocked = repository.isExplicitlyBlocked(pkgName)
            val isEssential = repository.isEssential(pkgName)

            // Determine if app should be accessible
            val shouldAllow = (isApproved || isAllowed || isEssential) && !isExplicitlyBlocked

            if (shouldAllow) {
                // Unhide and unsuspend if previously restricted
                try {
                    val isHidden = dpm.isApplicationHidden(adminComponent, pkgName)
                    if (isHidden) {
                        dpm.setApplicationHidden(adminComponent, pkgName, false)
                    }
                    dpm.setPackagesSuspended(adminComponent, arrayOf(pkgName), false)
                    dpm.setUninstallBlocked(adminComponent, pkgName, false)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not unhide allowed app $pkgName: ${e.message}")
                }
            } else {
                // Third-party app is not allowed:
                // Plan 1: Keep visible on launcher and unblock uninstall so the user can manually uninstall it.
                // Suspend the package so it cannot open or run in background.
                try {
                    dpm.setUninstallBlocked(adminComponent, pkgName, false)
                } catch (_: Exception) {}
                try {
                    dpm.setApplicationHidden(adminComponent, pkgName, false)
                } catch (_: Exception) {}
                try {
                    val suspendResult = dpm.setPackagesSuspended(adminComponent, arrayOf(pkgName), true)
                    if (suspendResult.isEmpty()) {
                        suspendedCount++
                    } else {
                        errors.add("Could not suspend $pkgName")
                    }
                } catch (e: Exception) {
                    errors.add("Failed to suspend $pkgName: ${e.message}")
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
     * Enforces the 1-hour Emergency Restricted Mode when 10/10 explicit detections occur.
     * In this mode, ONLY Phone, SMS, Maps, Notes, File Manager, and critical system framework
     * services are permitted. All other non-essential/AI/distraction apps are suspended.
     * When disabled, reverts cleanly to the standard allowlist policy.
     */
    fun enforceEmergencyRestrictedPolicy(enabled: Boolean, customAllowed: Set<String> = emptySet()) {
        if (!isDeviceOwner()) return
        Log.i(TAG, "enforceEmergencyRestrictedPolicy(enabled=$enabled)")

        try {
            val pm = context.packageManager
            // Package metadata is not needed for suspension decisions. Avoid
            // materializing it for every Samsung/system package on lockout resume.
            val packageFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PackageManager.MATCH_UNINSTALLED_PACKAGES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_UNINSTALLED_PACKAGES
            }
            val installedPackages = pm.getInstalledPackages(packageFlags)

            if (enabled) {
                val toSuspend = mutableListOf<String>()
                val toUnsuspend = mutableListOf<String>()
                val homeLaunchers = getHomeLauncherPackages()
                val inputMethods = getInputMethodPackages()

                for (pkgInfo in installedPackages) {
                    val pkgName = pkgInfo.packageName

                    // 1. Always unhide & unsuspend Focus Guard itself, critical system packages, launchers, keyboards and system infrastructure
                    if (isCriticalOrProtectedPackage(pkgName)) {
                        toUnsuspend.add(pkgName)
                        try {
                            dpm.setApplicationHidden(adminComponent, pkgName, false)
                        } catch (_: Exception) {}
                        continue
                    }

                    // 2. Emergency Allowed Packages: Phone, SMS, Maps, Notes, File Manager
                    if (EMERGENCY_ALLOWED_PACKAGES.contains(pkgName)) {
                        toUnsuspend.add(pkgName)
                        try {
                            dpm.setApplicationHidden(adminComponent, pkgName, false)
                        } catch (_: Exception) {}
                        continue
                    }

                    // 3. Samsung Galaxy Store update agent isolation
                    if (pkgName == GALAXY_STORE_PACKAGE) {
                        continue
                    }

                    // 4. All other installed applications must be suspended for 1 hour
                    toSuspend.add(pkgName)
                }

                if (toUnsuspend.isNotEmpty()) {
                    try {
                        dpm.setPackagesSuspended(adminComponent, toUnsuspend.toTypedArray(), false)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error unsuspending emergency packages: ${e.message}")
                    }
                }

                if (toSuspend.isNotEmpty()) {
                    try {
                        dpm.setPackagesSuspended(adminComponent, toSuspend.toTypedArray(), true)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error suspending non-emergency packages: ${e.message}")
                    }
                }
                Log.w(TAG, "Emergency Restricted Mode ENFORCED: ${toUnsuspend.size} emergency packages allowed, ${toSuspend.size} packages suspended.")
            } else {
                // Revert back to normal policy
                enforceAllPolicies(customAllowed)
                Log.i(TAG, "Emergency Restricted Mode REVERTED: Standard policy restored.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply emergency restricted policy: ${e.message}", e)
        }
    }

    /**
     * Enforces the Google Discover Feed to be turned off on Samsung One UI Launcher.
     * Setting page_minus_one_type = 0 completely removes the -1 Google Discover screen.
     */
    fun disableGoogleDiscoverFeed() {
        try {
            android.provider.Settings.System.putInt(context.contentResolver, "page_minus_one_type", 0)
            Log.i(TAG, "Enforced Samsung One UI: page_minus_one_type = 0 (Discover Feed Off)")
        } catch (e: Exception) {
            Log.d(TAG, "Notice setting page_minus_one_type: ${e.message}")
        }
    }

    /**
     * Disables Google Search Widget Provider components in com.google.android.googlequicksearchbox
     * so that the home screen search widget is disabled.
     */
    fun disableGoogleSearchWidgetComponents() {
        val widgetProviders = listOf(
            "com.google.android.googlequicksearchbox.SearchWidgetProvider",
            "com.google.android.apps.gsa.staticplugins.widget.SearchWidgetProvider",
            "com.google.android.apps.gsa.staticplugins.widget.GoogleAppWidgetProvider",
            "com.google.android.apps.gsa.widget.SearchWidgetProvider"
        )
        val pm = context.packageManager
        for (provider in widgetProviders) {
            try {
                val component = ComponentName("com.google.android.googlequicksearchbox", provider)
                pm.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                Log.i(TAG, "Disabled Google Search Widget component: $provider")
            } catch (e: Exception) {
                Log.d(TAG, "Notice setting widget component state: ${e.message}")
            }
        }
    }

    /**
     * Event-driven evaluation when a package is added or replaced.
     * Uses cryptographic baseline validation to permit updates to pre-existing apps
     * while intercepting unauthorized new apps and keeping blocked preinstalled apps hidden.
     */
    fun evaluatePackage(packageName: String, customAllowed: Set<String>) {
        if (!isDeviceOwner()) return
        if (packageName == PARENTAL_CARE_PACKAGE) {
            try {
                if (isParentalControlAccessBlocked()) {
                    dpm.setApplicationHidden(adminComponent, packageName, false)
                    dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), true)
                } else {
                    dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), false)
                    dpm.setApplicationHidden(adminComponent, packageName, false)
                }
            } catch (_: Exception) {}
            return
        }
        if (packageName == context.packageName ||
            CRITICAL_SYSTEM_PACKAGES.contains(packageName) ||
            packageName == GALAXY_STORE_PACKAGE ||
            isCriticalOrProtectedPackage(packageName)
        ) return

        val isApproved = approvedAppRegistry.isApproved(packageName)
        val isAllowed = repository.isExplicitlyAllowed(packageName, customAllowed)
        val isExplicitlyBlocked = repository.isExplicitlyBlocked(packageName)
        val isEssential = repository.isEssential(packageName)

        val shouldAllow = (isApproved || isAllowed || isEssential) && !isExplicitlyBlocked

        // 1. If package is approved, allowed or essential: permit install, update or reinstallation
        if (shouldAllow) {
            val certSha256 = baselineManager.getInstalledCertificateSha256(packageName)
            if (isApproved) {
                approvedAppRegistry.markInstalledState(packageName, isInstalled = true, certSha256 = certSha256)
            }
            Log.i(TAG, "Approved/allowed application $packageName verified (install/update permitted).")
            try {
                dpm.setApplicationHidden(adminComponent, packageName, false)
                dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), false)
                dpm.setUninstallBlocked(adminComponent, packageName, false)
            } catch (e: Exception) {
                Log.w(TAG, "Error ensuring unhidden state for allowed package $packageName: ${e.message}")
            }
            return
        }

        // 2. If package is NOT allowed, check if this is an update to an unapproved baseline package
        // (e.g. preinstalled Chrome, YouTube, or system bloatware updating in background).
        // It must NOT be unhidden! It must remain strictly hidden and suspended!
        if (baselineManager.isLegitimateBaselineUpdate(packageName)) {
            Log.i(TAG, "Update to unapproved baseline package $packageName detected. Keeping hidden and suspended.")
            try {
                dpm.setApplicationHidden(adminComponent, packageName, true)
                dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), true)
            } catch (e: Exception) {
                Log.w(TAG, "Error keeping unapproved baseline package hidden: ${e.message}")
            }
            return
        }

        // 3. Unauthorized new application:
        // Plan 1: Suspend immediately so it cannot open or run, while keeping it visible on launcher
        // and uninstall-unblocked so the user can manually uninstall it from Home Screen or Settings.
        Log.w(TAG, "UNAUTHORIZED APPLICATION DETECTED: $packageName! Immediate suspension initiated (manual uninstall allowed).")
        try {
            dpm.setUninstallBlocked(adminComponent, packageName, false)
        } catch (_: Exception) {}

        try {
            dpm.setApplicationHidden(adminComponent, packageName, false)
        } catch (_: Exception) {}

        try {
            dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), true)
            Log.i(TAG, "Suspended unauthorized package $packageName successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to suspend unauthorized package $packageName", e)
        }

        // Show immediate warning Toast
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                val appLabel = try {
                    val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                    context.packageManager.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) { packageName }

                android.widget.Toast.makeText(
                    context,
                    "⚠️ Focus Guard: '$appLabel' অনুমোদিত নয় এবং লক করা হয়েছে। আপনি চাইলে এটি আনইনস্টল করে দিতে পারেন।",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } catch (_: Exception) {}
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

    /**
     * Fast in-memory check to determine if a package is allowed without any blocking I/O.
     */
    fun isPackageAllowed(packageName: String): Boolean {
        if (packageName.isBlank()) return true
        if (packageName == PARENTAL_CARE_PACKAGE) {
            return !isParentalControlAccessBlocked()
        }
        if (packageName == context.packageName ||
            CRITICAL_SYSTEM_PACKAGES.contains(packageName) ||
            packageName == GALAXY_STORE_PACKAGE ||
            isCriticalOrProtectedPackage(packageName)
        ) {
            return true
        }

        if (repository.isExplicitlyBlocked(packageName) || RESTRICTED_SYSTEM_PACKAGES.contains(packageName)) {
            return false
        }

        if (approvedAppRegistry.isApproved(packageName) ||
            repository.isPredefinedAllowed(packageName) ||
            repository.isCustomAllowedCached(packageName) ||
            repository.isEssential(packageName)
        ) {
            return true
        }

        return false
    }

    /**
     * Instantly suspends an unauthorized package in < 2ms without waiting for
     * coroutines, DataStore disk reads, or cryptographic hashing.
     */
    fun suspendImmediatelyIfUnauthorized(packageName: String): Boolean {
        if (!isDeviceOwner() || packageName.isBlank()) return false
        if (isPackageAllowed(packageName)) return false

        return try {
            val isRestrictedSystem = RESTRICTED_SYSTEM_PACKAGES.contains(packageName)
            if (isRestrictedSystem) {
                try {
                    dpm.setApplicationHidden(adminComponent, packageName, true)
                } catch (_: Exception) {}
            } else {
                try {
                    dpm.setUninstallBlocked(adminComponent, packageName, false)
                } catch (_: Exception) {}
                try {
                    dpm.setApplicationHidden(adminComponent, packageName, false)
                } catch (_: Exception) {}
            }

            dpm.setPackagesSuspended(adminComponent, arrayOf(packageName), true)
            Log.i(TAG, "FAST SUSPEND: Package $packageName suspended instantly (restrictedSystem=$isRestrictedSystem).")
            true
        } catch (e: Exception) {
            Log.e(TAG, "FAST SUSPEND: Failed to suspend package $packageName", e)
            false
        }
    }
}
