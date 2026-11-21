package com.focusguard.study.model

/**
 * Model representing an approved educational website displayed in the Study Portal.
 */
data class ApprovedWebsite(
    val id: String,
    val title: String,
    val url: String,
    val domain: String,
    val iconName: String = "globe",
    val isPredefined: Boolean = false,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)

/**
 * Model representing a verified and approved Telegram destination (channel, supergroup, group)
 * accessible exclusively within Telegram Study Mode.
 */
data class ApprovedTelegramDestination(
    val peerId: String,
    val webkPeerId: String,
    val peerType: String, // "channel", "supergroup", "group"
    val username: String? = null,
    val displayTitle: String,
    val sourceUrl: String? = null,
    val approvedTimestamp: Long = System.currentTimeMillis(),
    val approvedStatus: String = "APPROVED",
    val isProtected: Boolean = true
)
