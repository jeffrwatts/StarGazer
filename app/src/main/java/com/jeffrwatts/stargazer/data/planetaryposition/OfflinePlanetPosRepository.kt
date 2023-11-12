package com.jeffrwatts.stargazer.data.planetaryposition

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeffrwatts.stargazer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class OfflinePlanetPosRepository (
    private val context: Context,
    private val dao: PlanetPosDao
) : PlanetPosRepository {

    init {
        CoroutineScope(Dispatchers.IO).launch {
            populateDatabaseIfEmpty()
        }
    }

    override fun getPlanetPositionForDate(planetName: String, date: Double): Flow<PlanetPos>
    = dao.getPlanetPositionForDate(planetName, date)

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
        return context.resources.openRawResource(R.raw.ephemeris).bufferedReader().use { it.readText() }
    }

    private fun parseJson(jsonString: String): List<PlanetPosJson> {
        val gson = Gson()
        val itemType = object : TypeToken<List<PlanetPosJson>>() {}.type
        return gson.fromJson(jsonString, itemType)
    }

    private fun convertJsonToItems(jsonItems: List<PlanetPosJson>): List<PlanetPos> {
        return jsonItems.map { it.toPlanetPosEntity() }
    }
}