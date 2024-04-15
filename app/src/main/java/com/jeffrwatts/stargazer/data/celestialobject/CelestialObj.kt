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
    val friendlyName: String,
    val objectId: String,
    val ngcId: String?,
    val ra: Double,
    val dec: Double,
    val type: ObjectType,
    val subType: String?,
    val constellation: String?,
    val magnitude: Double?,
    val observationNotes: String?,
    val recommended: Boolean,
    val tags: String
)

fun CelestialObj.getImageResource(): Int {
    return when (this.objectId) {
        "Mercury" -> R.drawable.mercury
        "Venus" -> R.drawable.venus
        "Mars" -> R.drawable.mars
        "Jupiter" -> R.drawable.jupiter
        "Saturn" -> R.drawable.saturn
        "Uranus" -> R.drawable.uranus
        "Neptune" -> R.drawable.neptune
        "Moon" -> R.drawable.moon
        "m1" -> R.drawable.m1
        "m13" -> R.drawable.m13
        "m31" -> R.drawable.m31
        "m33" -> R.drawable.m33
        "m42" -> R.drawable.m42
        "m44" -> R.drawable.m44
        "m45" -> R.drawable.m45
        "m51" -> R.drawable.m51
        "m63" -> R.drawable.m63
        "m78" -> R.drawable.m78
        "m81" -> R.drawable.m81
        "m82" -> R.drawable.m82
        "m97" -> R.drawable.m97
        "m101" -> R.drawable.m101
        "m104" -> R.drawable.m104
        "c14" -> R.drawable.caldwell14
        "c49" -> R.drawable.caldwell49
        "c64" -> R.drawable.caldwell64
        else -> when (this.type) {
            ObjectType.STAR -> R.drawable.star
            ObjectType.GALAXY -> R.drawable.galaxy
            ObjectType.NEBULA -> R.drawable.nebula
            ObjectType.CLUSTER -> R.drawable.cluster
            else -> R.drawable.star
        }
    }
}