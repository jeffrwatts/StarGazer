package com.jeffrwatts.stargazer.data.celestialobject

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jeffrwatts.stargazer.R

enum class ObjectType {
    STAR, GALAXY, NEBULA, CLUSTER, PLANET, UNKNOWN
}

@Entity(tableName = "celestial_objects")
//@TypeConverters(Converters::class)
data class CelestialObj(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val friendlyName: String,
    val catalogId: String?,
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
    return when (this.catalogId) {
        "Mercury" -> R.drawable.mercury
        "Venus" -> R.drawable.venus
        "Mars" -> R.drawable.mars
        "Jupiter" -> R.drawable.jupiter
        "Saturn" -> R.drawable.saturn
        "Uranus" -> R.drawable.uranus
        "Neptune" -> R.drawable.neptune
        "Moon" -> R.drawable.moon
        "M1" -> R.drawable.m1
        "M13" -> R.drawable.m13
        "M31" -> R.drawable.m31
        "M33" -> R.drawable.m33
        "M42" -> R.drawable.m42
        "M44" -> R.drawable.m44
        "M45" -> R.drawable.m45
        "M51" -> R.drawable.m51
        "M63" -> R.drawable.m63
        "M78" -> R.drawable.m78
        "M81" -> R.drawable.m81
        "M82" -> R.drawable.m82
        "M97" -> R.drawable.m97
        "M101" -> R.drawable.m101
        "M104" -> R.drawable.m104
        "Caldwell 14" -> R.drawable.caldwell14
        "Caldwell 49" -> R.drawable.caldwell49
        "Caldwell 64" -> R.drawable.caldwell64
        else -> when (this.type) {
            ObjectType.STAR -> R.drawable.star
            ObjectType.GALAXY -> R.drawable.galaxy
            ObjectType.NEBULA -> R.drawable.nebula
            ObjectType.CLUSTER -> R.drawable.cluster
            else -> R.drawable.star
        }
    }
}