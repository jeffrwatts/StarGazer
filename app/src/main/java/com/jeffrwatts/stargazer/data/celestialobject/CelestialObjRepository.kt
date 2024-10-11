package com.jeffrwatts.stargazer.data.celestialobject

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CelestialObjRepository @Inject constructor (
    private val celestialObjDao: CelestialObjDao
)
{
    fun getAllCelestialObjects(): Flow<List<CelestialObjWithImage>> {
        return celestialObjDao.getAll()
    }

    fun getCelestialObjByObjectId(objectId: String): Flow<CelestialObjWithImage?> {
        return celestialObjDao.getByObjectId(objectId)
    }

    fun getCelestialObj(id: Int): Flow<CelestialObjWithImage> {
        return celestialObjDao.get(id)
    }
}