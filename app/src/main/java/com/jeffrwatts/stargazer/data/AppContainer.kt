package com.jeffrwatts.stargazer.data

import android.content.Context
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.OfflineCelestialObjRepository
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.data.planetaryposition.OfflinePlanetPosRepository
import com.jeffrwatts.stargazer.data.planetaryposition.PlanetPosRepository

interface AppContainer {
    val celestialObjRepository: CelestialObjRepository
    val locationRepository: LocationRepository
    val planetPosRepository: PlanetPosRepository
}

class AppContainerImpl (private val context: Context) : AppContainer {
    override val celestialObjRepository: CelestialObjRepository by lazy {
        OfflineCelestialObjRepository(
            context = context,
            dao = StarGazerDatabase.getDatabase(context).celestialObjDao(),
            planetPosRepository)
    }

    override val locationRepository: LocationRepository by lazy {
        LocationRepository(context = context)
    }

    override val planetPosRepository: PlanetPosRepository by lazy {
        OfflinePlanetPosRepository(
            context = context,
            dao = StarGazerDatabase.getDatabase(context).planetPosDao())
    }
}

