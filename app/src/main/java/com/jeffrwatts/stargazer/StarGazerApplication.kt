package com.jeffrwatts.stargazer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class StarGazerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
    }
}