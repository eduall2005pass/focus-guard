package com.focusguard.study.filter

object StudyDownloadInterceptor {

    const val JS_BRIDGE_NAME = "StudyDownloadBridge"

    /**
     * Builds client-side JavaScript to seamlessly intercept blob: downloads,
     * 1DM / IDM / ADM intent download links, and HTML5 <a download> clicks.
     * Passes raw file payloads or direct URLs to the native Android StudyDownloadBridge.
     */
    fun buildDownloadInterceptorJs(): String {
        return """
(function() {
  'use strict';
  if (window.__focusDownloadInterceptorInjected) return;
  window.__focusDownloadInterceptorInjected = true;

  function convertBlobAndSend(blobUrl, filename, mimeType) {
    fetch(blobUrl)
      .then(function(res) {
        if (!mimeType) mimeType = res.headers.get('content-type') || 'application/pdf';
        return res.blob();
      })
      .then(function(blob) {
        var reader = new FileReader();
        reader.onloadend = function() {
          var base64 = reader.result;
          if (window.StudyDownloadBridge && window.StudyDownloadBridge.saveBlob) {
            window.StudyDownloadBridge.saveBlob(base64, filename || '', blob.type || mimeType || '');
          }
        };
        reader.readAsDataURL(blob);
      })
      .catch(function(err) {
        console.error('[FocusGuard] Blob conversion error:', err);
      });
  }

  // Intercept click on anchors
  document.addEventListener('click', function(e) {
    var a = e.target.closest('a');
    if (!a) return;

    var href = a.getAttribute('href') || '';
    var downloadAttr = a.getAttribute('download') || '';

    // 1. Intercept blob: hrefs
    if (href.indexOf('blob:') === 0) {
      e.preventDefault();
      e.stopPropagation();
      convertBlobAndSend(href, downloadAttr, '');
      return;
    }

    // 2. Intercept intent: download URLs (common in @googledrive/index and 1DM buttons)
    if (href.indexOf('intent:') === 0) {
      e.preventDefault();
      e.stopPropagation();
      var match = href.match(/^intent:(https?:\/\/[^#]+)/i);
      if (match && match[1]) {
        var directUrl = match[1];
        if (window.StudyDownloadBridge && window.StudyDownloadBridge.triggerDownload) {
          window.StudyDownloadBridge.triggerDownload(directUrl, downloadAttr || '');
        }
      }
      return;
    }
  }, true);

  // Hook window.open for blob downloads
  var origOpen = window.open;
  window.open = function(url, target, features) {
    if (typeof url === 'string' && url.indexOf('blob:') === 0) {
      convertBlobAndSend(url, '', '');
      return null;
    }
    return origOpen.apply(this, arguments);
  };
})();
        """.trimIndent()
    }
}
