package com.focusguard.data.security

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class SecurityPinManagerTest {

    private class FakeEditor(private val storage: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val tempStorage = mutableMapOf<String, Any?>()

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) tempStorage[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = this
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = this
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = this
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = this
        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) tempStorage.remove(key)
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            tempStorage.clear()
            return this
        }
        override fun commit(): Boolean {
            storage.putAll(tempStorage)
            return true
        }
        override fun apply() {
            storage.putAll(tempStorage)
        }
    }

    private class FakeSharedPreferences : SharedPreferences {
        val storage = mutableMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = storage
        override fun getString(key: String?, defValue: String?): String? = storage[key] as? String ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
        override fun getInt(key: String?, defValue: Int): Int = (storage[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (storage[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (storage[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = (storage[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = storage.containsKey(key)
        override fun edit(): SharedPreferences.Editor = FakeEditor(storage)
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    }

    private lateinit var fakePrefs: FakeSharedPreferences

    @Before
    fun setUp() {
        fakePrefs = FakeSharedPreferences()
        SecurityPinManager.lockSession()
    }

    @Test
    fun testPinLifecycle() {
        val manager = SecurityPinManager(fakePrefs)

        // 1. Initially no PIN is set
        assertFalse("Pin must not be set initially", manager.isPinSet())
        assertFalse("Session must not be unlocked initially", SecurityPinManager.isSessionUnlocked())

        // 2. Reject invalid PINs
        assertFalse("Reject too short PIN", manager.setPin("123"))
        assertFalse("Reject non-numeric PIN", manager.setPin("abcd"))
        assertFalse("Reject too long PIN", manager.setPin("123456789"))

        // 3. Set valid PIN
        assertTrue("Set valid 4-digit PIN", manager.setPin("1234"))
        assertTrue("Pin must now be set", manager.isPinSet())
        assertTrue("Session should be unlocked after setting PIN", SecurityPinManager.isSessionUnlocked())

        // Lock session for verification test
        SecurityPinManager.lockSession()
        assertFalse(SecurityPinManager.isSessionUnlocked())

        // 4. Verify correct and wrong PINs
        assertFalse("Wrong PIN must fail", manager.verifyPin("9999"))
        assertFalse("Session still locked after failed attempt", SecurityPinManager.isSessionUnlocked())

        assertTrue("Correct PIN must succeed", manager.verifyPin("1234"))
        assertTrue("Session must be unlocked after correct PIN", SecurityPinManager.isSessionUnlocked())

        // 5. Change PIN
        assertFalse("Change PIN fails with wrong old PIN", manager.changePin("0000", "5678"))
        assertTrue("Change PIN succeeds with correct old PIN", manager.changePin("1234", "5678"))

        assertFalse("Old PIN no longer works", manager.verifyPin("1234"))
        assertTrue("New PIN works", manager.verifyPin("5678"))
    }
}
