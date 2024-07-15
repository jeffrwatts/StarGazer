package com.jeffrwatts.stargazer.data.variablestarobject

data class VariableStarObjJson (
    val id: Int,
    val displayName: String,
    val ra: Double,
    val dec: Double,
    val period: Double,
    val magnitudeHigh: Double,
    val magnitudeLow: Double,
    val constellation: String,
    val type: String,
    val spectralType: String,
    val OID: Long
)

fun VariableStarObjJson.toVariableStarObjEntity(): VariableStarObj {
    return VariableStarObj(
        id = this.id,
        displayName = this.displayName,
        ra = this.ra,
        dec = this.dec,
        period = this.period,
        magnitudeHigh = this.magnitudeHigh,
        magnitudeLow = this.magnitudeLow,
        constellation = this.constellation,
        type = this.type,
        spectralType = this.spectralType,
        OID = this.OID
    )
}