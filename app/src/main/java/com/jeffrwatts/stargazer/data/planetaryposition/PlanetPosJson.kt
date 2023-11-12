package com.jeffrwatts.stargazer.data.planetaryposition

import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjJson
import com.jeffrwatts.stargazer.data.celestialobject.ObjectType
import com.jeffrwatts.stargazer.data.celestialobject.ObservationStatus

data class PlanetPosJson (
    val id: Int,
    val planetName: String,
    val dateLow: Double,
    val dateHigh: Double,
    val ra: Double,
    val dec: Double
)

fun PlanetPosJson.toPlanetPosEntity(): PlanetPos {
    return PlanetPos(
        id = this.id,
        planetName = this.planetName,
        dateLow = this.dateLow,
        dateHigh = this.dateHigh,
        ra = this.ra,
        dec = this.dec
     )
}