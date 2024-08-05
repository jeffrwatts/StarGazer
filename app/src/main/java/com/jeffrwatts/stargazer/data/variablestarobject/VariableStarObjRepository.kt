package com.jeffrwatts.stargazer.data.variablestarobject

import android.location.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class VariableStarObjRepository (
    private val variableStarObjDao: VariableStarObjDao
) {
    fun getAllVariableStarObjs(): Flow<List<VariableStarObj>> {
        return variableStarObjDao.getAll()
    }

    fun getVariableStarObj(id: Int, location: Location, date: Double): Flow<VariableStarObjPos> {
        return variableStarObjDao.get(id).map { obj->
            VariableStarObjPos.fromVariableStarObj(obj, date, location.latitude, location.longitude)
        }
    }
}