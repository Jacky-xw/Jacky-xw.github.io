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
        RootProtectionEntity::class, EightPreceptSessionEntity::class
    ],
    version = 5,
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

    companion object {
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
