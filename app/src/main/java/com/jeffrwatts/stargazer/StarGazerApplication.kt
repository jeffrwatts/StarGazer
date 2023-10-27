package com.jeffrwatts.stargazer

import android.app.Application
import com.jeffrwatts.stargazer.data.AppContainer
import com.jeffrwatts.stargazer.data.AppContainerImpl

class StarGazerApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainerImpl(this)
    }
}