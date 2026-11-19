package com.focusguard.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * Scoped Accessibility Service targeting WhatsApp to detect and prevent access
 * to WhatsApp Communities while keeping Chats, Calls, and Settings functional.
 *
 * Scoped strictly via XML packageNames="com.whatsapp,com.whatsapp.w4b"
 * to ensure zero overhead outside of WhatsApp.
 */
class WhatsAppCommunityAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastBlockTimestamp = 0L

    companion object {
        private const val TAG = "FocusGuard_WAGuard"
        private const val DEBOUNCE_MILLIS = 1000L

        // Known WhatsApp community UI identifiers and keywords
        private val COMMUNITY_VIEW_IDS = listOf(
            "com.whatsapp:id/community_tab",
            "com.whatsapp:id/tab_community",
            "com.whatsapp:id/community_home_container",
            "com.whatsapp:id/community_navigation_item",
            "com.whatsapp:id/communities_header"
        )

        private val COMMUNITY_KEYWORDS = listOf(
            "communities",
            "new community",
            "start a community",
            "community home",
            "stay connected with a community"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: return

        // 1. Samsung Galaxy Store UI Interception (Preserving background updates, blocking user browsing)
        if (pkg == "com.sec.android.app.samsungapps") {
            val now = System.currentTimeMillis()
            if (now - lastBlockTimestamp > DEBOUNCE_MILLIS) {
                lastBlockTimestamp = now
                Log.w(TAG, "User attempted to open Galaxy Store UI. Redirecting to home while preserving background updates.")
                performGlobalAction(GLOBAL_ACTION_HOME)
                mainHandler.post {
                    Toast.makeText(
                        applicationContext,
                        "Focus Guard: Galaxy Store browsing is restricted. Background updates for Samsung components remain active.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            return
        }

        // 2. WhatsApp Communities Guard
        if (pkg != "com.whatsapp" && pkg != "com.whatsapp.w4b") {
            // Ignore other events as defense-in-depth
            return
        }

        val root = rootInActiveWindow ?: return
        if (isCommunityScreenOrTabActive(root)) {
            val now = System.currentTimeMillis()
            if (now - lastBlockTimestamp > DEBOUNCE_MILLIS) {
                lastBlockTimestamp = now
                handleCommunityAccessDetected()
            }
        }
    }

    private fun isCommunityScreenOrTabActive(rootNode: AccessibilityNodeInfo): Boolean {
        // 1. Check known community view IDs
        for (viewId in COMMUNITY_VIEW_IDS) {
            val matchingNodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
            if (!matchingNodes.isNullOrEmpty()) {
                for (node in matchingNodes) {
                    val isSelected = node.isSelected
                    val isVisible = node.isVisibleToUser
                    if (isSelected || isVisible) {
                        return true
                    }
                }
            }
        }

        // 2. Check text or content description for Communities
        for (keyword in COMMUNITY_KEYWORDS) {
            val textNodes = rootNode.findAccessibilityNodeInfosByText(keyword)
            if (!textNodes.isNullOrEmpty()) {
                for (node in textNodes) {
                    val text = node.text?.toString()?.lowercase() ?: ""
                    val desc = node.contentDescription?.toString()?.lowercase() ?: ""

                    // Check if it is a tab indicator or main header
                    val isTabSelected = node.isSelected
                    val isMainHeader = text == "communities" || desc == "communities"

                    if (isTabSelected || isMainHeader) {
                        return true
                    }
                }
            }
        }

        return false
    }

    private fun handleCommunityAccessDetected() {
        Log.w(TAG, "Access to WhatsApp Communities detected. Triggering redirection.")
        // Perform Back action to return to previous valid screen (Chats)
        performGlobalAction(GLOBAL_ACTION_BACK)

        mainHandler.post {
            Toast.makeText(
                applicationContext,
                "Focus Guard: WhatsApp Communities is restricted",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "WhatsApp Community Accessibility Service interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "WhatsApp Community Accessibility Service destroyed.")
    }
}
