package com.focusguard.data.security

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.security.SecureRandom

class SecurityPinManager(
    private val prefs: SharedPreferences
) {
    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    companion object {
        private const val PREFS_NAME = "focusguard_security_pin_prefs"
        private const val KEY_PIN_HASH = "security_pin_hash"
        private const val KEY_PIN_SALT = "security_pin_salt"
        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 8

        @Volatile
        private var isSessionUnlocked: Boolean = false

        fun unlockSession() {
            isSessionUnlocked = true
        }

        fun lockSession() {
            isSessionUnlocked = false
        }

        fun isSessionUnlocked(): Boolean = isSessionUnlocked
    }

    fun isPinSet(): Boolean {
        return prefs.contains(KEY_PIN_HASH) && !prefs.getString(KEY_PIN_HASH, null).isNullOrBlank()
    }

    fun verifyPin(inputPin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSalt = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val computedHash = hashPin(inputPin.trim(), storedSalt)
        val valid = storedHash == computedHash
        if (valid) {
            unlockSession()
        }
        return valid
    }

    fun setPin(newPin: String): Boolean {
        val trimmed = newPin.trim()
        if (trimmed.length < MIN_PIN_LENGTH || trimmed.length > MAX_PIN_LENGTH || !trimmed.all { it.isDigit() }) {
            return false
        }
        val salt = generateSalt()
        val hash = hashPin(trimmed, salt)
        val success = prefs.edit()
            .putString(KEY_PIN_HASH, hash)
            .putString(KEY_PIN_SALT, salt)
            .commit()
        if (success) {
            unlockSession()
        }
        return success
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        if (!verifyPin(oldPin)) {
            return false
        }
        return setPin(newPin)
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val input = "$salt:$pin:$salt"
        val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
