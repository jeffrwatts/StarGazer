package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "optical_elements")
data class OpticalElement(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val displayName: String,
    val factor: Double
)