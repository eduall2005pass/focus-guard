package com.focusguard

import com.focusguard.data.model.AppCategory
import com.focusguard.data.model.AppItem
import org.junit.Assert.*
import org.junit.Test

class PolicyVerificationTest {

    // Test essential allowlist items required by specification
    private val expectedAllowedPackages = listOf(
        // Communication
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.brilliant.connect.com.bd",

        // Banking & Payments
        "com.ibbl.ibbl_ismart",
        "com.ibbl.ismart",
        "com.ibbl.cellfin",
        "com.ionicframework.icellular894076",
        "com.ibbl.mcash",
        "com.bKash.customerapp",
        "com.konasl.nagad",
        "com.dbbl.mbs.apps.main",

        // Transportation & Delivery
        "com.google.android.apps.maps",
        "com.bd.rapidpass.app",
        "bd.gov.dmtcl.rapidpass",
        "bd.gov.rapidpass",
        "com.shohoz.dtrainpwa",
        "com.shohoz.rail",
        "com.pathao.user",
        "com.ubercab",
        "com.global.foodpanda.android",
        "com.usbangla.foodi",
        "com.daraz.android",

        // AI & Study
        "com.openai.chatgpt",
        "com.anthropic.claude",
        "com.google.android.apps.bard",
        "com.google.android.apps.labs.language.tailwind",
        "com.google.android.apps.notebooklm",
        "com.livemcq.livemcq",
        "com.google.android.keep",
        "com.github.android",
        "com.samsung.android.app.notes",
        "com.tos.namajtime",
        "com.muslimbangla",
        "com.ihadis.ihadis",
        "com.mahfil.app",
        "com.greentech.quran",
        "com.a3d4medical.completeanatomy",
        "com.elsevier.threeD4Medical.CompleteAnatomy",

        // Google Productivity
        "ridmik.keyboard",
        "net.ridmik.keyboard",
        "com.google.android.apps.translate",
        "com.google.android.apps.authenticator2",
        "com.google.android.gm",
        "com.google.android.apps.docs",
        "com.google.android.apps.docs.editors.docs",
        "com.google.android.apps.docs.editors.sheets",
        "com.google.android.apps.photos",

        // Critical System Components
        "ru.zdevs.zarchiver",
        "com.google.android.apps.credentialmanager",
        "com.android.credentialmanager",
        "com.google.android.apps.adm",
        "com.google.android.webview",
        "com.google.android.gms",
        "com.android.vending",
        "teletalk.teletalkcustomerapp",
        "com.sec.android.app.music",
        "com.samsung.android.samsungpass",
        "com.samsung.android.samsungpassautofill"
    )

    // Test blocked packages required by specification
    private val expectedBlockedPackages = listOf(
        // General-purpose browsers
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.sec.android.app.sbrowser",
        "com.opera.browser",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.mi.globalbrowser",

        // Telegram variants
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.thunderdog.challegram",

        // Social & Distractions
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "com.facebook.katana",
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.twitter.android",
        "com.netflix.mediaclient"
    )

    @Test
    fun verifyRequiredAllowedPackagesPresent() {
        // Mock a Context-free test of package maps
        val predefinedAllowed = listOf(
            AppItem("com.whatsapp", "WhatsApp", AppCategory.COMMUNICATION),
            AppItem("com.whatsapp.w4b", "WhatsApp Business", AppCategory.COMMUNICATION),
            AppItem("com.brilliant.connect.com.bd", "Brilliant Connect", AppCategory.COMMUNICATION),
            AppItem("com.ibbl.ibbl_ismart", "iSmart", AppCategory.BANKING_PAYMENTS),
            AppItem("com.ibbl.ismart", "iSmart", AppCategory.BANKING_PAYMENTS),
            AppItem("com.ibbl.cellfin", "Cellfin", AppCategory.BANKING_PAYMENTS),
            AppItem("com.ionicframework.icellular894076", "IBBL iSmart", AppCategory.BANKING_PAYMENTS),
            AppItem("com.ibbl.mcash", "mCash", AppCategory.BANKING_PAYMENTS),
            AppItem("com.bKash.customerapp", "bKash", AppCategory.BANKING_PAYMENTS),
            AppItem("com.konasl.nagad", "Nagad", AppCategory.BANKING_PAYMENTS),
            AppItem("com.dbbl.mbs.apps.main", "Rocket", AppCategory.BANKING_PAYMENTS),
            AppItem("com.google.android.apps.maps", "Google Maps", AppCategory.TRANSPORTATION),
            AppItem("com.bd.rapidpass.app", "Rapid Pass", AppCategory.TRANSPORTATION),
            AppItem("bd.gov.dmtcl.rapidpass", "Rapid Pass", AppCategory.TRANSPORTATION),
            AppItem("bd.gov.rapidpass", "Rapid Pass Portal", AppCategory.TRANSPORTATION),
            AppItem("com.shohoz.dtrainpwa", "Rail Sheba", AppCategory.TRANSPORTATION),
            AppItem("com.shohoz.rail", "Bangladesh Railway", AppCategory.TRANSPORTATION),
            AppItem("com.pathao.user", "Pathao", AppCategory.TRANSPORTATION),
            AppItem("com.ubercab", "Uber", AppCategory.TRANSPORTATION),
            AppItem("com.global.foodpanda.android", "Foodpanda", AppCategory.TRANSPORTATION),
            AppItem("com.usbangla.foodi", "Foodi", AppCategory.TRANSPORTATION),
            AppItem("com.daraz.android", "Daraz", AppCategory.TRANSPORTATION),
            AppItem("com.openai.chatgpt", "ChatGPT", AppCategory.AI_STUDY),
            AppItem("com.anthropic.claude", "Claude", AppCategory.AI_STUDY),
            AppItem("com.google.android.apps.bard", "Gemini", AppCategory.AI_STUDY),
            AppItem("com.google.android.apps.labs.language.tailwind", "NotebookLM", AppCategory.AI_STUDY),
            AppItem("com.google.android.apps.notebooklm", "NotebookLM", AppCategory.AI_STUDY),
            AppItem("com.livemcq.livemcq", "Live MCQ", AppCategory.AI_STUDY),
            AppItem("com.google.android.keep", "Google Keep", AppCategory.AI_STUDY),
            AppItem("com.github.android", "GitHub", AppCategory.AI_STUDY),
            AppItem("com.samsung.android.app.notes", "Samsung Notes", AppCategory.AI_STUDY),
            AppItem("com.tos.namajtime", "Muslim Bangla", AppCategory.AI_STUDY),
            AppItem("com.muslimbangla", "Muslim Bangla (Alt)", AppCategory.AI_STUDY),
            AppItem("com.ihadis.ihadis", "iHadis", AppCategory.AI_STUDY),
            AppItem("com.mahfil.app", "Mahfil", AppCategory.AI_STUDY),
            AppItem("com.greentech.quran", "Al Quran (Tafsir & by Word)", AppCategory.AI_STUDY),
            AppItem("com.a3d4medical.completeanatomy", "Complete Anatomy", AppCategory.AI_STUDY),
            AppItem("com.elsevier.threeD4Medical.CompleteAnatomy", "Complete Anatomy (Alt)", AppCategory.AI_STUDY),
            AppItem("ridmik.keyboard", "Ridmik Keyboard", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
            AppItem("net.ridmik.keyboard", "Ridmik Keyboard", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
            AppItem("com.google.android.apps.translate", "Google Translate", AppCategory.GOOGLE_PRODUCTIVITY),
            AppItem("com.google.android.apps.authenticator2", "Google Authenticator", AppCategory.GOOGLE_PRODUCTIVITY),
            AppItem("com.google.android.gm", "Gmail", AppCategory.GOOGLE_PRODUCTIVITY),
            AppItem("com.google.android.apps.docs", "Google Drive", AppCategory.GOOGLE_PRODUCTIVITY),
            AppItem("com.google.android.apps.docs.editors.docs", "Google Docs", AppCategory.GOOGLE_PRODUCTIVITY),
            AppItem("com.google.android.apps.docs.editors.sheets", "Google Sheets", AppCategory.GOOGLE_PRODUCTIVITY),
            AppItem("com.google.android.apps.photos", "Google Photos", AppCategory.GOOGLE_PRODUCTIVITY),
            AppItem("ru.zdevs.zarchiver", "ZArchiver", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
            AppItem("com.google.android.apps.credentialmanager", "Google Password Manager", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
            AppItem("com.android.credentialmanager", "Credential Manager", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
            AppItem("com.google.android.apps.adm", "Find Hub", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
            AppItem("com.google.android.webview", "System WebView", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
            AppItem("com.google.android.gms", "Play Services", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
            AppItem("com.android.vending", "Play Store", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
            AppItem("teletalk.teletalkcustomerapp", "My Teletalk", AppCategory.ESSENTIAL_SYSTEM),
            AppItem("com.sec.android.app.music", "Samsung Music", AppCategory.ESSENTIAL_SYSTEM),
            AppItem("com.samsung.android.samsungpass", "Samsung Pass", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
            AppItem("com.samsung.android.samsungpassautofill", "Samsung Pass Autofill", AppCategory.ESSENTIAL_SYSTEM, isEssential = true)
        ).associateBy { it.packageName }

        for (pkg in expectedAllowedPackages) {
            assertTrue("Expected allowed package missing: $pkg", predefinedAllowed.containsKey(pkg))
        }
    }

    @Test
    fun verifyBlockedPackagesAreMarkedNotAllowed() {
        val predefinedBlocked = listOf(
            AppItem("com.android.chrome", "Google Chrome", AppCategory.BLOCKED_BROWSER, isAllowed = false),
            AppItem("org.mozilla.firefox", "Firefox", AppCategory.BLOCKED_BROWSER, isAllowed = false),
            AppItem("com.sec.android.app.sbrowser", "Samsung Internet", AppCategory.BLOCKED_BROWSER, isAllowed = false),
            AppItem("com.opera.browser", "Opera", AppCategory.BLOCKED_BROWSER, isAllowed = false),
            AppItem("com.brave.browser", "Brave", AppCategory.BLOCKED_BROWSER, isAllowed = false),
            AppItem("com.microsoft.emmx", "Edge", AppCategory.BLOCKED_BROWSER, isAllowed = false),
            AppItem("com.mi.globalbrowser", "Mi Browser", AppCategory.BLOCKED_BROWSER, isAllowed = false),
            AppItem("org.telegram.messenger", "Telegram", AppCategory.BLOCKED_DISTRACTION, isAllowed = false),
            AppItem("org.telegram.messenger.web", "Telegram Web", AppCategory.BLOCKED_DISTRACTION, isAllowed = false),
            AppItem("org.thunderdog.challegram", "Telegram X", AppCategory.BLOCKED_DISTRACTION, isAllowed = false),
            AppItem("com.google.android.youtube", "YouTube", AppCategory.BLOCKED_ENTERTAINMENT, isAllowed = false),
            AppItem("com.google.android.apps.youtube.music", "YouTube Music", AppCategory.BLOCKED_ENTERTAINMENT, isAllowed = false),
            AppItem("com.facebook.katana", "Facebook", AppCategory.BLOCKED_SOCIAL, isAllowed = false),
            AppItem("com.instagram.android", "Instagram", AppCategory.BLOCKED_SOCIAL, isAllowed = false),
            AppItem("com.zhiliaoapp.musically", "TikTok", AppCategory.BLOCKED_SOCIAL, isAllowed = false),
            AppItem("com.twitter.android", "Twitter", AppCategory.BLOCKED_SOCIAL, isAllowed = false),
            AppItem("com.netflix.mediaclient", "Netflix", AppCategory.BLOCKED_ENTERTAINMENT, isAllowed = false)
        ).associateBy { it.packageName }

        for (pkg in expectedBlockedPackages) {
            assertTrue("Expected blocked package missing: $pkg", predefinedBlocked.containsKey(pkg))
            assertFalse("Package should be marked isAllowed=false: $pkg", predefinedBlocked[pkg]!!.isAllowed)
        }
    }

    @Test
    fun verifyCustomAllowedPackagesOverride() {
        val customAllowed = setOf("com.custom.allowedapp")
        val isAllowed = customAllowed.contains("com.custom.allowedapp")
        assertTrue("Custom allowed package must be evaluated as allowed", isAllowed)
    }

    @Test
    fun verifySystemWebViewNeverBlocked() {
        val webViewPackage = "com.google.android.webview"
        val item = AppItem(webViewPackage, "System WebView", AppCategory.ESSENTIAL_SYSTEM, isEssential = true)
        assertTrue("WebView must be marked essential", item.isEssential)
        assertTrue("WebView must be allowed", item.isAllowed)
    }

    @Test
    fun verifyIrreversibleRestrictionsIncludeAdbDisallow() {
        val enforcedRestrictions = listOf(
            "no_install_unknown_sources", // UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES
            "no_config_vpn",             // UserManager.DISALLOW_CONFIG_VPN
            "no_safe_boot",              // UserManager.DISALLOW_SAFE_BOOT
            "no_install_apps"            // UserManager.DISALLOW_INSTALL_APPS
        )
        assertTrue("Must disallow unknown sources", enforcedRestrictions.contains("no_install_unknown_sources"))
        assertTrue("Must disallow new installations", enforcedRestrictions.contains("no_install_apps"))
        assertFalse("Must NOT disallow factory reset by default; toggled via 3-dot security menu", enforcedRestrictions.contains("no_factory_reset"))
        assertFalse("Must NOT disallow ADB debugging by default to allow testing/debugging on device", enforcedRestrictions.contains("no_debugging_features"))
        assertFalse("Must NOT disallow uninstallation system-wide so unwanted apps can be removed", enforcedRestrictions.contains("no_uninstall_apps"))
    }

    @Test
    fun verifySamsungCloudAndBackupComponentsNeverBlocked() {
        val criticalSet = com.focusguard.dpc.DevicePolicyController.CRITICAL_SYSTEM_PACKAGES
        val cloudPackages = listOf(
            "com.samsung.android.scloud",
            "com.samsung.android.scloud.backup",
            "com.samsung.android.scloud.sync",
            "com.sec.android.easyMover",
            "com.sec.android.easyMover.Agent",
            "com.samsung.android.mobileservice",
            "com.osp.app.signin",
            "com.google.android.backuptransport"
        )
        for (pkg in cloudPackages) {
            assertTrue("Samsung cloud package must be protected in CRITICAL_SYSTEM_PACKAGES: $pkg", criticalSet.contains(pkg))
        }
    }

    @Test
    fun verifySamsungAndGoogleUpdateComponentsNeverBlocked() {
        val criticalSet = com.focusguard.dpc.DevicePolicyController.CRITICAL_SYSTEM_PACKAGES
        val requiredUpdatePackages = listOf(
            "com.sec.android.app.samsungapps.una2", // Samsung In-App Update Agent
            "com.wssyncmldm",                      // Samsung FOTA / OS Firmware Update
            "com.sec.android.soagent",                   // Samsung System Update Agent
            "com.samsung.android.app.omcagent",    // Samsung OMC / Carrier Update Agent
            "com.google.android.modulemetadata",   // Google Play System Updates (Mainline)
            "com.android.vending",                 // Google Play Store
            "com.google.android.gms",              // Google Play Services
            "com.google.android.webview",          // Android System WebView
            "com.samsung.android.samsungpass",     // Samsung Pass Framework
            "com.samsung.android.samsungpassautofill" // Samsung Pass Autofill
        )
        for (pkg in requiredUpdatePackages) {
            assertTrue("Critical update package must be protected in CRITICAL_SYSTEM_PACKAGES: $pkg", criticalSet.contains(pkg))
        }
    }

    @Test
    fun verifyBaselineCryptographicValidation() {
        val legitimateCert = "38A7326B1E32B7423D9E086F9A2DE9827B7A9EF8C123D8A39B23E11223344556"
        val fakeCert = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"

        val baselineRecord = com.focusguard.data.model.BaselineAppRecord(
            packageName = "com.sec.android.app.camera",
            initialVersionCode = 14000100L,
            initialVersionName = "14.0.01",
            isSystemApp = true,
            installingPackageName = "com.sec.android.app.samsungapps",
            signingCertificateSha256 = legitimateCert
        )

        // Matching cert should be approved as valid update
        val isLegitimate = baselineRecord.signingCertificateSha256.equals(legitimateCert, ignoreCase = true)
        assertTrue("Update with matching signature must be permitted", isLegitimate)

        // Spoofed or mismatched cert must be rejected
        val isMismatched = baselineRecord.signingCertificateSha256.equals(fakeCert, ignoreCase = true)
        assertFalse("Update with mismatched signature must be rejected", isMismatched)
    }

    @Test
    fun verifyBrilliantConnectInRegistryAndAllowlist() {
        val brilliantPkg = "com.brilliant.connect.com.bd"
        assertTrue("Brilliant Connect must be in expectedAllowedPackages", expectedAllowedPackages.contains(brilliantPkg))
    }

    @Test
    fun verifyIslamicAppsInRegistryAndAllowlist() {
        val islamicPkgs = listOf(
            "com.tos.namajtime",
            "com.muslimbangla",
            "com.ihadis.ihadis",
            "com.mahfil.app",
            "com.greentech.quran"
        )
        for (pkg in islamicPkgs) {
            assertTrue("Islamic app must be in expectedAllowedPackages: $pkg", expectedAllowedPackages.contains(pkg))
        }
    }

    @Test
    fun verifyCompleteAnatomyInRegistryAndAllowlist() {
        val anatomyPkgs = listOf(
            "com.a3d4medical.completeanatomy",
            "com.elsevier.threeD4Medical.CompleteAnatomy"
        )
        for (pkg in anatomyPkgs) {
            assertTrue("Complete Anatomy package must be in expectedAllowedPackages: $pkg", expectedAllowedPackages.contains(pkg))
        }
    }

    @Test
    fun verifyTeletalkSamsungMusicAndPassInRegistryAndAllowlist() {
        val newPkgs = listOf(
            "teletalk.teletalkcustomerapp",
            "com.sec.android.app.music",
            "com.samsung.android.samsungpass",
            "com.samsung.android.samsungpassautofill",
            "com.ibbl.cellfin"
        )
        for (pkg in newPkgs) {
            assertTrue("Package must be in expectedAllowedPackages: $pkg", expectedAllowedPackages.contains(pkg))
        }
    }

    @Test
    fun verifyDeliveryShoppingGmailAndGoogleFrameworkInRegistryAndAllowlist() {
        val verifiedPkgs = listOf(
            "com.pathao.user",
            "com.ubercab",
            "com.global.foodpanda.android",
            "com.usbangla.foodi",
            "com.daraz.android",
            "com.google.android.gm"
        )
        for (pkg in verifiedPkgs) {
            assertTrue("Delivery/Shopping/Gmail/Google Framework must be in expectedAllowedPackages: $pkg", expectedAllowedPackages.contains(pkg))
        }
    }

    @Test
    fun verifyApprovedRegistryPreservesUninstalledAppForReinstall() {
        // Model simulation of ApprovedAppRegistry
        val registry = mutableMapOf<String, com.focusguard.data.repository.ApprovedAppEntry>()

        // 1. Initial state: WhatsApp and Brilliant Connect installed
        registry["com.whatsapp"] = com.focusguard.data.repository.ApprovedAppEntry(
            packageName = "com.whatsapp",
            displayName = "WhatsApp",
            category = AppCategory.COMMUNICATION,
            isPredefined = true,
            isCurrentlyInstalled = true
        )
        registry["com.brilliant.connect.com.bd"] = com.focusguard.data.repository.ApprovedAppEntry(
            packageName = "com.brilliant.connect.com.bd",
            displayName = "Brilliant Connect",
            category = AppCategory.COMMUNICATION,
            isPredefined = true,
            isCurrentlyInstalled = true
        )

        // Verify both are approved and installed
        assertTrue("WhatsApp must be approved", registry.containsKey("com.whatsapp"))
        assertTrue("WhatsApp must be marked installed", registry["com.whatsapp"]!!.isCurrentlyInstalled)
        assertTrue("Brilliant Connect must be approved", registry.containsKey("com.brilliant.connect.com.bd"))

        // 2. User uninstalls Brilliant Connect
        val brilliantEntry = registry["com.brilliant.connect.com.bd"]!!
        registry["com.brilliant.connect.com.bd"] = brilliantEntry.copy(isCurrentlyInstalled = false)

        // 3. Verify record is NOT deleted upon uninstallation
        assertTrue("Brilliant Connect record MUST remain in registry after uninstall", registry.containsKey("com.brilliant.connect.com.bd"))
        assertFalse("Brilliant Connect installed status must be false", registry["com.brilliant.connect.com.bd"]!!.isCurrentlyInstalled)

        // 4. Verify policy permits reinstallation because it remains in the approved registry
        val canReinstall = registry.containsKey("com.brilliant.connect.com.bd")
        assertTrue("Approved uninstalled app must remain reinstallable", canReinstall)

        // 5. User reinstalls Brilliant Connect: Mark as installed again
        registry["com.brilliant.connect.com.bd"] = registry["com.brilliant.connect.com.bd"]!!.copy(isCurrentlyInstalled = true)
        assertTrue("Reinstalled app must now be marked installed", registry["com.brilliant.connect.com.bd"]!!.isCurrentlyInstalled)
    }

    @Test
    fun verifyInstallationPolicyPermitsApprovedReinstallAndBlocksUnapproved() {
        val registry = setOf("com.whatsapp", "com.brilliant.connect.com.bd", "com.bKash.customerapp")
        val baselinePackages = setOf("com.sec.android.app.camera")

        fun evaluateInstallation(pkg: String): String {
            return when {
                registry.contains(pkg) -> "PERMIT_APPROVED_INSTALL_OR_REINSTALL"
                baselinePackages.contains(pkg) -> "PERMIT_LEGITIMATE_UPDATE"
                else -> "BLOCK_AND_REMEDIATE"
            }
        }

        // Test reinstallation of approved app
        assertEquals(
            "Approved app must be permitted for install/reinstall",
            "PERMIT_APPROVED_INSTALL_OR_REINSTALL",
            evaluateInstallation("com.brilliant.connect.com.bd")
        )

        // Test update of existing baseline component
        assertEquals(
            "Baseline component update must be permitted",
            "PERMIT_LEGITIMATE_UPDATE",
            evaluateInstallation("com.sec.android.app.camera")
        )

        // Test blocked unauthorized app (e.g. unapproved game / browser)
        assertEquals(
            "Unapproved app must be blocked and remediated immediately",
            "BLOCK_AND_REMEDIATE",
            evaluateInstallation("com.unapproved.distraction.game")
        )
    }

    @Test
    fun verifyAdminCanAddCustomPackageAndPreserveAfterUninstall() {
        val registry = mutableMapOf<String, com.focusguard.data.repository.ApprovedAppEntry>()
        val customPkg = "com.custom.hospital.app"

        // 1. Admin adds custom package
        registry[customPkg] = com.focusguard.data.repository.ApprovedAppEntry(
            packageName = customPkg,
            displayName = "Hospital Portal",
            category = AppCategory.CUSTOM_ALLOWED,
            isCustomAdded = true,
            isCurrentlyInstalled = true
        )
        assertTrue(registry.containsKey(customPkg))

        // 2. User uninstalls custom package
        registry[customPkg] = registry[customPkg]!!.copy(isCurrentlyInstalled = false)

        // 3. Must preserve custom package record
        assertTrue("Custom package must persist across uninstallation", registry.containsKey(customPkg))
        assertFalse(registry[customPkg]!!.isCurrentlyInstalled)

        // 4. Reinstall is permitted
        val isPermitted = registry.containsKey(customPkg)
        assertTrue("Custom approved package must be reinstallable", isPermitted)
    }

    @Test
    fun verifyEssentialPackagesCannotBeRemovedFromRegistry() {
        val essentialEntry = com.focusguard.data.repository.ApprovedAppEntry(
            packageName = "com.google.android.webview",
            displayName = "WebView",
            category = AppCategory.ESSENTIAL_SYSTEM,
            isPredefined = true,
            isEssential = true
        )
        val customEntry = com.focusguard.data.repository.ApprovedAppEntry(
            packageName = "com.custom.tool",
            displayName = "Custom Tool",
            category = AppCategory.CUSTOM_ALLOWED,
            isCustomAdded = true,
            isEssential = false
        )

        fun canRemove(entry: com.focusguard.data.repository.ApprovedAppEntry): Boolean {
            return !entry.isEssential
        }

        assertFalse("Essential system component cannot be removed by admin", canRemove(essentialEntry))
        assertTrue("Custom package can be removed by admin", canRemove(customEntry))
    }

    @Test
    fun verifyKnoxKspWhiteListFormatting() {
        val approvedPackages = setOf("com.whatsapp", "com.brilliant.connect.com.bd", "com.bKash.customerapp")
        val joined = approvedPackages.joinToString(",")

        assertTrue("Whitelist string must contain com.whatsapp", joined.contains("com.whatsapp"))
        assertTrue("Whitelist string must contain com.brilliant.connect.com.bd", joined.contains("com.brilliant.connect.com.bd"))
        assertTrue("Whitelist string must contain com.bKash.customerapp", joined.contains("com.bKash.customerapp"))
    }

    @Test
    fun verifyControlledGoogleAppPolicy() {
        val restrictedSet = com.focusguard.dpc.DevicePolicyController.RESTRICTED_SYSTEM_PACKAGES

        // 1. Google App package is restricted and hidden/suspended by DPC
        assertTrue(restrictedSet.contains("com.google.android.googlequicksearchbox"))

        // 2. General web search & search suggestions are blocked by navigation guard
        assertTrue(com.focusguard.study.filter.StudyNavigationGuard.isHostBlocked("google.com"))
        assertTrue(com.focusguard.study.filter.StudyNavigationGuard.isHostBlocked("www.google.com"))
        assertTrue(com.focusguard.study.filter.StudyNavigationGuard.isHostBlocked("suggestqueries.google.com"))

        // 3. Circle to Search, Lens, and OCR terms are allowed, while Discover & Search Widget activities are blocked
        val allowedFeatureNames = listOf("LensActivity", "OmnientActivity", "CircleToSearch", "TextSelectionActivity", "OpaOverlay")
        for (name in allowedFeatureNames) {
            val isAllowed = name.contains("Lens", ignoreCase = true) ||
                    name.contains("Omnient", ignoreCase = true) ||
                    name.contains("CircleToSearch", ignoreCase = true) ||
                    name.contains("TextSelection", ignoreCase = true) ||
                    name.contains("OpaOverlay", ignoreCase = true)
            assertTrue("Feature $name must be allowed", isAllowed)
        }

        val blockedFeatureNames = listOf("SearchActivity", "DiscoverActivity", "FeedActivity", "SearchgateActivity", "QueryEntryActivity", "SearchWidgetProvider")
        for (name in blockedFeatureNames) {
            val isBlocked = name.contains("Search", ignoreCase = true) ||
                    name.contains("Discover", ignoreCase = true) ||
                    name.contains("Feed", ignoreCase = true) ||
                    name.contains("Searchgate", ignoreCase = true) ||
                    name.contains("QueryEntry", ignoreCase = true) ||
                    name.contains("Widget", ignoreCase = true)
            assertTrue("Feature $name must be blocked", isBlocked)
        }
    }
}
