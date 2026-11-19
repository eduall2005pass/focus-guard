package com.focusguard.data.repository

import android.content.Context
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.focusguard.data.model.AppCategory
import com.focusguard.data.model.AppItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "focus_guard_allowlist")

class AllowlistRepository(private val context: Context) {

    private val customAllowedPackagesKey = stringSetPreferencesKey("custom_allowed_packages")

    val customAllowedPackagesFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[customAllowedPackagesKey] ?: emptySet()
    }

    suspend fun addCustomAllowedPackage(packageName: String) {
        val trimmed = packageName.trim()
        if (trimmed.isEmpty()) return
        context.dataStore.edit { preferences ->
            val current = preferences[customAllowedPackagesKey] ?: emptySet()
            preferences[customAllowedPackagesKey] = current + trimmed
        }
    }

    suspend fun removeCustomAllowedPackage(packageName: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[customAllowedPackagesKey] ?: emptySet()
            preferences[customAllowedPackagesKey] = current - packageName
        }
    }

    /**
     * Built-in allowed applications strictly curated according to system requirements.
     */
    val predefinedAllowedApps: List<AppItem> = listOf(
        // Communication
        AppItem("com.whatsapp", "WhatsApp", AppCategory.COMMUNICATION, note = "Communities guarded via Accessibility"),
        AppItem("com.whatsapp.w4b", "WhatsApp Business", AppCategory.COMMUNICATION, note = "Communities guarded via Accessibility"),
        AppItem("com.brilliant.connect.com.bd", "Brilliant Connect", AppCategory.COMMUNICATION, note = "IP Telephony & Communication"),

        // Banking and Payments
        AppItem("com.ibbl.ibbl_ismart", "iSmart (IBBL)", AppCategory.BANKING_PAYMENTS),
        AppItem("com.ibbl.ismart", "iSmart", AppCategory.BANKING_PAYMENTS),
        AppItem("com.ibbl.cellfin", "Cellfin", AppCategory.BANKING_PAYMENTS),
        AppItem("com.bKash.customerapp", "bKash", AppCategory.BANKING_PAYMENTS),
        AppItem("com.konasl.nagad", "Nagad", AppCategory.BANKING_PAYMENTS),
        AppItem("com.dbbl.mbs.apps.main", "Rocket (DBBL)", AppCategory.BANKING_PAYMENTS),
        AppItem("bd.com.ucb.upay", "Upay", AppCategory.BANKING_PAYMENTS),

        // Transportation, Delivery & Rides
        AppItem("com.google.android.apps.maps", "Google Maps", AppCategory.TRANSPORTATION),
        AppItem("bd.gov.dmtcl.rapidpass", "Rapid Pass", AppCategory.TRANSPORTATION),
        AppItem("bd.gov.rapidpass", "Rapid Pass Portal", AppCategory.TRANSPORTATION),
        AppItem("com.pathao.user", "Pathao", AppCategory.TRANSPORTATION, note = "Ride, Food, Courier & Delivery"),
        AppItem("com.ubercab", "Uber", AppCategory.TRANSPORTATION, note = "Rides & Transit"),
        AppItem("com.global.foodpanda.android", "Foodpanda", AppCategory.TRANSPORTATION, note = "Food & Grocery Delivery"),
        AppItem("com.usbangla.foodi", "Foodi", AppCategory.TRANSPORTATION, note = "Food Delivery by US-Bangla"),
        AppItem("com.foodiBd", "Foodi (Alt)", AppCategory.TRANSPORTATION),
        AppItem("com.daraz.android", "Daraz", AppCategory.TRANSPORTATION, note = "Online Shopping & Delivery"),
        AppItem("com.chaldal.poached", "Chaldal", AppCategory.TRANSPORTATION, note = "Online Grocery Delivery"),

        // AI and Study
        AppItem("com.openai.chatgpt", "ChatGPT", AppCategory.AI_STUDY),
        AppItem("com.anthropic.claude", "Claude", AppCategory.AI_STUDY),
        AppItem("com.google.android.apps.bard", "Gemini", AppCategory.AI_STUDY),
        AppItem("com.google.android.apps.notebooklm", "NotebookLM", AppCategory.AI_STUDY),
        AppItem("com.google.android.apps.pdfviewer", "Google PDF Viewer", AppCategory.AI_STUDY),
        AppItem("com.adobe.reader", "Adobe Acrobat Reader", AppCategory.AI_STUDY),
        AppItem("com.samsung.android.app.notes", "Samsung Notes", AppCategory.AI_STUDY),
        AppItem("com.tos.namajtime", "Muslim Bangla", AppCategory.AI_STUDY, note = "Prayer Times & Quran"),
        AppItem("com.muslimbangla", "Muslim Bangla (Alt)", AppCategory.AI_STUDY, note = "Prayer Times & Islamic Tools"),
        AppItem("com.ihadis.ihadis", "iHadis", AppCategory.AI_STUDY, note = "Authentic Hadith Collection"),
        AppItem("com.mahfil.app", "Mahfil", AppCategory.AI_STUDY, note = "Islamic Discussions & Waz"),
        AppItem("com.greentech.quran", "Al Quran (Tafsir & by Word)", AppCategory.AI_STUDY, note = "Quran Study & Tafsir"),
        AppItem("com.a3d4medical.completeanatomy", "Complete Anatomy", AppCategory.AI_STUDY, note = "3D Anatomy & Medical Learning"),
        AppItem("com.elsevier.threeD4Medical.CompleteAnatomy", "Complete Anatomy (Alt)", AppCategory.AI_STUDY, note = "3D Anatomy by Elsevier"),

        // Google Productivity
        AppItem("com.google.android.gm", "Gmail", AppCategory.GOOGLE_PRODUCTIVITY, note = "Email & Communication"),
        AppItem("com.google.android.apps.docs", "Google Drive", AppCategory.GOOGLE_PRODUCTIVITY),
        AppItem("com.google.android.apps.docs.editors.docs", "Google Docs", AppCategory.GOOGLE_PRODUCTIVITY),
        AppItem("com.google.android.apps.docs.editors.sheets", "Google Sheets", AppCategory.GOOGLE_PRODUCTIVITY),
        AppItem("com.google.android.apps.photos", "Google Photos", AppCategory.GOOGLE_PRODUCTIVITY),

        // Essential System Applications & Critical Components
        AppItem("com.android.dialer", "Phone (AOSP)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.google.android.dialer", "Phone (Google)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.samsung.android.dialer", "Phone (Samsung)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.samsung.android.incallui", "InCall UI", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.android.phone", "Phone Services", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.android.server.telecom", "Telecom Server", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),

        AppItem("com.google.android.apps.messaging", "Messages (Google)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.android.mms", "Messages (AOSP)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.samsung.android.messaging", "Messages (Samsung)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),

        AppItem("com.google.android.contacts", "Contacts (Google)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.android.contacts", "Contacts (AOSP)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.samsung.android.app.contacts", "Contacts (Samsung)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),

        AppItem("com.google.android.GoogleCamera", "Camera (Google)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.android.camera", "Camera (AOSP)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.sec.android.app.camera", "Camera (Samsung)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),

        AppItem("com.google.android.apps.photosgo", "Gallery Go", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.android.gallery3d", "Gallery (AOSP)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.sec.android.gallery3d", "Gallery (Samsung)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),

        AppItem("com.google.android.apps.nbu.files", "Files by Google", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.android.documentsui", "Files (System)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.sec.android.app.myfiles", "My Files (Samsung)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),

        AppItem("com.google.android.deskclock", "Clock (Google)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.android.deskclock", "Clock (AOSP)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.sec.android.app.clockpackage", "Clock (Samsung)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),

        AppItem("com.google.android.calculator", "Calculator (Google)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.android.calculator2", "Calculator (AOSP)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.sec.android.app.popupcalculator", "Calculator (Samsung)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),

        AppItem("com.google.android.inputmethod.latin", "Gboard Keyboard", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.samsung.android.honeyboard", "Samsung Keyboard", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.touchtype.swiftkey", "Microsoft SwiftKey", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("org.dslul.openboard.inputmethod.latin", "OpenBoard", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),

        AppItem("com.android.providers.downloads", "Download Manager", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.android.providers.downloads.ui", "Download UI", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),

        // Local Media & Utilities
        AppItem("com.sec.android.app.music", "Samsung Music", AppCategory.ESSENTIAL_SYSTEM, note = "Local audio player"),
        AppItem("teletalk.teletalkcustomerapp", "My Teletalk", AppCategory.ESSENTIAL_SYSTEM, note = "Teletalk carrier self-care"),
        AppItem("com.teletalk.myteletalk", "My Teletalk (Alt)", AppCategory.ESSENTIAL_SYSTEM, note = "Teletalk self-care"),
        AppItem("bd.gov.teletalk.myteletalk", "My Teletalk (Gov)", AppCategory.ESSENTIAL_SYSTEM, note = "Teletalk self-care"),

        // Samsung Pass & Security Credentials
        AppItem("com.samsung.android.samsungpass", "Samsung Pass", AppCategory.ESSENTIAL_SYSTEM, isEssential = true, note = "Biometric credentials & passkeys"),
        AppItem("com.samsung.android.samsungpassautofill", "Samsung Pass Autofill", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.samsung.android.spay", "Samsung Wallet / Pass", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.samsung.android.spaymini", "Samsung Pay Mini", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),

        // Critical Framework & Safety Components (NEVER HIDE)
        AppItem("com.google.android.webview", "Android System WebView", AppCategory.ESSENTIAL_SYSTEM, isEssential = true, note = "Preserves in-app web auth and banking"),
        AppItem("com.google.android.gms", "Google Play Services", AppCategory.ESSENTIAL_SYSTEM, isEssential = true, note = "Push notifications & security"),
        AppItem("com.google.android.gsf", "Google Services Framework", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.android.vending", "Google Play Store", AppCategory.ESSENTIAL_SYSTEM, isEssential = true, note = "Preserves app and security updates"),
        AppItem("com.android.settings", "System Settings", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.android.systemui", "System UI", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.google.android.packageinstaller", "Package Installer", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.android.packageinstaller", "Package Installer (AOSP)", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.google.android.permissioncontroller", "Permission Controller", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
        AppItem("com.focusguard", "Focus Guard", AppCategory.ESSENTIAL_SYSTEM, isEssential = true, note = "Protected DPC Controller"),
        AppItem("com.google.android.googlequicksearchbox", "Google App Framework", AppCategory.ESSENTIAL_SYSTEM, isEssential = true, note = "Circle to Search & text copy active; search/feed guarded")
    )

    /**
     * Explicitly blocked packages (Browsers, Social, Media, Unapproved Messengers).
     */
    val predefinedBlockedApps: List<AppItem> = listOf(
        // General-Purpose Browsers
        AppItem("com.android.chrome", "Google Chrome", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("org.mozilla.firefox", "Mozilla Firefox", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("org.mozilla.firefox_beta", "Firefox Beta", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("org.mozilla.fenix", "Firefox Fenix", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("com.sec.android.app.sbrowser", "Samsung Internet", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("com.sec.android.app.sbrowser.beta", "Samsung Internet Beta", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("com.opera.browser", "Opera Browser", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("com.opera.mini.native", "Opera Mini", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("com.opera.touch", "Opera Touch", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("com.brave.browser", "Brave Browser", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("com.microsoft.emmx", "Microsoft Edge", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("com.duckduckgo.mobile.android", "DuckDuckGo Browser", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("com.mi.globalbrowser", "Mi Browser", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("com.coloros.browser", "ColorOS Browser", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("com.heytap.browser", "HeyTap Browser", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("com.vivo.browser", "Vivo Browser", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("com.transsion.phoenix", "Phoenix Browser", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("com.UCMobile.intl", "UC Browser", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("com.uc.browser.en", "UC Browser Mini", AppCategory.BLOCKED_BROWSER, isAllowed = false),
        AppItem("org.torproject.torbrowser", "Tor Browser", AppCategory.BLOCKED_BROWSER, isAllowed = false),

        // Telegram variants
        AppItem("org.telegram.messenger", "Telegram", AppCategory.BLOCKED_DISTRACTION, isAllowed = false),
        AppItem("org.telegram.messenger.web", "Telegram Web", AppCategory.BLOCKED_DISTRACTION, isAllowed = false),
        AppItem("org.thunderdog.challegram", "Telegram X", AppCategory.BLOCKED_DISTRACTION, isAllowed = false),
        AppItem("nekox.messenger", "NekoX", AppCategory.BLOCKED_DISTRACTION, isAllowed = false),

        // Video / Streaming / Social Media
        AppItem("com.google.android.youtube", "YouTube", AppCategory.BLOCKED_ENTERTAINMENT, isAllowed = false),
        AppItem("com.google.android.apps.youtube.music", "YouTube Music", AppCategory.BLOCKED_ENTERTAINMENT, isAllowed = false),
        AppItem("com.google.android.apps.youtube.kids", "YouTube Kids", AppCategory.BLOCKED_ENTERTAINMENT, isAllowed = false),
        AppItem("com.facebook.katana", "Facebook", AppCategory.BLOCKED_SOCIAL, isAllowed = false),
        AppItem("com.facebook.lite", "Facebook Lite", AppCategory.BLOCKED_SOCIAL, isAllowed = false),
        AppItem("com.facebook.orca", "Messenger", AppCategory.BLOCKED_SOCIAL, isAllowed = false),
        AppItem("com.instagram.android", "Instagram", AppCategory.BLOCKED_SOCIAL, isAllowed = false),
        AppItem("com.zhiliaoapp.musically", "TikTok", AppCategory.BLOCKED_SOCIAL, isAllowed = false),
        AppItem("com.ss.android.ugc.trill", "TikTok", AppCategory.BLOCKED_SOCIAL, isAllowed = false),
        AppItem("com.twitter.android", "X / Twitter", AppCategory.BLOCKED_SOCIAL, isAllowed = false),
        AppItem("com.snapchat.android", "Snapchat", AppCategory.BLOCKED_SOCIAL, isAllowed = false),
        AppItem("com.reddit.frontpage", "Reddit", AppCategory.BLOCKED_SOCIAL, isAllowed = false),
        AppItem("com.pinterest", "Pinterest", AppCategory.BLOCKED_SOCIAL, isAllowed = false),
        AppItem("com.netflix.mediaclient", "Netflix", AppCategory.BLOCKED_ENTERTAINMENT, isAllowed = false)
    )

    private val predefinedAllowedMap = predefinedAllowedApps.associateBy { it.packageName }
    private val predefinedBlockedMap = predefinedBlockedApps.associateBy { it.packageName }

    fun isExplicitlyAllowed(packageName: String, customAllowed: Set<String>): Boolean {
        if (customAllowed.contains(packageName)) return true
        return predefinedAllowedMap.containsKey(packageName)
    }

    fun isExplicitlyBlocked(packageName: String): Boolean {
        return predefinedBlockedMap.containsKey(packageName)
    }

    fun isEssential(packageName: String): Boolean {
        return predefinedAllowedMap[packageName]?.isEssential == true || packageName == context.packageName
    }

    /**
     * Resolves all installed applications on device against the allowlist and blocklist.
     */
    fun getAllInstalledAppsWithStatus(customAllowed: Set<String>): List<AppItem> {
        val pm = context.packageManager
        val installed = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        val result = mutableListOf<AppItem>()

        for (pkg in installed) {
            val pkgName = pkg.packageName
            val label = try {
                pkg.applicationInfo?.loadLabel(pm)?.toString() ?: pkgName
            } catch (e: Exception) {
                pkgName
            }

            val predefinedAllowed = predefinedAllowedMap[pkgName]
            val predefinedBlocked = predefinedBlockedMap[pkgName]
            val isCustomAllowed = customAllowed.contains(pkgName)

            val item = when {
                predefinedAllowed != null -> predefinedAllowed.copy(
                    isInstalled = true,
                    displayName = label
                )
                predefinedBlocked != null -> predefinedBlocked.copy(
                    isInstalled = true,
                    displayName = label
                )
                isCustomAllowed -> AppItem(
                    packageName = pkgName,
                    displayName = label,
                    category = AppCategory.CUSTOM_ALLOWED,
                    isAllowed = true,
                    isInstalled = true
                )
                else -> {
                    // Check if it's a critical system package
                    val isSystem = (pkg.applicationInfo?.flags ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
                    if (isSystem && isCoreSystemComponent(pkgName)) {
                        AppItem(
                            packageName = pkgName,
                            displayName = label,
                            category = AppCategory.ESSENTIAL_SYSTEM,
                            isEssential = true,
                            isAllowed = true,
                            isInstalled = true
                        )
                    } else {
                        AppItem(
                            packageName = pkgName,
                            displayName = label,
                            category = AppCategory.OTHER_INSTALLED,
                            isAllowed = false,
                            isInstalled = true
                        )
                    }
                }
            }
            result.add(item)
        }

        return result.sortedWith(compareBy({ !it.isAllowed }, { it.category.ordinal }, { it.displayName }))
    }

    private fun isCoreSystemComponent(pkgName: String): Boolean {
        return pkgName.startsWith("com.android.server") ||
                pkgName.startsWith("com.android.internal") ||
                pkgName.startsWith("android") ||
                pkgName == "com.android.keychain" ||
                pkgName == "com.google.android.gsf" ||
                pkgName == "com.google.android.gms" ||
                pkgName == "com.google.android.webview"
    }
}
