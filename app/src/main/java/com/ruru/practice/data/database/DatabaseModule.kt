package com.ruru.practice.data.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.ruru.practice.data.dao.*

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RuruDatabase =
            Room.databaseBuilder(context, RuruDatabase::class.java, "ruru_database")
            .addMigrations(RuruDatabase.MIGRATION_2_3, RuruDatabase.MIGRATION_3_4, RuruDatabase.MIGRATION_4_5)
            .build()

    @Provides fun provideMeditationDao(db: RuruDatabase): MeditationDao = db.meditationDao()
    @Provides fun provideDailyPracticeDao(db: RuruDatabase): DailyPracticeDao = db.dailyPracticeDao()
    @Provides fun provideFiveCoverDao(db: RuruDatabase): FiveCoverDao = db.fiveCoverDao()
    @Provides fun provideReflectionDao(db: RuruDatabase): ReflectionDao = db.reflectionDao()
    @Provides fun provideFiveAggregateDao(db: RuruDatabase): FiveAggregateDao = db.fiveAggregateDao()
    @Provides fun provideMindfulnessEventDao(db: RuruDatabase): MindfulnessEventDao = db.mindfulnessEventDao()
    @Provides fun providePreceptDao(db: RuruDatabase): PreceptDao = db.preceptDao()
    @Provides fun provideDependentOriginationDao(db: RuruDatabase): DependentOriginationDao = db.dependentOriginationDao()
    @Provides fun provideWalkingSessionDao(db: RuruDatabase): WalkingSessionDao = db.walkingSessionDao()
    @Provides fun provideRootProtectionDao(db: RuruDatabase): RootProtectionDao = db.rootProtectionDao()
    @Provides fun provideEightPreceptSessionDao(db: RuruDatabase): EightPreceptSessionDao = db.eightPreceptSessionDao()
}
