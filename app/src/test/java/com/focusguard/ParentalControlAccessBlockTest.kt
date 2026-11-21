package com.focusguard

import com.focusguard.dpc.DevicePolicyController
import org.junit.Assert.*
import org.junit.Test

class ParentalControlAccessBlockTest {

    @Test
    fun testParentalCarePackageConstant() {
        assertEquals("com.samsung.android.app.parentalcare", DevicePolicyController.PARENTAL_CARE_PACKAGE)
    }

    @Test
    fun testParentalCareNotCriticalSystemPackage() {
        assertFalse(
            "Parental care package must not be in CRITICAL_SYSTEM_PACKAGES to allow suspension",
            DevicePolicyController.CRITICAL_SYSTEM_PACKAGES.contains(DevicePolicyController.PARENTAL_CARE_PACKAGE)
        )
    }

    @Test
    fun testParentalCareNotEmergencyAllowedPackage() {
        assertFalse(
            "Parental care package must not be in EMERGENCY_ALLOWED_PACKAGES",
            DevicePolicyController.EMERGENCY_ALLOWED_PACKAGES.contains(DevicePolicyController.PARENTAL_CARE_PACKAGE)
        )
    }

    @Test
    fun testSecurityPreferenceKeysConstants() {
        assertEquals("key_parental_control_blocked", DevicePolicyController.KEY_PARENTAL_CONTROL_BLOCKED)
        assertEquals("key_factory_reset_blocked", DevicePolicyController.KEY_FACTORY_RESET_BLOCKED)
        assertEquals("key_adb_debugging_blocked", DevicePolicyController.KEY_ADB_DEBUGGING_BLOCKED)
    }
}
