package com.jeffrwatts.stargazer.data.celestialobject

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jeffrwatts.stargazer.R
import io.github.cosinekitty.astronomy.Body

enum class ObjectType {
    STAR, GALAXY, NEBULA, CLUSTER, PLANET, UNKNOWN
}

const val SUN = "Sun"
const val MERCURY = "Mercury"
const val VENUS = "Venus"
const val EARTH = "Earth"
const val MARS = "Mars"
const val JUPITER = "Jupiter"
const val SATURN = "Saturn"
const val URANUS = "Uranus"
const val NEPTUNE = "Neptune"
const val PLUTO = "Pluto"
const val MOON = "Moon"

@Entity(tableName = "celestial_objects")
data class CelestialObj(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val displayName: String,
    val objectId: String,
    var ra: Double,                 // ra and dec are var for planets.
    var dec: Double,
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
        else -> when (this.objectId) {
            MERCURY -> R.drawable.mercury
            VENUS -> R.drawable.venus
            MARS -> R.drawable.mars
            JUPITER -> R.drawable.jupiter
            SATURN -> R.drawable.saturn
            URANUS -> R.drawable.uranus
            NEPTUNE -> R.drawable.neptune
            PLUTO -> R.drawable.pluto
            MOON -> R.drawable.moon
            else-> R.drawable.logo
        }
    }
}

fun mapBody(name: String): Body? {
    return when (name) {
        SUN -> Body.Sun
        MERCURY -> Body.Mercury
        VENUS -> Body.Venus
        EARTH -> Body.Earth
        MARS -> Body.Mars
        JUPITER -> Body.Jupiter
        SATURN -> Body.Saturn
        URANUS -> Body.Uranus
        NEPTUNE -> Body.Neptune
        PLUTO -> Body.Pluto
        MOON -> Body.Moon
        else -> null
    }
}