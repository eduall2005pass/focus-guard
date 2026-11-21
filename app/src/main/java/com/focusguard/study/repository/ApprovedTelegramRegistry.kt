package com.focusguard.study.repository

import android.content.Context
import android.util.Log
import com.focusguard.study.model.ApprovedTelegramDestination
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ApprovedTelegramRegistry(
    private val context: Context? = null,
    private val customStorageDir: File? = null
) {

    private val registryFile = File(customStorageDir ?: context?.filesDir ?: File("."), "approved_telegram_registry.json")

    companion object {
        private const val TAG = "FocusGuard_TGRegistry"

        // Default initial educational channel/group from Focus-Control
        val DEFAULT_DESTINATION = ApprovedTelegramDestination(
            peerId = "-1004415493319",
            webkPeerId = "-4415493319",
            peerType = "channel",
            username = "pdf258upload",
            displayTitle = "PDF Materials Upload",
            sourceUrl = "https://guessthecase.eu.org/",
            approvedTimestamp = 1732000000000L,
            approvedStatus = "APPROVED",
            isProtected = true
        )
    }

    /**
     * Converts a raw identifier or Bot API id (-100xxxx) into Web K's peer ID (-xxxx).
     * In Telegram Web K:
     * - Users have positive numbers: `123456789`
     * - Channels / supergroups / groups have negative numbers: `-123456789` (NOT with -100 prefix)
     */
    fun normalizeToWebKPeerId(rawId: String, isChatOrChannel: Boolean = true): String {
        val clean = rawId.trim()
        require(clean.matches(Regex("-?\\d+"))) { "Telegram peer ID must be numeric" }
        val digits = clean.removePrefix("-").let {
            if (clean.startsWith("-") && it.startsWith("100") && it.length > 3) it.substring(3) else it
        }
        return if (isChatOrChannel) "-$digits" else digits
    }

    @Synchronized
    fun loadApprovedDestinations(): List<ApprovedTelegramDestination> {
        if (!registryFile.exists()) {
            saveDestinations(listOf(DEFAULT_DESTINATION))
            return listOf(DEFAULT_DESTINATION)
        }

        try {
            val content = registryFile.readText()
            val root = JSONObject(content)
            val array = root.optJSONArray("approved_chats") ?: JSONArray()
            val list = mutableListOf<ApprovedTelegramDestination>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ApprovedTelegramDestination(
                        peerId = obj.getString("peerId"),
                        webkPeerId = obj.getString("webkPeerId"),
                        peerType = obj.getString("peerType"),
                        username = obj.optString("username").takeIf { it.isNotBlank() },
                        displayTitle = obj.getString("displayTitle"),
                        sourceUrl = obj.optString("sourceUrl").takeIf { it.isNotBlank() },
                        approvedTimestamp = obj.optLong("approvedTimestamp", System.currentTimeMillis()),
                        approvedStatus = obj.optString("approvedStatus", "APPROVED"),
                        isProtected = obj.optBoolean("isProtected", true)
                    )
                )
            }

            // Ensure the default educational destination is always available
            if (list.none { it.webkPeerId == DEFAULT_DESTINATION.webkPeerId || it.username == DEFAULT_DESTINATION.username }) {
                list.add(DEFAULT_DESTINATION)
                saveDestinations(list)
            }

            return list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load approved telegram destinations: ${e.message}", e)
            return listOf(DEFAULT_DESTINATION)
        }
    }

    @Synchronized
    fun saveDestinations(destinations: List<ApprovedTelegramDestination>) {
        try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("updatedAt", System.currentTimeMillis())

            val array = JSONArray()
            for (dest in destinations) {
                val obj = JSONObject()
                obj.put("peerId", dest.peerId)
                obj.put("webkPeerId", dest.webkPeerId)
                obj.put("peerType", dest.peerType)
                if (dest.username != null) obj.put("username", dest.username)
                obj.put("displayTitle", dest.displayTitle)
                if (dest.sourceUrl != null) obj.put("sourceUrl", dest.sourceUrl)
                obj.put("approvedTimestamp", dest.approvedTimestamp)
                obj.put("approvedStatus", dest.approvedStatus)
                obj.put("isProtected", dest.isProtected)
                array.put(obj)
            }
            root.put("approved_chats", array)

            val tempFile = File(registryFile.parentFile ?: File("."), "approved_telegram_registry.json.tmp")
            tempFile.writeText(root.toString(2))
            if (tempFile.renameTo(registryFile)) {
                Log.i(TAG, "Persisted ${destinations.size} approved Telegram destinations.")
            } else {
                registryFile.writeText(root.toString(2))
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save approved telegram destinations: ${e.message}", e)
        }
    }

    /**
     * Checks if a destination target (by webkPeerId, peerId, or username) is approved.
     */
    fun isApproved(target: String?): Boolean {
        if (target.isNullOrBlank()) return false
        val clean = target.trim().lowercase().removePrefix("@").removePrefix("#")
        val numeric = clean.matches(Regex("-?\\d+"))
        val cleanWebK = if (numeric) normalizeToWebKPeerId(clean) else null
        val destinations = loadApprovedDestinations()

        return destinations.any { dest ->
            dest.approvedStatus == "APPROVED" && (
                (cleanWebK != null && dest.webkPeerId == cleanWebK) ||
                dest.peerId.equals(clean, ignoreCase = true) ||
                dest.webkPeerId == clean ||
                (dest.username != null && dest.username.equals(clean, ignoreCase = true))
            )
        }
    }

    /**
     * Resolves an approved destination by target identifier or username.
     */
    fun findApprovedDestination(target: String?): ApprovedTelegramDestination? {
        if (target.isNullOrBlank()) return null
        val clean = target.trim().lowercase().removePrefix("@").removePrefix("#")
        val numeric = clean.matches(Regex("-?\\d+"))
        val cleanWebK = if (numeric) normalizeToWebKPeerId(clean) else null
        val destinations = loadApprovedDestinations()

        return destinations.find { dest ->
            dest.approvedStatus == "APPROVED" && (
                (cleanWebK != null && dest.webkPeerId == cleanWebK) ||
                dest.peerId.equals(clean, ignoreCase = true) ||
                dest.webkPeerId == clean ||
                (dest.username != null && dest.username.equals(clean, ignoreCase = true))
            )
        }
    }

    /**
     * Approves and records a new Telegram educational channel/group.
     */
    @Synchronized
    fun approveDestination(
        rawPeerId: String,
        peerType: String,
        displayTitle: String,
        username: String? = null,
        sourceUrl: String? = null
    ): ApprovedTelegramDestination {
        val normalizedType = peerType.trim().lowercase()
        require(normalizedType in setOf("channel", "supergroup", "group", "user")) { "Unsupported Telegram peer type" }
        val webkId = normalizeToWebKPeerId(rawPeerId, isChatOrChannel = normalizedType != "user")
        val cleanUsername = username?.trim()?.removePrefix("@")?.lowercase()
        val currentList = loadApprovedDestinations().toMutableList()

        // Check for existing record
        val existingIndex = currentList.indexOfFirst {
            (it.webkPeerId == webkId && it.peerType == normalizedType) ||
                (cleanUsername != null && it.username == cleanUsername)
        }

        val entry = ApprovedTelegramDestination(
            peerId = rawPeerId.trim(),
            webkPeerId = webkId,
            peerType = normalizedType,
            username = cleanUsername,
            displayTitle = displayTitle.trim(),
            sourceUrl = sourceUrl?.trim(),
            approvedTimestamp = System.currentTimeMillis(),
            approvedStatus = "APPROVED",
            isProtected = true
        )

        if (existingIndex >= 0) {
            currentList[existingIndex] = entry
        } else {
            currentList.add(entry)
        }

        saveDestinations(currentList)
        return entry
    }

    @Synchronized
    fun revokeDestination(webkPeerId: String): Boolean {
        val currentList = loadApprovedDestinations().toMutableList()
        val item = currentList.find { it.webkPeerId == webkPeerId } ?: return false
        if (item == DEFAULT_DESTINATION) {
            // Cannot remove the default educational group
            return false
        }
        currentList.remove(item)
        saveDestinations(currentList)
        return true
    }
}
