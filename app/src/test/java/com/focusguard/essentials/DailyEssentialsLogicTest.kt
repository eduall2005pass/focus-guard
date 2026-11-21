package com.focusguard.essentials

import com.focusguard.essentials.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DailyEssentialsLogicTest {

    @Test
    fun testSalahCompletionAndCounting() {
        val date = "2026-11-20"
        var record = DailySalahRecord(date = date)
        assertEquals("Initial count should be 0", 0, record.completedCount)
        assertFalse(record.isCompleted(SalahType.FAJR))

        // Toggle Fajr, Dhuhr, Isha
        record = record.copyWithToggle(SalahType.FAJR)
        record = record.copyWithToggle(SalahType.DHUHR)
        record = record.copyWithToggle(SalahType.ISHA)

        assertEquals("Completed count should be 3", 3, record.completedCount)
        assertTrue(record.fajr)
        assertTrue(record.dhuhr)
        assertFalse(record.asr)
        assertFalse(record.maghrib)
        assertTrue(record.isha)

        // Untoggle Dhuhr
        record = record.copyWithToggle(SalahType.DHUHR)
        assertEquals("Completed count should be 2 after untoggling Dhuhr", 2, record.completedCount)
        assertFalse(record.dhuhr)

        // Toggle remaining
        record = record.copyWithToggle(SalahType.DHUHR)
        record = record.copyWithToggle(SalahType.ASR)
        record = record.copyWithToggle(SalahType.MAGHRIB)
        assertEquals("All 5 Salah should be completed", 5, record.completedCount)
    }

    @Test
    fun testQuranAndHadithActivitySeparation() {
        val date = "2026-11-20"
        val record = QuranHadithRecord(
            date = date,
            quranStudyPages = 5,
            quranStudyAyahs = 15,
            quranMemoPages = 2,
            quranMemoAyahs = 7,
            hadithStudyCount = 20,
            hadithMemoCount = 4
        )

        // Assert Quran Study and Memorization are strictly distinct
        assertEquals(5, record.quranStudyPages)
        assertEquals(15, record.quranStudyAyahs)
        assertEquals(2, record.quranMemoPages)
        assertEquals(7, record.quranMemoAyahs)
        assertNotEquals(record.quranStudyPages, record.quranMemoPages)
        assertNotEquals(record.quranStudyAyahs, record.quranMemoAyahs)

        // Assert Hadith Study and Memorization are separate activities
        assertEquals(20, record.hadithStudyCount)
        assertEquals(4, record.hadithMemoCount)
        assertNotEquals(record.hadithStudyCount, record.hadithMemoCount)
    }

    @Test
    fun testMultipleBooksTrackingOnSameDay() {
        val date = "2026-11-20"
        val book1 = Book(title = "Riyadhus Saliheen", totalPages = 500)
        val book2 = Book(title = "Seerah of the Prophet", totalPages = 350)

        val reading1 = DailyReadingRecord(
            date = date,
            bookId = book1.id,
            bookTitle = book1.title,
            pagesRead = 15,
            totalPages = book1.totalPages
        )
        val reading2 = DailyReadingRecord(
            date = date,
            bookId = book2.id,
            bookTitle = book2.title,
            pagesRead = 25,
            totalPages = book2.totalPages
        )

        val dailyReadings = listOf(reading1, reading2)
        assertEquals(2, dailyReadings.size)

        val totalPagesToday = dailyReadings.sumOf { it.pagesRead }
        assertEquals(40, totalPagesToday)

        val riyadhusEntry = dailyReadings.first { it.bookId == book1.id }
        assertEquals(15, riyadhusEntry.pagesRead)
        assertEquals(500, riyadhusEntry.totalPages)
    }

    @Test
    fun testQazaSalahCarryForwardCalculation() {
        // Calculation required:
        // Remaining Qaza = Previous Outstanding + Newly Added Qaza - Completed Qaza
        val settings = QazaSettings(
            isConfigured = true,
            fajrInitial = 120,
            dhuhrInitial = 80,
            asrInitial = 70,
            maghribInitial = 50,
            ishaInitial = 90
        )

        assertEquals("Total initial balance", 410, settings.totalInitial)

        // Month 1: 2026-11
        val priorFajr = settings.fajrInitial // 120
        val month1AddedFajr = 3
        val month1CompletedFajr = 15
        val month1RemainingFajr = priorFajr + month1AddedFajr - month1CompletedFajr
        assertEquals("Month 1 Fajr remaining should be 108", 108, month1RemainingFajr)

        // Month 2: 2026-12 (Carries forward previous outstanding of 108)
        val priorFajrMonth2 = month1RemainingFajr // 108
        val month2AddedFajr = 1
        val month2CompletedFajr = 10
        val month2RemainingFajr = priorFajrMonth2 + month2AddedFajr - month2CompletedFajr
        assertEquals("Month 2 Fajr remaining should be 99", 99, month2RemainingFajr)

        // Verify summary object structure
        val fajrSummary = QazaPrayerSummary(
            type = SalahType.FAJR,
            priorOutstanding = priorFajrMonth2,
            addedThisMonth = month2AddedFajr,
            completedThisMonth = month2CompletedFajr,
            remaining = month2RemainingFajr
        )
        assertEquals(108, fajrSummary.priorOutstanding)
        assertEquals(1, fajrSummary.addedThisMonth)
        assertEquals(10, fajrSummary.completedThisMonth)
        assertEquals(99, fajrSummary.remaining)
    }

    @Test
    fun testQazaUnconfiguredSafety() {
        val defaultSettings = QazaSettings()
        assertFalse("Qaza settings should not be configured by default", defaultSettings.isConfigured)
        assertEquals("Initial total should be 0 before configuration", 0, defaultSettings.totalInitial)
    }

    @Test
    fun testDailyQazaRecordOperations() {
        val date = "2026-11-20"
        var record = DailyQazaRecord(date = date)
        assertEquals(0, record.totalCompletedToday)

        record = record.copyWithIncrement(SalahType.FAJR, 2)
        record = record.copyWithIncrement(SalahType.MAGHRIB, 1)

        assertEquals(2, record.fajrCompleted)
        assertEquals(1, record.maghribCompleted)
        assertEquals(3, record.totalCompletedToday)

        // Test decrement with lower bound at 0
        record = record.copyWithIncrement(SalahType.FAJR, -5)
        assertEquals("Count should not drop below 0", 0, record.fajrCompleted)
    }

    @Test
    fun testDateFormattingAndMonthBoundaries() {
        val dateStr = "2026-11-30"
        val date = LocalDate.parse(dateStr)
        val nextDay = date.plusDays(1)
        assertEquals("Next day should cross into next month", "2026-12-01", nextDay.toString())

        val ym = YearMonth.parse("2026-11")
        assertEquals(30, ym.lengthOfMonth())

        val ymFeb2024 = YearMonth.parse("2024-02")
        assertEquals("Leap year February should have 29 days", 29, ymFeb2024.lengthOfMonth())
    }

    @Test
    fun testQuranHadithRecordHasData() {
        val emptyRecord = QuranHadithRecord(date = "2026-11-20")
        assertFalse("Empty record should have hasData = false", emptyRecord.hasData)

        val studyPagesOnly = QuranHadithRecord(date = "2026-11-20", quranStudyPages = 3)
        assertTrue(studyPagesOnly.hasData)

        val hadithOnly = QuranHadithRecord(date = "2026-11-20", hadithStudyCount = 5)
        assertTrue(hadithOnly.hasData)

        val memoOnly = QuranHadithRecord(date = "2026-11-20", quranMemoAyahs = 10)
        assertTrue(memoOnly.hasData)
    }
}
