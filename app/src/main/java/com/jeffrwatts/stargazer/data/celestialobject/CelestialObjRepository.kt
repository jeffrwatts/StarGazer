package com.jeffrwatts.stargazer.data.celestialobject

import android.content.Context
import android.location.Location
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class CelestialObjRepository @Inject constructor (
    private val celestialObjDao: CelestialObjDao,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher) {

    init {
        CoroutineScope(ioDispatcher).launch {
            populateDatabaseIfEmpty()
        }
    }

    fun getAllCelestialObjsByType(types: List<ObjectType>, location: Location, date: Double): Flow<List<CelestialObjPos>> {
        return celestialObjDao.getByTypes(types).map { objects->
            objects.map { obj->
                CelestialObjPos.fromCelestialObjWithImage(obj, date, location.latitude, location.longitude)
            }
        }
    }

    fun getCelestialObj(id: Int, location: Location, date: Double): Flow<CelestialObjPos> {
        return celestialObjDao.get(id).map { obj->
            CelestialObjPos.fromCelestialObjWithImage(obj, date, location.latitude, location.longitude)
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