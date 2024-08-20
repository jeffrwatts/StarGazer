package com.jeffrwatts.stargazer.data.variablestarobject

import kotlinx.coroutines.flow.Flow

class VariableStarObjRepository (
    private val variableStarObjDao: VariableStarObjDao
) {
    fun getAllVariableStarObjs(): Flow<List<VariableStarObj>> {
        return variableStarObjDao.getAll()
    }

    fun getVariableStarObj(id: Int): Flow<VariableStarObj> {
        return variableStarObjDao.get(id)
    }
}