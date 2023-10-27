package com.jeffrwatts.stargazer.data.celestialobject

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeffrwatts.stargazer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class OfflineCelestialObjRepository (
    private val context: Context,
    private val dao: CelestialObjDao
) : CelestialObjRepository {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            populateDatabaseIfEmpty()
        }
    }

    override fun getAllStream(): Flow<List<CelestialObj>> = dao.getAll()
    override fun getAllByTypeStream(type: ObjectType): Flow<List<CelestialObj>> = dao.getAllWithType(type)
    override fun getStream(id: Int): Flow<CelestialObj?> = dao.get(id)
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