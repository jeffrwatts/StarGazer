package com.jeffrwatts.stargazer.data.celestialobject

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jeffrwatts.stargazer.R

enum class ObjectType {
    STAR, GALAXY, NEBULA, CLUSTER, PLANET, UNKNOWN
}

@Entity(tableName = "celestial_objects")
data class CelestialObj(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val displayName: String,
    val objectId: String,
    val ra: Double,
    val dec: Double,
    val type: ObjectType,
    val subType: String?,
    val magnitude: Double?,
    val constellation: String?,
    val ngcId: String?,
    val recommended: Boolean,
    val tags: String
)

fun CelestialObj.getDefaultImageResource(): Int {
    return when (this.type) {
        ObjectType.STAR -> R.drawable.star
        ObjectType.GALAXY -> R.drawable.galaxy
        ObjectType.NEBULA -> R.drawable.nebula
        ObjectType.CLUSTER -> R.drawable.cluster
        else -> R.drawable.star
    }
}