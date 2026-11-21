package com.focusguard.essentials.data.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class SalahType(val displayName: String) {
    FAJR("Fajr"),
    DHUHR("Dhuhr"),
    ASR("Asr"),
    MAGHRIB("Maghrib"),
    ISHA("Isha")
}

data class DailySalahRecord(
    val date: String,
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val completedCount: Int
        get() = listOf(fajr, dhuhr, asr, maghrib, isha).count { it }

    fun isCompleted(type: SalahType): Boolean = when (type) {
        SalahType.FAJR -> fajr
        SalahType.DHUHR -> dhuhr
        SalahType.ASR -> asr
        SalahType.MAGHRIB -> maghrib
        SalahType.ISHA -> isha
    }

    fun copyWithToggle(type: SalahType): DailySalahRecord = when (type) {
        SalahType.FAJR -> copy(fajr = !fajr, updatedAt = System.currentTimeMillis())
        SalahType.DHUHR -> copy(dhuhr = !dhuhr, updatedAt = System.currentTimeMillis())
        SalahType.ASR -> copy(asr = !asr, updatedAt = System.currentTimeMillis())
        SalahType.MAGHRIB -> copy(maghrib = !maghrib, updatedAt = System.currentTimeMillis())
        SalahType.ISHA -> copy(isha = !isha, updatedAt = System.currentTimeMillis())
    }
}

data class Book(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val totalPages: Int = 0,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class DailyReadingRecord(
    val id: String = UUID.randomUUID().toString(),
    val date: String,
    val bookId: String,
    val bookTitle: String = "",
    val pagesRead: Int = 0,
    val totalPages: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

data class BookMonthlySummary(
    val bookId: String,
    val bookTitle: String,
    val pagesThisMonth: Int,
    val lifetimePages: Int,
    val totalPages: Int
)

data class QuranHadithRecord(
    val date: String,
    val quranStudyPages: Int = 0,
    val quranStudyAyahs: Int = 0,
    val quranMemoPages: Int = 0,
    val quranMemoAyahs: Int = 0,
    val hadithStudyCount: Int = 0,
    val hadithMemoCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val hasData: Boolean
        get() = quranStudyPages > 0 || quranStudyAyahs > 0 ||
                quranMemoPages > 0 || quranMemoAyahs > 0 ||
                hadithStudyCount > 0 || hadithMemoCount > 0
}

data class QazaSettings(
    val isConfigured: Boolean = false,
    val startDate: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    val fajrInitial: Int = 0,
    val dhuhrInitial: Int = 0,
    val asrInitial: Int = 0,
    val maghribInitial: Int = 0,
    val ishaInitial: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalInitial: Int
        get() = fajrInitial + dhuhrInitial + asrInitial + maghribInitial + ishaInitial

    fun initialFor(type: SalahType): Int = when (type) {
        SalahType.FAJR -> fajrInitial
        SalahType.DHUHR -> dhuhrInitial
        SalahType.ASR -> asrInitial
        SalahType.MAGHRIB -> maghribInitial
        SalahType.ISHA -> ishaInitial
    }
}

data class DailyQazaRecord(
    val date: String,
    val fajrCompleted: Int = 0,
    val dhuhrCompleted: Int = 0,
    val asrCompleted: Int = 0,
    val maghribCompleted: Int = 0,
    val ishaCompleted: Int = 0,
    val fajrAdded: Int = 0,
    val dhuhrAdded: Int = 0,
    val asrAdded: Int = 0,
    val maghribAdded: Int = 0,
    val ishaAdded: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalCompletedToday: Int
        get() = fajrCompleted + dhuhrCompleted + asrCompleted + maghribCompleted + ishaCompleted

    fun completedFor(type: SalahType): Int = when (type) {
        SalahType.FAJR -> fajrCompleted
        SalahType.DHUHR -> dhuhrCompleted
        SalahType.ASR -> asrCompleted
        SalahType.MAGHRIB -> maghribCompleted
        SalahType.ISHA -> ishaCompleted
    }

    fun copyWithIncrement(type: SalahType, delta: Int): DailyQazaRecord = when (type) {
        SalahType.FAJR -> copy(fajrCompleted = maxOf(0, fajrCompleted + delta), updatedAt = System.currentTimeMillis())
        SalahType.DHUHR -> copy(dhuhrCompleted = maxOf(0, dhuhrCompleted + delta), updatedAt = System.currentTimeMillis())
        SalahType.ASR -> copy(asrCompleted = maxOf(0, asrCompleted + delta), updatedAt = System.currentTimeMillis())
        SalahType.MAGHRIB -> copy(maghribCompleted = maxOf(0, maghribCompleted + delta), updatedAt = System.currentTimeMillis())
        SalahType.ISHA -> copy(ishaCompleted = maxOf(0, ishaCompleted + delta), updatedAt = System.currentTimeMillis())
    }

    fun copyWithCount(type: SalahType, count: Int): DailyQazaRecord = when (type) {
        SalahType.FAJR -> copy(fajrCompleted = maxOf(0, count), updatedAt = System.currentTimeMillis())
        SalahType.DHUHR -> copy(dhuhrCompleted = maxOf(0, count), updatedAt = System.currentTimeMillis())
        SalahType.ASR -> copy(asrCompleted = maxOf(0, count), updatedAt = System.currentTimeMillis())
        SalahType.MAGHRIB -> copy(maghribCompleted = maxOf(0, count), updatedAt = System.currentTimeMillis())
        SalahType.ISHA -> copy(ishaCompleted = maxOf(0, count), updatedAt = System.currentTimeMillis())
    }
}

data class QazaPrayerSummary(
    val type: SalahType,
    val priorOutstanding: Int,
    val addedThisMonth: Int,
    val completedThisMonth: Int,
    val remaining: Int
)

data class MonthlyQazaSummary(
    val yearMonth: String,
    val isConfigured: Boolean,
    val prayers: Map<SalahType, QazaPrayerSummary>,
    val totalCompletedThisMonth: Int,
    val totalRemaining: Int
)

data class SalahConsistencyStat(
    val type: SalahType,
    val completedDays: Int,
    val totalDays: Int,
    val percentage: Float
)

data class MonthlyReportData(
    val yearMonth: String, // "YYYY-MM"
    val daysInMonth: Int,
    val totalSalahCompleted: Int,
    val totalSalahPossible: Int,
    val salahConsistency: List<SalahConsistencyStat>,
    val totalReadingPages: Int,
    val bookSummaries: List<BookMonthlySummary>,
    val quranStudyPages: Int,
    val quranStudyAyahs: Int,
    val quranMemoPages: Int,
    val quranMemoAyahs: Int,
    val hadithStudyCount: Int,
    val hadithMemoCount: Int,
    val qazaSummary: MonthlyQazaSummary,
    val dailyActivityIntensity: Map<String, Float> // date -> intensity 0f..1f for heatmap
)

data class DailyEssentialsDayData(
    val date: String,
    val salah: DailySalahRecord,
    val readings: List<DailyReadingRecord>,
    val quranHadith: QuranHadithRecord,
    val qaza: DailyQazaRecord,
    val booksCatalog: List<Book>
)
