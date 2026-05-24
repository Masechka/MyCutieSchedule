package com.example.mycutieschedule.di

import com.example.mycutieschedule.data.remote.api.FirestoreEventDataSource
import com.example.mycutieschedule.data.remote.api.FirestoreShareDataSource
import com.example.mycutieschedule.data.remote.api.FirestoreUserDataSource
import com.example.mycutieschedule.data.remote.datasource.FirestoreEventDataSourceImpl
import com.example.mycutieschedule.data.remote.datasource.FirestoreShareDataSourceImpl
import com.example.mycutieschedule.data.remote.datasource.FirestoreUserDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds Firestore data source interfaces to their implementations.
 *
 * Kept separate from [RepositoryModule] so the remote-layer wiring is
 * self-contained and easy to replace (e.g. swap to a test double in
 * instrumented tests without touching repository bindings).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RemoteDataSourceModule {

    @Binds
    @Singleton
    abstract fun bindFirestoreEventDataSource(
        impl: FirestoreEventDataSourceImpl,
    ): FirestoreEventDataSource

    @Binds
    @Singleton
    abstract fun bindFirestoreUserDataSource(
        impl: FirestoreUserDataSourceImpl,
    ): FirestoreUserDataSource

    @Binds
    @Singleton
    abstract fun bindFirestoreShareDataSource(
        impl: FirestoreShareDataSourceImpl,
    ): FirestoreShareDataSource
}
