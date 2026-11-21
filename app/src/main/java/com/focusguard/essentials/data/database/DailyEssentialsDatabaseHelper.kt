package com.focusguard.essentials.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DailyEssentialsDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        const val DATABASE_NAME = "daily_essentials.db"
        const val DATABASE_VERSION = 1

        const val TABLE_SALAH = "daily_salah"
        const val TABLE_BOOKS = "books"
        const val TABLE_READING = "daily_reading"
        const val TABLE_QURAN_HADITH = "daily_quran_hadith"
        const val TABLE_QAZA_SETTINGS = "qaza_settings"
        const val TABLE_QAZA_DAILY = "daily_qaza"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_SALAH (
                date TEXT PRIMARY KEY,
                fajr INTEGER NOT NULL DEFAULT 0,
                dhuhr INTEGER NOT NULL DEFAULT 0,
                asr INTEGER NOT NULL DEFAULT 0,
                maghrib INTEGER NOT NULL DEFAULT 0,
                isha INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_BOOKS (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL UNIQUE COLLATE NOCASE,
                total_pages INTEGER NOT NULL DEFAULT 0,
                is_active INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_READING (
                id TEXT PRIMARY KEY,
                date TEXT NOT NULL,
                book_id TEXT NOT NULL,
                pages_read INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL,
                UNIQUE(date, book_id) ON CONFLICT REPLACE,
                FOREIGN KEY(book_id) REFERENCES $TABLE_BOOKS(id) ON DELETE CASCADE
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_QURAN_HADITH (
                date TEXT PRIMARY KEY,
                quran_study_pages INTEGER NOT NULL DEFAULT 0,
                quran_study_ayahs INTEGER NOT NULL DEFAULT 0,
                quran_memo_pages INTEGER NOT NULL DEFAULT 0,
                quran_memo_ayahs INTEGER NOT NULL DEFAULT 0,
                hadith_study_count INTEGER NOT NULL DEFAULT 0,
                hadith_memo_count INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_QAZA_SETTINGS (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                is_configured INTEGER NOT NULL DEFAULT 0,
                start_date TEXT NOT NULL,
                fajr_initial INTEGER NOT NULL DEFAULT 0,
                dhuhr_initial INTEGER NOT NULL DEFAULT 0,
                asr_initial INTEGER NOT NULL DEFAULT 0,
                maghrib_initial INTEGER NOT NULL DEFAULT 0,
                isha_initial INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_QAZA_DAILY (
                date TEXT PRIMARY KEY,
                fajr_completed INTEGER NOT NULL DEFAULT 0,
                dhuhr_completed INTEGER NOT NULL DEFAULT 0,
                asr_completed INTEGER NOT NULL DEFAULT 0,
                maghrib_completed INTEGER NOT NULL DEFAULT 0,
                isha_completed INTEGER NOT NULL DEFAULT 0,
                fajr_added INTEGER NOT NULL DEFAULT 0,
                dhuhr_added INTEGER NOT NULL DEFAULT 0,
                asr_added INTEGER NOT NULL DEFAULT 0,
                maghrib_added INTEGER NOT NULL DEFAULT 0,
                isha_added INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL
            );
            """.trimIndent()
        )

        // Prepopulate default qaza row if not present
        db.execSQL(
            """
            INSERT OR IGNORE INTO $TABLE_QAZA_SETTINGS 
            (id, is_configured, start_date, fajr_initial, dhuhr_initial, asr_initial, maghrib_initial, isha_initial, updated_at)
            VALUES (1, 0, '2026-11-01', 0, 0, 0, 0, 0, ${System.currentTimeMillis()});
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Handle migration when upgrading schema
    }
}
