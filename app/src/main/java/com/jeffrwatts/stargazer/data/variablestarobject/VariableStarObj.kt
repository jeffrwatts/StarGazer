package com.jeffrwatts.stargazer.data.variablestarobject

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VariableType {
    SPP, EB, CV, UNKNOWN
}


@Entity(tableName = "variable_star_objects")
data class VariableStarObj (
    @PrimaryKey(autoGenerate = true) val id: Int,
    val displayName: String,
    val ra: Double,
    val dec: Double,
    val period: Double,
    val epoch: Double,
    val magnitudeHigh: Double,
    val magnitudeLow: Double,
    val constellation: String,
    val variableType: VariableType,
    val type: String,
    val spectralType: String,
    val OID: Long // Link to object in AAVSO VSX
)