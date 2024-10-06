package com.jeffrwatts.stargazer.data.celestialobjectimage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "celestial_object_images")
data class CelestialObjImage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val objectId: String,
    val filename: String,
    val thumbFilename: String
)
