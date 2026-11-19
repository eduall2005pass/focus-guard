package com.focusguard.data.repository

import android.content.Context
import android.util.Log
import com.focusguard.data.model.AppCategory
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class ApprovedAppEntry(
    val packageName: String,
    val displayName: String,
    val category: AppCategory,
    val isPredefined: Boolean = false,
    val isCustomAdded: Boolean = false,
    val isEssential: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val expectedSigningCertificateSha256: String? = null,
    val isCurrentlyInstalled: Boolean = false,
    val note: String? = null
)

class ApprovedAppRegistry(private val context: Context) {

    private val registryFile = File(context.filesDir, "approved_app_registry.json")

    companion object {
        private const val TAG = "FocusGuard_Registry"
    }

    /**
     * Initializes the approved application registry with pre-approved packages
     * if the registry does not already exist.
     */
    fun initializeRegistryIfNotExists(): Int {
        if (registryFile.exists()) {
            val count = loadRegistry().size
            Log.i(TAG, "Approved app registry already exists with $count packages.")
            return count
        }

        val predefinedEntries = listOf(
            // Communication
            ApprovedAppEntry("com.whatsapp", "WhatsApp", AppCategory.COMMUNICATION, isPredefined = true, note = "Communities guarded"),
            ApprovedAppEntry("com.whatsapp.w4b", "WhatsApp Business", AppCategory.COMMUNICATION, isPredefined = true, note = "Communities guarded"),
            ApprovedAppEntry("com.brilliant.connect.com.bd", "Brilliant Connect", AppCategory.COMMUNICATION, isPredefined = true, note = "IP Telephony & Communication"),

            // Banking & Payments
            ApprovedAppEntry("com.ibbl.ibbl_ismart", "iSmart (IBBL)", AppCategory.BANKING_PAYMENTS, isPredefined = true),
            ApprovedAppEntry("com.ibbl.ismart", "iSmart", AppCategory.BANKING_PAYMENTS, isPredefined = true),
            ApprovedAppEntry("com.ibbl.cellfin", "Cellfin", AppCategory.BANKING_PAYMENTS, isPredefined = true),
            ApprovedAppEntry("com.bKash.customerapp", "bKash", AppCategory.BANKING_PAYMENTS, isPredefined = true),
            ApprovedAppEntry("com.konasl.nagad", "Nagad", AppCategory.BANKING_PAYMENTS, isPredefined = true),
            ApprovedAppEntry("com.dbbl.mbs.apps.main", "Rocket", AppCategory.BANKING_PAYMENTS, isPredefined = true),
            ApprovedAppEntry("bd.com.ucb.upay", "Upay", AppCategory.BANKING_PAYMENTS, isPredefined = true),

            // Transportation
            ApprovedAppEntry("com.google.android.apps.maps", "Google Maps", AppCategory.TRANSPORTATION, isPredefined = true),
            ApprovedAppEntry("bd.gov.dmtcl.rapidpass", "Rapid Pass", AppCategory.TRANSPORTATION, isPredefined = true),
            ApprovedAppEntry("bd.gov.rapidpass", "Rapid Pass Portal", AppCategory.TRANSPORTATION, isPredefined = true),

            // AI & Study
            ApprovedAppEntry("com.openai.chatgpt", "ChatGPT", AppCategory.AI_STUDY, isPredefined = true),
            ApprovedAppEntry("com.anthropic.claude", "Claude", AppCategory.AI_STUDY, isPredefined = true),
            ApprovedAppEntry("com.google.android.apps.bard", "Gemini", AppCategory.AI_STUDY, isPredefined = true),
            ApprovedAppEntry("com.google.android.apps.notebooklm", "NotebookLM", AppCategory.AI_STUDY, isPredefined = true),
            ApprovedAppEntry("com.google.android.apps.pdfviewer", "Google PDF Viewer", AppCategory.AI_STUDY, isPredefined = true),
            ApprovedAppEntry("com.adobe.reader", "Adobe Acrobat", AppCategory.AI_STUDY, isPredefined = true),
            ApprovedAppEntry("com.samsung.android.app.notes", "Samsung Notes", AppCategory.AI_STUDY, isPredefined = true),

            // Google Productivity
            ApprovedAppEntry("com.google.android.apps.docs", "Google Drive", AppCategory.GOOGLE_PRODUCTIVITY, isPredefined = true),
            ApprovedAppEntry("com.google.android.apps.docs.editors.docs", "Google Docs", AppCategory.GOOGLE_PRODUCTIVITY, isPredefined = true),
            ApprovedAppEntry("com.google.android.apps.docs.editors.sheets", "Google Sheets", AppCategory.GOOGLE_PRODUCTIVITY, isPredefined = true),
            ApprovedAppEntry("com.google.android.apps.photos", "Google Photos", AppCategory.GOOGLE_PRODUCTIVITY, isPredefined = true),

            // Essential System Applications
            ApprovedAppEntry("com.google.android.webview", "Android System WebView", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.google.android.gms", "Google Play Services", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.google.android.gsf", "Google Services Framework", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.android.vending", "Google Play Store", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.google.android.dialer", "Phone (Google)", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.android.dialer", "Phone (AOSP)", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.samsung.android.dialer", "Phone (Samsung)", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.google.android.apps.messaging", "Messages (Google)", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.samsung.android.messaging", "Messages (Samsung)", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.google.android.contacts", "Contacts", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.samsung.android.app.contacts", "Contacts (Samsung)", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.google.android.GoogleCamera", "Camera (Google)", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.sec.android.app.camera", "Camera (Samsung)", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.google.android.apps.nbu.files", "Files by Google", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.sec.android.app.myfiles", "My Files (Samsung)", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.google.android.deskclock", "Clock", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.sec.android.app.clockpackage", "Clock (Samsung)", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.google.android.calculator", "Calculator", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.sec.android.app.popupcalculator", "Calculator (Samsung)", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.google.android.inputmethod.latin", "Gboard Keyboard", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.samsung.android.honeyboard", "Samsung Keyboard", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.touchtype.swiftkey", "SwiftKey", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.android.providers.downloads", "Download Manager", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.sec.android.app.samsungapps.una2", "Samsung Update Agent", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.wssyncmldm", "Samsung FOTA Update", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.sec.android.soagent", "Samsung OS Update Agent", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.samsung.android.app.omcagent", "Samsung Carrier Agent", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.google.android.modulemetadata", "Google Play System Updates", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true),
            ApprovedAppEntry("com.focusguard", "Focus Guard DPC", AppCategory.ESSENTIAL_SYSTEM, isPredefined = true, isEssential = true)
        )

        saveRegistry(predefinedEntries)
        Log.i(TAG, "Initialized Approved App Registry with ${predefinedEntries.size} packages.")
        return predefinedEntries.size
    }

    /**
     * Loads all entries in the approved registry.
     */
    fun loadRegistry(): Map<String, ApprovedAppEntry> {
        if (!registryFile.exists()) {
            initializeRegistryIfNotExists()
        }
        val result = mutableMapOf<String, ApprovedAppEntry>()
        try {
            val jsonString = registryFile.readText()
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val catStr = obj.optString("category", AppCategory.CUSTOM_ALLOWED.name)
                val category = try {
                    AppCategory.valueOf(catStr)
                } catch (e: Exception) {
                    AppCategory.CUSTOM_ALLOWED
                }

                val entry = ApprovedAppEntry(
                    packageName = obj.getString("packageName"),
                    displayName = obj.getString("displayName"),
                    category = category,
                    isPredefined = obj.optBoolean("isPredefined", false),
                    isCustomAdded = obj.optBoolean("isCustomAdded", false),
                    isEssential = obj.optBoolean("isEssential", false),
                    addedTimestamp = obj.optLong("addedTimestamp", System.currentTimeMillis()),
                    expectedSigningCertificateSha256 = if (obj.isNull("expectedSigningCertificateSha256")) null else obj.getString("expectedSigningCertificateSha256"),
                    isCurrentlyInstalled = obj.optBoolean("isCurrentlyInstalled", false),
                    note = if (obj.isNull("note")) null else obj.getString("note")
                )
                result[entry.packageName] = entry
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading approved app registry", e)
        }
        return result
    }

    fun isApproved(packageName: String): Boolean {
        return loadRegistry().containsKey(packageName)
    }

    fun getApprovedPackages(): Set<String> {
        return loadRegistry().keys
    }

    /**
     * Adds an application to the persistent approved registry.
     */
    fun addApprovedPackage(
        packageName: String,
        displayName: String? = null,
        category: AppCategory = AppCategory.CUSTOM_ALLOWED,
        certSha256: String? = null
    ): Boolean {
        val trimmed = packageName.trim()
        if (trimmed.isEmpty()) return false

        val currentMap = loadRegistry().toMutableMap()
        val finalName = displayName ?: trimmed
        val entry = ApprovedAppEntry(
            packageName = trimmed,
            displayName = finalName,
            category = category,
            isCustomAdded = true,
            expectedSigningCertificateSha256 = certSha256,
            isCurrentlyInstalled = true
        )
        currentMap[trimmed] = entry
        saveRegistry(currentMap.values.toList())
        Log.i(TAG, "Permanently approved package $trimmed in registry.")
        return true
    }

    /**
     * Removes an administrator-added package from the registry.
     * Essential packages cannot be removed.
     */
    fun removeApprovedPackage(packageName: String): Boolean {
        val currentMap = loadRegistry().toMutableMap()
        val entry = currentMap[packageName] ?: return false
        if (entry.isEssential) {
            Log.w(TAG, "Cannot remove essential package $packageName from registry.")
            return false
        }
        currentMap.remove(packageName)
        saveRegistry(currentMap.values.toList())
        Log.i(TAG, "Removed package $packageName from approved registry.")
        return true
    }

    /**
     * Updates the installed state of an approved package.
     * Crucial: When an app is uninstalled, its record remains in the registry!
     * Only isCurrentlyInstalled is toggled to false, so it remains reinstallable at any time.
     */
    fun markInstalledState(packageName: String, isInstalled: Boolean, certSha256: String? = null) {
        val currentMap = loadRegistry().toMutableMap()
        val entry = currentMap[packageName] ?: return
        val updated = entry.copy(
            isCurrentlyInstalled = isInstalled,
            expectedSigningCertificateSha256 = certSha256 ?: entry.expectedSigningCertificateSha256
        )
        currentMap[packageName] = updated
        saveRegistry(currentMap.values.toList())
        Log.i(TAG, "Updated installed status for $packageName: isInstalled=$isInstalled")
    }

    private fun saveRegistry(entries: List<ApprovedAppEntry>) {
        val array = JSONArray()
        for (entry in entries) {
            val obj = JSONObject().apply {
                put("packageName", entry.packageName)
                put("displayName", entry.displayName)
                put("category", entry.category.name)
                put("isPredefined", entry.isPredefined)
                put("isCustomAdded", entry.isCustomAdded)
                put("isEssential", entry.isEssential)
                put("addedTimestamp", entry.addedTimestamp)
                put("expectedSigningCertificateSha256", entry.expectedSigningCertificateSha256)
                put("isCurrentlyInstalled", entry.isCurrentlyInstalled)
                put("note", entry.note)
            }
            array.put(obj)
        }
        registryFile.writeText(array.toString(2))
    }
}
