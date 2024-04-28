package com.jeffrwatts.stargazer.data.celestialobject

import android.location.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CelestialObjRepository @Inject constructor (
    private val celestialObjDao: CelestialObjDao
)
{
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
}