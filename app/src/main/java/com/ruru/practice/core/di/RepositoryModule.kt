package com.ruru.practice.core.di

import com.ruru.practice.data.repository.PracticeRepository
import com.ruru.practice.data.repository.PracticeRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindPracticeRepository(
        impl: PracticeRepositoryImpl
    ): PracticeRepository
}
