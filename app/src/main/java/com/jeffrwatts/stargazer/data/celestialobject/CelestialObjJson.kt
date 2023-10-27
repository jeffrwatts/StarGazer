package com.jeffrwatts.stargazer.data.celestialobject

data class CelestialObjJson(
    val id: Int,
    val primaryName: String,
    val ngcId: String?,
    val ra: Double,
    val dec: Double,
    val mag: Double?,
    val type: String,  // To handle transformations if needed
    val constellation: String,
    val observationStatus: String  // To handle transformations if needed
)

fun CelestialObjJson.toCelestialObjEntity(): CelestialObj {
    return CelestialObj(
        id = this.id,
        primaryName = this.primaryName,
        ngcId = this.ngcId,
        ra = this.ra,
        dec = this.dec,
        mag = this.mag,
        type = ObjectType.values().find { it.name.equals(this.type, true) } ?: ObjectType.UNKNOWN,
        constellation = this.constellation,
        observationStatus = ObservationStatus.values().find { it.name.equals(this.observationStatus, true) } ?: ObservationStatus.NOT_OBSERVED
    )
}