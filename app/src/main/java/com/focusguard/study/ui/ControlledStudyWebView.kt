package com.focusguard.study.ui

import android.annotation.SuppressLint
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
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import com.focusguard.study.filter.FocusInputFilter
import com.focusguard.study.filter.StudyNavigationGuard
import com.focusguard.study.telegram.TelegramDestinationInfo
import com.focusguard.study.telegram.TelegramLinkParser
import com.focusguard.ui.theme.DarkBackground
import com.focusguard.ui.theme.GuardGreenText
import java.io.ByteArrayInputStream

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ControlledStudyWebView(
    initialUrl: String,
    approvedHosts: Set<String>,
    isTelegramMode: Boolean = false,
    isTelegramLoginFlow: Boolean = false,
    approvedTelegramPeerKeys: Set<String> = emptySet(),
    targetTelegramRoute: String = "",
    telegramSourceUrl: String = "",
    policyRevision: Int = 0,
    onTelegramLinkIntercepted: (TelegramDestinationInfo) -> Unit = {},
    onTelegramLoginCompleted: () -> Unit = {},
    onUnapprovedTelegramPeer: (String, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
    onExplicitAttempt: (String) -> Unit = {},
    onTitleChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var customVideoView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var lastPolicyRevision by remember { mutableIntStateOf(policyRevision) }
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

    Box(modifier = modifier.fillMaxSize().background(DarkBackground)) {
        if (customVideoView != null) {
            // Fullscreen video player view container
            AndroidView(
                factory = { customVideoView!! },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isLoading && loadProgress < 1f) {
                    LinearProgressIndicator(
                        progress = { loadProgress },
                        color = GuardGreenText,
                        modifier = Modifier.fillMaxWidth().height(3.dp)
                    )
                }

                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            val downloadMgr = StudyDownloadManager(ctx)
                            val twebAssetsHandler = WebViewAssetLoader.AssetsPathHandler(ctx)
                            val webAssetLoader = WebViewAssetLoader.Builder()
                                .addPathHandler("/assets/telegram-web-k/", object : WebViewAssetLoader.PathHandler {
                                    override fun handle(path: String): WebResourceResponse? {
                                        return if (path.startsWith("assets/")) {
                                            twebAssetsHandler.handle("focus-web-public/${path.removePrefix("assets/")}")
                                        } else {
                                            twebAssetsHandler.handle("telegram-web-k/$path")
                                        }
                                    }
                                })
                                .addPathHandler("/assets/", object : WebViewAssetLoader.PathHandler {
                                    override fun handle(path: String): WebResourceResponse? {
                                        return twebAssetsHandler.handle("focus-web-public/$path")
                                    }
                                })
                                .build()
                            val controlledWebView = this

                            fun currentPageAllowed(): Boolean = StudyNavigationGuard.isNavPermitted(
                                controlledWebView.url,
                                approvedHosts,
                                isTelegramMode
                            )

                            fun telegramFrontendActive(): Boolean = isTelegramMode &&
                                Uri.parse(controlledWebView.url ?: "").host.equals("appassets.androidplatform.net", ignoreCase = true)

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

                                // Custom User-Agent to ensure modern Web K compatibility and HTML5 video streaming
                                userAgentString = "Mozilla/5.0 (Linux; Android 15; SM-A356E) AppleWebKit/537.36 " +
                                        "(KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
                            }

                            // Enable persistent cookies
                            val cookieManager = CookieManager.getInstance()
                            cookieManager.setAcceptCookie(true)
                            cookieManager.setAcceptThirdPartyCookies(this, true)

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isTelegramMode) {
                                android.webkit.ServiceWorkerController.getInstance().setServiceWorkerClient(
                                    object : android.webkit.ServiceWorkerClient() {
                                        override fun shouldInterceptRequest(request: WebResourceRequest?): WebResourceResponse? {
                                            val requestUrl = request?.url ?: return null
                                            return webAssetLoader.shouldInterceptRequest(requestUrl)
                                        }
                                    }
                                )
                            }

                            // Download listener for PDFs, documents, videos, and blobs
                            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                                if (url.startsWith("blob:", ignoreCase = true)) {
                                    // Fetch blob via JavaScript and forward to native bridge
                                    evaluateJavascript(
                                        """
                                        (function() {
                                          fetch('$url')
                                            .then(function(res) { return res.blob(); })
                                            .then(function(blob) {
                                              var reader = new FileReader();
                                              reader.onloadend = function() {
                                                if (window.StudyDownloadBridge) {
                                                  window.StudyDownloadBridge.saveBlob(reader.result, '', blob.type);
                                                }
                                              };
                                              reader.readAsDataURL(blob);
                                            });
                                        })();
                                        """.trimIndent(),
                                        null
                                    )
                                } else {
                                    downloadMgr.startDownload(url, userAgent, contentDisposition, mimetype, contentLength)
                                }
                            }

                            // Native JavaScript bridge for in-page blob and intent downloads
                            addJavascriptInterface(
                                object {
                                    @JavascriptInterface
                                    fun saveBlob(base64Data: String, fileName: String?, mimeType: String?) {
                                        post {
                                            if (currentPageAllowed()) {
                                                downloadMgr.saveBlob(base64Data, fileName, mimeType)
                                            }
                                        }
                                    }

                                    @JavascriptInterface
                                    fun triggerDownload(url: String, fileName: String?) {
                                        post {
                                            if (currentPageAllowed() && StudyNavigationGuard.isNavPermitted(url, approvedHosts, isTelegramMode)) {
                                                downloadMgr.startDownload(
                                                    url = url,
                                                    userAgent = settings.userAgentString,
                                                    preferredFileName = fileName
                                                )
                                            } else {
                                                Toast.makeText(ctx, "Download URL blocked by policy", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                com.focusguard.study.filter.StudyDownloadInterceptor.JS_BRIDGE_NAME
                            )

                            // Native JavaScript bridge for Focus Input Filter
                            addJavascriptInterface(
                                object {
                                    @JavascriptInterface
                                    fun onExplicitAttempt(detectedWord: String) {
                                        post {
                                            onExplicitAttempt(detectedWord)
                                        }
                                    }
                                },
                                "FocusInputBridge"
                            )

                            // This bridge exists only on the app-bundled Telegram frontend.
                            if (isTelegramMode) addJavascriptInterface(
                                object {
                                    @JavascriptInterface
                                    fun onLoginSuccess() {
                                        post {
                                            if (isTelegramLoginFlow && telegramFrontendActive()) {
                                                onTelegramLoginCompleted()
                                            }
                                        }
                                    }

                                    @JavascriptInterface
                                    fun isPeerApproved(peerId: String, peerType: String): Boolean {
                                        if (isTelegramLoginFlow) return true
                                        return approvedTelegramPeerKeys.contains("$peerType:$peerId")
                                    }

                                    @JavascriptInterface
                                    fun onBlockedPeer(peerId: String, peerType: String, title: String, username: String, sourceUrl: String) {
                                        post {
                                            if (telegramFrontendActive() && !isTelegramLoginFlow) {
                                                onUnapprovedTelegramPeer(
                                                    peerId,
                                                    peerType,
                                                    title,
                                                    username,
                                                    telegramSourceUrl.ifBlank { sourceUrl }
                                                )
                                            }
                                        }
                                    }
                                },
                                "FocusGuardStudyBridge"
                            )

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    loadProgress = newProgress / 100f
                                    isLoading = newProgress < 100
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    if (!title.isNullOrBlank()) {
                                        onTitleChanged(title)
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
                                                if (!StudyNavigationGuard.isNavPermitted(popupUrl, approvedHosts, isTelegramMode)) {
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
                                                    if (!StudyNavigationGuard.isNavPermitted(url, approvedHosts, isTelegramMode)) {
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

                                    if (isTelegramMode && request.url.host.equals("appassets.androidplatform.net", true) &&
                                        StudyNavigationGuard.isNavPermitted(targetUrl, approvedHosts, telegramMode = true)) {
                                        return false
                                    }

                                    // 1. Intercept 1DM / IDM / ADM intent URLs from cloud drive indexes
                                    if (scheme == "intent") {
                                        val innerHttp = StudyDownloadManager.extractInnerHttpUrlFromIntent(targetUrl)
                                        if (innerHttp != null) {
                                            if (StudyNavigationGuard.isNavPermitted(innerHttp, approvedHosts, isTelegramMode)) {
                                                downloadMgr.startDownload(innerHttp, settings.userAgentString)
                                                return true
                                            }
                                        }
                                        return true // Prevent browser escape for unknown intent targets
                                    }

                                    // 2. Intercept Telegram links
                                    if (TelegramLinkParser.isTelegramUrl(targetUrl)) {
                                        val dest = TelegramLinkParser.parseDestination(targetUrl)
                                        if (dest != null) {
                                            if (isTelegramMode && dest.webkRouteHash == targetTelegramRoute) {
                                                return false // Allow loading the designated route inside Telegram view
                                            }
                                            onTelegramLinkIntercepted(dest)
                                            return true
                                        }
                                    }

                                    // 3. Intercept non-web schemes
                                    if (scheme != "http" && scheme != "https") {
                                        // Disallow external application intent launching to prevent browser escape
                                        return true
                                    }

                                    // 3.5. Intercept explicit keywords in URL navigation or search query
                                    val explicitHit = FocusInputFilter.explicitHit(targetUrl)
                                    if (explicitHit != null) {
                                        onExplicitAttempt(explicitHit)
                                        Toast.makeText(
                                            ctx,
                                            "Navigation blocked: explicit content detected",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return true
                                    }

                                    // 4. Intercept downloadable educational resources & documents directly
                                    if (StudyNavigationGuard.isDownloadableUrl(targetUrl)) {
                                        if (StudyNavigationGuard.isNavPermitted(targetUrl, approvedHosts, isTelegramMode)) {
                                            downloadMgr.startDownload(targetUrl, settings.userAgentString)
                                            return true // Handled via background DownloadManager, keep webpage view intact
                                        }
                                    }

                                    // 5. Check website allowlist
                                    if (!StudyNavigationGuard.isNavPermitted(targetUrl, approvedHosts, isTelegramMode)) {
                                        Toast.makeText(
                                            ctx,
                                            "Navigation blocked: Domain not in approved study list",
                                            Toast.LENGTH_SHORT
                                        ).show()
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
                                    if (isTelegramMode && uri.host.equals("appassets.androidplatform.net", true)) {
                                        return webAssetLoader.shouldInterceptRequest(uri)
                                    }
                                    val isMain = request?.isForMainFrame ?: false
                                    if (isMain) {
                                        val permitted = StudyNavigationGuard.isNavPermitted(
                                            urlStr,
                                            approvedHosts,
                                            telegramMode = isTelegramMode,
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
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    // Inject input filter & download interceptor at page start
                                    view?.evaluateJavascript(FocusInputFilter.buildInputFilterJs(), null)
                                    view?.evaluateJavascript(com.focusguard.study.filter.StudyDownloadInterceptor.buildDownloadInterceptorJs(), null)

                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    // Re-assert injections on page finish
                                    view?.evaluateJavascript(FocusInputFilter.buildInputFilterJs(), null)
                                    view?.evaluateJavascript(com.focusguard.study.filter.StudyDownloadInterceptor.buildDownloadInterceptorJs(), null)

                                }
                            }

                            loadUrl(initialUrl)
                            webViewInstance = this
                        }
                    },
                    update = { view ->
                        if (policyRevision != lastPolicyRevision) {
                            lastPolicyRevision = policyRevision
                            view.reload()
                        }
                        // Update route if in Telegram mode
                        if (isTelegramMode && targetTelegramRoute.isNotEmpty() && !isTelegramLoginFlow) {
                            view.evaluateJavascript(
                                "if (location.hash !== ${org.json.JSONObject.quote(targetTelegramRoute)}) location.hash = ${org.json.JSONObject.quote(targetTelegramRoute)};",
                                null
                            )
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
