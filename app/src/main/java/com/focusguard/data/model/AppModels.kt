package com.focusguard.data.model

enum class AppCategory(val title: String, val isApprovedByDefault: Boolean) {
    COMMUNICATION("Communication", true),
    BANKING_PAYMENTS("Banking & Payments", true),
    TRANSPORTATION("Transportation", true),
    AI_STUDY("AI & Study", true),
    GOOGLE_PRODUCTIVITY("Google Productivity", true),
    ESSENTIAL_SYSTEM("Essential System", true),
    CUSTOM_ALLOWED("Custom Allowed", true),
    BLOCKED_BROWSER("Blocked Browsers", false),
    BLOCKED_SOCIAL("Blocked Social & Media", false),
    BLOCKED_ENTERTAINMENT("Blocked Entertainment", false),
    BLOCKED_DISTRACTION("Blocked Distractions", false),
    OTHER_INSTALLED("Other Applications", false)
}

data class AppItem(
    val packageName: String,
    val displayName: String,
    val category: AppCategory,
    val isEssential: Boolean = false,
    val isAllowed: Boolean = true,
    val isInstalled: Boolean = false,
    val isHidden: Boolean = false,
    val isSuspended: Boolean = false,
    val note: String? = null
)
