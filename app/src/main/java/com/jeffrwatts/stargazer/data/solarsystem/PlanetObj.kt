package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.solarsystem

import com.jeffrwatts.stargazer.R

data class PlanetObj(
    val id: Int,
    val planetName: String,
    val ra: Double,
    val dec: Double,
)

fun PlanetObj.getImageResource(): Int {
    return when (this.planetName) {
        "Mercury" -> R.drawable.mercury
        "Venus" -> R.drawable.venus
        "Mars" -> R.drawable.mars
        "Jupiter" -> R.drawable.jupiter
        "Saturn" -> R.drawable.saturn
        "Uranus" -> R.drawable.uranus
        "Neptune" -> R.drawable.neptune
        "Moon" -> R.drawable.moon
        else -> R.drawable.logo
    }
}