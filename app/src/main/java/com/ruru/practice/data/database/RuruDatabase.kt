package com.ruru.practice.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ruru.practice.data.dao.*
import com.ruru.practice.data.entity.*

@Database(
    entities = [
        MeditationSessionEntity::class, PreceptEntity::class, DailyPracticeEntity::class,
        MindfulnessEventEntity::class, FiveCoverEntity::class, ReflectionEntity::class,
        DependentOriginationEntity::class, FiveAggregateEntity::class, WalkingSessionEntity::class,
        RootProtectionEntity::class, EightPreceptSessionEntity::class, WeeklyReportEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class RuruDatabase : RoomDatabase() {
    abstract fun meditationDao(): MeditationDao
    abstract fun dailyPracticeDao(): DailyPracticeDao
    abstract fun fiveCoverDao(): FiveCoverDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun fiveAggregateDao(): FiveAggregateDao
    abstract fun mindfulnessEventDao(): MindfulnessEventDao
    abstract fun preceptDao(): PreceptDao
    abstract fun dependentOriginationDao(): DependentOriginationDao
    abstract fun walkingSessionDao(): WalkingSessionDao
    abstract fun rootProtectionDao(): RootProtectionDao
    abstract fun eightPreceptSessionDao(): EightPreceptSessionDao
    abstract fun weeklyReportDao(): WeeklyReportDao

    companion object {
        /**
         * V1 -> V2 completed the original Room MVP schema. Very old installs can
         * therefore be upgraded without destructive migration. CREATE IF NOT EXISTS
         * deliberately preserves any tables already present in a V1 database.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS meditation_session (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, durationSeconds INTEGER NOT NULL, observation TEXT NOT NULL, afterState TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS daily_practice (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, focus TEXT NOT NULL, note TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS five_cover (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, type TEXT NOT NULL, intensity INTEGER NOT NULL, trigger TEXT NOT NULL, response TEXT NOT NULL, observation TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS reflection (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, content TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS five_aggregate (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, event TEXT NOT NULL, rupa TEXT NOT NULL, vedana TEXT NOT NULL, sanna TEXT NOT NULL, sankhara TEXT NOT NULL, vinnana TEXT NOT NULL, reflection TEXT NOT NULL, createdAt INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS mindfulness_event (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, scene TEXT NOT NULL, description TEXT NOT NULL, feeling TEXT NOT NULL, reaction TEXT NOT NULL, awareness TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS precept (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, event TEXT NOT NULL, reflection TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS dependent_origination (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `trigger` TEXT NOT NULL, feeling TEXT NOT NULL, craving TEXT NOT NULL, grasping TEXT NOT NULL, reflection TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meditation_session ADD COLUMN practiceStep INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE meditation_session ADD COLUMN stepTitle TEXT NOT NULL DEFAULT '入息、出息'")
                db.execSQL("CREATE TABLE IF NOT EXISTS walking_session (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, durationSeconds INTEGER NOT NULL, method TEXT NOT NULL, observation TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS root_protection (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, sense TEXT NOT NULL, contact TEXT NOT NULL, feeling TEXT NOT NULL, craving TEXT NOT NULL, grasping TEXT NOT NULL, response TEXT NOT NULL)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE precept ADD COLUMN date TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE IF NOT EXISTS eight_precept_session (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, date TEXT NOT NULL, checkedMask INTEGER NOT NULL, note TEXT NOT NULL)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS weekly_report (id INTEGER NOT NULL PRIMARY KEY, weekStart TEXT NOT NULL, weekEnd TEXT NOT NULL, content TEXT NOT NULL, generatedAt INTEGER NOT NULL)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // High-frequency date/timestamp filters used by the home summary,
                // history lists, and weekly retention pass.
                db.execSQL("CREATE INDEX IF NOT EXISTS index_meditation_session_date ON meditation_session(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_walking_session_date ON walking_session(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_root_protection_date ON root_protection(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_five_cover_date ON five_cover(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_precept_date ON precept(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_mindfulness_event_date ON mindfulness_event(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_daily_practice_date ON daily_practice(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_eight_precept_session_date ON eight_precept_session(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reflection_createdAt ON reflection(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_five_aggregate_createdAt ON five_aggregate(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_dependent_origination_createdAt ON dependent_origination(createdAt)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reflection ADD COLUMN linkedPractice TEXT")
                // Older builds wrote a new eight-precept row for every edit.
                // Keep the latest snapshot for each day before the new
                // update-in-place flow starts using the table.
                db.execSQL("DELETE FROM eight_precept_session WHERE id NOT IN (SELECT MAX(id) FROM eight_precept_session GROUP BY date)")
            }
        }
    }
}
