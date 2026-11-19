package com.focusguard.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.focusguard.data.model.BaselineAppRecord
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class BaselineInventoryManager(private val context: Context) {

    private val baselineFile = File(context.filesDir, "baseline_inventory.json")

    companion object {
        private const val TAG = "FocusGuard_Baseline"
    }

    /**
     * Initializes the baseline inventory during initial provisioning.
     * Records package names, version codes, system status, install sources,
     * and cryptographic signing certificate fingerprints.
     */
    fun createBaselineIfNotExists(): Int {
        if (baselineFile.exists()) {
            val existing = loadBaseline()
            Log.i(TAG, "Baseline inventory already exists with ${existing.size} packages.")
            return existing.size
        }
        return forceRefreshBaseline()
    }

    /**
     * Scans and stores all currently installed packages as the baseline.
     */
    fun forceRefreshBaseline(): Int {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_META_DATA
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES or PackageManager.GET_META_DATA
        }

        val installedPackages = pm.getInstalledPackages(flags)
        val baselineList = mutableListOf<BaselineAppRecord>()

        for (pkg in installedPackages) {
            val pkgName = pkg.packageName
            val isSystem = (pkg.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
            val certSha256 = extractSigningCertificateSha256(pkg)
            val installer = getInstallingPackage(pkgName)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkg.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                pkg.versionCode.toLong()
            }

            baselineList.add(
                BaselineAppRecord(
                    packageName = pkgName,
                    initialVersionCode = versionCode,
                    initialVersionName = pkg.versionName ?: "unknown",
                    isSystemApp = isSystem,
                    installingPackageName = installer,
                    signingCertificateSha256 = certSha256
                )
            )
        }

        saveBaseline(baselineList)
        Log.i(TAG, "Successfully created cryptographic baseline of ${baselineList.size} packages.")
        return baselineList.size
    }

    /**
     * Loads the baseline inventory from protected local storage.
     */
    fun loadBaseline(): Map<String, BaselineAppRecord> {
        if (!baselineFile.exists()) return emptyMap()
        val result = mutableMapOf<String, BaselineAppRecord>()
        try {
            val jsonString = baselineFile.readText()
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val record = BaselineAppRecord(
                    packageName = obj.getString("packageName"),
                    initialVersionCode = obj.getLong("initialVersionCode"),
                    initialVersionName = obj.optString("initialVersionName", "unknown"),
                    isSystemApp = obj.getBoolean("isSystemApp"),
                    installingPackageName = if (obj.isNull("installingPackageName")) null else obj.getString("installingPackageName"),
                    signingCertificateSha256 = obj.getString("signingCertificateSha256"),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
                result[record.packageName] = record
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read baseline inventory", e)
        }
        return result
    }

    /**
     * Validates whether a package event is a legitimate update to an existing baseline app.
     * Validates that:
     * 1. Package exists in the baseline.
     * 2. The new APK's signing certificate matches the baseline certificate SHA-256.
     */
    fun isLegitimateBaselineUpdate(packageName: String): Boolean {
        val baseline = loadBaseline()
        val baselineRecord = baseline[packageName] ?: return false

        val currentCert = getInstalledCertificateSha256(packageName)
        if (currentCert.isEmpty() || baselineRecord.signingCertificateSha256.isEmpty()) {
            return false
        }

        val certMatches = baselineRecord.signingCertificateSha256.equals(currentCert, ignoreCase = true)
        if (!certMatches) {
            Log.w(TAG, "Certificate mismatch for update to $packageName! Baseline: ${baselineRecord.signingCertificateSha256}, New: $currentCert")
        }
        return certMatches
    }

    /**
     * Checks if a package is in the baseline inventory.
     */
    fun isInBaseline(packageName: String): Boolean {
        return loadBaseline().containsKey(packageName)
    }

    private fun saveBaseline(records: List<BaselineAppRecord>) {
        val jsonArray = JSONArray()
        for (record in records) {
            val obj = JSONObject().apply {
                put("packageName", record.packageName)
                put("initialVersionCode", record.initialVersionCode)
                put("initialVersionName", record.initialVersionName)
                put("isSystemApp", record.isSystemApp)
                put("installingPackageName", record.installingPackageName)
                put("signingCertificateSha256", record.signingCertificateSha256)
                put("timestamp", record.timestamp)
            }
            jsonArray.put(obj)
        }
        baselineFile.writeText(jsonArray.toString(2))
    }

    fun extractSigningCertificateSha256(packageInfo: PackageInfo): String {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }
        if (signatures.isNullOrEmpty()) return ""

        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(signatures[0].toByteArray())
            digest.joinToString("") { "%02X".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating certificate hash for ${packageInfo.packageName}", e)
            ""
        }
    }

    fun getInstalledCertificateSha256(packageName: String): String {
        return try {
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
            val pkgInfo = context.packageManager.getPackageInfo(packageName, flags)
            extractSigningCertificateSha256(pkgInfo)
        } catch (e: Exception) {
            ""
        }
    }

    fun getInstallingPackage(packageName: String): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(packageName)
            }
        } catch (e: Exception) {
            null
        }
    }
}
