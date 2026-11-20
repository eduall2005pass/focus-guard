package com.focusguard.study

import com.focusguard.study.filter.StudyNavigationGuard
import org.junit.Assert.*
import org.junit.Test

class StudyNavigationGuardTest {

    private val sampleApprovedHosts = setOf("boimate.com", "du.ac.bd", "gtc.edu.bd")

    @Test
    fun testYouTubeUrlsAreStrictlyBlocked() {
        val youtubeUrls = listOf(
            "https://www.youtube.com",
            "https://m.youtube.com",
            "https://youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtu.be/dQw4w9WgXcQ",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "https://music.youtube.com",
            "https://www.youtube.com/shorts/abcdef12345"
        )
        for (url in youtubeUrls) {
            assertFalse("YouTube URL must be blocked: $url", StudyNavigationGuard.isNavPermitted(url, sampleApprovedHosts))
        }
    }

    @Test
    fun testApprovedHostsArePermitted() {
        val allowedUrls = listOf(
            "https://boimate.com",
            "https://du.ac.bd/study",
            "https://gtc.edu.bd/portal"
        )
        for (url in allowedUrls) {
            assertTrue("Approved study host must be allowed: $url", StudyNavigationGuard.isNavPermitted(url, sampleApprovedHosts))
        }
    }

    @Test
    fun testEssentialAuthHostsArePermitted() {
        val authUrls = listOf(
            "https://accounts.google.com/o/oauth2/auth",
            "https://appleid.apple.com/auth/authorize",
            "https://challenges.cloudflare.com/turnstile/v0/api.js"
        )
        for (url in authUrls) {
            assertTrue("Essential auth/supporting host must be allowed: $url", StudyNavigationGuard.isNavPermitted(url, sampleApprovedHosts))
        }
    }

    @Test
    fun testUnapprovedDomainsAreStrictlyBlocked() {
        val blockedUrls = listOf(
            "https://facebook.com",
            "https://instagram.com",
            "https://reddit.com",
            "https://google.com/search?q=test",
            "https://bing.com",
            "https://random-unapproved-site.org"
        )
        for (url in blockedUrls) {
            assertFalse("Unapproved URL must be blocked: $url", StudyNavigationGuard.isNavPermitted(url, sampleApprovedHosts))
        }
    }
}
