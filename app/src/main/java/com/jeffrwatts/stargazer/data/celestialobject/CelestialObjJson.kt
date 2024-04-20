package com.jeffrwatts.stargazer.data.celestialobject

data class CelestialObjJson(
    val id: Int,
    val displayName: String,
    val objectId: String,
    val ra: Double,
    val dec: Double,
    val type: String,
    val subType: String?,
    val magnitude: Double?,
    val constellation: String?,
    val ngcId: String?,
    val recommended: Boolean,
    val tags: String
)

fun CelestialObjJson.toCelestialObjEntity(): CelestialObj {
    return CelestialObj(
        id = this.id,
        displayName = this.displayName,
        objectId = this.objectId,
        ra = this.ra,
        dec = this.dec,
        type = ObjectType.values().find { it.name.equals(this.type, true) } ?: ObjectType.UNKNOWN,
        subType = this.subType,
        magnitude = this.magnitude,
        constellation = this.constellation,
        ngcId = this.ngcId,
        recommended = this.recommended,
        tags = this.tags
    )
}