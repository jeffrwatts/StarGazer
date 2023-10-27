package com.jeffrwatts.stargazer.data.celestialobject

import kotlinx.coroutines.flow.Flow

interface CelestialObjRepository {
    fun getAllStream(): Flow<List<CelestialObj>>
    fun getAllByTypeStream(type: ObjectType): Flow<List<CelestialObj>>
    fun getStream(id: Int): Flow<CelestialObj?>
    suspend fun insert(celestialObj: CelestialObj)
    suspend fun delete(celestialObj: CelestialObj)
    suspend fun update(celestialObj: CelestialObj)
}