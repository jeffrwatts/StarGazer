package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.variablestarobject

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "variable_star_objects")
data class VariableStarObj (
    @PrimaryKey(autoGenerate = true) val id: Int,
    val displayName: String,
    val ra: Double,
    val dec: Double,
    val period: Double,
    val magnitudeHigh: Double,
    val magnitudeLow: Double,
    val constellation: String,
    val type: String,
    val spectralType: String
)