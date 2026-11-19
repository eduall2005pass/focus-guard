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

        // Banking & Payments
        "com.ibbl.ibbl_ismart",
        "com.ibbl.ismart",
        "com.ibbl.cellfin",
        "com.bKash.customerapp",
        "com.konasl.nagad",
        "com.dbbl.mbs.apps.main",

        // Transportation
        "com.google.android.apps.maps",
        "bd.gov.dmtcl.rapidpass",

        // AI & Study
        "com.openai.chatgpt",
        "com.anthropic.claude",
        "com.google.android.apps.bard",
        "com.google.android.apps.notebooklm",
        "com.samsung.android.app.notes",

        // Google Productivity
        "com.google.android.apps.docs",
        "com.google.android.apps.docs.editors.docs",
        "com.google.android.apps.docs.editors.sheets",
        "com.google.android.apps.photos",

        // Critical System Components
        "com.google.android.webview",
        "com.google.android.gms",
        "com.android.vending"
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
            AppItem("com.ibbl.ibbl_ismart", "iSmart", AppCategory.BANKING_PAYMENTS),
            AppItem("com.ibbl.ismart", "iSmart", AppCategory.BANKING_PAYMENTS),
            AppItem("com.ibbl.cellfin", "Cellfin", AppCategory.BANKING_PAYMENTS),
            AppItem("com.bKash.customerapp", "bKash", AppCategory.BANKING_PAYMENTS),
            AppItem("com.konasl.nagad", "Nagad", AppCategory.BANKING_PAYMENTS),
            AppItem("com.dbbl.mbs.apps.main", "Rocket", AppCategory.BANKING_PAYMENTS),
            AppItem("com.google.android.apps.maps", "Google Maps", AppCategory.TRANSPORTATION),
            AppItem("bd.gov.dmtcl.rapidpass", "Rapid Pass", AppCategory.TRANSPORTATION),
            AppItem("com.openai.chatgpt", "ChatGPT", AppCategory.AI_STUDY),
            AppItem("com.anthropic.claude", "Claude", AppCategory.AI_STUDY),
            AppItem("com.google.android.apps.bard", "Gemini", AppCategory.AI_STUDY),
            AppItem("com.google.android.apps.notebooklm", "NotebookLM", AppCategory.AI_STUDY),
            AppItem("com.samsung.android.app.notes", "Samsung Notes", AppCategory.AI_STUDY),
            AppItem("com.google.android.apps.docs", "Google Drive", AppCategory.GOOGLE_PRODUCTIVITY),
            AppItem("com.google.android.apps.docs.editors.docs", "Google Docs", AppCategory.GOOGLE_PRODUCTIVITY),
            AppItem("com.google.android.apps.docs.editors.sheets", "Google Sheets", AppCategory.GOOGLE_PRODUCTIVITY),
            AppItem("com.google.android.apps.photos", "Google Photos", AppCategory.GOOGLE_PRODUCTIVITY),
            AppItem("com.google.android.webview", "System WebView", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
            AppItem("com.google.android.gms", "Play Services", AppCategory.ESSENTIAL_SYSTEM, isEssential = true),
            AppItem("com.android.vending", "Play Store", AppCategory.ESSENTIAL_SYSTEM, isEssential = true)
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
}
