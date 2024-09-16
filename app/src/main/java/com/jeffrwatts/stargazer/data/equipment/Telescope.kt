package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "telescopes")
data class Telescope(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val displayName: String,
    val focalLength: Double,
    val aperture: Double
)