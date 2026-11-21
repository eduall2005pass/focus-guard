package com.focusguard.study.ui

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.util.regex.Pattern

class StudyDownloadManager(private val context: Context) {

    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    companion object {
        private const val TAG = "FocusGuard_Download"

        // Approved document viewer packages from Focus Guard allowlist
        val APPROVED_DOCUMENT_VIEWERS = setOf(
            "com.google.android.apps.pdfviewer",
            "com.adobe.reader",
            "com.samsung.android.app.notes"
        )

        private val RFC5987_FILENAME_PATTERN = Pattern.compile("filename\\*=(?:UTF-8|utf-8)''([^;]+)")
        private val STANDARD_FILENAME_PATTERN = Pattern.compile("filename=\"?([^\";]+)\"?")

        /**
         * Resolves the most accurate and readable filename for an educational resource.
         * Correctly prioritizes RFC 5987 headers, URL query parameters (&name=...pdf),
         * path segments, and fallback guessers.
         */
        fun resolveFileName(
            url: String,
            contentDisposition: String? = null,
            mimeType: String? = null,
            preferredFileName: String? = null
        ): String {
            if (!preferredFileName.isNullOrBlank()) {
                val clean = sanitizeFileName(preferredFileName)
                return ensureExtension(clean, mimeType)
            }

            // 1. Check Content-Disposition header
            if (!contentDisposition.isNullOrBlank()) {
                val cdName = parseContentDisposition(contentDisposition)
                if (!cdName.isNullOrBlank()) {
                    val clean = sanitizeFileName(cdName)
                    return ensureExtension(clean, mimeType)
                }
            }

            // 2. Check URL query parameters (frequently used by Cloudflare Worker indexes and download bridges)
            try {
                val uri = URI(url)
                val query = uri.query
                if (!query.isNullOrBlank()) {
                    for (param in query.split("&")) {
                        val kv = param.split("=")
                        if (kv.size == 2) {
                            val key = kv[0].lowercase()
                            if (key == "name" || key == "filename" || key == "file" || key == "title") {
                                val decoded = try { URLDecoder.decode(kv[1], "UTF-8") } catch (_: Exception) { kv[1] }
                                if (decoded.isNotBlank()) {
                                    val clean = sanitizeFileName(decoded)
                                    return ensureExtension(clean, mimeType)
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            // 3. Check URL path last component
            try {
                val uri = URI(url)
                val path = uri.path
                if (!path.isNullOrBlank()) {
                    val lastSegment = path.trim('/').split('/').lastOrNull()
                    if (!lastSegment.isNullOrBlank() && lastSegment.contains(".")) {
                        val decoded = try { URLDecoder.decode(lastSegment, "UTF-8") } catch (_: Exception) { lastSegment }
                        val clean = sanitizeFileName(decoded)
                        return ensureExtension(clean, mimeType)
                    }
                }
            } catch (_: Exception) {}

            // 4. Default to URLUtil.guessFileName (with fallback for unit test environments)
            val guessed = try { URLUtil.guessFileName(url, contentDisposition, mimeType) } catch (_: Exception) { null }
            val cleanGuessed = if (guessed.isNullOrBlank()) "study_download_${System.currentTimeMillis()}" else sanitizeFileName(guessed)
            return ensureExtension(cleanGuessed, mimeType)
        }

        private fun parseContentDisposition(contentDisposition: String): String? {
            // RFC 5987 / RFC 6266 (UTF-8 encoded)
            val rfcMatcher = RFC5987_FILENAME_PATTERN.matcher(contentDisposition)
            if (rfcMatcher.find()) {
                val raw = rfcMatcher.group(1)
                return try { URLDecoder.decode(raw, "UTF-8") } catch (_: Exception) { raw }
            }
            // Standard format
            val stdMatcher = STANDARD_FILENAME_PATTERN.matcher(contentDisposition)
            if (stdMatcher.find()) {
                val raw = stdMatcher.group(1)?.trim()
                return try { URLDecoder.decode(raw ?: "", "UTF-8") } catch (_: Exception) { raw }
            }
            return null
        }

        /**
         * Sanitizes filename while strictly preserving Unicode letters (Bengali, Arabic, English, etc.),
         * diacritics/vowels (\p{M}), digits, dots, hyphens, underscores, brackets, and spaces.
         */
        fun sanitizeFileName(name: String): String {
            val decoded = try { URLDecoder.decode(name, "UTF-8") } catch (_: Exception) { name }
            val clean = decoded.replace('/', '_').replace('\\', '_')
            val safe = clean.replace(Regex("[^\\p{L}\\p{M}\\p{N}._ \\-\\[\\]()]"), "_").trim()
            return if (safe.isBlank()) "study_download_${System.currentTimeMillis()}" else safe
        }

        fun ensureExtension(fileName: String, mimeType: String?): String {
            if (fileName.contains(".") && !fileName.endsWith(".")) {
                return fileName
            }
            val ext = mimeTypeToExtension(mimeType) ?: "pdf"
            return "$fileName.$ext"
        }

        fun mimeTypeToExtension(mimeType: String?): String? {
            if (mimeType.isNullOrBlank()) return null
            val m = mimeType.lowercase().split(";")[0].trim()
            return when (m) {
                "application/pdf" -> "pdf"
                "application/epub+zip" -> "epub"
                "application/zip", "application/x-zip-compressed" -> "zip"
                "application/x-rar-compressed", "application/vnd.rar" -> "rar"
                "application/x-7z-compressed" -> "7z"
                "video/mp4" -> "mp4"
                "video/x-matroska" -> "mkv"
                "video/webm" -> "webm"
                "audio/mpeg", "audio/mp3" -> "mp3"
                "audio/mp4", "audio/m4a" -> "m4a"
                "application/msword" -> "doc"
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx"
                "application/vnd.ms-powerpoint" -> "ppt"
                "application/vnd.openxmlformats-officedocument.presentationml.presentation" -> "pptx"
                "application/vnd.ms-excel" -> "xls"
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" -> "xlsx"
                "image/jpeg" -> "jpg"
                "image/png" -> "png"
                "image/webp" -> "webp"
                "application/dicom" -> "dcm"
                else -> null
            }
        }

        /**
         * Extracts inner HTTP/HTTPS URL from 1DM / IDM / ADM intent strings.
         */
        fun extractInnerHttpUrlFromIntent(intentUrl: String): String? {
            if (!intentUrl.startsWith("intent:", ignoreCase = true)) return null
            val match = Regex("^intent:(https?://[^#]+)", RegexOption.IGNORE_CASE).find(intentUrl)
            if (match != null) {
                return match.groupValues[1]
            }
            return try {
                val parsed = Intent.parseUri(intentUrl, Intent.URI_INTENT_SCHEME)
                parsed.dataString ?: parsed.getStringExtra("browser_fallback_url")
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Starts a managed download for a study file (PDF, document, video, or lecture slide).
     */
    fun startDownload(
        url: String,
        userAgent: String? = null,
        contentDisposition: String? = null,
        mimeType: String? = null,
        contentLength: Long = -1L,
        preferredFileName: String? = null
    ): Long {
        try {
            val cleanFileName = resolveFileName(url, contentDisposition, mimeType, preferredFileName)

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle(cleanFileName)
                setDescription("Educational study resource download")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, cleanFileName)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)

                // Forward cookies for authenticated sessions (GTC, Boimate, Telegram)
                val cookies = CookieManager.getInstance().getCookie(url)
                if (!cookies.isNullOrEmpty()) {
                    addRequestHeader("Cookie", cookies)
                }
                if (!userAgent.isNullOrEmpty()) {
                    addRequestHeader("User-Agent", userAgent)
                }
                addRequestHeader("Referer", url)

                if (!mimeType.isNullOrEmpty()) {
                    setMimeType(mimeType)
                }
            }

            val downloadId = downloadManager.enqueue(request)
            Toast.makeText(context, "Downloading: $cleanFileName", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Enqueued download ID $downloadId for $cleanFileName ($contentLength bytes)")
            return downloadId
        } catch (e: Exception) {
            Log.e(TAG, "Download failed to initiate: ${e.message}", e)
            Toast.makeText(context, "Download error: ${e.message}", Toast.LENGTH_LONG).show()
            return -1L
        }
    }

    /**
     * Saves a blob or data URI downloaded in-page via JavaScript FileReader/fetch
     * directly into the public Downloads directory and registers it with DownloadManager.
     */
    fun saveBlob(
        base64Data: String,
        rawFileName: String? = null,
        mimeType: String? = null
    ): Boolean {
        try {
            val pureBase64 = if (base64Data.contains(",")) {
                base64Data.substringAfter(",")
            } else {
                base64Data
            }
            val bytes = Base64.decode(pureBase64, Base64.DEFAULT)
            val initialName = if (!rawFileName.isNullOrBlank()) {
                sanitizeFileName(rawFileName)
            } else {
                "study_document_${System.currentTimeMillis()}"
            }
            val finalName = ensureExtension(initialName, mimeType)

            val cleanMime = mimeType?.takeIf { it.isNotBlank() } ?: "application/octet-stream"
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, finalName)
                put(MediaStore.Downloads.MIME_TYPE, cleanMime)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Could not create a Downloads entry")
            try {
                resolver.openOutputStream(uri, "w")?.use { it.write(bytes) }
                    ?: throw IllegalStateException("Could not open the Downloads entry")
                val ready = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                resolver.update(uri, ready, null, null)
            } catch (error: Exception) {
                resolver.delete(uri, null, null)
                throw error
            }

            Toast.makeText(context, "Downloaded: $finalName", Toast.LENGTH_SHORT).show()
            Log.i(TAG, "Persisted blob download to Downloads/$finalName (${bytes.size} bytes)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save blob download: ${e.message}", e)
            Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
            return false
        }
    }

    /**
     * Attempts to open an educational document in an approved viewer (Adobe Acrobat, Google PDF Viewer)
     * without exposing a general-purpose browser.
     */
    fun openDownloadedFile(file: File, mimeType: String = "application/pdf"): Boolean {
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist: ${file.name}", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Find an approved viewer installed on device
            val packageManager = context.packageManager
            val activities = packageManager.queryIntentActivities(intent, 0)
            val approvedActivity = activities.find {
                APPROVED_DOCUMENT_VIEWERS.contains(it.activityInfo.packageName)
            }

            if (approvedActivity != null) {
                intent.setPackage(approvedActivity.activityInfo.packageName)
                context.startActivity(intent)
                return true
            }

            // Fallback: check if standard ACTION_VIEW can be resolved without launching unapproved browsers
            val resolved = intent.resolveActivity(packageManager)
            if (resolved != null && !isBrowserPackage(resolved.packageName)) {
                context.startActivity(intent)
                return true
            }

            Toast.makeText(
                context,
                "Downloaded to Downloads/${file.name}. Please open via Google PDF Viewer or Adobe Reader.",
                Toast.LENGTH_LONG
            ).show()
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open document: ${e.message}", e)
            Toast.makeText(context, "Unable to open document: ${e.message}", Toast.LENGTH_SHORT).show()
            return false
        }
    }

    private fun isBrowserPackage(pkg: String): Boolean {
        val lower = pkg.lowercase()
        return lower.contains("chrome") || lower.contains("firefox") || lower.contains("browser") ||
               lower.contains("opera") || lower.contains("brave") || lower.contains("edge")
    }
}
