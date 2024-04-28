package com.jeffrwatts.stargazer.data.solarsystem

import android.location.Location
import android.util.Log
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.network.StarGazerApi
import com.jeffrwatts.stargazer.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SolarSystemRepository @Inject constructor (
    private val dao: EphemerisDao,
    private val starGazerApi: StarGazerApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
)
{
    companion object {
        const val JULIAN_DAY = 1.0
        const val JULIAN_MINUTE = 1.0 / (24.0 * 60.0)
        const val PLANET_ID_START = 10000
    }

    private var ephemerisStartTime: Double? = null
    private val lengthOfDataDays = 2 * JULIAN_DAY
    private val staleDataOffsetMinutes = 2 * JULIAN_MINUTE

    private val solarSystem : List<PlanetObj> = listOf (
        PlanetObj(PLANET_ID_START, "Mercury", 0.0, 0.0),
        PlanetObj( PLANET_ID_START +1, "Venus", 0.0, 0.0),
        PlanetObj( PLANET_ID_START+2, "Mars", 0.0, 0.0),
        PlanetObj( PLANET_ID_START+3, "Jupiter", 0.0, 0.0),
        PlanetObj( PLANET_ID_START+4, "Saturn", 0.0, 0.0),
        PlanetObj( PLANET_ID_START+5, "Uranus", 0.0, 0.0),
        PlanetObj( PLANET_ID_START+6, "Neptune", 0.0, 0.0),
        PlanetObj( PLANET_ID_START+7, "Moon", 0.0, 0.0)
    )

    init {
        CoroutineScope (ioDispatcher).launch {
            ephemerisStartTime = dao.getEphemerisStartTime()
        }
    }

    suspend fun getAllPlanets(location: Location, date: Double): Flow<List<PlanetObjPos>> = flow {
        // Emit cached data immediately
        val cachedData = solarSystem.map {
            PlanetObjPos.fromPlanetObj(updatePlanetPosition(it, date), date, location.latitude, location.longitude)
        }
        emit(cachedData)

        // Update from API if needed and emit fresh data
        if (shouldUpdate(date)) {
            updateFromApi(date)
            val freshData = solarSystem.map {
                PlanetObjPos.fromPlanetObj(updatePlanetPosition(it, date), date, location.latitude, location.longitude)
            }
            emit(freshData)
        }
    }

    private suspend fun updatePlanetPosition(planetObj: PlanetObj, time: Double): PlanetObj {
        return try {
            val planetPos = dao.getPlanetPosition(planetObj.planetName, time).firstOrNull()
            planetPos?.let { planetObj.copy(ra = it.ra, dec = it.dec) } ?: planetObj
        } catch (e: Exception) {
            Log.e("OfflineCelestialObjRepo", "Error updating planet position", e)
            planetObj
        }
    }

    private fun shouldUpdate(time: Double):Boolean {
        return ephemerisStartTime?.let { startTime-> time > startTime+staleDataOffsetMinutes } ?: true
    }

    private suspend fun updateFromApi(time:Double) {
        val ephemeris = getFromApi(time)

        if (ephemeris.isNotEmpty()) {
            dao.deleteAll()
            dao.insertAll(ephemeris)
            ephemerisStartTime = dao.getEphemerisStartTime()
        }
    }

    private suspend fun getFromApi(time: Double) : List<EphemerisEntry> {
        return withContext(ioDispatcher){
            try {
                starGazerApi.getEphemeris(time, lengthOfDataDays).map {
                    it.toEphemerisEntry()
                }
            } catch (exception: Exception) {
                emptyList()
            }
        }
    }
}