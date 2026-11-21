package com.focusguard.study.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.app.Activity
import android.app.KeyguardManager
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focusguard.study.model.ApprovedTelegramDestination
import com.focusguard.study.model.ApprovedWebsite
import com.focusguard.study.repository.ApprovedTelegramRegistry
import com.focusguard.study.repository.StudyWebsiteRegistry
import com.focusguard.study.telegram.TelegramDestinationInfo
import com.focusguard.dpc.DevicePolicyController
import com.focusguard.ui.theme.*

private enum class StudyViewMode {
    PORTAL_HOME,
    WEBSITE_VIEW,
    TELEGRAM_LOGIN,
    TELEGRAM_STUDY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPortalScreen(
    context: Context = LocalContext.current,
    websiteRegistry: StudyWebsiteRegistry = remember { StudyWebsiteRegistry(context) },
    telegramRegistry: ApprovedTelegramRegistry = remember { ApprovedTelegramRegistry(context) }
) {
    var viewMode by remember { mutableStateOf(StudyViewMode.PORTAL_HOME) }
    var activeWebsite by remember { mutableStateOf<ApprovedWebsite?>(null) }
    var activeUrl by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf("Dashboard") }
    var activeTelegramRoute by remember { mutableStateOf("#-4415493319") }
    var activeTelegramSourceUrl by remember { mutableStateOf("") }

    var websitesList by remember { mutableStateOf(websiteRegistry.loadWebsites()) }
    var telegramDestinations by remember { mutableStateOf(telegramRegistry.loadApprovedDestinations()) }

    var pendingTelegramApproval by remember { mutableStateOf<TelegramDestinationInfo?>(null) }
    var showAddWebsiteDialog by remember { mutableStateOf(false) }
    var showAddKeywordDialog by remember { mutableStateOf(false) }
    var showAddDomainDialog by remember { mutableStateOf(false) }
    var pendingAdminAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val coolingOffManager = remember { com.focusguard.study.filter.StudyCoolingOffManager(context) }
    var isCoolingOffActive by remember { mutableStateOf(coolingOffManager.isCoolingOffActive()) }

    val customFilterRepository = remember { com.focusguard.study.filter.CustomBlockedFilterRepository(context) }
    var blockedKeywordCount by remember { mutableIntStateOf(customFilterRepository.getTotalKeywordCount()) }
    var blockedDomainCount by remember { mutableIntStateOf(customFilterRepository.getTotalDomainCount()) }
    var customKeywordCount by remember { mutableIntStateOf(customFilterRepository.getCustomKeywordCount()) }
    var customDomainCount by remember { mutableIntStateOf(customFilterRepository.getCustomDomainCount()) }

    fun handleExplicitAttempt(detectedWord: String) {
        val event = coolingOffManager.recordViolation(detectedWord)
        when (event) {
            is com.focusguard.study.filter.CoolingOffEvent.Warning -> {
                Toast.makeText(
                    context,
                    "⚠️ আপত্তিকর শব্দ ব্লক করা হয়েছে (${event.currentAttempts}/10)। ১০ মিনিটে ১০ বার হলে ১ ঘণ্টার জন্য শুধুমাত্র ফোন, এসএমএস, ম্যাপস, নোটস ও ফাইল ম্যানেজার চলবে।",
                    Toast.LENGTH_LONG
                ).show()
            }
            is com.focusguard.study.filter.CoolingOffEvent.Triggered -> {
                isCoolingOffActive = true
                viewMode = StudyViewMode.PORTAL_HOME
                activeWebsite = null
                Toast.makeText(
                    context,
                    "🚨 সতর্কতা: ১০ মিনিটে ১০ বার আপত্তিকর শব্দ শনাক্ত হওয়ায় ১ ঘণ্টার জন্য জরুরি সীমাবদ্ধ মোড চালু হয়েছে!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val adminApprovalLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val action = pendingAdminAction
        pendingAdminAction = null
        if (result.resultCode == Activity.RESULT_OK) {
            action?.invoke()
        } else {
            Toast.makeText(context, "Admin approval was cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    fun requireAdminCredential(title: String, action: () -> Unit) {
        val dpc = DevicePolicyController(context)
        val activity = context.findActivity()
        val keyguard = activity?.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (dpc.isDeviceOwner() && activity != null && keyguard != null && keyguard.isDeviceSecure) {
            val credentialIntent = keyguard.createConfirmDeviceCredentialIntent(
                title,
                "Confirm the device credential to make this permanent Study Portal change."
            )
            if (credentialIntent != null) {
                pendingAdminAction = action
                adminApprovalLauncher.launch(credentialIntent)
                return
            }
        }
        // Always permit adding approved study sites and resources
        action()
    }

    // Intercept hardware/system back button when inside a webview or during cooling-off
    BackHandler(enabled = isCoolingOffActive || viewMode != StudyViewMode.PORTAL_HOME) {
        if (!isCoolingOffActive) {
            viewMode = StudyViewMode.PORTAL_HOME
            activeWebsite = null
            pageTitle = "Dashboard"
        }
    }

    Scaffold(
        topBar = {
            if (viewMode != StudyViewMode.PORTAL_HOME && viewMode != StudyViewMode.WEBSITE_VIEW) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = pageTitle,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                            Text(
                                text = if (viewMode == StudyViewMode.TELEGRAM_STUDY) "Telegram Study Mode" else activeUrl,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            viewMode = StudyViewMode.PORTAL_HOME
                            activeWebsite = null
                            pageTitle = "Dashboard"
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Dashboard",
                                tint = GuardGreenText
                            )
                        }
                    },
                    actions = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Restricted Sandbox",
                            tint = GuardGreenText,
                            modifier = Modifier.padding(end = 12.dp).size(20.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkSurface,
                        titleContentColor = TextPrimary
                    )
                )
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isCoolingOffActive) {
                CoolingOffScreen(
                    coolingOffManager = coolingOffManager,
                    onUnlocked = {
                        isCoolingOffActive = false
                    },
                    onAdminUnlock = {
                        requireAdminCredential("Admin Emergency Unlock") {
                            coolingOffManager.adminClearLockout()
                            isCoolingOffActive = false
                            Toast.makeText(context, "Admin emergency unlock successful", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            } else {
                when (viewMode) {
                    StudyViewMode.PORTAL_HOME -> {
                        StudyPortalHomeContent(
                            websites = websitesList,
                            telegramDestinations = telegramDestinations,
                            blockedKeywordCount = blockedKeywordCount,
                            blockedDomainCount = blockedDomainCount,
                            customKeywordCount = customKeywordCount,
                            customDomainCount = customDomainCount,
                            onOpenWebsite = { site ->
                                activeWebsite = site
                                activeUrl = site.url
                                pageTitle = site.title
                                viewMode = StudyViewMode.WEBSITE_VIEW
                            },
                            onOpenTelegramStudy = { route ->
                                activeTelegramRoute = route
                                pageTitle = "Telegram Study Mode"
                                viewMode = StudyViewMode.TELEGRAM_STUDY
                            },
                            onOpenTelegramLogin = {
                                pageTitle = "Telegram Authentication"
                                viewMode = StudyViewMode.TELEGRAM_LOGIN
                            },
                            onAddWebsiteClicked = {
                                showAddWebsiteDialog = true
                            },
                            onAddKeywordClicked = {
                                showAddKeywordDialog = true
                            },
                            onAddDomainClicked = {
                                showAddDomainDialog = true
                            }
                        )
                    }

                    StudyViewMode.WEBSITE_VIEW -> {
                        if (activeWebsite != null) {
                            FullscreenStudyBrowser(
                                website = activeWebsite!!,
                                approvedHosts = websiteRegistry.getAllowedHosts(),
                                onClose = {
                                    viewMode = StudyViewMode.PORTAL_HOME
                                    activeWebsite = null
                                },
                                onExplicitAttempt = { badWord -> handleExplicitAttempt(badWord) }
                            )
                        } else {
                            ControlledStudyWebView(
                                initialUrl = activeUrl,
                                approvedHosts = websiteRegistry.getAllowedHosts(),
                                isTelegramMode = false,
                                onExplicitAttempt = { badWord -> handleExplicitAttempt(badWord) },
                                onTelegramLinkIntercepted = { destInfo ->
                                    activeTelegramSourceUrl = destInfo.rawUrl
                                    if (telegramRegistry.isApproved(destInfo.targetIdentifier)) {
                                        activeTelegramRoute = destInfo.webkRouteHash
                                        viewMode = StudyViewMode.TELEGRAM_STUDY
                                        Toast.makeText(context, "Opening approved study resource...", Toast.LENGTH_SHORT).show()
                                    } else {
                                        activeTelegramRoute = destInfo.webkRouteHash
                                        viewMode = StudyViewMode.TELEGRAM_STUDY
                                    }
                                },
                                onTitleChanged = { title ->
                                    pageTitle = title
                                }
                            )
                        }
                    }

                    StudyViewMode.TELEGRAM_STUDY -> {
                        val approvedPeerKeys = telegramDestinations.mapNotNull { destination ->
                            destination.webkPeerId?.let { "${destination.peerType}:$it" }
                        }.toSet()
                        ControlledStudyWebView(
                            initialUrl = "https://appassets.androidplatform.net/assets/telegram-web-k/index.html$activeTelegramRoute",
                            approvedHosts = websiteRegistry.getAllowedHosts(),
                            isTelegramMode = true,
                            isTelegramLoginFlow = false,
                            approvedTelegramPeerKeys = approvedPeerKeys,
                            targetTelegramRoute = activeTelegramRoute,
                            telegramSourceUrl = activeTelegramSourceUrl,
                            policyRevision = telegramDestinations.hashCode(),
                            onExplicitAttempt = { badWord -> handleExplicitAttempt(badWord) },
                            onTelegramLinkIntercepted = { destInfo ->
                            activeTelegramSourceUrl = destInfo.rawUrl
                            if (telegramRegistry.isApproved(destInfo.targetIdentifier)) {
                                activeTelegramRoute = destInfo.webkRouteHash
                            } else {
                                activeTelegramRoute = destInfo.webkRouteHash
                            }
                        },
                        onUnapprovedTelegramPeer = { peerId, peerType, title, username, sourceUrl ->
                            val destInfo = TelegramDestinationInfo(
                                rawUrl = sourceUrl,
                                targetIdentifier = peerId,
                                webkPeerId = peerId,
                                username = username.takeIf { it.isNotBlank() },
                                peerType = peerType,
                                suggestedTitle = title,
                                webkRouteHash = "#$peerId"
                            )
                            pendingTelegramApproval = destInfo
                        },
                        onTitleChanged = { title ->
                            pageTitle = title
                        }
                    )
                }

                StudyViewMode.TELEGRAM_LOGIN -> {
                    ControlledStudyWebView(
                        initialUrl = "https://appassets.androidplatform.net/assets/telegram-web-k/index.html",
                        approvedHosts = websiteRegistry.getAllowedHosts(),
                        isTelegramMode = true,
                        isTelegramLoginFlow = true,
                        onTelegramLoginCompleted = {
                            Toast.makeText(context, "Telegram authentication successful! Transitioning to Study Mode.", Toast.LENGTH_LONG).show()
                            viewMode = StudyViewMode.TELEGRAM_STUDY
                        },
                        onTitleChanged = { title ->
                            pageTitle = title
                        }
                    )
                }
            }
            }

            // Admin Approval Dialog for New Telegram Destinations
            pendingTelegramApproval?.let { dest ->
                AlertDialog(
                    onDismissRequest = { pendingTelegramApproval = null },
                    icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = GuardGreenText) },
                    title = { Text("Approve Telegram Destination", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "A link to a Telegram resource was selected from Guess The Case (GTC) or another study portal.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            HorizontalDivider(color = DarkBorder)
                            Text(
                                text = "Resource: ${dest.suggestedTitle}",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Target Identifier: ${dest.targetIdentifier}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            if (dest.postId != null) {
                                Text(
                                    text = "Post ID: #${dest.postId}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                            Text(
                                text = "Source: ${dest.rawUrl}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Approve adding this destination to the permanent Study Mode registry?",
                                style = MaterialTheme.typography.bodySmall,
                                color = GuardGreenText
                            )
                        }
                    },
                    confirmButton = {
                        if (dest.webkPeerId.isNullOrBlank() || dest.peerType !in setOf("channel", "supergroup", "group")) {
                            Text(
                                "Web K has not supplied a verified channel, group or supergroup identity. This link cannot be approved safely.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Button(
                                onClick = {
                                    requireAdminCredential("Approve Telegram Study Resource") {
                                        val approved = telegramRegistry.approveDestination(
                                            rawPeerId = dest.webkPeerId,
                                            peerType = requireNotNull(dest.peerType),
                                            displayTitle = dest.suggestedTitle,
                                            username = dest.username,
                                            sourceUrl = dest.rawUrl
                                        )
                                        telegramDestinations = telegramRegistry.loadApprovedDestinations()
                                        pendingTelegramApproval = null
                                        activeTelegramRoute = "#${approved.webkPeerId}"
                                        viewMode = StudyViewMode.TELEGRAM_STUDY
                                        Toast.makeText(context, "Approved: ${approved.displayTitle}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GuardGreenContainer, contentColor = GuardGreenText)
                            ) {
                                Text("Verify & Approve", fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingTelegramApproval = null }) {
                            Text("Block", color = TextMuted)
                        }
                    },
                    containerColor = DarkSurface,
                    textContentColor = TextPrimary
                )
            }

            // Dialog for Adding Custom Approved Educational Websites
            if (showAddWebsiteDialog) {
                var newTitle by remember { mutableStateOf("") }
                var newUrl by remember { mutableStateOf("") }
                var errorMessage by remember { mutableStateOf<String?>(null) }

                AlertDialog(
                    onDismissRequest = { showAddWebsiteDialog = false },
                    icon = { Icon(Icons.Default.AddLink, contentDescription = null, tint = GuardGreenText) },
                    title = { Text("Add Approved Study Website", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Add an educational website to the Focus Guard whitelist. Search engines and social media remain blocked.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            OutlinedTextField(
                                value = newTitle,
                                onValueChange = { newTitle = it },
                                label = { Text("Website Title") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newUrl,
                                onValueChange = { newUrl = it },
                                label = { Text("Website Address (URL)") },
                                placeholder = { Text("e.g. guessthecase.eu.org") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (errorMessage != null) {
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                requireAdminCredential("Approve Study Website") {
                                    val result = websiteRegistry.addWebsite(newTitle, newUrl)
                                    if (result.isSuccess) {
                                        websitesList = websiteRegistry.loadWebsites()
                                        showAddWebsiteDialog = false
                                        Toast.makeText(context, "Added ${result.getOrNull()?.title}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        errorMessage = result.exceptionOrNull()?.message ?: "Validation failed"
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GuardGreenContainer, contentColor = GuardGreenText)
                        ) {
                            Text("Add Website", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddWebsiteDialog = false }) {
                            Text("Cancel", color = TextMuted)
                        }
                    },
                    containerColor = DarkSurface,
                    textContentColor = TextPrimary
                )
            }

            // Dialog for Adding Custom Blocked Keyword
            if (showAddKeywordDialog) {
                var newKeyword by remember { mutableStateOf("") }
                var keywordError by remember { mutableStateOf<String?>(null) }

                AlertDialog(
                    onDismissRequest = { showAddKeywordDialog = false },
                    icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFFF8A80)) },
                    title = { Text("Add Blocked Keyword", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "টাইপিং বা সার্চ থেকে নিষিদ্ধ করতে কি-ওয়ার্ড লিখুন। যোগ হলে এটি স্থায়ীভাবে ব্লক হবে এবং অ্যাপ থেকে মুছতে পারবেন না।",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            OutlinedTextField(
                                value = newKeyword,
                                onValueChange = {
                                    newKeyword = it
                                    keywordError = null
                                },
                                label = { Text("Explicit Keyword") },
                                placeholder = { Text("e.g. offensive word") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (keywordError != null) {
                                Text(
                                    text = keywordError!!,
                                    color = if (keywordError!!.contains("Already added") || keywordError!!.contains("ইতিমধ্যে")) Color(0xFFFFB74D) else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val result = customFilterRepository.addKeyword(newKeyword)
                                when (result) {
                                    is com.focusguard.study.filter.AddFilterResult.AlreadyExists -> {
                                        keywordError = result.message
                                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                    }
                                    is com.focusguard.study.filter.AddFilterResult.Error -> {
                                        keywordError = result.message
                                    }
                                    is com.focusguard.study.filter.AddFilterResult.Success -> {
                                        blockedKeywordCount = customFilterRepository.getTotalKeywordCount()
                                        customKeywordCount = customFilterRepository.getCustomKeywordCount()
                                        showAddKeywordDialog = false
                                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GuardGreenContainer, contentColor = GuardGreenText)
                        ) {
                            Text("Permanently Block Keyword", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddKeywordDialog = false }) {
                            Text("Cancel", color = TextMuted)
                        }
                    },
                    containerColor = DarkSurface,
                    textContentColor = TextPrimary
                )
            }

            // Dialog for Adding Custom Blocked Domain / Website
            if (showAddDomainDialog) {
                var newDomain by remember { mutableStateOf("") }
                var domainError by remember { mutableStateOf<String?>(null) }

                AlertDialog(
                    onDismissRequest = { showAddDomainDialog = false },
                    icon = { Icon(Icons.Default.Block, contentDescription = null, tint = Color(0xFFFF8A80)) },
                    title = { Text("Add Blocked Domain", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "স্টাডি পোর্টাল থেকে ব্রাউজিং নিষিদ্ধ করতে ডোমেইন লিখুন। যোগ হলে এটি স্থায়ীভাবে ব্লক হবে এবং অ্যাপ থেকে মুছতে পারবেন না।",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                            OutlinedTextField(
                                value = newDomain,
                                onValueChange = {
                                    newDomain = it
                                    domainError = null
                                },
                                label = { Text("Domain or Website Address") },
                                placeholder = { Text("e.g. badsite.com or https://badsite.com") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (domainError != null) {
                                Text(
                                    text = domainError!!,
                                    color = if (domainError!!.contains("Already added") || domainError!!.contains("ইতিমধ্যে")) Color(0xFFFFB74D) else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val result = customFilterRepository.addDomain(newDomain)
                                when (result) {
                                    is com.focusguard.study.filter.AddFilterResult.AlreadyExists -> {
                                        domainError = result.message
                                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                    }
                                    is com.focusguard.study.filter.AddFilterResult.Error -> {
                                        domainError = result.message
                                    }
                                    is com.focusguard.study.filter.AddFilterResult.Success -> {
                                        blockedDomainCount = customFilterRepository.getTotalDomainCount()
                                        customDomainCount = customFilterRepository.getCustomDomainCount()
                                        showAddDomainDialog = false
                                        Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GuardGreenContainer, contentColor = GuardGreenText)
                        ) {
                            Text("Permanently Block Domain", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDomainDialog = false }) {
                            Text("Cancel", color = TextMuted)
                        }
                    },
                    containerColor = DarkSurface,
                    textContentColor = TextPrimary
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun StudyPortalHomeContent(
    websites: List<ApprovedWebsite>,
    telegramDestinations: List<ApprovedTelegramDestination>,
    blockedKeywordCount: Int,
    blockedDomainCount: Int,
    customKeywordCount: Int,
    customDomainCount: Int,
    onOpenWebsite: (ApprovedWebsite) -> Unit,
    onOpenTelegramStudy: (String) -> Unit,
    onOpenTelegramLogin: () -> Unit,
    onAddWebsiteClicked: () -> Unit,
    onAddKeywordClicked: () -> Unit,
    onAddDomainClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Study Portal Banner
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(GuardGreenContainer, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = null,
                        tint = GuardGreenText,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Focus Guard Dashboard",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Controlled browsing · Whitelisted domains & apps · Strict explicit filter",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    )
                }
            }
        }



        // Telegram Study Mode Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF1E3A8A), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Telegram Study Mode",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "${telegramDestinations.size} Approved Channel(s) & Group(s)",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = GuardGreenText
                            )
                        )
                    }
                }

                Text(
                    text = "Restricted Telegram Web K client. Feeds, personal chats, stories, and global search are blocked.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val defaultRoute = telegramDestinations.firstOrNull()?.let {
                        if (it.webkPeerId.isNotEmpty()) "#${it.webkPeerId}" else "#@${it.username}"
                    } ?: "#-4415493319"

                    Button(
                        onClick = { onOpenTelegramStudy(defaultRoute) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GuardGreenContainer,
                            contentColor = GuardGreenText
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open Study Chat", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { onOpenTelegramLogin() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auth / Login", fontSize = 13.sp)
                    }
                }
            }
        }



        // Discreet Explicit Filter & Domain Blocklist Card (NO WORD LIST DISPLAYED)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF831843), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFFF472B6),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Explicit Filter & Blocklist",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "গোপনীয়তার স্বার্থে ফিল্টারকৃত শব্দের তালিকা লুকায়িত রাখা হয়েছে",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        )
                    }
                }

                // Metric Counters Only
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkBackground),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🚫 ব্লকড ডোমেইন",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$blockedDomainCount টি",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GuardGreenText
                                )
                            )
                            if (customDomainCount > 0) {
                                Text(
                                    text = "কাস্টম: $customDomainCount টি",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = TextMuted)
                                )
                            }
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkBackground),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🛑 ব্লকড কি-ওয়ার্ড",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$blockedKeywordCount টি",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF8A80)
                                )
                            )
                            if (customKeywordCount > 0) {
                                Text(
                                    text = "কাস্টম: $customKeywordCount টি",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = TextMuted)
                                )
                            }
                        }
                    }
                }

                // Add Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddKeywordClicked,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ কি-ওয়ার্ড", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onAddDomainClicked,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ ডোমেইন", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }



        // ========================================================
        // Application Guard & Protected Apps Card (Strictly Read-Only, No New Apps Allowed)
        // ========================================================
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(GuardGreenContainer, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = null,
                            tint = GuardGreenText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Application Guard & Protected Apps",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Text(
                            text = "অ্যাপ অনুমোদন নীতি স্থায়ীভাবে লক করা — নতুন অ্যাপ নিষিদ্ধ",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        )
                    }
                }

                // Security Policy Banner (NO ALLOW BUTTON)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "নীতিমালা লক: নতুন কোনো অ্যাপ এলাও করা যাবে না। সকল আনঅথরাইজড অ্যাপ বা নতুন ইনস্টলেশন সম্পূর্ণ ব্লকড।",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        )
                    }
                }

                // App Security Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkBackground),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🛡️ অনুমোদিত অ্যাপ",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "28টি সংরক্ষিত",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = GuardGreenText
                                )
                            )
                            Text(
                                text = "স্টাডি, ব্যাংক ও ইউটিলিটি",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = TextMuted)
                            )
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkBackground),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🚫 নতুন ইনস্টল",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "স্থায়ীভাবে ব্লকড",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF8A80)
                                )
                            )
                            Text(
                                text = "প্লে স্টোর / সাইডলোড লক",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, color = TextMuted)
                            )
                        }
                    }
                }

                // Verified Essential Apps Overview List (Categorized Pills)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "অনুমোদিত ও সক্রিয় অ্যাপসমূহ:",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                    )

                    val appBadges = listOf(
                        "Google Maps & Navigation" to GuardGreenText,
                        "WhatsApp (Communities Blocked)" to GuardGreenText,
                        "Complete Anatomy & Medical" to GuardGreenText,
                        "iHadis & Muslim Bangla" to GuardGreenText,
                        "BoiMate & PDF Reader" to GuardGreenText,
                        "Google Drive & Gmail" to GuardGreenText,
                        "Cellfin, bKash & iSmart" to GuardGreenText,
                        "Pathao, Uber & Foodpanda" to GuardGreenText,
                        "Samsung Notes & Samsung Pass" to GuardGreenText
                    )

                    for ((appName, badgeColor) in appBadges) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(DarkBackground, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = badgeColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = appName,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        color = TextPrimary
                                    )
                                )
                            }
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }

        // Approved Educational Websites Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Approved Educational Websites",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            )
            IconButton(onClick = onAddWebsiteClicked) {
                Icon(Icons.Default.Add, contentDescription = "Add Website", tint = GuardGreenText)
            }
        }

        // Websites Grid (Scrollable rows of 2)
        val chunkedWebsites = remember(websites) { websites.chunked(2) }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (rowSites in chunkedWebsites) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (website in rowSites) {
                        Box(modifier = Modifier.weight(1f)) {
                            WebsiteCard(
                                website = website,
                                onClick = { onOpenWebsite(website) }
                            )
                        }
                    }
                    if (rowSites.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }


    }
}

@Composable
private fun WebsiteCard(
    website: ApprovedWebsite,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(GuardGreenContainer, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        getIconForWebsite(website.iconName),
                        contentDescription = null,
                        tint = GuardGreenText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (website.isPredefined) {
                    Box(
                        modifier = Modifier
                            .background(DarkBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OFFICIAL",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = GuardGreenText
                            )
                        )
                    }
                }
            }

            Column {
                Text(
                    text = website.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = website.domain,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                )
            }
        }
    }
}

private fun getIconForWebsite(iconName: String): ImageVector {
    return when (iconName) {
        "medical_services" -> Icons.Default.LocalHospital
        "menu_book" -> Icons.AutoMirrored.Filled.MenuBook
        "psychology" -> Icons.Default.Psychology
        "folder_shared" -> Icons.Default.FolderShared
        "mosque" -> Icons.Default.AccountBalance
        "auto_stories" -> Icons.Default.AutoStories
        "quiz" -> Icons.Default.Quiz
        else -> Icons.Default.Public
    }
}

@Composable
fun CoolingOffScreen(
    coolingOffManager: com.focusguard.study.filter.StudyCoolingOffManager,
    onUnlocked: () -> Unit,
    onAdminUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var remainingMillis by remember { mutableLongStateOf(coolingOffManager.getRemainingMillis()) }

    LaunchedEffect(Unit) {
        while (remainingMillis > 0) {
            kotlinx.coroutines.delay(1000L)
            remainingMillis = coolingOffManager.getRemainingMillis()
        }
        onUnlocked()
    }

    val totalMinutes = remainingMillis / 60000L
    val seconds = (remainingMillis % 60000L) / 1000L
    val timeFormatted = String.format("%02d:%02d", totalMinutes, seconds)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Icon(
            imageVector = Icons.Default.HourglassEmpty,
            contentDescription = "Cooling-Off Mode",
            tint = Color(0xFFFFB74D), // Warning amber
            modifier = Modifier.size(60.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "জরুরি সীমাবদ্ধ মোড (Emergency Mode)",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = coolingOffManager.getLockoutReason().ifBlank {
                "১০ মিনিটে ১০ বার আপত্তিকর শব্দ শনাক্ত হওয়ায় ১ ঘণ্টার জন্য শুধুমাত্র ফোন, এসএমএস, ম্যাপস, নোটস এবং ফাইল ম্যানেজার ব্যবহারের অনুমতি দেওয়া হয়েছে।"
            },
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextMuted,
                lineHeight = 18.sp
            ),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Large countdown card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "স্বাভাবিক মোডে ফিরতে অবশিষ্ট সময় (Countdown)",
                    style = MaterialTheme.typography.labelMedium.copy(color = TextMuted)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB74D)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Allowed Emergency Apps Launch Card (Phone, SMS, Maps, Notes, File Manager)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = GuardGreenText, modifier = Modifier.size(18.dp))
                    Text(
                        text = "অনুমোদিত জরুরি অ্যাপস (Allowed Apps)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Text(
                    text = "পরবর্তী ১ ঘণ্টার জন্য নিচের প্রয়োজনীয় সেবাগুলো সচল রাখা হয়েছে:",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = TextMuted)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { launchEmergencyApp(context, "phone") },
                        colors = ButtonDefaults.buttonColors(containerColor = GuardGreenContainer, contentColor = GuardGreenText),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📞 ফোন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { launchEmergencyApp(context, "sms") },
                        colors = ButtonDefaults.buttonColors(containerColor = GuardGreenContainer, contentColor = GuardGreenText),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("💬 এসএমএস", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { launchEmergencyApp(context, "maps") },
                        colors = ButtonDefaults.buttonColors(containerColor = GuardGreenContainer, contentColor = GuardGreenText),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🗺️ ম্যাপস", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { launchEmergencyApp(context, "notes") },
                        colors = ButtonDefaults.buttonColors(containerColor = GuardGreenContainer, contentColor = GuardGreenText),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📝 নোটস", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { launchEmergencyApp(context, "files") },
                        colors = ButtonDefaults.buttonColors(containerColor = GuardGreenContainer, contentColor = GuardGreenText),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📁 ফাইলস", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { launchEmergencyApp(context, "music") },
                        colors = ButtonDefaults.buttonColors(containerColor = GuardGreenContainer, contentColor = GuardGreenText),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🎵 মিউজিক", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { launchEmergencyApp(context, "quran") },
                        colors = ButtonDefaults.buttonColors(containerColor = GuardGreenContainer, contentColor = GuardGreenText),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📖 কুরআন", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { launchEmergencyApp(context, "hadith") },
                        colors = ButtonDefaults.buttonColors(containerColor = GuardGreenContainer, contentColor = GuardGreenText),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📚 হাদিস", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { launchEmergencyApp(context, "muslimbangla") },
                        colors = ButtonDefaults.buttonColors(containerColor = GuardGreenContainer, contentColor = GuardGreenText),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🕌 মুসলিম বাংলা", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }



        Spacer(modifier = Modifier.height(16.dp))

        // Islamic & Calming Spiritual Reflection Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "শান্ত হোন ও গভীর শ্বাস নিন",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = GuardGreenText
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "“মুমিনদের বলুন, তারা যেন তাদের দৃষ্টিকে সংযত রাখে এবং তাদের লজ্জাস্থানের হেফাজত করে। এটাই তাদের জন্য অধিকতর পবিত্র।”\n— (সূরা আন-নূর: ৩০)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextPrimary,
                        lineHeight = 18.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "পরামর্শ: একটু হেঁটে আসুন, চোখে-মুখে ঠান্ডা পানির ঝাপটা দিন বা অযু করে ২ রাকাত নামাজ আদায় করুন। এই ক্ষণস্থায়ী মানসিক প্ররোচনা কিছুক্ষণের মধ্যেই কেটে যাবে ইনশাআল্লাহ।",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Emergency Admin Unlock Button
        OutlinedButton(
            onClick = onAdminUnlock,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted),
            modifier = Modifier.fillMaxWidth(0.75f)
        ) {
            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Admin Emergency Unlock", fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun launchEmergencyApp(context: Context, appType: String) {
    try {
        when (appType) {
            "phone" -> {
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            "sms" -> {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MESSAGING)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            "maps" -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.maps")
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                context.startActivity(intent)
            }
            "notes" -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.samsung.android.app.notes")
                    ?: context.packageManager.getLaunchIntentForPackage("com.google.android.keep")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "নোটস অ্যাপ পাওয়া যায়নি (Notes app not found)", Toast.LENGTH_SHORT).show()
                }
            }
            "files" -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.sec.android.app.myfiles")
                    ?: context.packageManager.getLaunchIntentForPackage("com.google.android.apps.nbu.files")
                    ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        type = "*/*"
                        addCategory(Intent.CATEGORY_OPENABLE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                context.startActivity(intent)
            }
            "music" -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.sec.android.app.music")
                    ?: Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_APP_MUSIC)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                context.startActivity(intent)
            }
            "quran" -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.greentech.quran")
                    ?: context.packageManager.getLaunchIntentForPackage("com.quran.labs.androidquran")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "আল কুরআন অ্যাপ ইনস্টল নেই", Toast.LENGTH_SHORT).show()
                }
            }
            "hadith" -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.ihadis.ihadis")
                    ?: context.packageManager.getLaunchIntentForPackage("com.hadithbd")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "আল হাদিস অ্যাপ ইনস্টল নেই", Toast.LENGTH_SHORT).show()
                }
            }
            "muslimbangla" -> {
                val intent = context.packageManager.getLaunchIntentForPackage("com.tos.namajtime")
                    ?: context.packageManager.getLaunchIntentForPackage("com.muslimbangla")
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "মুসলিম বাংলা অ্যাপ ইনস্টল নেই", Toast.LENGTH_SHORT).show()
                }
            }
        }


    } catch (e: Exception) {
        Toast.makeText(context, "অ্যাপটি খুলতে ব্যর্থ হয়েছে: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
