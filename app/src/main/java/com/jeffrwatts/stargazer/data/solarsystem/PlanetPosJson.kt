package com.jeffrwatts.stargazer.data.solarsystem

data class PlanetPosJson (
    val name: String,
    val time: Double,
    val ra: Double,
    val dec: Double
)

fun PlanetPosJson.toPlanetPosEntity(): PlanetPos {
    return PlanetPos(
        id=0,
        planetName = this.name,
        time = this.time,
        ra = this.ra,
        dec = this.dec
     )
}