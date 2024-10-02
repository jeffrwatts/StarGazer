package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.starobj

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stars")
data class StarObj(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val displayName: String,
    val ra: Double,
    val dec: Double,
    val magnitude: Double,
    val constellation: String,
    val spectralType: String?
)