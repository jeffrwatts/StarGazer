package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.planetaryposition

import android.location.Location
import android.util.Log
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjPos
import com.jeffrwatts.stargazer.data.celestialobject.ObjectType
import com.jeffrwatts.stargazer.data.solarsystem.PlanetPos
import com.jeffrwatts.stargazer.data.solarsystem.PlanetPosDao
import com.jeffrwatts.stargazer.data.solarsystem.toPlanetPosEntity
import com.jeffrwatts.stargazer.di.IoDispatcher
import com.jeffrwatts.stargazer.network.EphemerisApi
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SolarSystemRepository @Inject constructor (
    private val dao: PlanetPosDao,
    private val ephemerisApi: EphemerisApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
)
{
    companion object {
        const val JULIAN_DAY = 1.0
        const val JULIAN_MINUTE = 1.0 / (24.0 * 60.0)   // Data is stale after 1 minute (for testing)
        const val PLANET_START_ID = 10000
    }

    private var ephemerisStartTime: Double? = null
    private val lengthOfDataDays = 2 * JULIAN_DAY
    private val staleDataOffsetMinutes = 2 * JULIAN_MINUTE


    private val solarSystem : List<CelestialObj> = listOf (
        CelestialObj(PLANET_START_ID, "Mercury", "Mercury", null, 0.0, 0.0, ObjectType.PLANET, null, null, null, null, true, ""),
        CelestialObj(PLANET_START_ID+1, "Venus", "Venus", null, 0.0, 0.0, ObjectType.PLANET, null, null, null, null, true, ""),
        CelestialObj(PLANET_START_ID+2, "Mars", "Mars", null, 0.0, 0.0, ObjectType.PLANET, null, null, null, null, true, ""),
        CelestialObj(PLANET_START_ID+3, "Jupiter", "Jupiter", null, 0.0, 0.0, ObjectType.PLANET, null, null, null, null, true, ""),
        CelestialObj(PLANET_START_ID+4, "Saturn", "Saturn", null, 0.0, 0.0, ObjectType.PLANET, null, null, null, null, true, ""),
        CelestialObj(PLANET_START_ID+5, "Uranus", "Uranus", null, 0.0, 0.0, ObjectType.PLANET, null, null, null, null, true, ""),
        CelestialObj(PLANET_START_ID+6, "Neptune", "Neptune", null, 0.0, 0.0, ObjectType.PLANET, null, null, null, null, true, ""),
        CelestialObj(PLANET_START_ID+7, "Moon", "Moon", null, 0.0, 0.0, ObjectType.PLANET, null, null, null, null, true, "")
    )

    init {
        CoroutineScope (ioDispatcher).launch {
            ephemerisStartTime = dao.getEphemerisStartTime()
        }
    }

    suspend fun getAllPlanets(location: Location, date: Double): Flow<List<CelestialObjPos>> = flow {
        // Emit cached data immediately
        val cachedData = solarSystem.map {
            CelestialObjPos.fromCelestialObj(updatePlanetPosition(it, date), date, location.latitude, location.longitude)
        }
        emit(cachedData)

        // Update from API if needed and emit fresh data
        if (shouldUpdate(date)) {
            updateFromApi(date)
            val freshData = solarSystem.map {
                CelestialObjPos.fromCelestialObj(updatePlanetPosition(it, date), date, location.latitude, location.longitude)
            }
            emit(freshData)
        }
    }

    private suspend fun updatePlanetPosition(celestialObj: CelestialObj, time: Double): CelestialObj {
        return try {
            val planetPos = dao.getPlanetPosition(celestialObj.friendlyName, time).firstOrNull()
            planetPos?.let { celestialObj.copy(ra = it.ra, dec = it.dec) } ?: celestialObj
        } catch (e: Exception) {
            Log.e("OfflineCelestialObjRepo", "Error updating planet position", e)
            celestialObj
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

    private suspend fun getFromApi(time: Double) : List<PlanetPos> {
        return withContext(ioDispatcher){
            try {
                ephemerisApi.getEphemeris(time, lengthOfDataDays).map {
                    it.toPlanetPosEntity()
                }
            } catch (exception: Exception) {
                emptyList<PlanetPos>()
            }
        }
    }
}