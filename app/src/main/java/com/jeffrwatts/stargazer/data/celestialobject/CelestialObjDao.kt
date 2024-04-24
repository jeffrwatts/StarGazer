package com.jeffrwatts.stargazer.data.celestialobject

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CelestialObjDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(celestialObjs: List<CelestialObj>)

    @Query("SELECT * FROM celestial_objects WHERE id = :itemId")
    fun get(itemId: Int): Flow<CelestialObjWithImage>

    @Query("SELECT * FROM celestial_objects WHERE type IN (:types)")
    fun getByTypes(types: List<ObjectType>): Flow<List<CelestialObjWithImage>>

    @Query("SELECT COUNT(*) FROM celestial_objects")
    fun getCount(): Int
}