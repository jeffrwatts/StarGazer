package com.jeffrwatts.stargazer.data.celestialobject

data class CelestialObjJson(
    val id: Int,
    val friendlyName: String,
    val ngcId: String?,
    val catalogId: String?,
    val ra: Double,
    val dec: Double,
    val mag: Double?,
    val desc: String?,
    val constellation: String,
    val type: String,
    val defaultImage: String,
    val observationStatus: String
)

fun CelestialObjJson.toCelestialObjEntity(): CelestialObj {
    return CelestialObj(
        id = this.id,
        friendlyName = this.friendlyName,
        ngcId = this.ngcId,
        catalogId = this.catalogId,
        ra = this.ra,
        dec = this.dec,
        mag = this.mag,
        desc = this.desc,
        constellation = this.constellation,
        type = ObjectType.values().find { it.name.equals(this.type, true) } ?: ObjectType.UNKNOWN,
        defaultImage = this.defaultImage,
        observationStatus = ObservationStatus.values().find { it.name.equals(this.observationStatus, true) } ?: ObservationStatus.NOT_OBSERVED
    )
}