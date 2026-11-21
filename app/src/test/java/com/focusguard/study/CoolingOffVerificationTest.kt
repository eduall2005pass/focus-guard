package com.focusguard.study

import com.focusguard.study.filter.CoolingOffEvent
import com.focusguard.study.filter.StudyCoolingOffManager
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CoolingOffVerificationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testInitialState() {
        val manager = StudyCoolingOffManager(customStorageDir = tempFolder.root)
        assertFalse(manager.isCoolingOffActive())
        assertEquals(0L, manager.getRemainingMillis())
        assertEquals(0, manager.getRecentAttemptsCount())
    }

    @Test
    fun testWarningsBelowThreshold() {
        val manager = StudyCoolingOffManager(customStorageDir = tempFolder.root)

        for (i in 1..9) {
            val event = manager.recordViolation("test_violation_$i")
            assertTrue("Violation $i must be a Warning", event is CoolingOffEvent.Warning)
            val warning = event as CoolingOffEvent.Warning
            assertEquals(i, warning.currentAttempts)
            assertEquals(10 - i, warning.remainingUntilLock)
            assertFalse(manager.isCoolingOffActive())
        }
        assertEquals(9, manager.getRecentAttemptsCount())
    }

    @Test
    fun testTriggerAtTenthAttempt() {
        val manager = StudyCoolingOffManager(customStorageDir = tempFolder.root)

        for (i in 1..9) {
            manager.recordViolation("violation_$i")
        }

        val event10 = manager.recordViolation("violation_10")
        assertTrue("10th violation must trigger lockout", event10 is CoolingOffEvent.Triggered)
        val triggered = event10 as CoolingOffEvent.Triggered
        assertEquals(60, triggered.durationMinutes) // 1 hour (60 minutes) emergency restricted mode
        assertTrue(manager.isCoolingOffActive())
        assertTrue(manager.getRemainingMillis() > 58 * 60 * 1000L)
        assertTrue(manager.getLockoutReason().contains("১০ মিনিটে ১০ বার"))
        assertTrue(manager.getLockoutReason().contains("১ ঘণ্টার জন্য শুধুমাত্র ফোন, এসএমএস, ম্যাপস, নোটস, ফাইল ম্যানেজার"))
    }

    @Test
    fun testRepeatTriggerEscalationToOneHour() {
        val manager = StudyCoolingOffManager(customStorageDir = tempFolder.root)

        // 1st trigger -> 30 mins
        for (i in 1..10) {
            manager.recordViolation("v_$i")
        }
        assertTrue(manager.isCoolingOffActive())

        // Admin clears for testing
        manager.adminClearLockout()
        assertFalse(manager.isCoolingOffActive())

        // 2nd trigger on same day -> escalates to 60 mins (1 hour)
        for (i in 1..9) {
            manager.recordViolation("v2_$i")
        }
        val secondEvent = manager.recordViolation("v2_10")
        assertTrue(secondEvent is CoolingOffEvent.Triggered)
        val secondTriggered = secondEvent as CoolingOffEvent.Triggered
        assertEquals(60, secondTriggered.durationMinutes) // 1 hour escalation
        assertTrue(manager.isCoolingOffActive())
        assertTrue(manager.getRemainingMillis() > 58 * 60 * 1000L)
    }

    @Test
    fun testPersistenceAcrossInstances() {
        val dir = tempFolder.root
        val manager1 = StudyCoolingOffManager(customStorageDir = dir)

        for (i in 1..10) {
            manager1.recordViolation("v_$i")
        }
        assertTrue(manager1.isCoolingOffActive())

        // Second instance reading the same directory (simulating app restart / reboot)
        val manager2 = StudyCoolingOffManager(customStorageDir = dir)
        assertTrue("Active lockout must survive restart", manager2.isCoolingOffActive())
        assertTrue(manager2.getRemainingMillis() > 25 * 60 * 1000L)

        // Admin unlock
        manager2.adminClearLockout()
        assertFalse(manager2.isCoolingOffActive())

        // Third instance after unlock
        val manager3 = StudyCoolingOffManager(customStorageDir = dir)
        assertFalse(manager3.isCoolingOffActive())
    }
}
