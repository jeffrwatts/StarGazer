package com.jeffrwatts.stargazer.data.celestialobject

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.JUPITER
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.MARS
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.MERCURY
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.NEPTUNE
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.PLUTO
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.SATURN
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.URANUS
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.VENUS

enum class ObjectType {
    STAR, GALAXY, NEBULA, CLUSTER, PLANET, MOON, UNKNOWN
}

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
        ObjectType.MOON -> R.drawable.moon
        else -> when (this.objectId) {
            MERCURY -> R.drawable.mercury
            VENUS -> R.drawable.venus
            MARS -> R.drawable.mars
            JUPITER -> R.drawable.jupiter
            SATURN -> R.drawable.saturn
            URANUS -> R.drawable.uranus
            NEPTUNE -> R.drawable.neptune
            PLUTO -> R.drawable.pluto
            else-> R.drawable.logo
        }
    }
}