package com.focusguard.study.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.focusguard.study.filter.FocusInputFilter
import com.focusguard.study.filter.StudyNavigationGuard
import com.focusguard.study.model.ApprovedWebsite
import com.focusguard.ui.theme.*
import java.io.ByteArrayInputStream

/**
 * Dedicated True Fullscreen Browser for educational study sites (BoiMate, Medical Contents, GTC, etc.).
 * Hides all app navigation chrome, giving the user 100% display area for reading books and lecture notes.
 */
@Composable
fun FullscreenStudyBrowser(
    website: ApprovedWebsite,
    approvedHosts: Set<String>,
    onClose: () -> Unit,
    onExplicitAttempt: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var currentTitle by remember { mutableStateOf(website.title) }
    var currentUrl by remember { mutableStateOf(website.url) }
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val cb = fileChooserCallback
        fileChooserCallback = null
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = result.data
            val uris: Array<Uri>? = when {
                intent?.clipData != null -> {
                    val clipData = intent.clipData!!
                    Array(clipData.itemCount) { i -> clipData.getItemAt(i).uri }
                }
                intent?.data != null -> {
                    arrayOf(intent.data!!)
                }
                else -> null
            }
            cb?.onReceiveValue(uris)
        } else {
            cb?.onReceiveValue(null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            fileChooserCallback?.onReceiveValue(null)
            fileChooserCallback = null
        }
    }

    val context = LocalContext.current

    // Hardware / gesture back button support
    BackHandler {
        if (customVideoView != null) {
            customViewCallback?.onCustomViewHidden()
            customVideoView = null
            customViewCallback = null
        } else if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onClose()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        if (customVideoView != null) {
            // Fullscreen video playback view
            AndroidView(
                factory = { customVideoView!! },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
                // Sleek, minimal compact fullscreen header
                Surface(
                    color = DarkSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Back / Exit Button
                            IconButton(
                                onClick = {
                                    if (webViewInstance?.canGoBack() == true) {
                                        webViewInstance?.goBack()
                                    } else {
                                        onClose()
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Site domain badge and title
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentTitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 13.sp
                                    )
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = GuardGreenText,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = website.domain,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = GuardGreenText,
                                            fontSize = 10.sp
                                        )
                                    )
                                }
                            }

                            // Reload button
                            IconButton(
                                onClick = { webViewInstance?.reload() },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Close Fullscreen Button
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close Fullscreen",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // Progress indicator
                        if (isLoading && loadProgress < 1f) {
                            LinearProgressIndicator(
                                progress = { loadProgress },
                                color = GuardGreenText,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp)
                            )
                        }
                    }
                }

                // Fullscreen WebView
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            val downloadMgr = StudyDownloadManager(ctx)

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                cacheMode = WebSettings.LOAD_DEFAULT
                                allowFileAccess = true
                                allowContentAccess = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                setSupportMultipleWindows(true)
                                javaScriptCanOpenWindowsAutomatically = true

                                // Modern mobile Chrome User-Agent
                                userAgentString = "Mozilla/5.0 (Linux; Android 15; SM-A356E) AppleWebKit/537.36 " +
                                        "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                            }

                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            // Native JavaScript bridge for Focus Input Filter
                            addJavascriptInterface(
                                object {
                                    @JavascriptInterface
                                    fun onExplicitAttempt(detectedWord: String) {
                                        post { onExplicitAttempt(detectedWord) }
                                    }
                                },
                                "FocusInputBridge"
                            )

                            // Downloads
                            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                                downloadMgr.startDownload(url, userAgent, contentDisposition, mimetype, contentLength)
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadProgress = newProgress / 100f
                                    isLoading = newProgress < 100
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    if (!title.isNullOrBlank()) {
                                        currentTitle = title
                                    }
                                }

                                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                    customVideoView = view
                                    customViewCallback = callback
                                }

                                override fun onHideCustomView() {
                                    customVideoView = null
                                    customViewCallback?.onCustomViewHidden()
                                    customViewCallback = null
                                }

                                override fun onShowFileChooser(
                                    webView: WebView?,
                                    filePathCallback: ValueCallback<Array<Uri>>?,
                                    fileChooserParams: FileChooserParams?
                                ): Boolean {
                                    fileChooserCallback?.onReceiveValue(null)
                                    fileChooserCallback = filePathCallback

                                    val intent = try {
                                        fileChooserParams?.createIntent()?.apply {
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                        } ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                                            type = "*/*"
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                        }
                                    } catch (_: Exception) {
                                        Intent(Intent.ACTION_GET_CONTENT).apply {
                                            type = "*/*"
                                            addCategory(Intent.CATEGORY_OPENABLE)
                                        }
                                    }

                                    return try {
                                        filePickerLauncher.launch(intent)
                                        true
                                    } catch (e: Exception) {
                                        Log.e("FocusGuard_StudyWeb", "Failed to launch file chooser: ${e.message}")
                                        fileChooserCallback?.onReceiveValue(null)
                                        fileChooserCallback = null
                                        false
                                    }
                                }

                                override fun onCreateWindow(
                                    view: WebView?,
                                    isDialog: Boolean,
                                    isUserGesture: Boolean,
                                    resultMsg: Message?
                                ): Boolean {
                                    val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                    val popupWv = WebView(ctx).apply {
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                        webViewClient = object : WebViewClient() {
                                            override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                                                val popupUrl = req?.url?.toString() ?: return false
                                                if (!StudyNavigationGuard.isNavPermitted(popupUrl, approvedHosts)) {
                                                    Toast.makeText(ctx, "Navigation blocked: Unapproved domain", Toast.LENGTH_SHORT).show()
                                                    v?.stopLoading()
                                                    v?.loadUrl("about:blank")
                                                    return true
                                                }
                                                view?.loadUrl(popupUrl)
                                                v?.stopLoading()
                                                v?.loadUrl("about:blank")
                                                return true
                                            }

                                            override fun onPageStarted(v: WebView?, url: String?, favicon: Bitmap?) {
                                                if (!url.isNullOrBlank() && url != "about:blank") {
                                                    if (!StudyNavigationGuard.isNavPermitted(url, approvedHosts)) {
                                                        Toast.makeText(ctx, "Navigation blocked: Unapproved domain", Toast.LENGTH_SHORT).show()
                                                        v?.stopLoading()
                                                        v?.loadUrl("about:blank")
                                                        return
                                                    }
                                                    view?.loadUrl(url)
                                                    v?.stopLoading()
                                                    v?.loadUrl("about:blank")
                                                }
                                            }
                                        }
                                    }
                                    transport.webView = popupWv
                                    resultMsg.sendToTarget()
                                    return true
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val targetUrl = request?.url?.toString() ?: return false
                                    val scheme = request.url.scheme?.lowercase() ?: ""

                                    // Intercept 1DM / intent URLs from cloud drive indexes
                                    if (scheme == "intent") {
                                        val innerHttp = StudyDownloadManager.extractInnerHttpUrlFromIntent(targetUrl)
                                        if (innerHttp != null && StudyNavigationGuard.isNavPermitted(innerHttp, approvedHosts)) {
                                            downloadMgr.startDownload(innerHttp, settings.userAgentString)
                                            return true
                                        }
                                        return true
                                    }

                                    if (scheme != "http" && scheme != "https") {
                                        return true
                                    }

                                    // Explicit terminology filter
                                    val explicitHit = FocusInputFilter.explicitHit(targetUrl)
                                    if (explicitHit != null) {
                                        onExplicitAttempt(explicitHit)
                                        Toast.makeText(ctx, "Navigation blocked: explicit content detected", Toast.LENGTH_SHORT).show()
                                        return true
                                    }

                                    // Check downloads
                                    if (StudyNavigationGuard.isDownloadableUrl(targetUrl)) {
                                        if (StudyNavigationGuard.isNavPermitted(targetUrl, approvedHosts)) {
                                            downloadMgr.startDownload(targetUrl, settings.userAgentString)
                                            return true
                                        }
                                    }

                                    // Verify navigation permit
                                    if (!StudyNavigationGuard.isNavPermitted(targetUrl, approvedHosts)) {
                                        Toast.makeText(ctx, "Navigation blocked: Unapproved domain", Toast.LENGTH_SHORT).show()
                                        return true
                                    }

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

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        android.util.Log.e("FocusGuard_Web", "WebView Error: ${error?.description} on ${request.url}")
                                    }
                                }

                                override fun onReceivedHttpError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    errorResponse: WebResourceResponse?
                                ) {
                                    super.onReceivedHttpError(view, request, errorResponse)
                                    if (request?.isForMainFrame == true) {
                                        android.util.Log.e("FocusGuard_Web", "HTTP Error: ${errorResponse?.statusCode} on ${request.url}")
                                    }
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    if (url != null) currentUrl = url
                                    try {
                                        view?.evaluateJavascript(FocusInputFilter.buildInputFilterJs(), null)
                                    } catch (_: Exception) {}
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    try {
                                        view?.evaluateJavascript(FocusInputFilter.buildInputFilterJs(), null)
                                    } catch (_: Exception) {}
                                }
                            }

                            loadUrl(website.url)
                            webViewInstance = this
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.apply {
                stopLoading()
                webChromeClient = null
                webViewClient = WebViewClient()
                loadUrl("about:blank")
                removeAllViews()
                destroy()
            }
            webViewInstance = null
        }
    }
}
