package com.jeffrwatts.stargazer.data.celestialobject

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.data.planetaryposition.PlanetPosRepository
import com.jeffrwatts.stargazer.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class OfflineCelestialObjRepository (
    private val context: Context,
    private val dao: CelestialObjDao,
    private val planetPosRepository: PlanetPosRepository
) : CelestialObjRepository {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            populateDatabaseIfEmpty()
        }
    }

    override fun getAllStream(): Flow<List<CelestialObj>> {
        val jdNow = Utils.calculateJulianDateNow()
        return dao.getAll().map { list ->
            list.map { obj ->
                if (obj.type == ObjectType.PLANET) {
                    updatePlanetPosition (obj, jdNow)
                } else {
                    obj
                }
            }
        }
    }

    override fun getAllByTypeStream(type: ObjectType): Flow<List<CelestialObj>> {
        val jdNow = Utils.calculateJulianDateNow()
        return dao.getAllWithType(type).map { list ->
            list.map { obj ->
                if (obj.type == ObjectType.PLANET) {
                    updatePlanetPosition (obj, jdNow)
                } else {
                    obj
                }
            }
        }
    }

    override fun getStream(id: Int): Flow<CelestialObj?> {
        val jdNow = Utils.calculateJulianDateNow()
        return dao.get(id).map { obj->
            if (obj.type == ObjectType.PLANET) {
                updatePlanetPosition (obj, jdNow)
            } else {
                obj
            }
        }
    }
    override suspend fun insert(celestialObj: CelestialObj) = dao.insert(celestialObj)
    override suspend fun delete(celestialObj: CelestialObj) = dao.delete(celestialObj)
    override suspend fun update(celestialObj: CelestialObj) = dao.update(celestialObj)

    private suspend fun populateDatabaseIfEmpty() {
        val count = dao.getCount()
        if (count == 0) {
            val jsonString = loadJsonFromRaw()
            val jsonItems = parseJson(jsonString)
            val items = convertJsonToItems(jsonItems)
            dao.insertAll(items)
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

    private fun loadJsonFromRaw(): String {
        return context.resources.openRawResource(R.raw.items).bufferedReader().use { it.readText() }
    }

    private fun parseJson(jsonString: String): List<CelestialObjJson> {
        val gson = Gson()
        val itemType = object : TypeToken<List<CelestialObjJson>>() {}.type
        return gson.fromJson(jsonString, itemType)
    }

    private fun convertJsonToItems(jsonItems: List<CelestialObjJson>): List<CelestialObj> {
        return jsonItems.map { it.toCelestialObjEntity() }
    }
}