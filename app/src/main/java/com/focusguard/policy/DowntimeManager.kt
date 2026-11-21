package com.focusguard.policy

import java.util.Calendar

/**
 * Manages Focus Guard's Nightly Downtime Policy:
 * - Start: 11:00 PM (23:00)
 * - End: 6:00 AM (06:00)
 * - Allowed during downtime: Maps, Quran, Brilliant, Phone, Uber, essential system input/launcher.
 * - Explicitly blocked: WhatsApp, AI apps (ChatGPT, Gemini, Claude), Mahfil, general browsing, and entertainment.
 */
object DowntimeManager {

    const val DOWNTIME_START_HOUR = 23 // 11:00 PM
    const val DOWNTIME_END_HOUR = 6    // 6:00 AM

    val DOWNTIME_ALLOWED_PACKAGES: Set<String> = setOf(
        // 1. Maps
        "com.google.android.apps.maps",

        // 2. Quran
        "com.greentech.quran",

        // 3. Brilliant
        "com.brilliant.connect.com.bd",

        // 4. Phone & Emergency Calling
        "com.samsung.android.dialer",
        "com.android.phone",
        "com.android.server.telecom",
        "com.google.android.dialer",
        "com.android.dialer",
        "com.samsung.android.incallui",

        // 5. Uber
        "com.ubercab",

        // Core System & Shell
        "com.focusguard",
        "com.android.systemui",
        "com.sec.android.app.launcher",
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
        "ridmik.keyboard",
        "net.ridmik.keyboard"
    )

    /**
     * Returns true if current time is within downtime (11:00 PM to 6:00 AM).
     */
    fun isDowntimeActive(calendar: Calendar = Calendar.getInstance()): Boolean {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour >= DOWNTIME_START_HOUR || hour < DOWNTIME_END_HOUR
    }

    /**
     * Checks if a package is permitted to run during downtime.
     */
    fun isPackageAllowedDuringDowntime(pkg: String): Boolean {
        if (DOWNTIME_ALLOWED_PACKAGES.contains(pkg)) {
            return true
        }
        val lower = pkg.lowercase()
        // Permit phone dialer & in-call UI components unconditionally
        if (lower.contains("dialer") || lower.contains("incallui") || lower.contains("telecom")) {
            return true
        }
        return false
    }
}
