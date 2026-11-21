package com.focusguard.policy

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages daily screen time limits for designated apps:
 * - Mahfil app (com.mahfil.app): 2 hours (120 minutes)
 * - AI apps (ChatGPT, Gemini, Claude): 1 hour 30 minutes (90 minutes) each
 *
 * Persists accumulated daily foreground usage in SharedPreferences and resets
 * automatically every midnight (00:00).
 */
class AppUsageLimitManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var currentForegroundPackage: String? = null
    @Volatile
    private var foregroundStartTime: Long = 0L

    companion object {
        private const val TAG = "FocusGuard_UsageLimit"
        private const val PREFS_NAME = "focus_guard_usage_limits"
        private const val KEY_LAST_RESET_DATE = "last_reset_date"
        private const val KEY_PREFIX_USAGE = "usage_millis_"

        // Daily limits in minutes
        val DAILY_LIMITS_MINUTES: Map<String, Int> = mapOf(
            "com.mahfil.app" to 120,          // 2 hours
            "com.openai.chatgpt" to 90,        // 1 hour 30 minutes
            "com.google.android.apps.bard" to 90, // 1 hour 30 minutes (Gemini)
            "com.anthropic.claude" to 90       // 1 hour 30 minutes
        )

        fun isMonitoredPackage(packageName: String): Boolean {
            return DAILY_LIMITS_MINUTES.containsKey(packageName)
        }

        fun getLimitMinutes(packageName: String): Int {
            return DAILY_LIMITS_MINUTES[packageName] ?: Int.MAX_VALUE
        }

        fun getLimitMillis(packageName: String): Long {
            val minutes = DAILY_LIMITS_MINUTES[packageName] ?: return Long.MAX_VALUE
            return minutes * 60 * 1000L
        }

        fun isLimitExceeded(packageName: String, currentUsageMillis: Long): Boolean {
            val limit = getLimitMillis(packageName)
            return currentUsageMillis >= limit
        }

        fun getDisplayName(packageName: String): String {
            return when (packageName) {
                "com.mahfil.app" -> "মাহফিল (Mahfil)"
                "com.openai.chatgpt" -> "ChatGPT"
                "com.google.android.apps.bard" -> "Gemini"
                "com.anthropic.claude" -> "Claude"
                else -> packageName
            }
        }
    }

    /**
     * Checks if date has changed and resets daily counters if a new day has started.
     */
    @Synchronized
    fun checkAndResetDailyUsage(): Boolean {
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        val lastReset = prefs.getString(KEY_LAST_RESET_DATE, null)

        if (lastReset != today) {
            Log.i(TAG, "New day detected ($today vs $lastReset). Resetting all app screen time limits.")
            val editor = prefs.edit()
            for (pkg in DAILY_LIMITS_MINUTES.keys) {
                editor.remove(KEY_PREFIX_USAGE + pkg)
            }
            editor.putString(KEY_LAST_RESET_DATE, today)
            editor.apply()
            return true
        }
        return false
    }

    /**
     * Returns accumulated usage today in milliseconds.
     */
    @Synchronized
    fun getTodayUsageMillis(packageName: String): Long {
        checkAndResetDailyUsage()
        var stored = prefs.getLong(KEY_PREFIX_USAGE + packageName, 0L)
        if (currentForegroundPackage == packageName && foregroundStartTime > 0L) {
            val ongoing = System.currentTimeMillis() - foregroundStartTime
            if (ongoing > 0L) {
                stored += ongoing
            }
        }
        return stored
    }

    /**
     * Checks if the package has exceeded its daily screen time limit.
     */
    fun isPackageLimitExceeded(packageName: String): Boolean {
        if (!isMonitoredPackage(packageName)) return false
        val usage = getTodayUsageMillis(packageName)
        return isLimitExceeded(packageName, usage)
    }

    /**
     * Called when an app enters the foreground.
     */
    @Synchronized
    fun onAppForegrounded(packageName: String) {
        checkAndResetDailyUsage()
        // If another monitored app was foregrounded, flush its time first
        if (currentForegroundPackage != null && currentForegroundPackage != packageName) {
            flushOngoingUsage()
        }

        if (isMonitoredPackage(packageName)) {
            currentForegroundPackage = packageName
            foregroundStartTime = System.currentTimeMillis()
        } else {
            currentForegroundPackage = null
            foregroundStartTime = 0L
        }
    }

    /**
     * Called when the active app transitions away.
     */
    @Synchronized
    fun onAppBackgrounded() {
        flushOngoingUsage()
        currentForegroundPackage = null
        foregroundStartTime = 0L
    }

    /**
     * Flushes currently active foreground time to SharedPreferences.
     */
    @Synchronized
    fun flushOngoingUsage() {
        val pkg = currentForegroundPackage ?: return
        if (foregroundStartTime <= 0L) return

        val now = System.currentTimeMillis()
        val duration = now - foregroundStartTime
        if (duration > 0L) {
            val previous = prefs.getLong(KEY_PREFIX_USAGE + pkg, 0L)
            val updated = previous + duration
            prefs.edit().putLong(KEY_PREFIX_USAGE + pkg, updated).apply()
            Log.d(TAG, "Flushed usage for $pkg: +${duration / 1000}s, total: ${updated / 60000}m")
        }
        foregroundStartTime = now
    }
}
