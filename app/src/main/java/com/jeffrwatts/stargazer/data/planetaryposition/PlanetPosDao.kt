package com.jeffrwatts.stargazer.data.planetaryposition

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanetPosDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(planetPos: List<PlanetPos>)

    @Query("SELECT * FROM ephemeris WHERE planetName = :planetName AND :date >= dateLow AND :date < dateHigh LIMIT 1")
    fun getPlanetPositionForDate(planetName: String, date: Double): Flow<PlanetPos>

    @Query("SELECT COUNT(*) FROM celestial_objects")
    fun getCount(): Int
}