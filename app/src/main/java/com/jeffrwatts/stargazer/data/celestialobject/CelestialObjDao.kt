package com.jeffrwatts.stargazer.data.celestialobject

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CelestialObjDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) // Reconsider REPLACE
    suspend fun insert(celestialObj: CelestialObj)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(celestialObjs: List<CelestialObj>)

    @Update
    suspend fun update(celestialObj: CelestialObj)

    @Delete
    suspend fun delete(celestialObj: CelestialObj)

    @Query("SELECT * FROM celestial_objects WHERE id = :itemId")
    fun get(itemId: Int): Flow<CelestialObj>

    @Query("SELECT * FROM celestial_objects")
    fun getAll(): Flow<List<CelestialObj>>

    @Query("SELECT * FROM celestial_objects WHERE type = :type")
    fun getAllWithType(type: ObjectType): Flow<List<CelestialObj>>

    @Query("SELECT COUNT(*) FROM celestial_objects")
    fun getCount(): Int
}