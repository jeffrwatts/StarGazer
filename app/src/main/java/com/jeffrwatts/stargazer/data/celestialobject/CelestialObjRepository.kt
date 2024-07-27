package com.jeffrwatts.stargazer.data.celestialobject

import android.location.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CelestialObjRepository @Inject constructor (
    private val celestialObjDao: CelestialObjDao
)
{
    fun getAllCelestialObjects(): Flow<List<CelestialObjWithImage>> {
        return celestialObjDao.getAll()
    }

    fun getCelestialObj(id: Int, location: Location, date: Double): Flow<CelestialObjPos> {
        return celestialObjDao.get(id).map { obj->
            CelestialObjPos.fromCelestialObjWithImage(obj, date, location.latitude, location.longitude)
        }
    }
}