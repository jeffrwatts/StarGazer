package com.jeffrwatts.stargazer.data

import android.content.Context
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.OfflineCelestialObjRepository
import com.jeffrwatts.stargazer.data.location.LocationRepository

interface AppContainer {
    val celestialObjRepository: CelestialObjRepository
    val locationRepository: LocationRepository
}

class AppContainerImpl (private val context: Context) : AppContainer {
    override val celestialObjRepository: CelestialObjRepository by lazy {
        OfflineCelestialObjRepository(
            context = context,
            dao = StarGazerDatabase.getDatabase(context).celestialObjDao())
    }

    override val locationRepository: LocationRepository by lazy {
        LocationRepository(context = context)
    }
}