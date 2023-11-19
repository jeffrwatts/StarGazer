package com.jeffrwatts.stargazer.data.planetaryposition

data class PlanetPosJson (
    val id: Int,
    val planetName: String,
    val time: Double,
    val ra: Double,
    val dec: Double
)

fun PlanetPosJson.toPlanetPosEntity(): PlanetPos {
    return PlanetPos(
        id = this.id,
        planetName = this.planetName,
        time = this.time,
        ra = this.ra,
        dec = this.dec
     )
}