package com.focusguard.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.focusguard.data.repository.ApprovedAppRegistry
import com.focusguard.dpc.DevicePolicyController
import com.focusguard.installer.FastInstallSuspender
import com.focusguard.study.filter.CoolingOffEvent
import com.focusguard.study.filter.FocusInputFilter
import com.focusguard.study.filter.StudyCoolingOffManager

/**
 * System-wide Accessibility Service for Focus Guard:
 * 1. Global real-time explicit input filtering across all applications.
 * 2. WhatsApp Communities protection while keeping Chats & Calls functional.
 * 3. Restricting Google Search Widget / Discover while preserving Circle to Search & Lens.
 * 4. Permitting Galaxy Store, Play Store & system component updates (Samsung Pass, etc.) while preventing new unauthorized app installs.
 */
class WhatsAppCommunityAccessibilityService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastBlockTimestamp = 0L
    private var lastExplicitDetectionTime = 0L
    private var lastExplicitHitWord: String? = null
    private var isCircleToSearchActive = false
    private var accessibilityWatcher: AccessibilityServiceWatcher? = null
    private var devicePolicyController: DevicePolicyController? = null
    private var lastActionNotAllowedToastTime = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Focus Guard Accessibility Service connected.")
        if (accessibilityWatcher == null) {
            accessibilityWatcher = AccessibilityServiceWatcher(applicationContext, mainHandler).apply {
                startWatching()
            }
        }
        FastInstallSuspender.getInstance(applicationContext).startWatching()
    }

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

        // 0. Global Real-Time Explicit Input Filtering (Across all applications on device: Views + Compose)
        val isInputEvent = when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> true
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // NEVER inspect on raw layout/scroll changes; only when text actually changed
                (event.contentChangeTypes and AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) != 0 ||
                        (!event.text.isNullOrEmpty() && event.text.any { !it.isNullOrBlank() })
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                event.isPassword == false && (!event.text.isNullOrEmpty() || event.source?.isEditable == true)
            }
            else -> false
        }

        if (isInputEvent) {
            inspectTextForExplicitContent(event)
        }

        val pkg = event.packageName?.toString() ?: return

        // 0.5. Settings Tamper Protection: Prevent turning off Focus Guard accessibility service
        // Other accessibility services (TalkBack, Select to Speak, etc.) can be freely managed.
        if (pkg == "com.android.settings" || pkg == "com.samsung.accessibility") {
            handleSettingsAccessibilityEvent(event)
            return
        }

        // 0.6. Instant Unauthorized App Interception:
        // If an unauthorized app window appears (e.g. opened immediately after install before broadcast),
        // instantly boot it back to home screen and suspend it so it cannot run.
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val controller = devicePolicyController ?: DevicePolicyController(applicationContext).also { devicePolicyController = it }
            if (controller.isDeviceOwner() && !controller.isPackageAllowed(pkg)) {
                Log.w(TAG, "Unauthorized window state changed: $pkg. Closing with Home and suspending.")
                performGlobalAction(GLOBAL_ACTION_HOME)
                controller.suspendImmediatelyIfUnauthorized(pkg)
                showActionNotAllowedToast(pkg)
                return
            }
        }

        // Reset Circle to Search session state when top-level window switches to another package
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            pkg != "com.google.android.googlequicksearchbox"
        ) {
            isCircleToSearchActive = false
        }

        // 1. Samsung Galaxy Store Guard (com.sec.android.app.samsungapps)
        // Permitted: Browsing updates, updating Samsung Pass, One UI components, existing system apps, "Update all"
        // Blocked: Installing new, unapproved applications/games
        if (pkg == "com.sec.android.app.samsungapps") {
            inspectStoreOrInstallerEvent(event)
            return
        }

        // 2. Google App & Circle to Search Guard (com.google.android.googlequicksearchbox)
        // Permitted: Circle to Search (Omnient), Google Lens, OCR, Text selection, On-screen text copy
        // Blocked: Standalone Google App feed / Discover & standalone Google Search Widget
        if (pkg == "com.google.android.googlequicksearchbox") {
            handleGoogleAppEvent(event)
            return
        }

        // 3. Package Installer Guard (com.google.android.packageinstaller, etc.)
        // Permitted: Updating existing system apps, Samsung components, approved apps
        // Blocked: Installing new unapproved APKs
        if (pkg.contains("packageinstaller", ignoreCase = true)) {
            inspectStoreOrInstallerEvent(event)
            return
        }

        // 4. Google Play Store Guard (com.android.vending)
        // Permitted: App Updates (Manage apps, Update all, Update existing allowed apps)
        // Blocked: Installing new, unapproved applications
        if (pkg == "com.android.vending") {
            inspectStoreOrInstallerEvent(event)
            return
        }

        // 5. WhatsApp Communities Guard
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

    private fun showActionNotAllowedToast(packageName: String) {
        val now = System.currentTimeMillis()
        if (now - lastActionNotAllowedToastTime < 2500L) return
        lastActionNotAllowedToastTime = now
        mainHandler.post {
            try {
                val appLabel = try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (_: Exception) { packageName }
                Toast.makeText(
                    applicationContext,
                    "⚠️ Focus Guard: '$appLabel' অনুমোদিত নয় এবং লক করা হয়েছে।",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (_: Exception) {}
        }
    }

    private fun handleGoogleAppEvent(event: AccessibilityEvent) {
        val className = event.className?.toString() ?: ""

        // Allowed Features: Circle to Search (Omnient), Google Lens OCR, On-screen Text Copy, Assistant/Overlay
        val isCtsOrLensClass = className.contains("Lens", ignoreCase = true) ||
                className.contains("Omnient", ignoreCase = true) ||
                className.contains("CircleToSearch", ignoreCase = true) ||
                className.contains("ContextualSearch", ignoreCase = true) ||
                className.contains("TextSelection", ignoreCase = true) ||
                className.contains("TextOverlay", ignoreCase = true) ||
                className.contains("OpaOverlay", ignoreCase = true) ||
                className.contains("TouchOutside", ignoreCase = true) ||
                className.contains("TranslateOverlay", ignoreCase = true) ||
                className.contains("Selection", ignoreCase = true) ||
                className.contains("SelectText", ignoreCase = true) ||
                className.contains("NowBar", ignoreCase = true)

        if (isCtsOrLensClass || isCircleToSearchActive) {
            isCircleToSearchActive = true
            return
        }

        // Only evaluate blocking on top-level window transitions (TYPE_WINDOW_STATE_CHANGED)
        // NEVER block on internal View events (FrameLayout, LinearLayout, etc.)
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        // Standalone Google App launched directly by user (SearchActivity, GoogleAppActivity, etc.)
        val now = System.currentTimeMillis()
        if (now - lastBlockTimestamp > DEBOUNCE_MILLIS) {
            lastBlockTimestamp = now
            Log.w(TAG, "Google App launched directly ($className). Closing with Action Not Allowed.")
            performGlobalAction(GLOBAL_ACTION_HOME)
            mainHandler.post {
                Toast.makeText(
                    applicationContext,
                    "Action not allowed: Google App is restricted",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val samsungSystemComponents = setOf(
        "samsung pass",
        "samsung wallet",
        "samsung pay",
        "samsung keyboard",
        "one ui",
        "device care",
        "always on display",
        "good lock",
        "knox",
        "samsung account",
        "smart switch",
        "voice recorder",
        "samsung capture",
        "samsung gallery",
        "samsung camera",
        "samsung clock",
        "samsung calendar",
        "bixby",
        "soundalive"
    )

    private fun inspectStoreOrInstallerEvent(event: AccessibilityEvent) {
        // Temporarily paused as requested by user to allow updating all apps and installing allowed apps from Play Store freely.
        return
    }

    private fun inspectStoreOrInstallerEventInternal(event: AccessibilityEvent) {
        val rootNode = rootInActiveWindow ?: return
        try {
            val isClick = event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
            val isWindowState = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED

            if (!isClick && !isWindowState) {
                return
            }

            val clickedNode = try { event.source } catch (_: Exception) { null }
            val parentNode = try { clickedNode?.parent } catch (_: Exception) { null }

            val eventText = buildString {
                event.text?.forEach { append(it).append(" ") }
                event.contentDescription?.let { append(it).append(" ") }
                clickedNode?.text?.let { append(it).append(" ") }
                clickedNode?.contentDescription?.let { append(it).append(" ") }
                parentNode?.text?.let { append(it).append(" ") }
                parentNode?.contentDescription?.let { append(it).append(" ") }
            }.trim().lowercase()

            // 1. Explicit Update / Safe Action Check:
            val isUpdateOrSafeAction = eventText.contains("update") ||
                    eventText.contains("আপডেট") ||
                    eventText.contains("open") ||
                    eventText.contains("খুলুন") ||
                    eventText.contains("retry") ||
                    eventText.contains("চেষ্টা করুন") ||
                    eventText.contains("continue") ||
                    eventText.contains("চালিয়ে যান") ||
                    eventText.contains("cancel") ||
                    eventText.contains("বাতিল") ||
                    eventText.contains("uninstall") ||
                    eventText.contains("আনইনস্টল")

            if (isUpdateOrSafeAction) {
                return // Allowed! Update operations run freely.
            }

            // Check if clicked node or its children have install text
            var childHasInstall = false
            if (clickedNode != null && isClick) {
                for (i in 0 until clickedNode.childCount) {
                    val child = clickedNode.getChild(i) ?: continue
                    val cText = ((child.text?.toString() ?: "") + " " + (child.contentDescription?.toString() ?: "")).lowercase()
                    if ((cText.contains("install") || cText.contains("ইনস্টল") || cText.contains("download") || cText.contains("ডাউনলোড") || cText == "get") &&
                        !cText.contains("uninstall") && !cText.contains("আনইনস্টল")) {
                        childHasInstall = true
                        break
                    }
                }
            }

            // 2. Explicit Install Action Check:
            val isInstallAction = (eventText.contains("install") && !eventText.contains("uninstall")) ||
                    (eventText.contains("ইনস্টল") && !eventText.contains("আনইনস্টল")) ||
                    eventText.contains("download") ||
                    eventText.contains("ডাউনলোড") ||
                    eventText == "get" ||
                    eventText.startsWith("get ") ||
                    childHasInstall

            // If it's a click and it's NOT an install action, allow normal navigation/browsing
            if (isClick && !isInstallAction) {
                return
            }

            // 3. For install actions, check if the target app is approved, a system component, or already installed
            if (isInstallAction) {
                val isAllowed = isAllowedStoreItem(rootNode, clickedNode)
                if (!isAllowed) {
                    val now = System.currentTimeMillis()
                    if (now - lastBlockTimestamp > DEBOUNCE_MILLIS) {
                        lastBlockTimestamp = now
                        Log.w(TAG, "Blocked unauthorized app installation in ${event.packageName}: $eventText")
                        performGlobalAction(GLOBAL_ACTION_BACK)
                        mainHandler.post {
                            Toast.makeText(
                                applicationContext,
                                "Focus Guard: নতুন অ্যাপ ইনস্টল করা বন্ধ রয়েছে। শুধুমাত্র অনুমোদিত অ্যাপ ও সিস্টেম কম্পোনেন্ট আপডেট করা যাবে।",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inspecting store or installer event", e)
        }
    }

    private fun isAllowedStoreItem(rootNode: AccessibilityNodeInfo, clickedNode: AccessibilityNodeInfo?): Boolean {
        // 1. Check if the screen is a system update dialog or "Update all"
        val textNodes = mutableListOf<String>()
        collectTextNodes(rootNode, textNodes)
        val combinedText = textNodes.joinToString(" ").lowercase()

        if (combinedText.contains("update all") ||
            combinedText.contains("সব আপডেট") ||
            combinedText.contains("do you want to update this app") ||
            combinedText.contains("এই অ্যাপটি আপডেট করতে চান")
        ) {
            return true
        }

        // 2. Extract primary candidates for the app title
        // In search list items, check clickedNode's parent/siblings first
        val candidateTitles = mutableListOf<String>()

        if (clickedNode != null) {
            val parent = clickedNode.parent
            if (parent != null) {
                for (i in 0 until parent.childCount) {
                    val sibling = parent.getChild(i) ?: continue
                    val sibText = sibling.text?.toString()?.trim() ?: continue
                    if (sibText.length >= 3 && !isStoreActionOrChrome(sibText)) {
                        candidateTitles.add(sibText)
                    }
                }
            }
        }

        // Also extract the top non-chrome text nodes from the active window
        val nonChrome = textNodes.filter { text ->
            val t = text.trim()
            t.length >= 3 && !isStoreActionOrChrome(t)
        }
        // Only inspect the top 2 non-chrome text items (title and subtitle/developer)
        // to strictly prevent matching recommended/similar apps at the bottom
        for (item in nonChrome.take(2)) {
            if (!candidateTitles.contains(item)) {
                candidateTitles.add(item)
            }
        }

        if (candidateTitles.isEmpty()) {
            return false
        }

        // 3. Match against ApprovedAppRegistry
        val approvedRegistry = ApprovedAppRegistry(applicationContext)
        val approvedEntries = approvedRegistry.loadRegistry().values

        for (candidate in candidateTitles) {
            val cLower = candidate.lowercase().trim()

            // Check approved apps
            val matchesApproved = approvedEntries.any { entry ->
                val appTitle = entry.displayName.trim().lowercase()
                if (appTitle.length < 3) return@any false
                cLower == appTitle ||
                        cLower.startsWith("$appTitle ") ||
                        cLower.startsWith("$appTitle:") ||
                        appTitle.startsWith("$cLower ") ||
                        (appTitle.length >= 5 && cLower.contains(appTitle))
            }
            if (matchesApproved) return true

            // Check Samsung system components (Strict whitelist, NO generic "samsung")
            val matchesSamsung = samsungSystemComponents.any { comp ->
                if (cLower.contains("samsung internet")) return@any false
                cLower == comp || cLower.startsWith("$comp ") || comp.startsWith("$cLower ")
            }
            if (matchesSamsung) return true
        }

        return false
    }

    private fun isStoreActionOrChrome(text: String): Boolean {
        val t = text.lowercase().trim()
        return t.contains("google play") ||
                t.contains("search") ||
                t.contains("অনুসন্ধান") ||
                t.contains("install") ||
                t.contains("ইনস্টল") ||
                t.contains("update") ||
                t.contains("আপডেট") ||
                t.contains("open") ||
                t.contains("খুলুন") ||
                t.contains("uninstall") ||
                t.contains("আনইনস্টল") ||
                t.contains("contains ads") ||
                t.contains("in-app purchase") ||
                t.contains("বিজ্ঞাপন রয়েছে") ||
                t.contains("sponsored") ||
                t.contains("বিজ্ঞাপন") ||
                t.contains("editor's choice") ||
                t.contains("editors' choice") ||
                t.contains("ad •") ||
                t.startsWith("ad ") ||
                t == "ad" ||
                t.contains("rated for") ||
                t.contains("verified by") ||
                t.contains("reviews") ||
                t.contains("রিভিউ") ||
                t.contains("downloads") ||
                t.contains("ডাউনলোড") ||
                t.contains("similar apps") ||
                t.contains("suggested for you") ||
                t.contains("you might also like") ||
                t.contains("top charts") ||
                t.contains("manage apps") ||
                t.contains("works on your device") ||
                t.contains("galaxy a35") ||
                t.contains("android")
    }

    private fun collectTextNodes(node: AccessibilityNodeInfo, results: MutableList<String>) {
        val text = node.text?.toString()
        if (!text.isNullOrBlank()) {
            results.add(text)
        }
        val count = node.childCount
        for (i in 0 until count) {
            val child = node.getChild(i) ?: continue
            collectTextNodes(child, results)
        }
    }

    private fun collectEditableNodes(node: AccessibilityNodeInfo, results: MutableList<AccessibilityNodeInfo>) {
        if (node.isEditable) {
            results.add(node)
        }
        val count = node.childCount
        for (i in 0 until count) {
            val child = node.getChild(i) ?: continue
            collectEditableNodes(child, results)
        }
    }

    private fun inspectTextForExplicitContent(event: AccessibilityEvent) {
        val candidateTexts = mutableListOf<String>()

        // 1. Text from event itself (zero IPC overhead)
        event.text?.forEach { charSeq ->
            val str = charSeq?.toString()
            if (!str.isNullOrBlank()) candidateTexts.add(str)
        }

        // 2. Text from event source (single IPC call)
        val sourceNode = try { event.source } catch (_: Exception) { null }
        val sourceText = try { sourceNode?.text?.toString() } catch (_: Exception) { null }
        if (!sourceText.isNullOrBlank()) candidateTexts.add(sourceText)

        // Fast check: did event or source text contain an explicit hit?
        var detectedHit: String? = null
        for (candidate in candidateTexts) {
            val hit = FocusInputFilter.explicitHit(candidate)
            if (hit != null) {
                detectedHit = hit
                break
            }
        }

        // 3. If no hit yet, check focused input node
        var rootNode: AccessibilityNodeInfo? = null
        var focusedInput: AccessibilityNodeInfo? = null
        if (detectedHit == null) {
            rootNode = rootInActiveWindow
            focusedInput = rootNode?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            val focusedText = try { focusedInput?.text?.toString() } catch (_: Exception) { null }
            if (!focusedText.isNullOrBlank() && !candidateTexts.contains(focusedText)) {
                candidateTexts.add(focusedText)
                detectedHit = FocusInputFilter.explicitHit(focusedText)
            }
        }

        val explicitHit = detectedHit ?: return

        val now = System.currentTimeMillis()
        if (now - lastExplicitDetectionTime < 1500L && explicitHit == lastExplicitHitWord) {
            return // Debounce rapid keystrokes of the same word
        }
        lastExplicitDetectionTime = now
        lastExplicitHitWord = explicitHit

        Log.w(TAG, "EXPLICIT INPUT DETECTED on device in ${event.packageName}: '$explicitHit'")

        // 1. Immediately wipe the explicit text ONLY from editable/focused nodes containing the violation
        val clearArgs = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
        }

        if (rootNode == null) {
            rootNode = rootInActiveWindow
        }
        if (focusedInput == null) {
            focusedInput = rootNode?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        }

        val targetsToClear = mutableListOf<AccessibilityNodeInfo>()
        focusedInput?.let {
            val fText = try { it.text?.toString() ?: "" } catch (_: Exception) { "" }
            if (FocusInputFilter.explicitHit(fText) != null) {
                targetsToClear.add(it)
            }
        }
        sourceNode?.let {
            if (it.isEditable) {
                val sText = try { it.text?.toString() ?: "" } catch (_: Exception) { "" }
                if (FocusInputFilter.explicitHit(sText) != null) {
                    targetsToClear.add(it)
                }
            }
        }

        val editableNodes = mutableListOf<AccessibilityNodeInfo>()
        if (rootNode != null) {
            collectEditableNodes(rootNode, editableNodes)
            for (node in editableNodes) {
                val nodeText = try { node.text?.toString() ?: "" } catch (_: Exception) { "" }
                if (FocusInputFilter.explicitHit(nodeText) != null) {
                    targetsToClear.add(node)
                }
            }
        }

        // Fallback: If focused input was active and typing triggered the hit, clear it
        if (targetsToClear.isEmpty() && focusedInput != null) {
            val fText = try { focusedInput.text?.toString() ?: "" } catch (_: Exception) { "" }
            if (FocusInputFilter.explicitHit(fText) != null) {
                targetsToClear.add(focusedInput)
            }
        }

        for (target in targetsToClear.distinct()) {
            try {
                target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, clearArgs)
            } catch (e: Exception) {
                Log.w(TAG, "Could not clear text field: ${e.message}")
            }
        }

        // 2. Record violation in StudyCoolingOffManager
        val coolingOff = StudyCoolingOffManager(applicationContext)
        val eventResult = coolingOff.recordViolation(explicitHit)

        // 3. Silent Action: Clear silently without intrusive toasts/alerts (User request)
        when (eventResult) {
            is CoolingOffEvent.Warning -> {
                // Silently cleared! Zero noisy toasts or popups.
                Log.w(TAG, "Explicit input '$explicitHit' wiped silently. Violation count: ${eventResult.currentAttempts}/10")
            }
            is CoolingOffEvent.Triggered -> {
                mainHandler.post {
                    Toast.makeText(
                        applicationContext,
                        "🚨 Focus Guard: ১০ মিনিটে ১০ বার আপত্তিকর শব্দ শনাক্ত হওয়ায় পরবর্তী ১ ঘণ্টার জন্য ইমার্জেন্সি রেস্ট্রিকশন চালু হলো!",
                        Toast.LENGTH_LONG
                    ).show()
                }

                // Launch Focus Guard Cooling-Off Screen
                try {
                    val intent = Intent(applicationContext, com.focusguard.ui.MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("COOLING_OFF_TRIGGERED", true)
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error launching cooling-off screen: ${e.message}")
                }
            }
        }
    }

    private fun handleSettingsAccessibilityEvent(event: AccessibilityEvent) {
        val root = try { rootInActiveWindow } catch (_: Exception) { null } ?: return
        val textList = mutableListOf<String>()
        collectTextNodes(root, textList)
        val combinedText = textList.joinToString(" ").lowercase()

        // 1. Intercept attempts to factory reset the device from Settings
        val isFactoryResetScreen = combinedText.contains("factory data reset") ||
                combinedText.contains("erase all data") ||
                combinedText.contains("ফ্যাক্টরি ডাটা রিসেট") ||
                combinedText.contains("সব ডেটা মুছুন") ||
                combinedText.contains("সব ডাটা মুছুন") ||
                combinedText.contains("সব রিসেট করুন") ||
                (combinedText.contains("reset") && combinedText.contains("all data"))

        if (isFactoryResetScreen) {
            val now = System.currentTimeMillis()
            if (now - lastBlockTimestamp > DEBOUNCE_MILLIS) {
                lastBlockTimestamp = now
                performGlobalAction(GLOBAL_ACTION_BACK)
                mainHandler.post {
                    Toast.makeText(
                        applicationContext,
                        "Focus Guard: ফোন রিসেট করতে Focus Guard ড্যাশবোর্ডে গিয়ে পিন প্রদান করুন।",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            return
        }

        // 1.5. Intercept attempts to access Parental Controls when blocked by policy
        val isParentalControlBlocked = try {
            DevicePolicyController(applicationContext).isParentalControlAccessBlocked()
        } catch (_: Exception) { false }

        if (isParentalControlBlocked) {
            val isParentalControlScreen = combinedText.contains("parental controls") ||
                    combinedText.contains("parental control") ||
                    combinedText.contains("অভিভাবকীয় নিয়ন্ত্রণ") ||
                    (event.className?.toString()?.contains("SupervisionDashboardActivity", ignoreCase = true) == true)

            // Do not block when we are only in main settings homepage list
            val isDashboardScreen = event.className?.toString()?.contains("SupervisionDashboardActivity", ignoreCase = true) == true ||
                    (isParentalControlScreen && !combinedText.contains("device care") && !combinedText.contains("general management"))

            if (isDashboardScreen) {
                val now = System.currentTimeMillis()
                if (now - lastBlockTimestamp > DEBOUNCE_MILLIS) {
                    lastBlockTimestamp = now
                    performGlobalAction(GLOBAL_ACTION_BACK)
                    mainHandler.post {
                        Toast.makeText(
                            applicationContext,
                            "Focus Guard: প্যারেন্টাল কন্ট্রোল অ্যাক্সেস পলিসি অনুযায়ী ব্লক করা রয়েছে।",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                return
            }
        }

        // 2. Check if this screen or dialog is specifically attempting to stop/turn off Focus Guard
        val isFocusGuardDialog = combinedText.contains("focus guard") && (
                combinedText.contains("stop focus guard") ||
                combinedText.contains("turn off focus guard") ||
                combinedText.contains("focus guard বন্ধ") ||
                combinedText.contains("stop?") ||
                combinedText.contains("turn off?")
        )

        if (isFocusGuardDialog) {
            performGlobalAction(GLOBAL_ACTION_BACK)
            mainHandler.post {
                Toast.makeText(
                    applicationContext,
                    "নিরাপত্তা নীতিমালা অনুযায়ী Focus Guard এক্সেসিবিলিটি সার্ভিস বন্ধ করা যাবে না।",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "WhatsApp Community Accessibility Service interrupted.")
    }

    override fun onDestroy() {
        super.onDestroy()
        accessibilityWatcher?.stopWatching()
        accessibilityWatcher = null
        Log.i(TAG, "WhatsApp Community Accessibility Service destroyed.")
    }
}

/**
 * Continuous Secure Settings Observer:
 * Guarantees that Focus Guard's Accessibility Service is ALWAYS enabled.
 * If Focus Guard is removed from enabled_accessibility_services, it immediately re-injects it.
 * Preserves ALL other accessibility services in the list, allowing users to freely manage them.
 */
class AccessibilityServiceWatcher(
    private val context: Context,
    private val handler: Handler
) : ContentObserver(handler) {

    private val serviceComponent = "${context.packageName}/com.focusguard.accessibility.WhatsAppCommunityAccessibilityService"
    private val shortComponent = "${context.packageName}/.accessibility.WhatsAppCommunityAccessibilityService"

    fun startWatching() {
        try {
            context.contentResolver.registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
                false,
                this
            )
            ensureEnabled()
        } catch (e: Exception) {
            Log.w("FocusGuard_Watcher", "Could not register accessibility observer: ${e.message}")
        }
    }

    fun stopWatching() {
        try {
            context.contentResolver.unregisterContentObserver(this)
        } catch (_: Exception) {}
    }

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        ensureEnabled()
    }

    fun ensureEnabled() {
        try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""

            val servicesList = enabledServices.split(":").filter { it.isNotBlank() }
            val hasOurService = servicesList.any { it == serviceComponent || it == shortComponent }

            if (!hasOurService) {
                // Focus Guard was turned off! Re-add it immediately while preserving all other services.
                val updated = (servicesList + serviceComponent).joinToString(":")
                Settings.Secure.putString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    updated
                )
                Settings.Secure.putInt(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    1
                )
                Log.i("FocusGuard_Watcher", "Auto-restored Focus Guard Accessibility Service.")
            }
        } catch (e: Exception) {
            Log.w("FocusGuard_Watcher", "Error ensuring accessibility: ${e.message}")
        }
    }
}
