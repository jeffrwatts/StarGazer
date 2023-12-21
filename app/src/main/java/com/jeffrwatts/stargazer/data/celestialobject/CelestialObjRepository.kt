package com.jeffrwatts.stargazer.data.celestialobject

import android.content.Context
import android.location.Location
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.data.planetaryposition.PlanetPosRepository
import com.jeffrwatts.stargazer.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

class CelestialObjRepository @Inject constructor (
    private val celestialObjDao: CelestialObjDao,
    private val planetPosRepository: PlanetPosRepository,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher) {

    init {
        CoroutineScope(ioDispatcher).launch {
            populateDatabaseIfEmpty()
        }
    }

    fun getAllCelestialObjs(location: Location, date: Double): Flow<List<CelestialObjPos>> {
        return celestialObjDao.getAll().map { objects->
            objects.map {
                mapObjectPosition(it, location, date)
            }
        }
    }

    fun getAllCelestialObjsByType(types: List<ObjectType>, location: Location, date: Double): Flow<List<CelestialObjPos>> {
        return celestialObjDao.getByTypes(types).map { objects->
            objects.map {
                mapObjectPosition(it, location, date)
            }
        }
    }

    fun getCelestialObj(id: Int, location: Location, date: Double): Flow<CelestialObjPos?> {
        return celestialObjDao.get(id).map { obj->
            mapObjectPosition(obj, location, date)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getCelestialObjsByRaDec(ra: Double, dec: Double, date: Double): Flow<List<CelestialObj>> {
        val (raThreshold, decThreshold) = thresholdsFromDec(dec)

        val planetsFlow = celestialObjDao.getAllWithType(ObjectType.PLANET).map { planets ->
            planets.map { updatePlanetPosition(it, date) }
                .filter { filterByThreshold(it, ra, dec, raThreshold, decThreshold) }
        }.take(1)  // Take only the first emission

        val types = listOf(ObjectType.STAR, ObjectType.GALAXY, ObjectType.NEBULA, ObjectType.CLUSTER)
        val objectsType = celestialObjDao.findByRaDec(types, ra, dec, raThreshold, decThreshold).take(1)  // Take only the first emission

        return planetsFlow.flatMapConcat { planets ->
            objectsType.map { objs ->
                planets + objs
            }
        }
    }

    suspend fun update(celestialObj: CelestialObj) = celestialObjDao.update(celestialObj)

    private fun thresholdsFromDec(dec: Double) : Pair<Double, Double> {
        val absDec = abs(dec)

        return when {
            absDec > 87.0 -> Pair(180.0, 5.0)
            absDec > 80.0 -> Pair(60.0, 5.0)
            absDec > 60.0 -> Pair(11.0, 5.0)
            else -> Pair (6.0, 5.0)
        }
    }

    private fun filterByThreshold(celestialObj: CelestialObj, ra: Double, dec: Double, raThreshold: Double, decThreshold: Double): Boolean {
        //SELECT * FROM celestial_objects
        //        WHERE type IN (:types)
        //AND (
        //    (ra BETWEEN :ra - :raThreshold AND :ra + :raThreshold) OR
        //        (:ra - :raThreshold < 0 AND ra BETWEEN 360 + (:ra - :raThreshold) AND 360) OR
        //(:ra + :raThreshold > 360 AND ra BETWEEN 0 AND (:ra + :raThreshold - 360))
        //)
        //AND dec BETWEEN :dec - :decThreshold AND :dec + :decThreshold

        val raMatch = (celestialObj.ra in ra-raThreshold .. ra+raThreshold) ||
                (ra-raThreshold < 0.0 && celestialObj.ra in 360.0+(ra-raThreshold)..360.0) ||
                (ra+raThreshold > 360.0 && celestialObj.ra in 0.0 .. (ra+raThreshold-360.0))

        return raMatch && (celestialObj.dec in dec-decThreshold .. dec+decThreshold)
    }

    private suspend fun mapObjectPosition(celestialObj: CelestialObj, location: Location, date: Double): CelestialObjPos {
        return if (celestialObj.type == ObjectType.PLANET) {
            CelestialObjPos.fromCelestialObj(updatePlanetPosition(celestialObj, date), date, location.latitude, location.longitude)
        } else {
            CelestialObjPos.fromCelestialObj(celestialObj, date, location.latitude, location.longitude)
        }
    }

    private suspend fun updatePlanetPosition(celestialObj: CelestialObj, time: Double): CelestialObj {
        return try {
            val planetPos = planetPosRepository.getPlanetPosition(celestialObj.friendlyName, time).firstOrNull()
            planetPos?.let { celestialObj.copy(ra = it.ra, dec = it.dec) } ?: celestialObj
        } catch (e: Exception) {
            Log.e("OfflineCelestialObjRepo", "Error updating planet position", e)
            celestialObj
        }
    }
    private suspend fun populateDatabaseIfEmpty() {
        val count = celestialObjDao.getCount()
        if (count == 0) {
            populateDao()
        }
    }

    private suspend fun populateDao()  {
        val jsonString = context.resources.openRawResource(R.raw.items).bufferedReader().use { it.readText() }

        val gson = Gson()
        val itemType = object : TypeToken<List<CelestialObjJson>>() {}.type
        val jsonItems: List<CelestialObjJson> = gson.fromJson(jsonString, itemType)
        val celestialObjs = jsonItems.map { it.toCelestialObjEntity() }
        celestialObjDao.insertAll(celestialObjs)
    }
}