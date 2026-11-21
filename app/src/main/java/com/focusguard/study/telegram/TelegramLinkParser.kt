package com.focusguard.study.telegram

import java.net.URI

data class TelegramDestinationInfo(
    val rawUrl: String,
    val targetIdentifier: String, // e.g. "pdf258upload", "-123456789", or invite code
    val webkPeerId: String?,      // Normalized Web K peer ID (-xxxx for channel/group)
    val username: String?,        // Clean username without @
    val postId: Int? = null,
    val inviteHash: String? = null,
    val peerType: String? = null,
    val suggestedTitle: String,
    val webkRouteHash: String     // Hash to load in Web K, e.g. "#@username" or "#-123456789"
)

object TelegramLinkParser {

    private val TELEGRAM_HOSTS = setOf(
        "t.me", "telegram.me", "telegram.dog", "web.telegram.org"
    )

    fun isTelegramUrl(rawUrl: String?): Boolean {
        if (rawUrl.isNullOrBlank()) return false
        val clean = rawUrl.trim()
        if (clean.startsWith("tg://", ignoreCase = true)) return true

        return try {
            val uri = URI(if (clean.contains("://")) clean else "https://$clean")
            val host = (uri.host ?: "").lowercase()
            TELEGRAM_HOSTS.contains(host) || host.endsWith(".telegram.org")
        } catch (_: Exception) {
            clean.contains("t.me/") || clean.contains("telegram.me/")
        }
    }

    /**
     * Parses a Telegram link into a TelegramDestinationInfo object.
     */
    fun parseDestination(rawUrl: String): TelegramDestinationInfo? {
        if (!isTelegramUrl(rawUrl)) return null
        val clean = rawUrl.trim()

        try {
            // Case 1: tg:// scheme
            if (clean.startsWith("tg://", ignoreCase = true)) {
                return parseTgScheme(clean)
            }

            val uri = URI(if (clean.contains("://")) clean else "https://$clean")
            val host = (uri.host ?: "").lowercase()
            val path = (uri.path ?: "").trim('/')
            val fragment = (uri.fragment ?: "").trim('#')

            // Case 2: web.telegram.org
            if (host == "web.telegram.org" || host.endsWith(".web.telegram.org")) {
                if (fragment.isNotEmpty()) {
                    val cleanFrag = fragment.removePrefix("/im?p=").removePrefix("/im")
                    if (cleanFrag.startsWith("@")) {
                        val user = cleanFrag.removePrefix("@")
                        return TelegramDestinationInfo(
                            rawUrl = clean,
                            targetIdentifier = user,
                            webkPeerId = null,
                            username = user,
                            suggestedTitle = "Telegram: @$user",
                            webkRouteHash = "#@$user"
                        )
                    } else if (cleanFrag.startsWith("-") || cleanFrag.matches(Regex("\\d+"))) {
                        val peerNum = if (cleanFrag.startsWith("-100")) cleanFrag.substring(4) else cleanFrag.removePrefix("-")
                        val webkId = "-$peerNum"
                        return TelegramDestinationInfo(
                            rawUrl = clean,
                            targetIdentifier = webkId,
                            webkPeerId = webkId,
                            username = null,
                            suggestedTitle = "Telegram Channel ($webkId)",
                            webkRouteHash = "#$webkId"
                        )
                    }
                }
            }

            // Case 3: t.me/c/123456789/456 (private channel post)
            if (path.startsWith("c/")) {
                val parts = path.removePrefix("c/").split('/')
                val rawChannelId = parts.getOrNull(0) ?: ""
                val postId = parts.getOrNull(1)?.toIntOrNull()
                val webkId = "-$rawChannelId"
                val routeHash = if (postId != null) "#/im?p=$webkId&post=$postId" else "#$webkId"
                return TelegramDestinationInfo(
                    rawUrl = clean,
                    targetIdentifier = webkId,
                    webkPeerId = webkId,
                    username = null,
                    postId = postId,
                    suggestedTitle = "Study Channel ($webkId)",
                    webkRouteHash = routeHash
                )
            }

            // Case 4: t.me/+hash or t.me/joinchat/hash (invite links)
            if (path.startsWith("+") || path.startsWith("joinchat/")) {
                val invite = if (path.startsWith("+")) path.substring(1) else path.removePrefix("joinchat/")
                return TelegramDestinationInfo(
                    rawUrl = clean,
                    targetIdentifier = invite,
                    webkPeerId = null,
                    username = null,
                    inviteHash = invite,
                    suggestedTitle = "Telegram Group (Invite: $invite)",
                    webkRouteHash = "#?tgaddr=" + java.net.URLEncoder.encode("tg://join?invite=$invite", "UTF-8")
                )
            }

            // Case 5: t.me/username or t.me/username/123
            if (path.isNotEmpty()) {
                val parts = path.split('/')
                val user = parts[0].removePrefix("@")
                val postId = parts.getOrNull(1)?.toIntOrNull()
                val routeHash = if (postId != null) "#/im?p=@$user&post=$postId" else "#@$user"
                return TelegramDestinationInfo(
                    rawUrl = clean,
                    targetIdentifier = user,
                    webkPeerId = null,
                    username = user,
                    postId = postId,
                    suggestedTitle = "Study Resource (@$user)",
                    webkRouteHash = routeHash
                )
            }
        } catch (_: Exception) {
            // Fallback: extraction failed
        }
        return null
    }

    private fun parseTgScheme(tgUrl: String): TelegramDestinationInfo? {
        val clean = tgUrl.trim()
        val withoutScheme = clean.removePrefix("tg://").removePrefix("tg:")

        if (withoutScheme.startsWith("resolve")) {
            val query = withoutScheme.substringAfter("?", "")
            var domain: String? = null
            var post: Int? = null
            for (param in query.split("&")) {
                val kv = param.split("=")
                if (kv.size == 2) {
                    if (kv[0] == "domain") domain = kv[1]
                    if (kv[0] == "post") post = kv[1].toIntOrNull()
                }
            }
            if (domain != null) {
                val route = if (post != null) "#/im?p=@$domain&post=$post" else "#@$domain"
                return TelegramDestinationInfo(
                    rawUrl = tgUrl,
                    targetIdentifier = domain,
                    webkPeerId = null,
                    username = domain,
                    postId = post,
                    suggestedTitle = "Study Resource (@$domain)",
                    webkRouteHash = route
                )
            }
        } else if (withoutScheme.startsWith("join")) {
            val invite = withoutScheme.substringAfter("invite=", "").substringBefore("&")
            if (invite.isNotEmpty()) {
                return TelegramDestinationInfo(
                    rawUrl = tgUrl,
                    targetIdentifier = invite,
                    webkPeerId = null,
                    username = null,
                    inviteHash = invite,
                    suggestedTitle = "Telegram Group (Invite: $invite)",
                    webkRouteHash = "#?tgaddr=" + java.net.URLEncoder.encode(tgUrl, "UTF-8")
                )
            }
        }
        return null
    }
}
