package com.jeffrwatts.stargazer.data.variablestarobject

data class VariableStarObjJson (
    val id: Int,
    val displayName: String,
    val ra: Double,
    val dec: Double,
    val period: Double,
    val epoch: Double,
    val magnitudeHigh: Double,
    val magnitudeLow: Double,
    val constellation: String,
    val variableType: String,
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
        epoch = this.epoch,
        magnitudeHigh = this.magnitudeHigh,
        magnitudeLow = this.magnitudeLow,
        constellation = this.constellation,
        variableType = VariableType.entries.find { it.name.equals(this.type, true) } ?: VariableType.UNKNOWN,
        type = this.type,
        spectralType = this.spectralType,
        OID = this.OID
    )
}