package com.example.mycutieschedule.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier for the application-scoped [CoroutineScope].
 *
 * Use this wherever you need a long-lived coroutine scope that:
 *   - Lives as long as the application process (not tied to any Activity/ViewModel).
 *   - Survives configuration changes.
 *   - Cancels all children automatically when the process dies.
 *
 * Example usage:
 * ```kotlin
 * class MyManager @Inject constructor(
 *     @ApplicationScope private val scope: CoroutineScope
 * )
 * ```
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    /**
     * Provides a single application-wide [CoroutineScope].
     *
     * - [SupervisorJob]: a child coroutine failure does NOT cancel the scope or
     *   other children — critical for a manager that runs multiple independent jobs.
     * - [Dispatchers.Default]: CPU-bound default; individual coroutines can switch
     *   to [Dispatchers.IO] for database/network work via `withContext`.
     */
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
