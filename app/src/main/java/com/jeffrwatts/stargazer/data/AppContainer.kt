package com.jeffrwatts.stargazer.data

import android.content.Context
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.OfflineCelestialObjRepository

interface AppContainer {
    val celestialObjRepository: CelestialObjRepository
}

class AppContainerImpl (private val context: Context) : AppContainer {
    override val celestialObjRepository: CelestialObjRepository by lazy {
        OfflineCelestialObjRepository(
            context = context,
            dao = StarGazerDatabase.getDatabase(context).celestialObjDao())
    }
}