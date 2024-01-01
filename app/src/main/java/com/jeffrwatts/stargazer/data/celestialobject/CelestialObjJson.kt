package com.jeffrwatts.stargazer.data.celestialobject

data class CelestialObjJson(
    val id: Int,
    val friendlyName: String,
    val catalogId: String?,
    val ngcId: String?,
    val ra: Double,
    val dec: Double,
    val type: String,
    val subType: String,
    val constellation: String,
    val magnitude: Double?,
    val observationNotes: String?,
    val recommended: Boolean,
    val observationStatus: String
)

fun CelestialObjJson.toCelestialObjEntity(): CelestialObj {
    return CelestialObj(
        id = this.id,
        friendlyName = this.friendlyName,
        catalogId = this.catalogId,
        ngcId = this.ngcId,
        ra = this.ra,
        dec = this.dec,
        type = ObjectType.values().find { it.name.equals(this.type, true) } ?: ObjectType.UNKNOWN,
        subType = this.subType,
        constellation = this.constellation,
        magnitude = this.magnitude,
        observationNotes = this.observationNotes,
        recommended = this.recommended,
        observationStatus = ObservationStatus.values().find { it.name.equals(this.observationStatus, true) } ?: ObservationStatus.NOT_OBSERVED
    )
}