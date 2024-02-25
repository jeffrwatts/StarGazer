package com.jeffrwatts.stargazer.data.solarsystem

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanetPosDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(planetPos: List<PlanetPos>)

    @Query("SELECT * FROM ephemeris WHERE planetName = :planetName ORDER BY ABS(time - :time) ASC, time DESC LIMIT 1")
    fun getPlanetPosition(planetName: String, time: Double): Flow<PlanetPos>

    @Query("SELECT COUNT(*) FROM celestial_objects")
    fun getCount(): Int

    @Query("SELECT MIN(time) FROM ephemeris")
    suspend fun getEphemerisStartTime(): Double?

    @Query("SELECT MAX(time) FROM ephemeris")
    suspend fun getEphemerisEndTime(): Double?

    @Query("DELETE FROM ephemeris")
    suspend fun deleteAll()
}