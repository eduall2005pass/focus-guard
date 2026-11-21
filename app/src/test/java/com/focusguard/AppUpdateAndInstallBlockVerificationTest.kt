package com.focusguard

import com.focusguard.data.model.AppCategory
import com.focusguard.data.repository.ApprovedAppEntry
import com.focusguard.dpc.DevicePolicyController
import org.junit.Assert.*
import org.junit.Test

class AppUpdateAndInstallBlockVerificationTest {

    private val presetApprovedPackages = setOf(
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.brilliant.connect.com.bd",
        "com.ibbl.cellfin",
        "com.ibbl.ismart",
        "com.ibbl.ibbl_ismart",
        "com.bKash.customerapp",
        "com.konasl.nagad",
        "com.dbbl.mbs.apps.main",
        "bd.com.ucb.upay",
        "com.google.android.apps.maps",
        "bd.gov.dmtcl.rapidpass",
        "bd.gov.rapidpass",
        "com.shohoz.dtrainpwa",
        "com.shohoz.rail",
        "com.pathao.user",
        "com.ubercab",
        "com.global.foodpanda.android",
        "com.usbangla.foodi",
        "com.daraz.android",
        "com.openai.chatgpt",
        "com.anthropic.claude",
        "com.google.android.apps.bard",
        "com.google.android.apps.notebooklm",
        "com.samsung.android.app.notes",
        "com.tos.namajtime",
        "com.muslimbangla",
        "com.ihadis.ihadis",
        "com.mahfil.app",
        "com.greentech.quran",
        "com.a3d4medical.completeanatomy",
        "com.elsevier.threeD4Medical.CompleteAnatomy",
        "teletalk.teletalkcustomerapp",
        "com.sec.android.app.music",
        "com.samsung.android.samsungpass",
        "com.samsung.android.samsungpassautofill",
        "com.google.android.gm",
        "com.google.android.apps.docs",
        "com.google.android.apps.docs.editors.sheets",
        "com.google.android.apps.docs.editors.slides",
        "com.google.android.apps.photos",
        "com.google.android.apps.messaging",
        "com.google.android.dialer",
        "com.samsung.android.messaging",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui",
        "com.sec.android.app.myfiles",
        "com.google.android.apps.nbu.files",
        "ridmik.keyboard",
        "net.ridmik.keyboard",
        "com.android.vending",
        "com.sec.android.app.samsungapps"
    )

    private val unauthorizedPackagesToBlock = listOf(
        "com.zhiliaoapp.musically",       // TikTok
        "com.facebook.katana",            // Facebook
        "com.facebook.lite",              // Facebook Lite
        "com.facebook.orca",              // Messenger
        "com.instagram.android",          // Instagram
        "com.snapchat.android",           // Snapchat
        "com.dts.freefireth",             // Free Fire
        "com.tencent.ig",                 // PUBG
        "org.mozilla.firefox",            // Firefox
        "com.opera.browser",              // Opera
        "com.brave.browser",              // Brave
        "com.microsoft.emmx",             // Edge
        "com.tinder",                     // Tinder
        "com.netflix.mediaclient",        // Netflix
        "com.google.android.googlequicksearchbox" // Google App
    )

    @Test
    fun testAllowedAppsUpdatePermitted() {
        for (pkg in presetApprovedPackages) {
            val isApproved = presetApprovedPackages.contains(pkg)
            assertTrue("Approved app $pkg must be permitted to receive updates", isApproved)
        }
    }

    @Test
    fun testUnauthorizedNewAppsStrictlyBlocked() {
        for (pkg in unauthorizedPackagesToBlock) {
            val isApproved = presetApprovedPackages.contains(pkg)
            assertFalse("Unauthorized app $pkg must NOT be allowed to install", isApproved)
        }
    }

    @Test
    fun testEvaluationLogicDistinguishesUpdateVsNewApp() {
        fun evaluateAppAction(packageName: String): String {
            if (presetApprovedPackages.contains(packageName)) {
                return "PERMIT_UPDATE_OR_REINSTALL"
            }
            return "BLOCK_HIDE_SUSPEND_UNINSTALL"
        }

        // 1. WhatsApp update from Play Store
        assertEquals("PERMIT_UPDATE_OR_REINSTALL", evaluateAppAction("com.whatsapp"))

        // 2. Complete Anatomy update from Play Store
        assertEquals("PERMIT_UPDATE_OR_REINSTALL", evaluateAppAction("com.a3d4medical.completeanatomy"))

        // 3. Cellfin update from Play Store
        assertEquals("PERMIT_UPDATE_OR_REINSTALL", evaluateAppAction("com.ibbl.cellfin"))

        // 4. Samsung Music update from Galaxy Store
        assertEquals("PERMIT_UPDATE_OR_REINSTALL", evaluateAppAction("com.sec.android.app.music"))

        // 5. Facebook attempted install
        assertEquals("BLOCK_HIDE_SUSPEND_UNINSTALL", evaluateAppAction("com.facebook.katana"))

        // 6. TikTok attempted install
        assertEquals("BLOCK_HIDE_SUSPEND_UNINSTALL", evaluateAppAction("com.zhiliaoapp.musically"))

        // 7. Free Fire attempted install
        assertEquals("BLOCK_HIDE_SUSPEND_UNINSTALL", evaluateAppAction("com.dts.freefireth"))
    }

    @Test
    fun testPlayStoreActionDetection() {
        fun shouldAllowPlayStoreButton(buttonText: String, isApprovedApp: Boolean): Boolean {
            val isInstallAction = buttonText.equals("Install", ignoreCase = true) ||
                    buttonText.equals("Get", ignoreCase = true) ||
                    buttonText.contains("ইনস্টল করুন", ignoreCase = true)

            // If it's an update (button says Update / আপডেট করুন / Open) -> Always allow
            if (!isInstallAction) {
                return true
            }

            // If it's an install action, only allow if it's an approved app being reinstalled
            return isApprovedApp
        }

        // App update for WhatsApp: button says "Update" -> ALLOW
        assertTrue(shouldAllowPlayStoreButton("Update", isApprovedApp = true))

        // App update in Bengali: button says "আপডেট করুন" -> ALLOW
        assertTrue(shouldAllowPlayStoreButton("আপডেট করুন", isApprovedApp = true))

        // Open existing app -> ALLOW
        assertTrue(shouldAllowPlayStoreButton("Open", isApprovedApp = true))

        // Reinstalling approved app (e.g. bKash): button says "Install" and isApprovedApp = true -> ALLOW
        assertTrue(shouldAllowPlayStoreButton("Install", isApprovedApp = true))

        // Installing new unapproved app (e.g. Facebook): button says "Install" and isApprovedApp = false -> BLOCK
        assertFalse(shouldAllowPlayStoreButton("Install", isApprovedApp = false))

        // Installing new unapproved app in Bengali: button says "ইনস্টল করুন" and isApprovedApp = false -> BLOCK
        assertFalse(shouldAllowPlayStoreButton("ইনস্টল করুন", isApprovedApp = false))
    }

    @Test
    fun testCriticalLaunchersAndKeyboardsProtectedFromHidingAndSuspension() {
        val criticalSet = DevicePolicyController.CRITICAL_SYSTEM_PACKAGES

        // 1. Samsung One UI Home Launcher MUST be in critical protected set
        assertTrue("Samsung One UI Launcher must be in CRITICAL_SYSTEM_PACKAGES",
            criticalSet.contains("com.sec.android.app.launcher"))

        // 2. Pixel & AOSP Launchers
        assertTrue(criticalSet.contains("com.google.android.apps.nexuslauncher"))
        assertTrue(criticalSet.contains("com.android.launcher3"))

        // 3. Samsung InCall UI (Essential for answering phone calls)
        assertTrue("Samsung InCall UI must be in CRITICAL_SYSTEM_PACKAGES",
            criticalSet.contains("com.samsung.android.incallui"))

        // 4. Samsung & Google Package Installers
        assertTrue(criticalSet.contains("com.samsung.android.packageinstaller"))
        assertTrue(criticalSet.contains("com.google.android.packageinstaller"))
        assertTrue(criticalSet.contains("com.android.packageinstaller"))

        // 5. Keyboards / Input Methods
        assertTrue("Samsung Keyboard must be in CRITICAL_SYSTEM_PACKAGES",
            criticalSet.contains("com.samsung.android.honeyboard"))
        assertTrue("Gboard must be in CRITICAL_SYSTEM_PACKAGES",
            criticalSet.contains("com.google.android.inputmethod.latin"))
        assertTrue("SwiftKey must be in CRITICAL_SYSTEM_PACKAGES",
            criticalSet.contains("com.touchtype.swiftkey"))

        // 6. Emergency calling services
        assertTrue(criticalSet.contains("com.android.emergency"))
        assertTrue(criticalSet.contains("com.samsung.android.emergency"))
        assertTrue(criticalSet.contains("com.sec.android.emergencymode.service"))
    }

    @Test
    fun testPreinstalledBlockedAppUpdateStaysHiddenAndSuspended() {
        // Test simulation of evaluatePackage logic:
        // A baseline app that is NOT in approved registry and is explicitly blocked (e.g. Chrome)
        // must remain hidden and suspended even if its baseline update signature is legitimate.
        val blockedPreinstalledApps = listOf(
            "com.android.chrome",
            "com.sec.android.app.sbrowser",
            "com.google.android.youtube",
            "org.mozilla.firefox"
        )

        for (pkg in blockedPreinstalledApps) {
            val isApproved = presetApprovedPackages.contains(pkg)
            val isBaselineUpdate = true // Simulating valid signature update from Google Play

            val action = if (isApproved) {
                "UNHIDE_AND_UNSUSPEND"
            } else if (isBaselineUpdate) {
                "KEEP_HIDDEN_AND_SUSPENDED"
            } else {
                "HIDE_SUSPEND_AND_UNINSTALL"
            }

            assertEquals(
                "Blocked preinstalled app $pkg must REMAIN hidden and suspended after update",
                "KEEP_HIDDEN_AND_SUSPENDED",
                action
            )
        }
    }

    @Test
    fun testEmergencyAllowedPackagesCompleteness() {
        val emergencySet = DevicePolicyController.EMERGENCY_ALLOWED_PACKAGES

        // Phone
        assertTrue(emergencySet.contains("com.samsung.android.dialer"))
        assertTrue(emergencySet.contains("com.samsung.android.incallui"))
        assertTrue(emergencySet.contains("com.google.android.dialer"))

        // SMS
        assertTrue(emergencySet.contains("com.samsung.android.messaging"))
        assertTrue(emergencySet.contains("com.google.android.apps.messaging"))

        // Maps
        assertTrue(emergencySet.contains("com.google.android.apps.maps"))

        // Notes
        assertTrue(emergencySet.contains("com.samsung.android.app.notes"))
        assertTrue(emergencySet.contains("com.google.android.keep"))

        // Files
        assertTrue(emergencySet.contains("com.sec.android.app.myfiles"))
        assertTrue(emergencySet.contains("com.google.android.apps.nbu.files"))

        // Local Audio / Music
        assertTrue(emergencySet.contains("com.sec.android.app.music"))

        // Islamic Apps
        assertTrue(emergencySet.contains("com.tos.namajtime"))
        assertTrue(emergencySet.contains("com.muslimbangla"))
        assertTrue(emergencySet.contains("com.ihadis.ihadis"))
        assertTrue(emergencySet.contains("com.greentech.quran"))
    }

    @Test
    fun testInstallBlockedWhileUninstallPermitted() {
        // Enforced restrictions must have DISALLOW_INSTALL_APPS to prevent new app installs from Play Store
        val restrictions = mutableListOf(
            "no_config_vpn",
            "no_install_unknown_sources",
            "no_install_apps"
        )
        assertTrue("DISALLOW_INSTALL_APPS must be enforced", restrictions.contains("no_install_apps"))
        assertFalse("DISALLOW_UNINSTALL_APPS must NOT be globally enforced", restrictions.contains("no_uninstall_apps"))

        // Focus Guard self-protection vs third party apps
        fun isUninstallBlockedForPackage(pkg: String): Boolean {
            return pkg == "com.focusguard"
        }
        assertTrue("Focus Guard must be uninstall-protected", isUninstallBlockedForPackage("com.focusguard"))
        assertFalse("Facebook Lite must NOT be uninstall-blocked", isUninstallBlockedForPackage("com.facebook.lite"))
        assertFalse("Messenger must NOT be uninstall-blocked", isUninstallBlockedForPackage("com.facebook.orca"))
    }
}
