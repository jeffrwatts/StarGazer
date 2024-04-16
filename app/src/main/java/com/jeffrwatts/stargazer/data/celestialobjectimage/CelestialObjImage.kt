package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.celestialobjectimage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "celestial_object_images")
data class CelestialObjImage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val objectId: String,
    val crop: Int,
    val filename: String
)
