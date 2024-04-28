package com.jeffrwatts.stargazer.data.variablestarobject

import android.location.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VariableStarObjRepository (
    private val variableStarObjDao: VariableStarObjDao
) {
    fun getAllVariableStarObjs(location: Location, date: Double): Flow<List<VariableStarObjPos>> {
        return variableStarObjDao.getAll().map { objects->
            objects.map {
                VariableStarObjPos.fromVariableStarObj(it, date, location.latitude, location.longitude)
            }
        }
    }

    fun getVariableStarObj(id: Int, location: Location, date: Double): Flow<VariableStarObjPos> {
        return variableStarObjDao.get(id).map { obj->
            VariableStarObjPos.fromVariableStarObj(obj, date, location.latitude, location.longitude)
        }
    }
}