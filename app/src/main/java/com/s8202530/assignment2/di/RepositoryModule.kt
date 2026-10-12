package com.s8202530.assignment2.di

import com.s8202530.assignment2.data.repository.AuthRepository
import com.s8202530.assignment2.data.repository.AuthRepositoryImpl
import com.s8202530.assignment2.data.repository.DashboardRepository
import com.s8202530.assignment2.data.repository.DashboardRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Tells Hilt which class to use when something asks for a repository
 * interface. ViewModels only ever see the interfaces.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository
}
