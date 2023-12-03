package com.jeffrwatts.stargazer.data.planetaryposition

import com.jeffrwatts.stargazer.di.IoDispatcher
import com.jeffrwatts.stargazer.network.EphemerisApi
import com.jeffrwatts.stargazer.utils.Utils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DefaultPlanetPosRepository @Inject constructor(
    private val dao: PlanetPosDao,
    private val ephemerisApi: EphemerisApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
):PlanetPosRepository {
    companion object {
        const val JULIAN_DAY = 1.0
        const val JULIAN_MINUTE = 1.0 / (24.0 * 60.0)   // Data is stale after 1 minute (for testing)
    }

    private var ephemerisStartTime: Double? = null
    private val updateMutex = Mutex()
    private val lengthOfDataDays = 2 * JULIAN_DAY
    private val stateDataOffsetMinutes = 2 * JULIAN_MINUTE

    init {
        CoroutineScope (ioDispatcher).launch {
            ephemerisStartTime = dao.getEphemerisStartTime()
            val currentTime = Utils.calculateJulianDateNow()
            updateFromApiIfNeeded(currentTime)
        }
    }

    override fun getPlanetPosition(planetName: String, time: Double): Flow<PlanetPos> {
        updateFromApiIfNeeded(time)
        return dao.getPlanetPosition(planetName, time)
    }

    private fun updateFromApiIfNeeded(time: Double) {
        // This method is called multiple times as the caller tries to update the ra and dec values of
        // each of the planets at the same time (individually), generally the first will do the work that
        // is needed, and the others can simply bail (and use the cached data in the database) while this
        // network operation is going on.
        // To allow for this behavior use tryLock (with a finally unlock) instead of withLock so that those
        // calls don't wait on the network call to finish.
        CoroutineScope(ioDispatcher).launch {
            if (updateMutex.tryLock()) {
                try {
                    if (shouldUpdate(time)) {
                        val ephemeris = getFromApi(time)

                        if (ephemeris.isNotEmpty()) {
                            dao.deleteAll()
                            dao.insertAll(ephemeris)
                            ephemerisStartTime = dao.getEphemerisStartTime()
                        }
                    }
                } finally {
                    updateMutex.unlock()
                }
            }
        }
    }


    private fun shouldUpdate(time: Double):Boolean {
        return ephemerisStartTime?.let { startTime-> time > startTime+stateDataOffsetMinutes } ?: true
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