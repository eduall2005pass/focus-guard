package com.focusguard.study

import com.focusguard.dpc.DevicePolicyController
import com.focusguard.study.filter.AddFilterResult
import com.focusguard.study.filter.CustomBlockedFilterRepository
import com.focusguard.study.filter.FocusInputFilter
import com.focusguard.study.filter.StudyNavigationGuard
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CustomBlockedFilterVerificationTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        FocusInputFilter.clearCustomKeywords()
        StudyNavigationGuard.clearCustomDomains()
    }

    @After
    fun tearDown() {
        FocusInputFilter.clearCustomKeywords()
        StudyNavigationGuard.clearCustomDomains()
    }

    @Test
    fun testInitialCounts() {
        val repo = CustomBlockedFilterRepository(customStorageDir = tempFolder.root)
        assertEquals(0, repo.getCustomKeywordCount())
        assertEquals(0, repo.getCustomDomainCount())
        assertEquals(FocusInputFilter.BLOCKED_INPUT_WORDS.size, repo.getTotalKeywordCount())
        assertEquals(StudyNavigationGuard.STUDY_BLOCKED_HOSTS.size, repo.getTotalDomainCount())
    }

    @Test
    fun testRejectShortOrInvalidKeywords() {
        val repo = CustomBlockedFilterRepository(customStorageDir = tempFolder.root)
        val resX = repo.addKeyword("x")
        assertTrue(resX is AddFilterResult.Error)
        val resXX = repo.addKeyword("xx")
        assertTrue(resXX is AddFilterResult.Error)
        val resA = repo.addKeyword("a")
        assertTrue(resA is AddFilterResult.Error)
    }

    @Test
    fun testAddDuplicatePredefinedKeyword() {
        val repo = CustomBlockedFilterRepository(customStorageDir = tempFolder.root)

        // Predefined English word
        val res1 = repo.addKeyword("porn")
        assertTrue("Duplicate predefined keyword must return AlreadyExists", res1 is AddFilterResult.AlreadyExists)
        assertTrue((res1 as AddFilterResult.AlreadyExists).message.contains("Already added"))

        // Uppercase predefined word
        val res2 = repo.addKeyword("PORNOGRAPHY")
        assertTrue("Case-insensitive duplicate must return AlreadyExists", res2 is AddFilterResult.AlreadyExists)

        // Predefined Bangla script word
        val res3 = repo.addKeyword("পর্ন")
        assertTrue("Bangla predefined word duplicate must return AlreadyExists", res3 is AddFilterResult.AlreadyExists)

        // Predefined word with leetspeak/spacing
        val res4 = repo.addKeyword("p 0 r n")
        assertTrue(res4 is AddFilterResult.AlreadyExists)

        // Verify count didn't increase
        assertEquals(0, repo.getCustomKeywordCount())
    }

    @Test
    fun testAddNewCustomKeywordAndImmediateFiltering() {
        val repo = CustomBlockedFilterRepository(customStorageDir = tempFolder.root)

        val newKeyword = "customexplicitbadword123"
        assertFalse(FocusInputFilter.isBlocked(newKeyword))

        val addResult = repo.addKeyword(newKeyword)
        assertTrue("Adding new keyword should succeed", addResult is AddFilterResult.Success)
        assertEquals(1, repo.getCustomKeywordCount())
        assertEquals(FocusInputFilter.BLOCKED_INPUT_WORDS.size + 1, repo.getTotalKeywordCount())

        // Duplicate attempt must now return AlreadyExists
        val dupResult = repo.addKeyword("CUSTOMEXPLICITBADWORD123")
        assertTrue(dupResult is AddFilterResult.AlreadyExists)
        assertEquals(1, repo.getCustomKeywordCount())

        // Verify immediate filtering by FocusInputFilter
        assertTrue("New custom keyword must be blocked by FocusInputFilter", FocusInputFilter.isBlocked(newKeyword))
        val hit = FocusInputFilter.explicitHit("hello customexplicitbadword123 world")
        assertEquals(newKeyword, hit)

        // Legitimate medical terms must remain unblocked
        assertFalse(FocusInputFilter.isBlocked("antiviral therapy"))
        assertFalse(FocusInputFilter.isBlocked("therapist"))
        assertFalse(FocusInputFilter.isBlocked("document"))
    }

    @Test
    fun testAddDuplicatePredefinedDomain() {
        val repo = CustomBlockedFilterRepository(customStorageDir = tempFolder.root)

        // Predefined blocked adult host
        val res1 = repo.addDomain("pornhub.com")
        assertTrue("Predefined adult host must return AlreadyExists", res1 is AddFilterResult.AlreadyExists)
        assertTrue((res1 as AddFilterResult.AlreadyExists).message.contains("Already added"))

        // Predefined URL with https and path
        val res2 = repo.addDomain("https://www.xvideos.com/video/test")
        assertTrue("Predefined URL with path must return AlreadyExists", res2 is AddFilterResult.AlreadyExists)

        // Predefined social media host
        val res3 = repo.addDomain("facebook.com")
        assertTrue("Predefined social host must return AlreadyExists", res3 is AddFilterResult.AlreadyExists)

        // Wildcard match
        val res4 = repo.addDomain("m.youtube.com")
        assertTrue("Wildcard matched domain must return AlreadyExists", res4 is AddFilterResult.AlreadyExists)

        assertEquals(0, repo.getCustomDomainCount())
    }

    @Test
    fun testAddNewCustomDomainAndImmediateBlocking() {
        val repo = CustomBlockedFilterRepository(customStorageDir = tempFolder.root)

        val customDomain = "distractionportal.net"
        assertFalse(StudyNavigationGuard.isHostBlocked(customDomain))

        val res = repo.addDomain("https://$customDomain/feed?tab=explore")
        assertTrue("Adding new custom domain must succeed", res is AddFilterResult.Success)
        assertEquals(1, repo.getCustomDomainCount())
        assertEquals(StudyNavigationGuard.STUDY_BLOCKED_HOSTS.size + 1, repo.getTotalDomainCount())

        // Duplicate attempt must return AlreadyExists
        val dup = repo.addDomain(customDomain)
        assertTrue(dup is AddFilterResult.AlreadyExists)

        // Verify immediate blocking by StudyNavigationGuard
        assertTrue("Custom domain must be blocked", StudyNavigationGuard.isHostBlocked(customDomain))
        assertTrue("Subdomain of custom domain must be blocked", StudyNavigationGuard.isHostBlocked("sub.$customDomain"))

        // Approved study sites must remain unblocked
        assertFalse(StudyNavigationGuard.isHostBlocked("guessthecase.eu.org"))
        assertFalse(StudyNavigationGuard.isHostBlocked("eco.du.ac.bd"))
        assertFalse(StudyNavigationGuard.isHostBlocked("du.ac.bd"))
        assertFalse(StudyNavigationGuard.isHostBlocked("boimate.com"))
        assertFalse(StudyNavigationGuard.isHostBlocked("medical-contents.abdursakib.workers.dev"))
    }

    @Test
    fun testPersistenceAcrossInstances() {
        val dir = tempFolder.root
        val repo1 = CustomBlockedFilterRepository(customStorageDir = dir)

        repo1.addKeyword("persistedbadword99")
        repo1.addDomain("persisteddomain99.org")

        assertEquals(1, repo1.getCustomKeywordCount())
        assertEquals(1, repo1.getCustomDomainCount())

        // Second instance reading the same directory
        val repo2 = CustomBlockedFilterRepository(customStorageDir = dir)
        assertEquals(1, repo2.getCustomKeywordCount())
        assertEquals(1, repo2.getCustomDomainCount())

        // Duplicate checks must work across restarts
        val dupKw = repo2.addKeyword("persistedbadword99")
        assertTrue(dupKw is AddFilterResult.AlreadyExists)

        val dupDm = repo2.addDomain("persisteddomain99.org")
        assertTrue(dupDm is AddFilterResult.AlreadyExists)
    }

    @Test
    fun testEmergencyAllowedPackages() {
        // Verification that ONLY Maps, Phone, SMS, Notes, and File Manager packages are in EMERGENCY_ALLOWED_PACKAGES
        val emergencyPkgs = DevicePolicyController.EMERGENCY_ALLOWED_PACKAGES

        // 1. Phone
        assertTrue(emergencyPkgs.contains("com.samsung.android.dialer"))
        assertTrue(emergencyPkgs.contains("com.android.phone"))
        assertTrue(emergencyPkgs.contains("com.google.android.dialer"))

        // 2. SMS
        assertTrue(emergencyPkgs.contains("com.samsung.android.messaging"))
        assertTrue(emergencyPkgs.contains("com.google.android.apps.messaging"))

        // 3. Maps
        assertTrue(emergencyPkgs.contains("com.google.android.apps.maps"))

        // 4. Notes
        assertTrue(emergencyPkgs.contains("com.samsung.android.app.notes"))
        assertTrue(emergencyPkgs.contains("com.google.android.keep"))

        // 5. File Manager
        assertTrue(emergencyPkgs.contains("com.sec.android.app.myfiles"))
        assertTrue(emergencyPkgs.contains("com.google.android.apps.nbu.files"))
        assertTrue(emergencyPkgs.contains("com.android.documentsui"))

        // 6. Samsung Music
        assertTrue(emergencyPkgs.contains("com.sec.android.app.music"))

        // 7. Islamic & Spiritual Apps (Muslim Bangla, Quran, Hadith)
        assertTrue(emergencyPkgs.contains("com.tos.namajtime"))
        assertTrue(emergencyPkgs.contains("com.muslimbangla"))
        assertTrue(emergencyPkgs.contains("com.ihadis.ihadis"))
        assertTrue(emergencyPkgs.contains("com.greentech.quran"))

        // Ensure non-emergency distraction apps are NOT in the emergency set
        assertFalse(emergencyPkgs.contains("com.whatsapp"))
        assertFalse(emergencyPkgs.contains("com.bKash.customerapp"))
        assertFalse(emergencyPkgs.contains("com.openai.chatgpt"))
        assertFalse(emergencyPkgs.contains("com.anthropic.claude"))
        assertFalse(emergencyPkgs.contains("com.google.android.apps.bard"))
        assertFalse(emergencyPkgs.contains("com.pathao.user"))
        assertFalse(emergencyPkgs.contains("com.global.foodpanda.android"))
    }
}
