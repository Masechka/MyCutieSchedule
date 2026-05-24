package com.example.mycutieschedule

import android.app.Application
import com.example.mycutieschedule.data.sync.EventSyncManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MyCutieScheduleApp : Application() {

    /**
     * Injected by Hilt after [onCreate] is called by the framework.
     * [EventSyncManager] is a singleton — starting it here ensures the
     * Firestore ↔ Room sync loop is active for the entire app lifetime.
     */
    @Inject
    lateinit var syncManager: EventSyncManager

    override fun onCreate() {
        super.onCreate()
        initTimber()
        syncManager.start()
    }

    /**
     * Plants a Timber DebugTree only in debug builds.
     * In release builds no logging is active, so no sensitive data leaks to logcat.
     */
    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
