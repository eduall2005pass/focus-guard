package com.focusguard

import com.focusguard.policy.DowntimeManager
import com.focusguard.policy.AppUsageLimitManager
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class DowntimeAndLimitsTest {

    @Test
    fun testDowntimeTimeWindow() {
        val calendar = Calendar.getInstance()

        // 10:59 PM (22:59) -> Inactive
        calendar.set(Calendar.HOUR_OF_DAY, 22)
        calendar.set(Calendar.MINUTE, 59)
        assertFalse("10:59 PM must not be downtime", DowntimeManager.isDowntimeActive(calendar))

        // 11:00 PM (23:00) -> Active
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 0)
        assertTrue("11:00 PM must be downtime", DowntimeManager.isDowntimeActive(calendar))

        // 2:30 AM (02:30) -> Active
        calendar.set(Calendar.HOUR_OF_DAY, 2)
        calendar.set(Calendar.MINUTE, 30)
        assertTrue("2:30 AM must be downtime", DowntimeManager.isDowntimeActive(calendar))

        // 5:59 AM (05:59) -> Active
        calendar.set(Calendar.HOUR_OF_DAY, 5)
        calendar.set(Calendar.MINUTE, 59)
        assertTrue("5:59 AM must be downtime", DowntimeManager.isDowntimeActive(calendar))

        // 6:00 AM (06:00) -> Inactive
        calendar.set(Calendar.HOUR_OF_DAY, 6)
        calendar.set(Calendar.MINUTE, 0)
        assertFalse("6:00 AM must not be downtime", DowntimeManager.isDowntimeActive(calendar))
    }

    @Test
    fun testDowntimeAllowedAndBlockedPackages() {
        // Allowed: Maps, Quran, Brilliant, Phone, Uber
        assertTrue(DowntimeManager.isPackageAllowedDuringDowntime("com.google.android.apps.maps"))
        assertTrue(DowntimeManager.isPackageAllowedDuringDowntime("com.greentech.quran"))
        assertTrue(DowntimeManager.isPackageAllowedDuringDowntime("com.brilliant.connect.com.bd"))
        assertTrue(DowntimeManager.isPackageAllowedDuringDowntime("com.samsung.android.dialer"))
        assertTrue(DowntimeManager.isPackageAllowedDuringDowntime("com.android.phone"))
        assertTrue(DowntimeManager.isPackageAllowedDuringDowntime("com.ubercab"))
        assertTrue(DowntimeManager.isPackageAllowedDuringDowntime("com.focusguard"))

        // Explicitly Blocked during downtime: AI, WhatsApp, Mahfil, etc.
        assertFalse(DowntimeManager.isPackageAllowedDuringDowntime("com.whatsapp"))
        assertFalse(DowntimeManager.isPackageAllowedDuringDowntime("com.whatsapp.w4b"))
        assertFalse(DowntimeManager.isPackageAllowedDuringDowntime("com.openai.chatgpt"))
        assertFalse(DowntimeManager.isPackageAllowedDuringDowntime("com.google.android.apps.bard"))
        assertFalse(DowntimeManager.isPackageAllowedDuringDowntime("com.anthropic.claude"))
        assertFalse(DowntimeManager.isPackageAllowedDuringDowntime("com.mahfil.app"))
        assertFalse(DowntimeManager.isPackageAllowedDuringDowntime("com.android.chrome"))
    }

    @Test
    fun testDailyScreenTimeLimitsConfiguredCorrectly() {
        // Mahfil = 2 hours (120 minutes)
        assertEquals(120, AppUsageLimitManager.getLimitMinutes("com.mahfil.app"))

        // AI apps = 1:30 hour (90 minutes) each
        assertEquals(90, AppUsageLimitManager.getLimitMinutes("com.openai.chatgpt"))
        assertEquals(90, AppUsageLimitManager.getLimitMinutes("com.google.android.apps.bard"))
        assertEquals(90, AppUsageLimitManager.getLimitMinutes("com.anthropic.claude"))

        // Verify in milliseconds
        assertEquals(120 * 60 * 1000L, AppUsageLimitManager.getLimitMillis("com.mahfil.app"))
        assertEquals(90 * 60 * 1000L, AppUsageLimitManager.getLimitMillis("com.openai.chatgpt"))
        assertEquals(90 * 60 * 1000L, AppUsageLimitManager.getLimitMillis("com.google.android.apps.bard"))
        assertEquals(90 * 60 * 1000L, AppUsageLimitManager.getLimitMillis("com.anthropic.claude"))
    }

    @Test
    fun testUsageLimitCalculation() {
        val mahfilPkg = "com.mahfil.app"
        val limit = AppUsageLimitManager.getLimitMillis(mahfilPkg)

        // Under limit (1 hour used)
        assertFalse(AppUsageLimitManager.isLimitExceeded(mahfilPkg, 60 * 60 * 1000L))

        // At limit (2 hours used)
        assertTrue(AppUsageLimitManager.isLimitExceeded(mahfilPkg, limit))

        // Over limit (2 hours 1 sec used)
        assertTrue(AppUsageLimitManager.isLimitExceeded(mahfilPkg, limit + 1000L))

        val gptPkg = "com.openai.chatgpt"
        val gptLimit = AppUsageLimitManager.getLimitMillis(gptPkg)

        // Under limit (89 minutes used)
        assertFalse(AppUsageLimitManager.isLimitExceeded(gptPkg, 89 * 60 * 1000L))

        // Over limit (91 minutes used)
        assertTrue(AppUsageLimitManager.isLimitExceeded(gptPkg, 91 * 60 * 1000L))
    }
}
