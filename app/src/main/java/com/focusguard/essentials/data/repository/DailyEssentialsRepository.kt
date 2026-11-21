package com.focusguard.essentials.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.focusguard.essentials.data.database.DailyEssentialsDatabaseHelper
import com.focusguard.essentials.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID

class DailyEssentialsRepository(context: Context) {

    private val dbHelper = DailyEssentialsDatabaseHelper(context)

    private val readableDb: SQLiteDatabase
        get() = dbHelper.readableDatabase

    private val writableDb: SQLiteDatabase
        get() = dbHelper.writableDatabase

    // ==========================================
    // 1. SALAH
    // ==========================================

    suspend fun getSalahRecord(date: String): DailySalahRecord = withContext(Dispatchers.IO) {
        val cursor = readableDb.query(
            DailyEssentialsDatabaseHelper.TABLE_SALAH,
            null,
            "date = ?",
            arrayOf(date),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                parseSalahRecord(it)
            } else {
                DailySalahRecord(date = date)
            }
        }
    }

    suspend fun saveSalahRecord(record: DailySalahRecord) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("date", record.date)
            put("fajr", if (record.fajr) 1 else 0)
            put("dhuhr", if (record.dhuhr) 1 else 0)
            put("asr", if (record.asr) 1 else 0)
            put("maghrib", if (record.maghrib) 1 else 0)
            put("isha", if (record.isha) 1 else 0)
            put("updated_at", record.updatedAt)
        }
        writableDb.insertWithOnConflict(
            DailyEssentialsDatabaseHelper.TABLE_SALAH,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    suspend fun toggleSalah(date: String, type: SalahType): DailySalahRecord = withContext(Dispatchers.IO) {
        val current = getSalahRecord(date)
        val updated = current.copyWithToggle(type)
        saveSalahRecord(updated)
        updated
    }

    private fun parseSalahRecord(cursor: Cursor): DailySalahRecord {
        return DailySalahRecord(
            date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
            fajr = cursor.getInt(cursor.getColumnIndexOrThrow("fajr")) == 1,
            dhuhr = cursor.getInt(cursor.getColumnIndexOrThrow("dhuhr")) == 1,
            asr = cursor.getInt(cursor.getColumnIndexOrThrow("asr")) == 1,
            maghrib = cursor.getInt(cursor.getColumnIndexOrThrow("maghrib")) == 1,
            isha = cursor.getInt(cursor.getColumnIndexOrThrow("isha")) == 1,
            updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
        )
    }

    // ==========================================
    // 2. BOOKS & READING
    // ==========================================

    suspend fun getBooksCatalog(): List<Book> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Book>()
        val cursor = readableDb.query(
            DailyEssentialsDatabaseHelper.TABLE_BOOKS,
            null,
            "is_active = 1",
            null,
            null,
            null,
            "title ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Book(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        totalPages = it.getInt(it.getColumnIndexOrThrow("total_pages")),
                        isActive = it.getInt(it.getColumnIndexOrThrow("is_active")) == 1,
                        createdAt = it.getLong(it.getColumnIndexOrThrow("created_at"))
                    )
                )
            }
        }
        list
    }

    suspend fun getOrCreateBook(title: String, totalPages: Int = 0): Book = withContext(Dispatchers.IO) {
        val trimmed = title.trim()
        val cursor = readableDb.query(
            DailyEssentialsDatabaseHelper.TABLE_BOOKS,
            null,
            "LOWER(title) = LOWER(?)",
            arrayOf(trimmed),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val book = Book(
                    id = it.getString(it.getColumnIndexOrThrow("id")),
                    title = it.getString(it.getColumnIndexOrThrow("title")),
                    totalPages = it.getInt(it.getColumnIndexOrThrow("total_pages")),
                    isActive = it.getInt(it.getColumnIndexOrThrow("is_active")) == 1,
                    createdAt = it.getLong(it.getColumnIndexOrThrow("created_at"))
                )
                if (totalPages > 0 && book.totalPages != totalPages) {
                    val updated = book.copy(totalPages = totalPages)
                    saveBook(updated)
                    return@withContext updated
                }
                return@withContext book
            }
        }

        val newBook = Book(
            id = UUID.randomUUID().toString(),
            title = trimmed,
            totalPages = totalPages,
            isActive = true,
            createdAt = System.currentTimeMillis()
        )
        saveBook(newBook)
        newBook
    }

    suspend fun saveBook(book: Book) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", book.id)
            put("title", book.title.trim())
            put("total_pages", book.totalPages)
            put("is_active", if (book.isActive) 1 else 0)
            put("created_at", book.createdAt)
        }
        writableDb.insertWithOnConflict(
            DailyEssentialsDatabaseHelper.TABLE_BOOKS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    suspend fun deleteBook(bookId: String) = withContext(Dispatchers.IO) {
        writableDb.delete(
            DailyEssentialsDatabaseHelper.TABLE_BOOKS,
            "id = ?",
            arrayOf(bookId)
        )
        writableDb.delete(
            DailyEssentialsDatabaseHelper.TABLE_READING,
            "book_id = ?",
            arrayOf(bookId)
        )
    }

    suspend fun getReadingsForDate(date: String): List<DailyReadingRecord> = withContext(Dispatchers.IO) {
        val query = """
            SELECT r.id, r.date, r.book_id, r.pages_read, r.updated_at, b.title, b.total_pages
            FROM ${DailyEssentialsDatabaseHelper.TABLE_READING} r
            LEFT JOIN ${DailyEssentialsDatabaseHelper.TABLE_BOOKS} b ON r.book_id = b.id
            WHERE r.date = ?
            ORDER BY r.updated_at ASC
        """.trimIndent()

        val list = mutableListOf<DailyReadingRecord>()
        val cursor = readableDb.rawQuery(query, arrayOf(date))
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    DailyReadingRecord(
                        id = it.getString(0),
                        date = it.getString(1),
                        bookId = it.getString(2),
                        pagesRead = it.getInt(3),
                        updatedAt = it.getLong(4),
                        bookTitle = it.getString(5) ?: "Unknown Book",
                        totalPages = it.getInt(6)
                    )
                )
            }
        }
        list
    }

    suspend fun saveReading(date: String, bookTitle: String, pagesRead: Int, totalPages: Int = 0): DailyReadingRecord = withContext(Dispatchers.IO) {
        val book = getOrCreateBook(bookTitle, totalPages)
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val values = ContentValues().apply {
            put("id", id)
            put("date", date)
            put("book_id", book.id)
            put("pages_read", pagesRead)
            put("updated_at", now)
        }
        writableDb.insertWithOnConflict(
            DailyEssentialsDatabaseHelper.TABLE_READING,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )

        DailyReadingRecord(
            id = id,
            date = date,
            bookId = book.id,
            bookTitle = book.title,
            pagesRead = pagesRead,
            totalPages = book.totalPages,
            updatedAt = now
        )
    }

    suspend fun deleteReading(readingId: String) = withContext(Dispatchers.IO) {
        writableDb.delete(
            DailyEssentialsDatabaseHelper.TABLE_READING,
            "id = ?",
            arrayOf(readingId)
        )
    }

    suspend fun getLifetimePagesForBook(bookId: String): Int = withContext(Dispatchers.IO) {
        val cursor = readableDb.rawQuery(
            "SELECT SUM(pages_read) FROM ${DailyEssentialsDatabaseHelper.TABLE_READING} WHERE book_id = ?",
            arrayOf(bookId)
        )
        cursor.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
    }

    // ==========================================
    // 3. QURAN & HADITH
    // ==========================================

    suspend fun getQuranHadithRecord(date: String): QuranHadithRecord = withContext(Dispatchers.IO) {
        val cursor = readableDb.query(
            DailyEssentialsDatabaseHelper.TABLE_QURAN_HADITH,
            null,
            "date = ?",
            arrayOf(date),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                QuranHadithRecord(
                    date = it.getString(it.getColumnIndexOrThrow("date")),
                    quranStudyPages = it.getInt(it.getColumnIndexOrThrow("quran_study_pages")),
                    quranStudyAyahs = it.getInt(it.getColumnIndexOrThrow("quran_study_ayahs")),
                    quranMemoPages = it.getInt(it.getColumnIndexOrThrow("quran_memo_pages")),
                    quranMemoAyahs = it.getInt(it.getColumnIndexOrThrow("quran_memo_ayahs")),
                    hadithStudyCount = it.getInt(it.getColumnIndexOrThrow("hadith_study_count")),
                    hadithMemoCount = it.getInt(it.getColumnIndexOrThrow("hadith_memo_count")),
                    updatedAt = it.getLong(it.getColumnIndexOrThrow("updated_at"))
                )
            } else {
                QuranHadithRecord(date = date)
            }
        }
    }

    suspend fun saveQuranHadithRecord(record: QuranHadithRecord) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("date", record.date)
            put("quran_study_pages", record.quranStudyPages)
            put("quran_study_ayahs", record.quranStudyAyahs)
            put("quran_memo_pages", record.quranMemoPages)
            put("quran_memo_ayahs", record.quranMemoAyahs)
            put("hadith_study_count", record.hadithStudyCount)
            put("hadith_memo_count", record.hadithMemoCount)
            put("updated_at", record.updatedAt)
        }
        writableDb.insertWithOnConflict(
            DailyEssentialsDatabaseHelper.TABLE_QURAN_HADITH,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    // ==========================================
    // 4. QAZA SALAH & SETTINGS
    // ==========================================

    suspend fun getQazaSettings(): QazaSettings = withContext(Dispatchers.IO) {
        val cursor = readableDb.query(
            DailyEssentialsDatabaseHelper.TABLE_QAZA_SETTINGS,
            null,
            "id = 1",
            null,
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                QazaSettings(
                    isConfigured = it.getInt(it.getColumnIndexOrThrow("is_configured")) == 1,
                    startDate = it.getString(it.getColumnIndexOrThrow("start_date")),
                    fajrInitial = it.getInt(it.getColumnIndexOrThrow("fajr_initial")),
                    dhuhrInitial = it.getInt(it.getColumnIndexOrThrow("dhuhr_initial")),
                    asrInitial = it.getInt(it.getColumnIndexOrThrow("asr_initial")),
                    maghribInitial = it.getInt(it.getColumnIndexOrThrow("maghrib_initial")),
                    ishaInitial = it.getInt(it.getColumnIndexOrThrow("isha_initial")),
                    updatedAt = it.getLong(it.getColumnIndexOrThrow("updated_at"))
                )
            } else {
                QazaSettings()
            }
        }
    }

    suspend fun saveQazaSettings(settings: QazaSettings) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("id", 1)
            put("is_configured", if (settings.isConfigured) 1 else 0)
            put("start_date", settings.startDate)
            put("fajr_initial", settings.fajrInitial)
            put("dhuhr_initial", settings.dhuhrInitial)
            put("asr_initial", settings.asrInitial)
            put("maghrib_initial", settings.maghribInitial)
            put("isha_initial", settings.ishaInitial)
            put("updated_at", settings.updatedAt)
        }
        writableDb.insertWithOnConflict(
            DailyEssentialsDatabaseHelper.TABLE_QAZA_SETTINGS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    suspend fun getDailyQazaRecord(date: String): DailyQazaRecord = withContext(Dispatchers.IO) {
        val cursor = readableDb.query(
            DailyEssentialsDatabaseHelper.TABLE_QAZA_DAILY,
            null,
            "date = ?",
            arrayOf(date),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                DailyQazaRecord(
                    date = it.getString(it.getColumnIndexOrThrow("date")),
                    fajrCompleted = it.getInt(it.getColumnIndexOrThrow("fajr_completed")),
                    dhuhrCompleted = it.getInt(it.getColumnIndexOrThrow("dhuhr_completed")),
                    asrCompleted = it.getInt(it.getColumnIndexOrThrow("asr_completed")),
                    maghribCompleted = it.getInt(it.getColumnIndexOrThrow("maghrib_completed")),
                    ishaCompleted = it.getInt(it.getColumnIndexOrThrow("isha_completed")),
                    fajrAdded = it.getInt(it.getColumnIndexOrThrow("fajr_added")),
                    dhuhrAdded = it.getInt(it.getColumnIndexOrThrow("dhuhr_added")),
                    asrAdded = it.getInt(it.getColumnIndexOrThrow("asr_added")),
                    maghribAdded = it.getInt(it.getColumnIndexOrThrow("maghrib_added")),
                    ishaAdded = it.getInt(it.getColumnIndexOrThrow("isha_added")),
                    updatedAt = it.getLong(it.getColumnIndexOrThrow("updated_at"))
                )
            } else {
                DailyQazaRecord(date = date)
            }
        }
    }

    suspend fun saveDailyQazaRecord(record: DailyQazaRecord) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("date", record.date)
            put("fajr_completed", record.fajrCompleted)
            put("dhuhr_completed", record.dhuhrCompleted)
            put("asr_completed", record.asrCompleted)
            put("maghrib_completed", record.maghribCompleted)
            put("isha_completed", record.ishaCompleted)
            put("fajr_added", record.fajrAdded)
            put("dhuhr_added", record.dhuhrAdded)
            put("asr_added", record.asrAdded)
            put("maghrib_added", record.maghribAdded)
            put("isha_added", record.ishaAdded)
            put("updated_at", record.updatedAt)
        }
        writableDb.insertWithOnConflict(
            DailyEssentialsDatabaseHelper.TABLE_QAZA_DAILY,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    suspend fun incrementDailyQaza(date: String, type: SalahType, delta: Int): DailyQazaRecord = withContext(Dispatchers.IO) {
        val current = getDailyQazaRecord(date)
        val updated = current.copyWithIncrement(type, delta)
        saveDailyQazaRecord(updated)
        updated
    }

    suspend fun setDailyQazaCount(date: String, type: SalahType, count: Int): DailyQazaRecord = withContext(Dispatchers.IO) {
        val current = getDailyQazaRecord(date)
        val updated = current.copyWithCount(type, count)
        saveDailyQazaRecord(updated)
        updated
    }

    // ==========================================
    // 5. MONTHLY CALCULATIONS & CARRY FORWARD
    // ==========================================

    suspend fun getMonthlyQazaSummary(yearMonth: String): MonthlyQazaSummary = withContext(Dispatchers.IO) {
        val settings = getQazaSettings()
        val monthStart = "$yearMonth-01"
        val nextMonth = YearMonth.parse(yearMonth).plusMonths(1).toString()
        val nextMonthStart = "$nextMonth-01"

        // 1. Prior to this month: date < monthStart
        var priorFajrCompleted = 0
        var priorDhuhrCompleted = 0
        var priorAsrCompleted = 0
        var priorMaghribCompleted = 0
        var priorIshaCompleted = 0

        var priorFajrAdded = 0
        var priorDhuhrAdded = 0
        var priorAsrAdded = 0
        var priorMaghribAdded = 0
        var priorIshaAdded = 0

        val priorCursor = readableDb.rawQuery(
            """
            SELECT 
                SUM(fajr_completed), SUM(dhuhr_completed), SUM(asr_completed), SUM(maghrib_completed), SUM(isha_completed),
                SUM(fajr_added), SUM(dhuhr_added), SUM(asr_added), SUM(maghrib_added), SUM(isha_added)
            FROM ${DailyEssentialsDatabaseHelper.TABLE_QAZA_DAILY}
            WHERE date < ?
            """.trimIndent(),
            arrayOf(monthStart)
        )
        priorCursor.use {
            if (it.moveToFirst()) {
                priorFajrCompleted = it.getInt(0)
                priorDhuhrCompleted = it.getInt(1)
                priorAsrCompleted = it.getInt(2)
                priorMaghribCompleted = it.getInt(3)
                priorIshaCompleted = it.getInt(4)

                priorFajrAdded = it.getInt(5)
                priorDhuhrAdded = it.getInt(6)
                priorAsrAdded = it.getInt(7)
                priorMaghribAdded = it.getInt(8)
                priorIshaAdded = it.getInt(9)
            }
        }

        // 2. This month: date >= monthStart AND date < nextMonthStart
        var monthFajrCompleted = 0
        var monthDhuhrCompleted = 0
        var monthAsrCompleted = 0
        var monthMaghribCompleted = 0
        var monthIshaCompleted = 0

        var monthFajrAdded = 0
        var monthDhuhrAdded = 0
        var monthAsrAdded = 0
        var monthMaghribAdded = 0
        var monthIshaAdded = 0

        val monthCursor = readableDb.rawQuery(
            """
            SELECT 
                SUM(fajr_completed), SUM(dhuhr_completed), SUM(asr_completed), SUM(maghrib_completed), SUM(isha_completed),
                SUM(fajr_added), SUM(dhuhr_added), SUM(asr_added), SUM(maghrib_added), SUM(isha_added)
            FROM ${DailyEssentialsDatabaseHelper.TABLE_QAZA_DAILY}
            WHERE date >= ? AND date < ?
            """.trimIndent(),
            arrayOf(monthStart, nextMonthStart)
        )
        monthCursor.use {
            if (it.moveToFirst()) {
                monthFajrCompleted = it.getInt(0)
                monthDhuhrCompleted = it.getInt(1)
                monthAsrCompleted = it.getInt(2)
                monthMaghribCompleted = it.getInt(3)
                monthIshaCompleted = it.getInt(4)

                monthFajrAdded = it.getInt(5)
                monthDhuhrAdded = it.getInt(6)
                monthAsrAdded = it.getInt(7)
                monthMaghribAdded = it.getInt(8)
                monthIshaAdded = it.getInt(9)
            }
        }

        // Calculation: Remaining Qaza = Previous Outstanding + Newly Added Qaza - Completed Qaza
        fun buildSummary(
            type: SalahType,
            initial: Int,
            pAdded: Int,
            pComp: Int,
            mAdded: Int,
            mComp: Int
        ): QazaPrayerSummary {
            val priorOutstanding = maxOf(0, initial + pAdded - pComp)
            val remaining = maxOf(0, priorOutstanding + mAdded - mComp)
            return QazaPrayerSummary(
                type = type,
                priorOutstanding = priorOutstanding,
                addedThisMonth = mAdded,
                completedThisMonth = mComp,
                remaining = remaining
            )
        }

        val prayersMap = mapOf(
            SalahType.FAJR to buildSummary(
                SalahType.FAJR,
                settings.fajrInitial,
                priorFajrAdded,
                priorFajrCompleted,
                monthFajrAdded,
                monthFajrCompleted
            ),
            SalahType.DHUHR to buildSummary(
                SalahType.DHUHR,
                settings.dhuhrInitial,
                priorDhuhrAdded,
                priorDhuhrCompleted,
                monthDhuhrAdded,
                monthDhuhrCompleted
            ),
            SalahType.ASR to buildSummary(
                SalahType.ASR,
                settings.asrInitial,
                priorAsrAdded,
                priorAsrCompleted,
                monthAsrAdded,
                monthAsrCompleted
            ),
            SalahType.MAGHRIB to buildSummary(
                SalahType.MAGHRIB,
                settings.maghribInitial,
                priorMaghribAdded,
                priorMaghribCompleted,
                monthMaghribAdded,
                monthMaghribCompleted
            ),
            SalahType.ISHA to buildSummary(
                SalahType.ISHA,
                settings.ishaInitial,
                priorIshaAdded,
                priorIshaCompleted,
                monthIshaAdded,
                monthIshaCompleted
            )
        )

        val totalCompletedThisMonth = prayersMap.values.sumOf { it.completedThisMonth }
        val totalRemaining = prayersMap.values.sumOf { it.remaining }

        MonthlyQazaSummary(
            yearMonth = yearMonth,
            isConfigured = settings.isConfigured,
            prayers = prayersMap,
            totalCompletedThisMonth = totalCompletedThisMonth,
            totalRemaining = totalRemaining
        )
    }

    // ==========================================
    // 6. COMPLETE MONTHLY REPORT
    // ==========================================

    suspend fun getMonthlyReport(yearMonth: String): MonthlyReportData = withContext(Dispatchers.IO) {
        val ym = YearMonth.parse(yearMonth)
        val daysInMonth = ym.lengthOfMonth()
        val monthPrefix = "$yearMonth-%"

        // 1. Salah consistency
        var fajrDone = 0
        var dhuhrDone = 0
        var asrDone = 0
        var maghribDone = 0
        var ishaDone = 0

        val salahCursor = readableDb.rawQuery(
            """
            SELECT SUM(fajr), SUM(dhuhr), SUM(asr), SUM(maghrib), SUM(isha)
            FROM ${DailyEssentialsDatabaseHelper.TABLE_SALAH}
            WHERE date LIKE ?
            """.trimIndent(),
            arrayOf(monthPrefix)
        )
        salahCursor.use {
            if (it.moveToFirst()) {
                fajrDone = it.getInt(0)
                dhuhrDone = it.getInt(1)
                asrDone = it.getInt(2)
                maghribDone = it.getInt(3)
                ishaDone = it.getInt(4)
            }
        }

        val totalSalahCompleted = fajrDone + dhuhrDone + asrDone + maghribDone + ishaDone
        val totalSalahPossible = daysInMonth * 5

        fun makeConsistency(type: SalahType, count: Int): SalahConsistencyStat {
            val pct = if (daysInMonth > 0) (count.toFloat() / daysInMonth.toFloat()) * 100f else 0f
            return SalahConsistencyStat(
                type = type,
                completedDays = count,
                totalDays = daysInMonth,
                percentage = pct.coerceIn(0f, 100f)
            )
        }

        val consistencyList = listOf(
            makeConsistency(SalahType.FAJR, fajrDone),
            makeConsistency(SalahType.DHUHR, dhuhrDone),
            makeConsistency(SalahType.ASR, asrDone),
            makeConsistency(SalahType.MAGHRIB, maghribDone),
            makeConsistency(SalahType.ISHA, ishaDone)
        )

        // 2. Reading summaries for this month
        val bookSummaries = mutableListOf<BookMonthlySummary>()
        var totalReadingPages = 0

        val readingCursor = readableDb.rawQuery(
            """
            SELECT b.id, b.title, b.total_pages, SUM(r.pages_read)
            FROM ${DailyEssentialsDatabaseHelper.TABLE_READING} r
            JOIN ${DailyEssentialsDatabaseHelper.TABLE_BOOKS} b ON r.book_id = b.id
            WHERE r.date LIKE ?
            GROUP BY b.id
            ORDER BY SUM(r.pages_read) DESC
            """.trimIndent(),
            arrayOf(monthPrefix)
        )
        readingCursor.use {
            while (it.moveToNext()) {
                val bookId = it.getString(0)
                val bookTitle = it.getString(1)
                val totalPages = it.getInt(2)
                val pagesInMonth = it.getInt(3)
                totalReadingPages += pagesInMonth

                val lifetimePages = getLifetimePagesForBook(bookId)
                bookSummaries.add(
                    BookMonthlySummary(
                        bookId = bookId,
                        bookTitle = bookTitle,
                        pagesThisMonth = pagesInMonth,
                        lifetimePages = lifetimePages,
                        totalPages = totalPages
                    )
                )
            }
        }

        // 3. Quran & Hadith summaries
        var quranStudyPages = 0
        var quranStudyAyahs = 0
        var quranMemoPages = 0
        var quranMemoAyahs = 0
        var hadithStudyCount = 0
        var hadithMemoCount = 0

        val qhCursor = readableDb.rawQuery(
            """
            SELECT 
                SUM(quran_study_pages), SUM(quran_study_ayahs),
                SUM(quran_memo_pages), SUM(quran_memo_ayahs),
                SUM(hadith_study_count), SUM(hadith_memo_count)
            FROM ${DailyEssentialsDatabaseHelper.TABLE_QURAN_HADITH}
            WHERE date LIKE ?
            """.trimIndent(),
            arrayOf(monthPrefix)
        )
        qhCursor.use {
            if (it.moveToFirst()) {
                quranStudyPages = it.getInt(0)
                quranStudyAyahs = it.getInt(1)
                quranMemoPages = it.getInt(2)
                quranMemoAyahs = it.getInt(3)
                hadithStudyCount = it.getInt(4)
                hadithMemoCount = it.getInt(5)
            }
        }

        // 4. Qaza summary
        val qazaSummary = getMonthlyQazaSummary(yearMonth)

        // 5. Daily activity intensity map for calendar heat-map
        val activityMap = mutableMapOf<String, Float>()
        for (day in 1..daysInMonth) {
            val dStr = String.format("%s-%02d", yearMonth, day)
            activityMap[dStr] = 0f
        }

        // Aggregate daily activity score:
        // Salah: up to 5 points (1 pt each)
        // Reading: 2 points if > 0 pages
        // Quran/Hadith: 2 points if studied/memorized
        // Qaza: 1 point if completed > 0
        val dailySalahMap = mutableMapOf<String, Int>()
        val dsCursor = readableDb.rawQuery(
            "SELECT date, (fajr + dhuhr + asr + maghrib + isha) FROM ${DailyEssentialsDatabaseHelper.TABLE_SALAH} WHERE date LIKE ?",
            arrayOf(monthPrefix)
        )
        dsCursor.use {
            while (it.moveToNext()) {
                dailySalahMap[it.getString(0)] = it.getInt(1)
            }
        }

        val dailyReadingMap = mutableMapOf<String, Int>()
        val drCursor = readableDb.rawQuery(
            "SELECT date, SUM(pages_read) FROM ${DailyEssentialsDatabaseHelper.TABLE_READING} WHERE date LIKE ? GROUP BY date",
            arrayOf(monthPrefix)
        )
        drCursor.use {
            while (it.moveToNext()) {
                dailyReadingMap[it.getString(0)] = it.getInt(1)
            }
        }

        val dailyQhMap = mutableMapOf<String, Int>()
        val dqCursor = readableDb.rawQuery(
            """
            SELECT date, (quran_study_pages + quran_study_ayahs + quran_memo_pages + quran_memo_ayahs + hadith_study_count + hadith_memo_count)
            FROM ${DailyEssentialsDatabaseHelper.TABLE_QURAN_HADITH} WHERE date LIKE ?
            """.trimIndent(),
            arrayOf(monthPrefix)
        )
        dqCursor.use {
            while (it.moveToNext()) {
                dailyQhMap[it.getString(0)] = it.getInt(1)
            }
        }

        val dailyQazaMap = mutableMapOf<String, Int>()
        val dqzCursor = readableDb.rawQuery(
            "SELECT date, (fajr_completed + dhuhr_completed + asr_completed + maghrib_completed + isha_completed) FROM ${DailyEssentialsDatabaseHelper.TABLE_QAZA_DAILY} WHERE date LIKE ?",
            arrayOf(monthPrefix)
        )
        dqzCursor.use {
            while (it.moveToNext()) {
                dailyQazaMap[it.getString(0)] = it.getInt(1)
            }
        }

        for ((date, _) in activityMap) {
            val sScore = (dailySalahMap[date] ?: 0) // 0..5
            val rScore = if ((dailyReadingMap[date] ?: 0) > 0) 2 else 0
            val qScore = if ((dailyQhMap[date] ?: 0) > 0) 2 else 0
            val zScore = if ((dailyQazaMap[date] ?: 0) > 0) 1 else 0
            val totalScore = sScore + rScore + qScore + zScore // max 10
            val intensity = (totalScore / 10f).coerceIn(0f, 1f)
            activityMap[date] = intensity
        }

        MonthlyReportData(
            yearMonth = yearMonth,
            daysInMonth = daysInMonth,
            totalSalahCompleted = totalSalahCompleted,
            totalSalahPossible = totalSalahPossible,
            salahConsistency = consistencyList,
            totalReadingPages = totalReadingPages,
            bookSummaries = bookSummaries,
            quranStudyPages = quranStudyPages,
            quranStudyAyahs = quranStudyAyahs,
            quranMemoPages = quranMemoPages,
            quranMemoAyahs = quranMemoAyahs,
            hadithStudyCount = hadithStudyCount,
            hadithMemoCount = hadithMemoCount,
            qazaSummary = qazaSummary,
            dailyActivityIntensity = activityMap
        )
    }

    // ==========================================
    // 7. COMPREHENSIVE DAY DATA
    // ==========================================

    suspend fun getDayData(date: String): DailyEssentialsDayData = withContext(Dispatchers.IO) {
        val salah = getSalahRecord(date)
        val readings = getReadingsForDate(date)
        val quranHadith = getQuranHadithRecord(date)
        val qaza = getDailyQazaRecord(date)
        val catalog = getBooksCatalog()

        DailyEssentialsDayData(
            date = date,
            salah = salah,
            readings = readings,
            quranHadith = quranHadith,
            qaza = qaza,
            booksCatalog = catalog
        )
    }
}
