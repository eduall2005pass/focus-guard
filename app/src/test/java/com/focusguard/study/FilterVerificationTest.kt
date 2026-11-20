package com.focusguard.study

import com.focusguard.study.filter.FocusInputFilter
import com.focusguard.study.filter.StudyNavigationGuard
import org.junit.Assert.*
import org.junit.Test

class FilterVerificationTest {

    @Test
    fun testExplicitHitsInEnglishAndLeet() {
        assertNotNull(FocusInputFilter.explicitHit("porn"))
        assertNotNull(FocusInputFilter.explicitHit("p0rn"))
        assertNotNull(FocusInputFilter.explicitHit("p o r n"))
        assertNotNull(FocusInputFilter.explicitHit("this is xxx content"))
        assertNotNull(FocusInputFilter.explicitHit("https://example.com/search?q=sexy+video"))
        assertNotNull(FocusInputFilter.explicitHit("chudacudi"))
        assertNotNull(FocusInputFilter.explicitHit("cuda cudi"))
    }

    @Test
    fun testExplicitHitsInBengali() {
        assertNotNull(FocusInputFilter.explicitHit("পর্ন"))
        assertNotNull(FocusInputFilter.explicitHit("পর্নোগ্রাফি"))
        assertNotNull(FocusInputFilter.explicitHit("চটি গল্প"))
        assertNotNull(FocusInputFilter.explicitHit("চুদাচুদি"))
        assertNotNull(FocusInputFilter.explicitHit("যৌন"))
    }

    @Test
    fun testSafeWordsNotBlocked() {
        assertNull(FocusInputFilter.explicitHit("x"))
        assertNull(FocusInputFilter.explicitHit("X"))
        assertNull(FocusInputFilter.explicitHit("xx"))
        assertNull(FocusInputFilter.explicitHit("box"))
        assertNull(FocusInputFilter.explicitHit("taxi"))
        assertNull(FocusInputFilter.explicitHit("pixel"))
        assertNull(FocusInputFilter.explicitHit("exercise"))
        assertNull(FocusInputFilter.explicitHit("extra"))
        assertNull(FocusInputFilter.explicitHit("imagine"))
        assertNull(FocusInputFilter.explicitHit("branding"))
        assertNull(FocusInputFilter.explicitHit("medical case discussion"))
        assertNull(FocusInputFilter.explicitHit("bhabishyat"))
        assertNull(FocusInputFilter.explicitHit("https://guessthecase.eu.org/cases/radiology"))
        assertNull(FocusInputFilter.explicitHit("https://booimate.com/courses/pathology"))
    }

    @Test
    fun testNoInWordBlockingInEnglish() {
        // Scunthorpe problem prevention: internal substrings must never trigger a block
        assertNull(FocusInputFilter.explicitHit("antiviral"))
        assertNull(FocusInputFilter.explicitHit("antiviral therapy for hepatitis"))
        assertNull(FocusInputFilter.explicitHit("viral pneumonia"))
        assertNull(FocusInputFilter.explicitHit("viral load testing"))
        assertNull(FocusInputFilter.explicitHit("therapist"))
        assertNull(FocusInputFilter.explicitHit("physical therapist"))
        assertNull(FocusInputFilter.explicitHit("consultant"))
        assertNull(FocusInputFilter.explicitHit("document"))
        assertNull(FocusInputFilter.explicitHit("clinical documentation"))
        assertNull(FocusInputFilter.explicitHit("tetanus"))
        assertNull(FocusInputFilter.explicitHit("tetanus toxoid vaccine"))
        assertNull(FocusInputFilter.explicitHit("manuscript"))
        assertNull(FocusInputFilter.explicitHit("assessment"))
        assertNull(FocusInputFilter.explicitHit("clinical assessment"))
        assertNull(FocusInputFilter.explicitHit("Samsung Pass"))
        assertNull(FocusInputFilter.explicitHit("password manager"))
        assertNull(FocusInputFilter.explicitHit("appetite"))
        assertNull(FocusInputFilter.explicitHit("loss of appetite"))
        assertNull(FocusInputFilter.explicitHit("title"))
        assertNull(FocusInputFilter.explicitHit("case title"))
        assertNull(FocusInputFilter.explicitHit("bamboo"))
        assertNull(FocusInputFilter.explicitHit("compassion"))
        assertNull(FocusInputFilter.explicitHit("secular"))
    }

    @Test
    fun testNoInWordBlockingInBengali() {
        // Bengali legitimate words & medical expressions must never be blocked
        assertNull(FocusInputFilter.explicitHit("রোগীর জ্বর বাড়া"))
        assertNull(FocusInputFilter.explicitHit("ওজন বাড়া"))
        assertNull(FocusInputFilter.explicitHit("মায়ের দুধ শিশুর জন্য উত্তম"))
        assertNull(FocusInputFilter.explicitHit("গরুর খাঁটি দুধ"))
        assertNull(FocusInputFilter.explicitHit("প্রস্তাবনা"))
        assertNull(FocusInputFilter.explicitHit("মস্তক ও করোটি"))
        assertNull(FocusInputFilter.explicitHit("আমার ভাবি অসুস্থ"))
        assertNull(FocusInputFilter.explicitHit("পুংলিঙ্গ ও স্ত্রীলিঙ্গ"))
    }

    @Test
    fun testStudyNavigationGuardApprovedSites() {
        val approvedHosts = setOf(
            "guessthecase.eu.org",
            "eco.du.ac.bd",
            "*.eco.du.ac.bd",
            "du.ac.bd",
            "*.du.ac.bd",
            "boimate.com",
            "*.boimate.com",
            "gemini.google.com",
            "*.gemini.google.com",
            "notebooklm.google.com",
            "medical-contents.abdursakib.workers.dev",
            "muslimbangla.com",
            "ihadis.com",
            "livemcq.com",
            "web.livemcq.com"
        )

        // Approved study sites
        assertTrue(StudyNavigationGuard.isNavPermitted("https://guessthecase.eu.org/", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://guessthecase.eu.org/cases/123", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://eco.du.ac.bd/", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://eco.du.ac.bd/application_tracking", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://eco.du.ac.bd/instructions", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://du.ac.bd/", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://admission.eis.du.ac.bd/", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://boimate.com/", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://gemini.google.com/", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://gemini.google.com/app", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://notebooklm.google.com/", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://web.livemcq.com/exam", approvedHosts))

        // Supporting CDNs and static APIs
        assertTrue(StudyNavigationGuard.isNavPermitted("https://fonts.googleapis.com/css?family=Roboto", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://cdnjs.cloudflare.com/ajax/libs/pdf.js/2.6.347/pdf.min.js", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://cdn.jsdelivr.net/npm/bootstrap@5/dist/css/bootstrap.min.css", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://accounts.google.com/signin/v2/identifier", approvedHosts))
    }

    @Test
    fun testStudyNavigationGuardBlockedSites() {
        val approvedHosts = setOf("guessthecase.eu.org", "booimate.com")

        // Search engines
        assertFalse(StudyNavigationGuard.isNavPermitted("https://www.google.com/search?q=test", approvedHosts))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://google.com/", approvedHosts))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://www.bing.com/", approvedHosts))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://duckduckgo.com/", approvedHosts))

        // Social media
        assertFalse(StudyNavigationGuard.isNavPermitted("https://www.facebook.com/", approvedHosts))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://twitter.com/home", approvedHosts))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://x.com/", approvedHosts))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://www.instagram.com/", approvedHosts))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://www.reddit.com/r/medical", approvedHosts))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://www.tiktok.com/", approvedHosts))

        // Adult content
        assertFalse(StudyNavigationGuard.isNavPermitted("https://www.pornhub.com/", approvedHosts))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://www.xvideos.com/", approvedHosts))

        // Explicit input in URL
        assertFalse(StudyNavigationGuard.isNavPermitted("https://guessthecase.eu.org/search?q=sexy+porn", approvedHosts))
    }

    @Test
    fun testYouTubeRestriction() {
        val approvedHosts = setOf("guessthecase.eu.org")

        // Specific video strictly blocked (zero YouTube exceptions)
        assertFalse(StudyNavigationGuard.isNavPermitted("https://www.youtube.com/watch?v=dQw4w9WgXcQ", approvedHosts))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://youtu.be/dQw4w9WgXcQ", approvedHosts))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://www.youtube.com/embed/dQw4w9WgXcQ", approvedHosts))

        // Full browsing / homepage denied
        assertFalse(StudyNavigationGuard.isNavPermitted("https://www.youtube.com/", approvedHosts))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://www.youtube.com/feed/explore", approvedHosts))
    }

    @Test
    fun testTelegramLinkParser() {
        val dest1 = com.focusguard.study.telegram.TelegramLinkParser.parseDestination("https://t.me/pdf258upload")
        assertNotNull(dest1)
        assertEquals("pdf258upload", dest1?.username)
        assertEquals("#@pdf258upload", dest1?.webkRouteHash)

        val dest2 = com.focusguard.study.telegram.TelegramLinkParser.parseDestination("https://t.me/c/4415493319/55")
        assertNotNull(dest2)
        assertEquals("-4415493319", dest2?.webkPeerId)
        assertEquals(55, dest2?.postId)
        assertEquals("#/im?p=-4415493319&post=55", dest2?.webkRouteHash)

        val dest3 = com.focusguard.study.telegram.TelegramLinkParser.parseDestination("tg://resolve?domain=medcase&post=10")
        assertNotNull(dest3)
        assertEquals("medcase", dest3?.username)
        assertEquals(10, dest3?.postId)
    }

    @Test
    fun testDownloadableUrlsAllowed() {
        val approvedHosts = setOf(
            "guessthecase.eu.org",
            "boimate.com",
            "*.boimate.com",
            "medical-contents.abdursakib.workers.dev",
            "*.workers.dev"
        )

        val boimateWorkerDl = "https://download.boimatebd.workers.dev/?id=Qk1fMTVyaG9WNC1iNUQzVktualZCYTg0OHVoVFl4UUduRkY4&name=book.pdf"
        assertTrue(StudyNavigationGuard.isDownloadableUrl(boimateWorkerDl))
        assertTrue(StudyNavigationGuard.isNavPermitted(boimateWorkerDl, approvedHosts))

        val googleDriveUsercontentDl = "https://drive.usercontent.google.com/download?id=15rhoV4-b5D3VKnjVBa848uhTYxQGnFF8&nyYw7FbT6v"
        assertTrue(StudyNavigationGuard.isDownloadableUrl(googleDriveUsercontentDl))
        assertTrue(StudyNavigationGuard.isNavPermitted(googleDriveUsercontentDl, approvedHosts))

        val googleDriveUcDl = "https://drive.google.com/uc?export=download&id=15rhoV4"
        assertTrue(StudyNavigationGuard.isDownloadableUrl(googleDriveUcDl))
        assertTrue(StudyNavigationGuard.isNavPermitted(googleDriveUcDl, approvedHosts))

        val medicalContentsWorkerDl = "https://medical-contents.abdursakib.workers.dev/0:/Radiology/Brain_MRI.pdf"
        assertTrue(StudyNavigationGuard.isDownloadableUrl(medicalContentsWorkerDl))
        assertTrue(StudyNavigationGuard.isNavPermitted(medicalContentsWorkerDl, approvedHosts))

        val boimateMediaDl = "https://boimate.com/media/1892/book.pdf"
        assertTrue(StudyNavigationGuard.isDownloadableUrl(boimateMediaDl))
        assertTrue(StudyNavigationGuard.isNavPermitted(boimateMediaDl, approvedHosts))
    }

    @Test
    fun testCategoricallyBlockedDownloadsDenied() {
        val approvedHosts = setOf("guessthecase.eu.org")

        // Adult site video cannot bypass download check
        val pornUrl = "https://www.pornhub.com/video.mp4"
        assertFalse(StudyNavigationGuard.isDownloadableUrl(pornUrl))
        assertFalse(StudyNavigationGuard.isNavPermitted(pornUrl, approvedHosts))

        // Search engine cannot masquerade as PDF download
        val searchPdf = "https://www.google.com/search.pdf"
        assertFalse(StudyNavigationGuard.isDownloadableUrl(searchPdf))
        assertFalse(StudyNavigationGuard.isNavPermitted(searchPdf, approvedHosts))

        // Social media feed file cannot bypass
        val fbFile = "https://www.facebook.com/download/archive.zip"
        assertFalse(StudyNavigationGuard.isDownloadableUrl(fbFile))
        assertFalse(StudyNavigationGuard.isNavPermitted(fbFile, approvedHosts))
    }

    @Test
    fun testTelegramFrontendRequiresStudyModeAndOnlyKFrontendCanBeTopLevel() {
        val approvedHosts = setOf("guessthecase.eu.org")

        assertFalse(StudyNavigationGuard.isNavPermitted("https://web.telegram.org/k/", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://web.telegram.org/k/", approvedHosts, telegramMode = true))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://web.telegram.org/a/", approvedHosts, telegramMode = true))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://t.me/pdf258upload", approvedHosts, telegramMode = true))
        assertTrue(StudyNavigationGuard.isNavPermitted(
            "https://web.telegram.org/assets/app.js", approvedHosts, telegramMode = true, mainFrame = false
        ))
        assertFalse(StudyNavigationGuard.isNavPermitted(
            "https://unapproved.example/assets/app.js", approvedHosts, telegramMode = true, mainFrame = false
        ))
        assertFalse(StudyNavigationGuard.isNavPermitted(
            "https://appassets.androidplatform.net/assets/telegram-web-k/index.html", approvedHosts
        ))
        assertTrue(StudyNavigationGuard.isNavPermitted(
            "https://appassets.androidplatform.net/assets/telegram-web-k/index.html", approvedHosts, telegramMode = true
        ))
    }

    @Test
    fun testDownloadExtensionDoesNotBypassApprovedHostList() {
        assertFalse(StudyNavigationGuard.isNavPermitted("https://unapproved.example/lecture.pdf", emptySet()))
    }

    @Test
    fun testStudyDownloadManagerFileNameResolution() {
        // Query param resolution (used by download.boimatebd.workers.dev)
        val queryUrl = "https://download.boimatebd.workers.dev/?id=123&name=%E0%A6%B6%E0%A6%BF%E0%A6%95%E0%A7%8D%E0%A6%B7%E0%A6%BE%E0%A6%B0%E0%A7%8D%E0%A6%A5%E0%A7%80%E0%A6%A6%E0%A7%87%E0%A6%B0_%E0%A6%B8%E0%A6%AB%E0%A6%B2%E0%A6%A4%E0%A6%BE%E0%A6%B0_%E0%A6%B0%E0%A6%BE%E0%A6%9C%E0%A6%AA%E0%A6%A5_%5Bboimate.com%5D.pdf"
        val name1 = com.focusguard.study.ui.StudyDownloadManager.resolveFileName(queryUrl)
        assertTrue(name1.contains("শিক্ষার্থীদের_সফলতার_রাজপথ"))
        assertTrue(name1.endsWith(".pdf"))

        // Standard Content-Disposition
        val name2 = com.focusguard.study.ui.StudyDownloadManager.resolveFileName(
            url = "https://drive.usercontent.google.com/download?id=123",
            contentDisposition = "attachment; filename=\"radiology_atlas.pdf\""
        )
        assertEquals("radiology_atlas.pdf", name2)

        // RFC 5987 UTF-8 encoded Content-Disposition
        val name3 = com.focusguard.study.ui.StudyDownloadManager.resolveFileName(
            url = "https://example.com/dl",
            contentDisposition = "attachment; filename*=UTF-8''%E0%A6%AC%E0%A6%87.pdf"
        )
        assertEquals("বই.pdf", name3)

        // Intent URL extraction
        val intentUrl = "intent:https://medical-contents.abdursakib.workers.dev/0:/lecture1.pdf#Intent;package=com.dv.adm;end"
        val extracted = com.focusguard.study.ui.StudyDownloadManager.extractInnerHttpUrlFromIntent(intentUrl)
        assertEquals("https://medical-contents.abdursakib.workers.dev/0:/lecture1.pdf", extracted)
    }

    @Test
    fun testHardlyBlockedWebsitesNeverAllowedEvenIfInApprovedList() {
        // Blocked is strictly blocked under ANY circumstance.
        // Even if a blocked domain is passed into approvedHosts, it MUST BE REJECTED!
        val maliciousApprovedList = setOf(
            "pornhub.com",
            "www.pornhub.com",
            "facebook.com",
            "www.facebook.com",
            "google.com",
            "www.google.com",
            "x.com",
            "twitter.com",
            "instagram.com",
            "tiktok.com"
        )

        // Adult sites
        assertFalse(StudyNavigationGuard.isNavPermitted("https://pornhub.com/", maliciousApprovedList))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://www.pornhub.com/video", maliciousApprovedList))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://pornhub.com/video.mp4", maliciousApprovedList))

        // Social media
        assertFalse(StudyNavigationGuard.isNavPermitted("https://facebook.com/", maliciousApprovedList))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://www.facebook.com/feed", maliciousApprovedList))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://x.com/home", maliciousApprovedList))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://instagram.com/", maliciousApprovedList))

        // Search engine
        assertFalse(StudyNavigationGuard.isNavPermitted("https://google.com/", maliciousApprovedList))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://www.google.com/search?q=test", maliciousApprovedList))
    }

    @Test
    fun testBackendAndCdnAllowlist() {
        val approvedStudyHosts = setOf("guessthecase.eu.org", "boimate.com")

        // 1. Google Fonts
        assertTrue(StudyNavigationGuard.isNavPermitted("https://fonts.googleapis.com/css2?family=Inter", approvedStudyHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://fonts.gstatic.com/s/inter/v13/font.woff2", approvedStudyHosts))

        // 2. Cloudflare CDN
        assertTrue(StudyNavigationGuard.isNavPermitted("https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.0.0/pdf.min.js", approvedStudyHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://static.cloudflareinsights.com/beacon.min.js", approvedStudyHosts))

        // 3. jsDelivr
        assertTrue(StudyNavigationGuard.isNavPermitted("https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css", approvedStudyHosts))

        // 4. CloudFront
        assertTrue(StudyNavigationGuard.isNavPermitted("https://d123456789.cloudfront.net/assets/viewer.js", approvedStudyHosts))

        // 5. Supabase Backend
        assertTrue(StudyNavigationGuard.isNavPermitted("https://xyzcompany.supabase.co/rest/v1/cases", approvedStudyHosts))

        // 6. Firebase Backend
        assertTrue(StudyNavigationGuard.isNavPermitted("https://my-study-app.firebaseapp.com/bundle.js", approvedStudyHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://my-study-app.firebaseio.com/.json", approvedStudyHosts))

        // 7. Cloudflare Workers
        assertTrue(StudyNavigationGuard.isNavPermitted("https://medical-contents.abdursakib.workers.dev/0:/lecture.pdf", approvedStudyHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://download.boimatebd.workers.dev/?id=99", approvedStudyHosts))

        // 8. Google Drive Direct Binary Downloads (PDF, Videos, Docs)
        assertTrue(StudyNavigationGuard.isNavPermitted("https://drive.usercontent.google.com/download?id=12345&export=download", approvedStudyHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://drive.google.com/uc?export=download&id=67890", approvedStudyHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://docs.googleusercontent.com/docs/securesc/xyz123", approvedStudyHosts))
    }

    @Test
    fun testGeminiPermanentlyAllowedAsStudyResource() {
        // gemini.google.com must NOT be blocked by host blocklist or categorical filter
        assertFalse(StudyNavigationGuard.isHostBlocked("gemini.google.com"))
        assertFalse(StudyNavigationGuard.isCategoricallyForbidden("gemini.google.com"))

        val approvedHosts = setOf("gemini.google.com", "*.gemini.google.com")
        assertTrue(StudyNavigationGuard.isNavPermitted("https://gemini.google.com/", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://gemini.google.com/app", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://gemini.google.com/share/abc", approvedHosts))

        // Supporting Google APIs required by Gemini Web
        assertTrue(StudyNavigationGuard.isNavPermitted("https://alkali-pa.googleapis.com/v1/models", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://accounts.google.com/signin/v2/identifier", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://bard.google.com/", approvedHosts))
    }

    @Test
    fun testGoogleAccountSwitchingAndAvatarPermitted() {
        // Account switcher, avatar and session endpoints must not be blocked
        assertFalse(StudyNavigationGuard.isHostBlocked("myaccount.google.com"))
        assertFalse(StudyNavigationGuard.isHostBlocked("accounts.google.com"))
        assertFalse(StudyNavigationGuard.isHostBlocked("ogs.google.com"))
        assertFalse(StudyNavigationGuard.isCategoricallyForbidden("myaccount.google.com"))
        assertFalse(StudyNavigationGuard.isCategoricallyForbidden("accounts.google.com"))
        assertFalse(StudyNavigationGuard.isCategoricallyForbidden("ogs.google.com"))

        val approvedHosts = setOf("gemini.google.com", "*.gemini.google.com")
        // Account chooser and session addition navigation
        assertTrue(StudyNavigationGuard.isNavPermitted("https://accounts.google.com/AccountChooser?continue=https://gemini.google.com/", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://accounts.google.com/AddSession?continue=https://gemini.google.com/", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://myaccount.google.com/", approvedHosts))
        assertTrue(StudyNavigationGuard.isNavPermitted("https://ogs.google.com/u/0/widget/app", approvedHosts))

        // Web search remains strictly blocked
        assertFalse(StudyNavigationGuard.isNavPermitted("https://www.google.com/search?q=test", approvedHosts))
        assertFalse(StudyNavigationGuard.isNavPermitted("https://google.com/", approvedHosts))
    }
}
