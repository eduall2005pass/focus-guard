package com.focusguard.study.repository

import android.content.Context
import android.util.Log
import com.focusguard.study.filter.FocusInputFilter
import com.focusguard.study.filter.StudyNavigationGuard
import com.focusguard.study.model.ApprovedWebsite
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URI

class StudyWebsiteRegistry(
    private val context: Context? = null,
    private val customStorageDir: File? = null
) {

    private val registryFile = File(customStorageDir ?: context?.filesDir ?: File("."), "approved_study_websites.json")

    companion object {
        private const val TAG = "FocusGuard_StudyWeb"

        val PREDEFINED_WEBSITES = listOf(
            ApprovedWebsite(
                id = "gtc",
                title = "Guess The Case (GTC)",
                url = "https://guessthecase.eu.org/",
                domain = "guessthecase.eu.org",
                iconName = "medical_services",
                isPredefined = true,
                note = "Interactive clinical & radiology case solving"
            ),
            ApprovedWebsite(
                id = "gemini",
                title = "Google Gemini",
                url = "https://gemini.google.com/",
                domain = "gemini.google.com",
                iconName = "psychology",
                isPredefined = true,
                note = "AI study assistant & learning companion"
            ),
            ApprovedWebsite(
                id = "boimate",
                title = "BoiMate",
                url = "https://boimate.com/",
                domain = "boimate.com",
                iconName = "menu_book",
                isPredefined = true,
                note = "Bangla Islamic & educational PDF book collection"
            ),
            ApprovedWebsite(
                id = "du_portal",
                title = "DU Service Portal",
                url = "https://eco.du.ac.bd/",
                domain = "eco.du.ac.bd",
                iconName = "account_balance",
                isPredefined = true,
                note = "Dhaka University Academic Services & Exam Controller Portal"
            ),
            ApprovedWebsite(
                id = "medical_contents",
                title = "Medical Contents",
                url = "https://medical-contents.abdursakib.workers.dev/0:/",
                domain = "medical-contents.abdursakib.workers.dev",
                iconName = "folder_shared",
                isPredefined = true,
                note = "Curated medical drives & lecture notes"
            ),
            ApprovedWebsite(
                id = "muslimbangla",
                title = "Muslim Bangla",
                url = "https://muslimbangla.com/",
                domain = "muslimbangla.com",
                iconName = "mosque",
                isPredefined = true,
                note = "Authentic Islamic reference & prayer timings"
            ),
            ApprovedWebsite(
                id = "ihadis",
                title = "iHadis",
                url = "https://ihadis.com/en",
                domain = "ihadis.com",
                iconName = "auto_stories",
                isPredefined = true,
                note = "Hadith database & authenticity verification"
            ),
            ApprovedWebsite(
                id = "livemcq",
                title = "LiveMCQ",
                url = "https://web.livemcq.com/",
                domain = "livemcq.com",
                iconName = "quiz",
                isPredefined = true,
                note = "Medical competitive exam preparation & MCQs"
            )
        )
    }

    @Synchronized
    fun loadWebsites(): List<ApprovedWebsite> {
        if (!registryFile.exists()) {
            saveWebsites(PREDEFINED_WEBSITES)
            return PREDEFINED_WEBSITES
        }

        try {
            val content = registryFile.readText()
            val root = JSONObject(content)
            val array = root.optJSONArray("websites") ?: JSONArray()
            val list = mutableListOf<ApprovedWebsite>()

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    ApprovedWebsite(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        url = obj.getString("url"),
                        domain = obj.getString("domain"),
                        iconName = obj.optString("iconName", "globe"),
                        isPredefined = obj.optBoolean("isPredefined", false),
                        addedTimestamp = obj.optLong("addedTimestamp", System.currentTimeMillis()),
                        note = obj.optString("note").takeIf { it.isNotBlank() }
                    )
                )
            }

            // Purge removed legacy/duplicate entries
            var modified = list.removeAll { it.id == "booimate" || it.id == "notebooklm" }

            // Ensure predefined websites remain present by ID
            for (pre in PREDEFINED_WEBSITES) {
                if (list.none { it.id == pre.id }) {
                    list.add(pre)
                    modified = true
                }
            }
            if (modified) {
                saveWebsites(list)
            }

            return list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load approved study websites: ${e.message}", e)
            return PREDEFINED_WEBSITES
        }
    }

    @Synchronized
    fun saveWebsites(websites: List<ApprovedWebsite>) {
        try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("updatedAt", System.currentTimeMillis())

            val array = JSONArray()
            for (site in websites) {
                val obj = JSONObject()
                obj.put("id", site.id)
                obj.put("title", site.title)
                obj.put("url", site.url)
                obj.put("domain", site.domain)
                obj.put("iconName", site.iconName)
                obj.put("isPredefined", site.isPredefined)
                obj.put("addedTimestamp", site.addedTimestamp)
                if (site.note != null) obj.put("note", site.note)
                array.put(obj)
            }
            root.put("websites", array)

            val tempFile = File(registryFile.parentFile ?: File("."), "approved_study_websites.json.tmp")
            tempFile.writeText(root.toString(2))
            if (tempFile.renameTo(registryFile)) {
                Log.i(TAG, "Persisted ${websites.size} approved study websites.")
            } else {
                registryFile.writeText(root.toString(2))
                tempFile.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save approved study websites: ${e.message}", e)
        }
    }

    fun getAllowedHosts(): Set<String> {
        val sites = loadWebsites()
        val hosts = mutableSetOf<String>()
        for (site in sites) {
            val d = site.domain.lowercase()
            // HARD BLOCK: Never allow any domain that is categorically forbidden or in blocklist
            if (StudyNavigationGuard.isHostBlocked(d) || StudyNavigationGuard.isCategoricallyForbidden(d)) {
                continue
            }
            hosts.add(d)
            hosts.add("*.$d")
        }
        // Gemini AI study service & Google Account Switcher
        hosts.add("gemini.google.com")
        hosts.add("*.gemini.google.com")
        hosts.add("bard.google.com")
        hosts.add("*.bard.google.com")
        hosts.add("accounts.google.com")
        hosts.add("*.accounts.google.com")
        hosts.add("myaccount.google.com")
        hosts.add("*.myaccount.google.com")
        hosts.add("ogs.google.com")
        hosts.add("*.ogs.google.com")

        // Aliases and CDN / download subdomains for Boimate and Medical Contents
        hosts.add("boimate.com")
        hosts.add("*.boimate.com")
        hosts.add("boimatebd.workers.dev")
        hosts.add("*.boimatebd.workers.dev")
        hosts.add("medical-contents.abdursakib.workers.dev")
        hosts.add("*.medical-contents.abdursakib.workers.dev")
        hosts.add("workers.dev")
        hosts.add("*.workers.dev")

        // Dhaka University Academic Services & DU subdomains
        hosts.add("du.ac.bd")
        hosts.add("*.du.ac.bd")
        hosts.add("eco.du.ac.bd")
        hosts.add("*.eco.du.ac.bd")

        // University service & fee payment gateways (Sonali Bank, bKash, SSLCommerz, Nagad, DBBL)
        hosts.add("sonalibank.com.bd")
        hosts.add("*.sonalibank.com.bd")
        hosts.add("sslcommerz.com")
        hosts.add("*.sslcommerz.com")
        hosts.add("bkash.com")
        hosts.add("*.bkash.com")
        hosts.add("nagad.com.bd")
        hosts.add("*.nagad.com.bd")
        hosts.add("dutchbanglabank.com")
        hosts.add("*.dutchbanglabank.com")
        hosts.add("shurjopay.com.bd")
        hosts.add("*.shurjopay.com.bd")
        return hosts
    }

    /**
     * Validates and adds a new educational website with Admin authorization.
     * Prevents adding blocked hosts, search engines, adult domains, or explicit keywords.
     */
    @Synchronized
    fun addWebsite(title: String, rawUrl: String, note: String? = null): Result<ApprovedWebsite> {
        val cleanTitle = title.trim()
        val cleanUrl = rawUrl.trim()

        if (cleanTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Title cannot be empty"))
        }

        val targetUrl = if (cleanUrl.contains("://")) cleanUrl else "https://$cleanUrl"
        val (host, _) = StudyNavigationGuard.parseUriHostAndPath(targetUrl)

        if (host.isNullOrEmpty()) {
            return Result.failure(IllegalArgumentException("Invalid URL or domain"))
        }

        if (FocusInputFilter.explicitHit(targetUrl) != null || FocusInputFilter.explicitHit(cleanTitle) != null) {
            return Result.failure(IllegalArgumentException("Blocked explicit terminology detected"))
        }

        if (StudyNavigationGuard.isHostBlocked(host) || StudyNavigationGuard.isCategoricallyForbidden(host)) {
            return Result.failure(IllegalArgumentException("Host '$host' is permanently blocked and cannot be approved under any condition"))
        }

        val currentList = loadWebsites().toMutableList()
        val existing = currentList.find { it.domain.equals(host, ignoreCase = true) }
        if (existing != null) {
            return Result.success(existing)
        }

        val newId = "custom_${System.currentTimeMillis()}"
        val newWebsite = ApprovedWebsite(
            id = newId,
            title = cleanTitle,
            url = targetUrl,
            domain = host,
            iconName = "school",
            isPredefined = false,
            addedTimestamp = System.currentTimeMillis(),
            note = note?.trim()
        )

        currentList.add(newWebsite)
        saveWebsites(currentList)
        return Result.success(newWebsite)
    }

    @Synchronized
    fun removeWebsite(id: String): Boolean {
        val currentList = loadWebsites().toMutableList()
        val item = currentList.find { it.id == id } ?: return false
        if (item.isPredefined) {
            // Predefined educational websites cannot be removed to prevent breaking study workflow
            return false
        }
        currentList.remove(item)
        saveWebsites(currentList)
        return true
    }
}
