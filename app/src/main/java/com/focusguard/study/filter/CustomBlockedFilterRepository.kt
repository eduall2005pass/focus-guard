package com.focusguard.study.filter

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

sealed class AddFilterResult {
    data class Success(val message: String, val totalCount: Int) : AddFilterResult()
    data class AlreadyExists(val message: String) : AddFilterResult()
    data class Error(val message: String) : AddFilterResult()
}

/**
 * Manages append-only blocked keywords and domains added by the administrator.
 *
 * Privacy & Trigger-Free Design:
 * - NEVER exposes the raw list of blocked keywords or domains to the UI.
 * - Only exposes counts (e.g. total blocked keywords, total blocked domains).
 * - Detects duplicates and informs the user if an item is already added.
 * - Has no remove or clear operation: accepted additions remain blocked across app restarts.
 * - Persists state in app-private custom_blocked_filters.json.
 * - Automatically registers custom filters with FocusInputFilter and StudyNavigationGuard.
 */
class CustomBlockedFilterRepository(
    private val context: Context? = null,
    private val customStorageDir: File? = null
) {

    private val storageFile = File(
        customStorageDir ?: context?.filesDir ?: File("."),
        "custom_blocked_filters.json"
    )

    private val customKeywords = mutableSetOf<String>()
    private val customDomains = mutableSetOf<String>()

    companion object {
        private const val TAG = "FocusGuard_CustomFilter"
    }

    init {
        loadFilters()
        syncToEngine()
    }

    @Synchronized
    fun addKeyword(rawKeyword: String): AddFilterResult {
        val trimmed = rawKeyword.trim()
        if (trimmed.isEmpty()) {
            return AddFilterResult.Error("কি-ওয়ার্ড ফাঁকা রাখা যাবে না (Keyword cannot be empty)")
        }
        if (trimmed.length < 3) {
            return AddFilterResult.Error("কি-ওয়ার্ড কমপক্ষে ৩ অক্ষরের হতে হবে (Keyword must be at least 3 characters)")
        }
        if (trimmed.all { it == 'x' }) {
            return AddFilterResult.Error("অকার্যকর কি-ওয়ার্ড (Invalid keyword)")
        }

        val lower = trimmed.lowercase()
        val normalized = FocusInputFilter.inputNorm(lower)

        // 1. Check if already in predefined list or blocked by filter engine
        val alreadyInPredefined = FocusInputFilter.BLOCKED_INPUT_WORDS.any {
            it.equals(lower, ignoreCase = true) || FocusInputFilter.inputNorm(it) == normalized
        } || FocusInputFilter.isBlocked(lower)
        if (alreadyInPredefined) {
            return AddFilterResult.AlreadyExists("এই কি-ওয়ার্ডটি ইতিমধ্যে তালিকায় যুক্ত রয়েছে (Already added)")
        }

        // 2. Check if already in custom list
        val alreadyInCustom = customKeywords.any {
            it.equals(lower, ignoreCase = true) || FocusInputFilter.inputNorm(it) == normalized
        }
        if (alreadyInCustom) {
            return AddFilterResult.AlreadyExists("এই কি-ওয়ার্ডটি ইতিমধ্যে তালিকায় যুক্ত রয়েছে (Already added)")
        }

        // 3. Add to custom list & persist
        customKeywords.add(lower)
        if (!saveFilters()) {
            customKeywords.remove(lower)
            syncToEngine()
            return AddFilterResult.Error("স্থায়ীভাবে সংরক্ষণ করা যায়নি; keyword blocklist-এ যোগ হয়নি")
        }
        FocusInputFilter.addCustomKeyword(lower)
        Log.i(TAG, "Added new custom blocked keyword. Total custom: ${customKeywords.size}")

        return AddFilterResult.Success(
            "কি-ওয়ার্ড সফলভাবে ব্লকলিস্টে যুক্ত করা হয়েছে",
            getTotalKeywordCount()
        )
    }

    @Synchronized
    fun addDomain(rawDomainOrUrl: String): AddFilterResult {
        val trimmed = rawDomainOrUrl.trim()
        if (trimmed.isEmpty()) {
            return AddFilterResult.Error("ডোমেইন বা ওয়েবসাইট ফাঁকা রাখা যাবে না (Domain cannot be empty)")
        }

        // Clean domain: strip scheme, path, query parameters, port
        var cleanDomain = trimmed.lowercase()
        cleanDomain = cleanDomain.removePrefix("https://").removePrefix("http://")
        cleanDomain = cleanDomain.split("/")[0].split("?")[0].split("#")[0].split(":")[0].trim()

        if (cleanDomain.isEmpty()) {
            return AddFilterResult.Error("সঠিক ডোমেইন বা ওয়েবসাইটের ঠিকানা দিন (Invalid domain)")
        }

        // 1. Check predefined blocked hosts
        if (StudyNavigationGuard.STUDY_BLOCKED_HOSTS.contains(cleanDomain) ||
            StudyNavigationGuard.CATEGORICAL_BLOCKED_HOSTS.contains(cleanDomain)) {
            return AddFilterResult.AlreadyExists("এই ওয়েবসাইট/ডোমেইনটি ইতিমধ্যে ব্লকলিস্টে যুক্ত রয়েছে (Already added)")
        }

        // 2. Check predefined wildcards
        val matchesWildcard = StudyNavigationGuard.STUDY_BLOCKED_WILDCARDS.any { pattern ->
            val base = pattern.removePrefix("*.")
            cleanDomain == base || cleanDomain.endsWith(".$base")
        }
        if (matchesWildcard) {
            return AddFilterResult.AlreadyExists("এই ওয়েবসাইট/ডোমেইনটি ইতিমধ্যে ব্লকলিস্টে যুক্ত রয়েছে (Already added)")
        }

        // 3. Check custom domains
        if (customDomains.contains(cleanDomain)) {
            return AddFilterResult.AlreadyExists("এই ওয়েবসাইট/ডোমেইনটি ইতিমধ্যে ব্লকলিস্টে যুক্ত রয়েছে (Already added)")
        }

        // 4. Add to custom list & persist
        customDomains.add(cleanDomain)
        if (!saveFilters()) {
            customDomains.remove(cleanDomain)
            syncToEngine()
            return AddFilterResult.Error("স্থায়ীভাবে সংরক্ষণ করা যায়নি; domain blocklist-এ যোগ হয়নি")
        }
        StudyNavigationGuard.addCustomDomain(cleanDomain)
        Log.i(TAG, "Added new custom blocked domain: $cleanDomain. Total custom: ${customDomains.size}")

        return AddFilterResult.Success(
            "ডোমেইন সফলভাবে ব্লকলিস্টে যুক্ত করা হয়েছে",
            getTotalDomainCount()
        )
    }

    @Synchronized
    fun getTotalKeywordCount(): Int {
        return FocusInputFilter.BLOCKED_INPUT_WORDS.size + customKeywords.size
    }

    @Synchronized
    fun getTotalDomainCount(): Int {
        return StudyNavigationGuard.STUDY_BLOCKED_HOSTS.size + customDomains.size
    }

    @Synchronized
    fun getCustomKeywordCount(): Int {
        return customKeywords.size
    }

    @Synchronized
    fun getCustomDomainCount(): Int {
        return customDomains.size
    }

    // Internal access for engine synchronization (NEVER exposed to user UI)
    @Synchronized
    internal fun getCustomKeywords(): Set<String> {
        return customKeywords.toSet()
    }

    @Synchronized
    internal fun getCustomDomains(): Set<String> {
        return customDomains.toSet()
    }

    private fun syncToEngine() {
        FocusInputFilter.setCustomKeywords(customKeywords)
        StudyNavigationGuard.setCustomBlockedHosts(customDomains)
    }

    private fun loadFilters() {
        if (!storageFile.exists()) return
        try {
            val content = storageFile.readText()
            val json = JSONObject(content)

            customKeywords.clear()
            val kwArray = json.optJSONArray("keywords")
            var hadInvalid = false
            if (kwArray != null) {
                for (i in 0 until kwArray.length()) {
                    val kw = kwArray.optString(i, "").trim().lowercase()
                    if (kw.length >= 3 && !kw.all { it == 'x' }) {
                        customKeywords.add(kw)
                    } else if (kw.isNotEmpty()) {
                        hadInvalid = true
                    }
                }
            }
            if (hadInvalid) {
                saveFilters()
            }

            customDomains.clear()
            val dmArray = json.optJSONArray("domains")
            if (dmArray != null) {
                for (i in 0 until dmArray.length()) {
                    val dm = dmArray.optString(i, "").trim().lowercase()
                    if (dm.isNotEmpty()) customDomains.add(dm)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load custom blocked filters: ${e.message}", e)
        }
    }

    private fun saveFilters(): Boolean {
        try {
            storageFile.parentFile?.mkdirs()
            val json = JSONObject()

            val kwArray = JSONArray()
            for (kw in customKeywords) {
                kwArray.put(kw)
            }
            json.put("keywords", kwArray)

            val dmArray = JSONArray()
            for (dm in customDomains) {
                dmArray.put(dm)
            }
            json.put("domains", dmArray)

            val tempFile = File(storageFile.parentFile ?: File("."), "custom_blocked_filters.json.tmp")
            tempFile.writeText(json.toString(2))
            if (!tempFile.renameTo(storageFile)) {
                storageFile.writeText(json.toString(2))
                tempFile.delete()
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom blocked filters: ${e.message}", e)
            return false
        }
    }
}
