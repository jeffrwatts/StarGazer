package com.jeffrwatts.stargazer.data.solarsystem

data class EphemerisEntryJson (
    val name: String,
    val time: Double,
    val ra: Double,
    val dec: Double
)

fun EphemerisEntryJson.toEphemerisEntry(): EphemerisEntry {
    return EphemerisEntry(
        id=0,
        planetName = this.name,
        time = this.time,
        ra = this.ra,
        dec = this.dec
     )
}