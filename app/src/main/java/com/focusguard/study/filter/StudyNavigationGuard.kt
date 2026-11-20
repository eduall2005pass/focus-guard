package com.focusguard.study.filter

import java.net.URI
import java.net.URLDecoder
import java.util.regex.Pattern

/**
 * Ported directly from Focus-Control's `_is_nav_permitted`, `_host_blocked`,
 * `_STUDY_BLOCKED_HOSTS`, and `_STUDY_BLOCKED_WILDCARDS`.
 *
 * Enforces:
 * - General educational study sites allowed (GTC, Booimate, NotebookLM, etc.)
 * - Subresources, CDNs, fonts, and authentication APIs needed by study sites allowed
 * - Search engines, social media platforms, adult content, and general unapproved browsing blocked
 * - Video playback allowed for approved embeds/sites
 * - Pinned Telegram navigation allowed
 */
object StudyNavigationGuard {

    val STUDY_BLOCKED_HOSTS: Set<String> = setOf(
        "google.com", "www.google.com", "docs.google.com", "drive.google.com",
        "sheets.google.com", "slides.google.com",
        "translate.google.com", "translate.googleapis.com",
        "accounts.youtube.com", "youtube.com", "www.youtube.com", "m.youtube.com",
        "music.youtube.com", "youtu.be", "youtubei.googleapis.com", "i.ytimg.com",
        "youtube-nocookie.com", "www.youtube-nocookie.com",
        // Search engines and regional / lightweight front doors
        "bing.com", "www.bing.com", "cn.bing.com", "duckduckgo.com",
        "www.duckduckgo.com", "html.duckduckgo.com", "search.yahoo.com",
        "yahoo.com", "www.yahoo.com", "yandex.com", "www.yandex.com",
        "yandex.ru", "search.brave.com", "ecosia.org", "www.ecosia.org",
        "qwant.com", "www.qwant.com", "startpage.com", "www.startpage.com",
        "search.naver.com", "search.daum.net", "baidu.com", "www.baidu.com",
        "ask.com", "www.ask.com", "aol.com", "search.aol.com",
        // Additional international and AI search providers
        "sogou.com", "www.sogou.com", "sogou.net", "seznam.cz", "www.seznam.cz",
        "search.seznam.cz", "kagi.com", "www.kagi.com", "mojeek.com", "www.mojeek.com",
        "swisscows.com", "www.swisscows.com", "metager.de", "www.metager.de",
        "yep.com", "www.yep.com", "you.com", "www.you.com", "phind.com",
        "www.phind.com", "perplexity.ai", "www.perplexity.ai", "komo.ai",
        "www.komo.ai", "andisearch.com", "www.andisearch.com", "360.cn",
        "www.360.cn", "so.com", "www.so.com", "search.nifty.com",
        // Social networks, messaging feeds, short-video platforms
        "facebook.com", "www.facebook.com", "m.facebook.com", "fb.com",
        "instagram.com", "www.instagram.com", "threads.net", "www.threads.net",
        "twitter.com", "www.twitter.com", "x.com", "www.x.com", "t.co",
        "tiktok.com", "www.tiktok.com", "vm.tiktok.com", "reddit.com",
        "www.reddit.com", "old.reddit.com", "redd.it", "snapchat.com",
        "www.snapchat.com", "pinterest.com", "www.pinterest.com", "pin.it",
        "linkedin.com", "www.linkedin.com", "discord.com", "www.discord.com",
        "discord.gg", "web.whatsapp.com", "whatsapp.com", "www.whatsapp.com",
        "whatsapp.net", "twitch.tv", "www.twitch.tv", "tumblr.com", "www.tumblr.com",
        "mastodon.social", "bsky.app", "www.bsky.app", "vk.com", "www.vk.com", "ok.ru",
        "wechat.com", "weibo.com", "www.weibo.com", "quora.com", "www.quora.com",
        "medium.com", "www.medium.com", "threads.com", "kooapp.com",
        // Adult-content domains
        "pornhub.com", "www.pornhub.com", "xvideos.com", "www.xvideos.com",
        "xnxx.com", "www.xnxx.com", "xhamster.com", "www.xhamster.com",
        "redtube.com", "www.redtube.com", "youporn.com", "www.youporn.com",
        "youjizz.com", "www.youjizz.com", "eporner.com", "www.eporner.com",
        "spankbang.com", "www.spankbang.com", "tube8.com", "www.tube8.com",
        "chaturbate.com", "www.chaturbate.com", "onlyfans.com", "www.onlyfans.com",
        "xvideos2.com", "xhamsterlive.com", "brazzers.com", "www.brazzers.com",
        "rule34.xxx", "nhentai.net", "www.nhentai.net",
        "tipsnetbd.com", "www.tipsnetbd.com", "viralbd.com", "www.viralbd.com",
        "sabitabhabi.com", "www.sabitabhabi.com"
    )

    val STUDY_BLOCKED_WILDCARDS: List<String> = listOf(
        "*.youtube.com", "*.youtu.be", "*.googlevideo.com", "*.youtube-nocookie.com",
        "*.bing.com", "*.duckduckgo.com", "*.yahoo.com", "*.yandex.com",
        "*.yandex.ru", "*.ecosia.org", "*.qwant.com", "*.startpage.com",
        "*.naver.com", "*.daum.net", "*.baidu.com", "*.brave.com",
        "*.sogou.com", "*.sogou.net", "*.seznam.cz", "*.kagi.com",
        "*.mojeek.com", "*.swisscows.com", "*.metager.de", "*.yep.com",
        "*.you.com", "*.phind.com", "*.perplexity.ai", "*.komo.ai",
        "*.andisearch.com", "*.360.cn", "*.so.com", "*.nifty.com",
        "*.facebook.com", "*.instagram.com", "*.threads.net", "*.twitter.com",
        "*.x.com", "*.tiktok.com", "*.reddit.com", "*.snapchat.com",
        "*.pinterest.com", "*.linkedin.com", "*.discord.com", "*.whatsapp.com",
        "*.whatsapp.net", "*.twitch.tv", "*.tumblr.com", "*.mastodon.social",
        "*.bsky.app", "*.vk.com", "*.ok.ru", "*.wechat.com", "*.weibo.com",
        "*.quora.com", "*.medium.com", "*.kooapp.com", "*.pornhub.com",
        "*.xvideos.com", "*.xnxx.com", "*.xhamster.com", "*.redtube.com",
        "*.youporn.com", "*.youjizz.com", "*.eporner.com", "*.spankbang.com",
        "*.tube8.com", "*.chaturbate.com", "*.onlyfans.com", "*.brazzers.com",
        "*.nhentai.net", "*.tipsnetbd.com", "*.viralbd.com", "*.sabitabhabi.com"
    )

    // Allowed supporting domains for Web CDN, font assets, and cloud workers
    val BUILTIN_SUPPORTING_DOMAINS: Set<String> = setOf(
        // Google Fonts & Web Typography
        "fonts.googleapis.com", "*.fonts.googleapis.com",
        "fonts.gstatic.com", "*.fonts.gstatic.com",
        "gstatic.com", "*.gstatic.com", "ssl.gstatic.com",

        // Cloudflare CDN, Workers & Turnstile Verification
        "cdnjs.cloudflare.com", "*.cloudflare.com", "cloudflare.com",
        "cloudflareinsights.com", "*.cloudflareinsights.com",
        "challenges.cloudflare.com", "*.challenges.cloudflare.com",
        "*.workers.dev", "workers.dev",

        // jsDelivr CDN & Bootstrap Assets
        "cdn.jsdelivr.net", "*.jsdelivr.net", "jsdelivr.net",
        "*.bootstrapcdn.com", "stackpath.bootstrapcdn.com",
        "cdn.plyr.io", "*.plyr.io",

        // Amazon CloudFront & AWS S3
        "*.cloudfront.net", "cloudfront.net",
        "*.amazonaws.com", "*.s3.amazonaws.com",

        // Supabase Backend & Database
        "*.supabase.co", "supabase.co",
        "*.supabase.in", "supabase.in",

        // Firebase Auth & Cloud Firestore
        "*.firebaseapp.com", "firebaseapp.com",
        "*.firebaseio.com", "firebaseio.com",

        // Standard Auth SSO Providers (Google, Apple, Microsoft, GitHub)
        "appleid.apple.com", "*.appleid.apple.com",
        "login.microsoftonline.com", "*.login.microsoftonline.com",
        "login.live.com", "*.login.live.com",
        "github.com", "*.github.com",

        // Supporting Web CDN, Image Hosts & Asset Libraries
        "unpkg.com", "*.unpkg.com",
        "*.b-cdn.net", "*.digitaloceanspaces.com",
        "i.imgur.com", "*.imgur.com",

        // Google APIs & Direct Drive Downloads (PDF, Videos, Documents)
        "googleusercontent.com", "*.googleusercontent.com",
        "usercontent.google.com", "*.usercontent.google.com",
        "apis.google.com", "accounts.google.com", "clients6.google.com",
        "notebooklm-pa.googleapis.com", "content-push.googleapis.com", "csp.withgoogle.com",
        "alkali-coba-pa.googleapis.com", "alkali-pa.googleapis.com", "alkalicoba-pa.googleapis.com",
        "generativelanguage.googleapis.com", "*.generativelanguage.googleapis.com",
        "proactivebackend-pa.googleapis.com", "*.proactivebackend-pa.googleapis.com",
        "bard.google.com", "*.bard.google.com",
        "myaccount.google.com", "*.myaccount.google.com",
        "ogs.google.com", "*.ogs.google.com",
        "ogads-pa.googleapis.com", "*.ogads-pa.googleapis.com",
        "googleapis.com", "*.googleapis.com",
        "drive.usercontent.google.com", "*.drive.usercontent.google.com",
        "docs.googleusercontent.com", "*.docs.googleusercontent.com"
    )

    // Downloadable educational file extensions
    val DOWNLOAD_EXTENSIONS: Set<String> = setOf(
        // Documents & eBooks
        "pdf", "epub", "mobi", "djvu", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "rtf", "odt", "csv",
        // Compressed archives
        "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso", "apk",
        // Audio & Video lectures
        "mp4", "mkv", "avi", "mov", "webm", "mp3", "m4a", "wav", "aac", "flac", "opus", "ogg",
        // Medical imaging & scientific data
        "dcm", "nii", "tsv", "json", "xml"
    )

    // Categorically forbidden domains: search engines, social media feeds, adult content
    val CATEGORICAL_BLOCKED_HOSTS: Set<String> = setOf(
        "bing.com", "duckduckgo.com", "yahoo.com", "yandex.com", "yandex.ru", "ecosia.org",
        "qwant.com", "startpage.com", "naver.com", "daum.net", "baidu.com", "brave.com",
        "sogou.com", "sogou.net", "seznam.cz", "kagi.com", "mojeek.com", "swisscows.com",
        "metager.de", "yep.com", "you.com", "phind.com", "perplexity.ai", "komo.ai",
        "andisearch.com", "360.cn", "so.com", "nifty.com",
        "facebook.com", "instagram.com", "threads.net", "twitter.com", "x.com", "t.co",
        "tiktok.com", "reddit.com", "redd.it", "snapchat.com", "pinterest.com", "pin.it",
        "linkedin.com", "discord.com", "discord.gg", "whatsapp.com", "whatsapp.net",
        "twitch.tv", "tumblr.com", "mastodon.social", "bsky.app", "vk.com", "ok.ru",
        "wechat.com", "weibo.com", "quora.com", "medium.com", "kooapp.com",
        "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "redtube.com",
        "youporn.com", "youjizz.com", "eporner.com", "spankbang.com", "tube8.com",
        "chaturbate.com", "onlyfans.com", "brazzers.com", "rule34.xxx", "nhentai.net",
        "tipsnetbd.com", "viralbd.com", "sabitabhabi.com"
    )

    // Pattern for Google service check
    private val GOOGLE_SEARCH_PATTERN = Pattern.compile("(?:^|\\.)google\\.[a-z]{2,}(?:\\.[a-z]{2,})?$")
    private val ADULT_LABEL_PATTERN = Pattern.compile("(?:^|[.-])(adult|porn|porno|xxx|sex|hentai|nsfw|nude)(?:[.-]|$)")

    private val GOOGLE_SERVICE_EXCEPTIONS = setOf(
        "notebooklm.google.com", "notebook.google.com", "notebook.google",
        "gemini.google.com", "bard.google.com",
        "accounts.google.com", "myaccount.google.com", "ogs.google.com",
        "clients6.google.com", "apis.google.com",
        "drive.google.com", "docs.google.com", "drive.usercontent.google.com",
        "docs.googleusercontent.com"
    )

    private val customBlockedHosts = mutableSetOf<String>()

    @Synchronized
    internal fun setCustomBlockedHosts(hosts: Set<String>) {
        customBlockedHosts.clear()
        customBlockedHosts.addAll(hosts.map { it.trim().lowercase() }.filter { it.isNotEmpty() })
    }

    @Synchronized
    internal fun addCustomDomain(domain: String) {
        val clean = domain.trim().lowercase()
        if (clean.isNotEmpty()) {
            customBlockedHosts.add(clean)
        }
    }

    @Synchronized
    internal fun clearCustomDomains() {
        customBlockedHosts.clear()
    }

    fun isHostBlocked(hostRaw: String?): Boolean {
        if (hostRaw.isNullOrBlank()) return false
        val host = hostRaw.trim().lowercase().split(":")[0]
        if (host.isEmpty()) return false

        if (STUDY_BLOCKED_HOSTS.contains(host)) {
            return true
        }

        synchronized(this) {
            if (customBlockedHosts.contains(host)) {
                return true
            }
            for (custom in customBlockedHosts) {
                if (custom.startsWith("*.")) {
                    val base = custom.substring(1)
                    if (host.endsWith(base)) return true
                } else if (host == custom || host.endsWith(".$custom")) {
                    return true
                }
            }
        }

        // Check Google country search domains while preserving NotebookLM, sign-in, and Drive download endpoints
        if (!GOOGLE_SERVICE_EXCEPTIONS.contains(host) &&
            !host.endsWith(".googleusercontent.com") &&
            !host.endsWith(".usercontent.google.com") &&
            GOOGLE_SEARCH_PATTERN.matcher(host).find()) {
            return true
        }

        // Block any search domain
        val parts = host.split(".")
        if (parts.contains("search") || parts.contains("websearch")) {
            return true
        }

        // Catch obvious adult labels in domain
        if (ADULT_LABEL_PATTERN.matcher(host).find()) {
            return true
        }

        // Check wildcards
        for (pattern in STUDY_BLOCKED_WILDCARDS) {
            val base = pattern.substring(1) // e.g. ".youtube.com"
            if (host.endsWith(base) && host != pattern.substring(2)) {
                return true
            }
        }

        return false
    }

    fun isYoutubeHost(hostRaw: String?): Boolean {
        if (hostRaw.isNullOrBlank()) return false
        val host = hostRaw.trim().lowercase().split(":")[0]
        return host == "youtu.be" || host.endsWith(".youtu.be") ||
               host == "youtube.com" || host.endsWith(".youtube.com") ||
               host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com") ||
               host == "ytimg.com" || host.endsWith(".ytimg.com") ||
               host == "googlevideo.com" || host.endsWith(".googlevideo.com")
    }

    fun parseUriHostAndPath(rawUrl: String): Pair<String?, String?> {
        return try {
            val uri = URI(rawUrl)
            Pair(uri.host?.lowercase(), uri.path)
        } catch (_: Exception) {
            // Regex fallback if URI constructor rejects special characters in path/query
            val match = Regex("^https?://([^/?#]+)([^?#]*)").find(rawUrl)
            if (match != null) {
                Pair(match.groupValues[1].lowercase().split(":")[0], match.groupValues[2])
            } else {
                Pair(null, null)
            }
        }
    }


    fun matchesHostPattern(host: String, pattern: String): Boolean {
        val p = pattern.trim().lowercase()
        val h = host.trim().lowercase()
        if (p == h) return true
        if (p.startsWith("*.")) {
            val suffix = p.substring(1) // .domain.com
            return h.endsWith(suffix)
        }
        if (h.endsWith(".$p")) return true
        return false
    }

    fun isCategoricallyForbidden(host: String): Boolean {
        val h = host.trim().lowercase().split(":")[0]
        if (h.isEmpty()) return false

        // Check adult domain regex
        if (ADULT_LABEL_PATTERN.matcher(h).find()) return true

        // Check search engine keywords in domain
        val parts = h.split(".")
        if (parts.contains("search") || parts.contains("websearch")) return true
        if (GOOGLE_SEARCH_PATTERN.matcher(h).find() &&
            !GOOGLE_SERVICE_EXCEPTIONS.contains(h) &&
            !h.endsWith(".googleusercontent.com") &&
            !h.endsWith(".usercontent.google.com")) {
            return true
        }

        // Check against known search engines, social networks, and adult platforms
        for (blocked in CATEGORICAL_BLOCKED_HOSTS) {
            if (h == blocked || h.endsWith(".$blocked")) return true
        }

        return false
    }

    private fun isGoogleDriveDirectDownload(host: String, path: String?, uLower: String): Boolean {
        val isDriveOrDocs = host == "drive.google.com" || host.endsWith(".drive.google.com") ||
                host == "docs.google.com" || host.endsWith(".docs.google.com") ||
                host == "drive.usercontent.google.com" || host.endsWith(".drive.usercontent.google.com") ||
                host.endsWith(".googleusercontent.com") || host.endsWith(".usercontent.google.com")

        if (!isDriveOrDocs) return false

        val p = (path ?: "").lowercase()
        if (p.contains("/uc") || p.contains("/download") || p.contains("/view") || p.contains("/preview") || p.contains("/file/d/")) {
            return true
        }
        if (uLower.contains("export=download") || uLower.contains("id=") || uLower.contains("download=true")) {
            return true
        }
        if (uLower.contains("export=download") || uLower.contains("/download?")) return true
        return false
    }

    private fun hasDownloadQueryIndicators(uLower: String): Boolean {
        if (uLower.contains("export=download") || uLower.contains("response-content-disposition=attachment")) {
            return true
        }
        if (uLower.contains("/server/gotodl") || uLower.contains("/server/download")) {
            return true
        }
        // E.g. name=...pdf or filename=...pdf or file=...mp4
        val nameMatch = Regex("[?&](?:name|filename|file)=([^&#]+)").find(uLower)
        if (nameMatch != null) {
            val fileName = try { URLDecoder.decode(nameMatch.groupValues[1], "UTF-8") } catch (_: Exception) { nameMatch.groupValues[1] }
            val ext = fileName.substringAfterLast('.', "").lowercase()
            if (DOWNLOAD_EXTENSIONS.contains(ext)) {
                return true
            }
        }
        return false
    }

    /**
     * Determines whether a URL is a downloadable educational resource (PDF, video, audio, archive, doc).
     * Prevents search engines, social media, and adult sites from being treated as downloads.
     */
    fun isDownloadableUrl(rawUrl: String?): Boolean {
        if (rawUrl.isNullOrBlank()) return false
        val u = rawUrl.trim()
        val uLower = u.lowercase()

        // 1. Explicit content in URL is forbidden
        if (FocusInputFilter.explicitHit(uLower) != null) {
            return false
        }

        val (host, path) = parseUriHostAndPath(u)
        if (host.isNullOrEmpty()) return false

        // 2. Absolutely forbidden hosts (Adult, search engines, social media feeds)
        if (isCategoricallyForbidden(host)) {
            return false
        }

        // 3. Google Drive / Docs direct binary download endpoints
        if (isGoogleDriveDirectDownload(host, path, uLower)) {
            return true
        }

        // 4. Download query indicators (e.g. export=download, /download?, name=*.pdf, &dl=)
        if (hasDownloadQueryIndicators(uLower)) {
            return true
        }

        // 5. File extension in URL path
        val cleanPath = (path ?: "").split("?")[0].split("#")[0].trim().lowercase()
        val dotIndex = cleanPath.lastIndexOf('.')
        if (dotIndex != -1 && dotIndex < cleanPath.length - 1) {
            val ext = cleanPath.substring(dotIndex + 1)
            if (DOWNLOAD_EXTENSIONS.contains(ext)) {
                return true
            }
        }

        return false
    }

    /**
     * Determines whether a URI is permitted to be navigated to or loaded as a resource.
     * Evaluates input filter, blocked categories, allowlist, and Telegram destinations.
     */
    fun isNavPermitted(
        uriString: String?,
        approvedHosts: Set<String>,
        telegramMode: Boolean = false,
        mainFrame: Boolean = true
    ): Boolean {
        if (uriString.isNullOrBlank()) return false
        val u = uriString.trim()
        val uLower = u.lowercase()

        // Safe browser internal schemes
        if (uLower == "about:blank" || uLower == "about:srcdoc") return true
        if (uLower.startsWith("data:") || uLower.startsWith("blob:") || uLower.startsWith("file://")) return true

        // Check if explicit content keyword is contained in the URL or query parameters
        if (FocusInputFilter.explicitHit(uLower) != null) {
            return false
        }

        val (host, path) = parseUriHostAndPath(u)
        if (host.isNullOrEmpty()) return false

        if (host == "appassets.androidplatform.net") {
            val assetPath = path ?: ""
            return telegramMode && (
                assetPath.startsWith("/assets/telegram-web-k/") ||
                    assetPath.startsWith("/assets/audio/") ||
                    assetPath.startsWith("/assets/fonts/") ||
                    assetPath.startsWith("/assets/img/") ||
                    assetPath.startsWith("/assets/tgs/")
                )
        }

        // Strictly block categorical domains (Search engines, social media, adult sites)
        if (isCategoricallyForbidden(host)) {
            return false
        }

        // Categorically block YouTube unconditionally (zero exceptions)
        if (isYoutubeHost(host)) {
            return false
        }

        val directDriveDownload = isGoogleDriveDirectDownload(host, path, uLower)
        if (isHostBlocked(host) && !directDriveDownload) {
            return false
        }

        val allApproved = approvedHosts + BUILTIN_SUPPORTING_DOMAINS
        val approvedHost = allApproved.any { pattern -> matchesHostPattern(host, pattern) }

        // Direct Google Drive document endpoints are allowed only when they are
        // download routes. The general Drive UI remains outside the whitelist.
        if (directDriveDownload) return true

        // Never let a file extension or download query bypass the host allowlist.
        if (isDownloadableUrl(u)) return approvedHost

        // Telegram links are routed by the native parser. Only the official K
        // frontend may be a top-level page; other Telegram endpoints are
        // subresources and are available only while that mode is active.
        if (host == "web.telegram.org" || host.endsWith(".telegram.org") ||
            host == "t.me" || host.endsWith(".t.me") || host == "telegram.me") {
            if (!telegramMode) return false
            if (mainFrame) {
                val cleanPath = (path ?: "").trimEnd('/')
                return host == "web.telegram.org" &&
                    (cleanPath == "/k" || cleanPath.startsWith("/k/"))
            }
            return true
        }

        if (approvedHost) return true

        return false
    }
}

