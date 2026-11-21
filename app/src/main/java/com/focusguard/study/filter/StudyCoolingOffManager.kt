package com.focusguard.study.filter

import android.content.Context
import android.util.Log
import com.focusguard.dpc.DevicePolicyController
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

sealed class CoolingOffEvent {
    data class Warning(val currentAttempts: Int, val remainingUntilLock: Int) : CoolingOffEvent()
    data class Triggered(val lockoutUntilMillis: Long, val durationMinutes: Int) : CoolingOffEvent()
}

/**
 * Manages the sliding-window violation tracker and cooling-off lockout.
 *
 * Rule:
 * - Window: 10 minutes (600,000 ms)
 * - Threshold: 10 explicit attempts
 * - Consequence: 1-hour Emergency Restricted Mode (instead of complete lockout)
 * - Permitted apps during 1 hour: Phone, SMS, Maps, Notes, and File Manager ONLY
 * - All other distraction and AI apps suspended via Device Policy Manager
 * - Non-dismissible in-app countdown with spiritual & calming reminders
 * - Persisted across app restarts and phone reboots
 */
class StudyCoolingOffManager(
    private val context: Context? = null,
    private val customStorageDir: File? = null
) {

    private val storageFile = File(
        customStorageDir ?: context?.filesDir ?: File("."),
        "study_cooling_off.json"
    )

    companion object {
        private const val TAG = "FocusGuard_CoolingOff"

        const val WINDOW_MILLIS = 10 * 60 * 1000L       // 10 minutes sliding window
        const val THRESHOLD_ATTEMPTS = 10                // 10 violations threshold
        const val EMERGENCY_RESTRICTION_MINUTES = 60    // 1 hour
        const val BASE_LOCKOUT_MINUTES = 60              // 1 hour
        const val ESCALATED_LOCKOUT_MINUTES = 60         // 1 hour
    }

    private val violationTimestamps = mutableListOf<Long>()
    private var lockoutUntil: Long = 0L
    private var lockoutReason: String = ""
    private var triggerCountToday: Int = 0
    private var lastTriggerDay: Long = 0L
    private var totalViolationsCount: Int = 0

    init {
        loadState()
    }

    @Synchronized
    fun isCoolingOffActive(): Boolean {
        pruneExpired()
        return System.currentTimeMillis() < lockoutUntil
    }

    @Synchronized
    fun getRemainingMillis(): Long {
        val now = System.currentTimeMillis()
        return if (now < lockoutUntil) lockoutUntil - now else 0L
    }

    @Synchronized
    fun getLockoutReason(): String {
        return lockoutReason
    }

    @Synchronized
    fun getRecentAttemptsCount(): Int {
        pruneExpired()
        return violationTimestamps.size
    }

    @Synchronized
    fun getTotalViolationsCount(): Int {
        return totalViolationsCount
    }

    @Synchronized
    fun getRemainingUntilLock(): Int {
        pruneExpired()
        return (THRESHOLD_ATTEMPTS - violationTimestamps.size).coerceAtLeast(0)
    }

    /**
     * Records an explicit detection attempt.
     * Evaluates the 10-minute sliding window and triggers 1-hour Emergency Restricted Mode if >= 10 attempts occur.
     */
    @Synchronized
    fun recordViolation(detectedWord: String): CoolingOffEvent {
        val now = System.currentTimeMillis()
        pruneExpired()

        // If already in emergency restricted mode, keep active
        if (now < lockoutUntil) {
            val remainingMin = ((lockoutUntil - now) / 60000L).toInt() + 1
            return CoolingOffEvent.Triggered(lockoutUntil, remainingMin)
        }

        violationTimestamps.add(now)
        totalViolationsCount++

        if (violationTimestamps.size >= THRESHOLD_ATTEMPTS) {
            val durationMinutes = EMERGENCY_RESTRICTION_MINUTES // 1 hour

            lockoutUntil = now + (durationMinutes * 60 * 1000L)
            lockoutReason = "১০ মিনিটে ১০ বার আপত্তিকর শব্দ শনাক্ত হওয়ায় পরবর্তী ১ ঘণ্টার জন্য শুধুমাত্র ফোন, এসএমএস, ম্যাপস, নোটস, ফাইল ম্যানেজার, মিউজিক ও ইসলামিক অ্যাপস ব্যবহারের অনুমতি দেওয়া হয়েছে।"
            violationTimestamps.clear()
            saveState()

            // Enforce Emergency Restricted Mode via Device Owner (Phone, SMS, Maps, Notes, Files, Music, Quran, Hadith)
            // No complete phone lockout; selective restriction active for 1 hour.
            context?.let { ctx ->
                try {
                    val dpc = DevicePolicyController(ctx)
                    dpc.enforceEmergencyRestrictedPolicy(true)
                } catch (e: Exception) {
                    Log.w(TAG, "Emergency policy enforcement notice: ${e.message}")
                }
            }

            Log.w(TAG, "Emergency Restricted Mode triggered: active for $durationMinutes minutes until $lockoutUntil")
            return CoolingOffEvent.Triggered(lockoutUntil, durationMinutes)
        } else {
            saveState()
            val remaining = THRESHOLD_ATTEMPTS - violationTimestamps.size
            return CoolingOffEvent.Warning(violationTimestamps.size, remaining)
        }
    }

    /**
     * Allows Admin to clear the lockout using secure device credentials in an emergency.
     */
    @Synchronized
    fun adminClearLockout() {
        val wasActive = System.currentTimeMillis() < lockoutUntil
        lockoutUntil = 0L
        lockoutReason = ""
        violationTimestamps.clear()
        saveState()

        if (wasActive) {
            context?.let { ctx ->
                try {
                    val dpc = DevicePolicyController(ctx)
                    dpc.enforceEmergencyRestrictedPolicy(false)
                } catch (e: Exception) {
                    Log.w(TAG, "Error reverting emergency policy: ${e.message}")
                }
            }
        }
        Log.i(TAG, "Admin cleared emergency cooling-off lockout.")
    }

    @Synchronized
    fun forceTriggerLockout(durationMinutes: Int = EMERGENCY_RESTRICTION_MINUTES) {
        val now = System.currentTimeMillis()
        lockoutUntil = now + (durationMinutes * 60 * 1000L)
        lockoutReason = "১০ মিনিটে ১০ বার আপত্তিকর শব্দ শনাক্ত হওয়ায় পরবর্তী $durationMinutes মিনিটের জন্য শুধুমাত্র ফোন, এসএমএস, ম্যাপস, নোটস এবং ফাইল ম্যানেজার ব্যবহারের অনুমতি দেওয়া হয়েছে।"
        violationTimestamps.clear()
        saveState()
        context?.let { ctx ->
            try {
                DevicePolicyController(ctx).enforceEmergencyRestrictedPolicy(true)
            } catch (_: Exception) {}
        }
    }

    private fun pruneExpired() {
        val now = System.currentTimeMillis()
        violationTimestamps.removeAll { now - it > WINDOW_MILLIS }
        if (now >= lockoutUntil && lockoutUntil != 0L) {
            lockoutUntil = 0L
            lockoutReason = ""
            saveState()
            context?.let { ctx ->
                try {
                    val dpc = DevicePolicyController(ctx)
                    dpc.enforceEmergencyRestrictedPolicy(false)
                } catch (e: Exception) {
                    Log.w(TAG, "Error reverting emergency policy after expiry: ${e.message}")
                }
            }
        }
    }

    private fun loadState() {
        if (!storageFile.exists()) return
        try {
            val content = storageFile.readText()
            val json = JSONObject(content)
            lockoutUntil = json.optLong("lockoutUntil", 0L)
            lockoutReason = json.optString("lockoutReason", "")
            triggerCountToday = json.optInt("triggerCountToday", 0)
            lastTriggerDay = json.optLong("lastTriggerDay", 0L)
            totalViolationsCount = json.optInt("totalViolationsCount", 0)

            val array = json.optJSONArray("violations")
            violationTimestamps.clear()
            if (array != null) {
                for (i in 0 until array.length()) {
                    violationTimestamps.add(array.getLong(i))
                }
            }
            pruneExpired()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cooling-off state: ${e.message}", e)
        }
    }

    private fun saveState() {
        try {
            val json = JSONObject()
            json.put("lockoutUntil", lockoutUntil)
            json.put("lockoutReason", lockoutReason)
            json.put("triggerCountToday", triggerCountToday)
            json.put("lastTriggerDay", lastTriggerDay)
            json.put("totalViolationsCount", totalViolationsCount)

            val array = JSONArray()
            for (ts in violationTimestamps) {
                array.put(ts)
            }
            json.put("violations", array)

            val tempFile = File(storageFile.parentFile ?: File("."), "study_cooling_off.json.tmp")
            tempFile.writeText(json.toString(2))
            if (!tempFile.renameTo(storageFile)) {
                storageFile.writeText(json.toString(2))
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cooling-off state: ${e.message}", e)
        }
    }
}
