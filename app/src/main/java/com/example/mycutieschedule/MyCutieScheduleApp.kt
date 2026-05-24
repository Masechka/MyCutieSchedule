package com.example.mycutieschedule

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MyCutieScheduleApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initTimber()
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
