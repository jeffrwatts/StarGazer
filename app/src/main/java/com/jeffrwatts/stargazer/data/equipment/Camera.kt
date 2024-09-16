package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cameras")
data class Camera(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val displayName: String,
    val sensorWidth: Double,
    val sensorHeight: Double,
    val pixelSize: Double,
    val resolutionWidth: Int,
    val resolutionHeight: Int
)