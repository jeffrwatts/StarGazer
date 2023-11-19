package com.jeffrwatts.stargazer.data.planetaryposition

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ephemeris")
data class PlanetPos(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val planetName: String,
    val time: Double,
    val ra: Double,
    val dec: Double,
)