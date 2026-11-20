package com.focusguard.study.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.focusguard.study.filter.FocusInputFilter
import com.focusguard.study.filter.StudyNavigationGuard
import com.focusguard.study.repository.StudyWebsiteRegistry
import com.focusguard.ui.theme.DarkBackground
import java.io.ByteArrayInputStream
import com.focusguard.ui.theme.DarkSurface
import com.focusguard.ui.theme.FocusGuardTheme
import com.focusguard.ui.theme.GuardGreenText

private const val DESKTOP_FALLBACK_UA =
    "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"

/**
 * Restricted In-App WebView Activity for App Logins & Web Authentication.
 *
 * Provides a dedicated, secure WebView handler for ACTION_VIEW / Custom Tabs intents
 * allowing third-party apps (e.g. Live MCQ, GitHub, ChatGPT, Banking apps) to complete
 * OAuth / SSO logins (Google, Apple, Facebook, Email, etc.) even when standalone
 * general-purpose browsers (Chrome, Firefox, etc.) are restricted.
 *
 * Real-time explicit filtering protects against distracting or explicit sites,
 * while authentication flows and OAuth callback redirections are fully preserved.
 */
class RestrictedAuthWebViewActivity : ComponentActivity() {

    private var targetUrl: String = ""
    private var webView: WebView? = null
    private var cachedApprovedHosts: Set<String> = emptySet()

    private fun extractUrlFromIntent(intent: Intent?): String {
        val dataString = intent?.dataString
        val extraText = intent?.getStringExtra(Intent.EXTRA_TEXT)
        return when {
            !dataString.isNullOrBlank() -> dataString
            !extraText.isNullOrBlank() && (extraText.startsWith("http://") || extraText.startsWith("https://")) -> extraText
            else -> ""
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        targetUrl = extractUrlFromIntent(intent)
        if (targetUrl.isBlank()) {
            Toast.makeText(this, "No valid login URL provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        cachedApprovedHosts = StudyWebsiteRegistry(this).getAllowedHosts()
        val isAllowed = StudyNavigationGuard.isNavPermitted(targetUrl, cachedApprovedHosts, telegramMode = false)
        if (!isAllowed) {
            Toast.makeText(this, "Focus Guard: এই ডোমেইনটি অনুমোদিত নয় ($targetUrl)", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            FocusGuardTheme {
                RestrictedAuthScreen(
                    initialUrl = targetUrl,
                    approvedHosts = cachedApprovedHosts,
                    onClose = { finish() },
                    onRegisterWebView = { wv -> webView = wv }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val newUrl = extractUrlFromIntent(intent)
        if (newUrl.isNotBlank()) {
            val hosts = cachedApprovedHosts.ifEmpty { StudyWebsiteRegistry(this).getAllowedHosts() }
            if (StudyNavigationGuard.isNavPermitted(newUrl, hosts, telegramMode = false)) {
                targetUrl = newUrl
                webView?.loadUrl(newUrl)
            } else {
                Toast.makeText(this, "Focus Guard: এই ডোমেইনটি অনুমোদিত নয় ($newUrl)", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView?.canGoBack() == true) {
            webView?.goBack()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView?.let { wv ->
            wv.stopLoading()
            wv.webChromeClient = null
            wv.webViewClient = WebViewClient()
            (wv.parent as? ViewGroup)?.removeView(wv)
            wv.destroy()
        }
        webView = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "FocusGuard_AuthWeb"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun RestrictedAuthScreen(
    initialUrl: String,
    approvedHosts: Set<String>,
    onClose: () -> Unit,
    onRegisterWebView: (WebView) -> Unit
) {
    var currentTitle by remember { mutableStateOf("Secure Login") }
    var currentHost by remember { mutableStateOf(try { Uri.parse(initialUrl).host ?: "" } catch (_: Exception) { "" }) }
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (currentHost.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Secure",
                                        tint = GuardGreenText,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = currentHost,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onClose) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        if (webViewInstance?.canGoBack() == true) {
                            IconButton(onClick = { webViewInstance?.goBack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                        IconButton(onClick = { webViewInstance?.reload() }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reload")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkSurface
                    )
                )

                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { loadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = GuardGreenText
                    )
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            javaScriptCanOpenWindowsAutomatically = true
                            setSupportMultipleWindows(true)
                            cacheMode = WebSettings.LOAD_DEFAULT
                            userAgentString = DESKTOP_FALLBACK_UA
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                            allowFileAccess = false
                            allowContentAccess = false
                        }

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val uri = request?.url ?: return false
                                val url = uri.toString()

                                // 1. Custom app schemes (e.g. livemcq://, oauth://, intent://) -> fire back to app
                                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                    try {
                                        val intent = if (url.startsWith("intent://")) {
                                            Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                                        } else {
                                            Intent(Intent.ACTION_VIEW, uri)
                                        }
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        ctx.startActivity(intent)
                                        (ctx as? ComponentActivity)?.finish()
                                    } catch (e: Exception) {
                                        Log.w("FocusGuard_AuthWeb", "Could not start intent for $url: ${e.message}")
                                    }
                                    return true
                                }

                                // 2. Real-time explicit filter check
                                val explicitHit = FocusInputFilter.explicitHit(url)
                                if (explicitHit != null) {
                                    Toast.makeText(ctx, "Focus Guard: Explicit website blocked ($explicitHit)", Toast.LENGTH_SHORT).show()
                                    return true
                                }

                                // 3. Domain permit check:
                                val isAllowed = StudyNavigationGuard.isNavPermitted(url, approvedHosts, telegramMode = false)
                                if (!isAllowed) {
                                    Toast.makeText(ctx, "Focus Guard: এই ডোমেইনটি অনুমোদিত নয় ($url)", Toast.LENGTH_SHORT).show()
                                    return true // Cancel navigation
                                }

                                currentHost = uri.host ?: ""
                                return false
                            }

                            override fun shouldInterceptRequest(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): WebResourceResponse? {
                                val uri = request?.url ?: return null
                                val urlStr = uri.toString()
                                val isMain = request.isForMainFrame

                                if (isMain) {
                                    val permitted = StudyNavigationGuard.isNavPermitted(
                                        urlStr,
                                        approvedHosts,
                                        telegramMode = false,
                                        mainFrame = true
                                    )
                                    if (!permitted) {
                                        return WebResourceResponse(
                                            "text/plain",
                                            "UTF-8",
                                            403,
                                            "Forbidden",
                                            emptyMap(),
                                            ByteArrayInputStream(ByteArray(0))
                                        )
                                    }
                                } else {
                                    val host = uri.host?.lowercase() ?: ""
                                    if (StudyNavigationGuard.isCategoricallyForbidden(host) ||
                                        StudyNavigationGuard.isHostBlocked(host) ||
                                        StudyNavigationGuard.isYoutubeHost(host)) {
                                        return WebResourceResponse(
                                            "text/plain",
                                            "UTF-8",
                                            403,
                                            "Forbidden",
                                            emptyMap(),
                                            ByteArrayInputStream(ByteArray(0))
                                        )
                                    }
                                }

                                return super.shouldInterceptRequest(view, request)
                            }

                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                url?.let {
                                    currentHost = try { Uri.parse(it).host ?: "" } catch (_: Exception) { "" }
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                view?.title?.let { t ->
                                    if (t.isNotBlank() && !t.startsWith("http")) currentTitle = t
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress / 100f
                                if (newProgress >= 100) isLoading = false
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (!title.isNullOrBlank() && !title.startsWith("http")) {
                                    currentTitle = title
                                }
                            }

                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: Message?
                            ): Boolean {
                                // For OAuth popup flows (e.g. Google Sign-in popup), create a transport WebView
                                val transport = resultMsg?.obj as? WebView.WebViewTransport
                                val popupWv = WebView(ctx).apply {
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                                            val popupUrl = req?.url?.toString() ?: return false
                                            val isAllowed = StudyNavigationGuard.isNavPermitted(popupUrl, approvedHosts, telegramMode = false)
                                            if (!isAllowed) {
                                                Toast.makeText(ctx, "Focus Guard: এই ডোমেইনটি অনুমোদিত নয় ($popupUrl)", Toast.LENGTH_SHORT).show()
                                                v?.stopLoading()
                                                v?.loadUrl("about:blank")
                                                return true
                                            }
                                            view?.loadUrl(popupUrl)
                                            return true
                                        }

                                        override fun onPageStarted(v: WebView?, url: String?, favicon: Bitmap?) {
                                            if (!url.isNullOrBlank() && url != "about:blank") {
                                                val isAllowed = StudyNavigationGuard.isNavPermitted(url, approvedHosts, telegramMode = false)
                                                if (!isAllowed) {
                                                    Toast.makeText(ctx, "Focus Guard: এই ডোমেইনটি অনুমোদিত নয় ($url)", Toast.LENGTH_SHORT).show()
                                                    v?.stopLoading()
                                                    v?.loadUrl("about:blank")
                                                    return
                                                }
                                                view?.loadUrl(url)
                                            }
                                        }
                                    }
                                }
                                transport?.webView = popupWv
                                resultMsg?.sendToTarget()
                                return true
                            }
                        }

                        loadUrl(initialUrl)
                        webViewInstance = this
                        onRegisterWebView(this)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
