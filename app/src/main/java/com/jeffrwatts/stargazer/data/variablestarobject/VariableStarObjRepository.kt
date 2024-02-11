package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.variablestarobject

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

class VariableStarObjRepository (
    private val variableStarObjDao: VariableStarObjDao,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    init {
        CoroutineScope(ioDispatcher).launch {
            populateDatabaseIfEmpty()
        }
    }

    fun getAllVariableStarObjs(location: Location, date: Double): Flow<List<VariableStarObjPos>> {
        return variableStarObjDao.getAll().map { objects->
            objects.map {
                VariableStarObjPos.fromVariableStarObj(it, date, location.latitude, location.longitude)
            }
        }
    }

    private suspend fun populateDatabaseIfEmpty() {
        val count = variableStarObjDao.getCount()
        if (count == 0) {
            populateDao()
        }
    }

    private suspend fun populateDao()  {
        val jsonString = context.resources.openRawResource(R.raw.variable_stars).bufferedReader().use { it.readText() }

        val gson = Gson()
        val itemType = object : TypeToken<List<VariableStarObjJson>>() {}.type
        val jsonItems: List<VariableStarObjJson> = gson.fromJson(jsonString, itemType)
        val variableStarObjs = jsonItems.map { it.toVariableStarObjEntity() }
        variableStarObjDao.insertAll(variableStarObjs)
    }
}