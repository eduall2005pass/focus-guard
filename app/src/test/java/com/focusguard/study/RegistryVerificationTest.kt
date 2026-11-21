package com.focusguard.study

import android.content.Context
import com.focusguard.study.repository.ApprovedTelegramRegistry
import com.focusguard.study.repository.StudyWebsiteRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RegistryVerificationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testStudyWebsiteRegistryPredefinedWebsites() {
        val registry = StudyWebsiteRegistry(customStorageDir = tempFolder.root)
        val websites = registry.loadWebsites()

        assertTrue(websites.isNotEmpty())
        assertTrue(websites.any { it.id == "gtc" && it.domain == "guessthecase.eu.org" })
        assertTrue(websites.any { it.id == "gemini" && it.domain == "gemini.google.com" && it.isPredefined })
        assertTrue(websites.any { it.id == "boimate" && it.domain == "boimate.com" })
        assertTrue(websites.any { it.id == "du_portal" && it.domain == "eco.du.ac.bd" && it.url == "https://eco.du.ac.bd/" })
        assertTrue(websites.none { it.id == "booimate" })
        assertTrue(websites.any { it.id == "medical_contents" && it.domain == "medical-contents.abdursakib.workers.dev" })
        assertTrue(websites.none { it.id == "notebooklm" })

        // Predefined website cannot be removed
        assertFalse(registry.removeWebsite("gemini"))

        val allowedHosts = registry.getAllowedHosts()
        assertTrue(allowedHosts.contains("gemini.google.com"))
        assertTrue(allowedHosts.contains("*.gemini.google.com"))
        assertTrue(allowedHosts.contains("accounts.google.com"))
        assertTrue(allowedHosts.contains("myaccount.google.com"))
        assertTrue(allowedHosts.contains("ogs.google.com"))
        assertTrue(allowedHosts.contains("boimate.com"))
        assertTrue(allowedHosts.contains("eco.du.ac.bd"))
        assertTrue(allowedHosts.contains("du.ac.bd"))
        assertTrue(allowedHosts.contains("*.du.ac.bd"))
        assertTrue(allowedHosts.contains("medical-contents.abdursakib.workers.dev"))
        assertTrue(allowedHosts.contains("*.workers.dev"))
    }

    @Test
    fun testStudyWebsiteRegistryAddAndValidate() {
        val registry = StudyWebsiteRegistry(customStorageDir = tempFolder.root)

        // Valid educational website
        val successResult = registry.addWebsite("PubMed", "https://pubmed.ncbi.nlm.nih.gov/")
        assertTrue(successResult.isSuccess)
        assertEquals("pubmed.ncbi.nlm.nih.gov", successResult.getOrNull()?.domain)

        // Invalid: Search engine blocked
        val searchResult = registry.addWebsite("Google Search", "https://www.google.com")
        assertTrue(searchResult.isFailure)

        // Invalid: Adult site blocked
        val adultResult = registry.addWebsite("Bad Site", "https://www.pornhub.com")
        assertTrue(adultResult.isFailure)

        // Invalid: Explicit word blocked
        val explicitResult = registry.addWebsite("Porn Hub", "https://example.org")
        assertTrue(explicitResult.isFailure)
    }

    @Test
    fun testApprovedTelegramRegistryWebKNormalization() {
        val registry = ApprovedTelegramRegistry(customStorageDir = tempFolder.root)

        // Bot API style (-100xxxx) must be normalized to Web K style (-xxxx)
        assertEquals("-4415493319", registry.normalizeToWebKPeerId("-1004415493319"))
        assertEquals("-123456789", registry.normalizeToWebKPeerId("-100123456789"))
        assertEquals("-123456789", registry.normalizeToWebKPeerId("-123456789"))
        assertEquals("-123456789", registry.normalizeToWebKPeerId("123456789"))
    }

    @Test
    fun testApprovedTelegramRegistryDefaultAndApproval() {
        val registry = ApprovedTelegramRegistry(customStorageDir = tempFolder.root)
        val initial = registry.loadApprovedDestinations()

        // Default educational channel must be present
        assertTrue(initial.isNotEmpty())
        assertTrue(initial.any { it.username == "pdf258upload" })

        // Check isApproved with different identifier formats
        assertTrue(registry.isApproved("pdf258upload"))
        assertTrue(registry.isApproved("@pdf258upload"))
        assertTrue(registry.isApproved("-4415493319"))
        assertTrue(registry.isApproved("-1004415493319"))
        assertFalse(registry.isApproved("unapproved_channel_xyz"))

        // Approve new educational channel
        val newApproved = registry.approveDestination(
            rawPeerId = "-100987654321",
            peerType = "channel",
            displayTitle = "Radiology Masterclass",
            username = "radiologymaster",
            sourceUrl = "https://guessthecase.eu.org/cases/101"
        )

        assertEquals("-987654321", newApproved.webkPeerId)
        assertTrue(registry.isApproved("radiologymaster"))
        assertTrue(registry.isApproved("-987654321"))
        assertTrue(registry.isApproved("-100987654321"))

        // Re-load from persistent file to verify durability
        val reloaded = registry.loadApprovedDestinations()
        assertTrue(reloaded.any { it.webkPeerId == "-987654321" })
    }

    @Test
    fun testTelegramRegistryKeepsPeerTypeWhenIdsMatch() {
        val registry = ApprovedTelegramRegistry(customStorageDir = tempFolder.root)
        val channel = registry.approveDestination("-123456789", "channel", "Study channel")
        val group = registry.approveDestination("-123456789", "group", "Study group")

        assertEquals(channel.webkPeerId, group.webkPeerId)
        assertNotEquals(channel.peerType, group.peerType)
        assertTrue(registry.loadApprovedDestinations().any { it.webkPeerId == channel.webkPeerId && it.peerType == "channel" })
        assertTrue(registry.loadApprovedDestinations().any { it.webkPeerId == group.webkPeerId && it.peerType == "group" })
    }
}
