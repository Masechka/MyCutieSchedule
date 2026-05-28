package com.example.mycutieschedule

import android.app.Application
import com.example.mycutieschedule.data.sync.EventSyncManager
import com.example.mycutieschedule.data.sync.SharedSchedulesSyncManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class MyCutieScheduleApp : Application() {

    /**
     * Injected by Hilt after [onCreate] is called by the framework.
     * Both sync managers are singletons — starting them here ensures the
     * Firestore ↔ Room sync loops are active for the entire app lifetime.
     */
    @Inject
    lateinit var syncManager: EventSyncManager

    /** Keeps locally-imported shared schedules in sync with their owners' Firestore data. */
    @Inject
    lateinit var sharedSchedulesSyncManager: SharedSchedulesSyncManager

    override fun onCreate() {
        super.onCreate()
        initTimber()
        syncManager.start()
        sharedSchedulesSyncManager.start()
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
