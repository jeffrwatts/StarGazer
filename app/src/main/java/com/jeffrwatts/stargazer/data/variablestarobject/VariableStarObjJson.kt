package com.jeffrwatts.stargazer.data.variablestarobject

data class VariableStarObjJson (
    val id: Int,
    val displayName: String,
    val OID: Long,
    val ra: Double,
    val dec: Double,
    val constellation: String,
    val period: Double,
    val epoch: Double,
    val magnitudeHigh: String,
    val magnitudeLow: String,
    val riseDuration: Double,
    val variableType: String,
    val type: String,
    val spectralType: String,
)

fun VariableStarObjJson.toVariableStarObjEntity(): VariableStarObj {
    return VariableStarObj(
        id = this.id,
        displayName = this.displayName,
        OID = this.OID,
        ra = this.ra,
        dec = this.dec,
        constellation = this.constellation,
        period = this.period,
        epoch = this.epoch,
        magnitudeHigh = this.magnitudeHigh,
        magnitudeLow = this.magnitudeLow,
        riseDuration = this.riseDuration,
        variableType = VariableType.entries.find { it.name.equals(this.type, true) } ?: VariableType.UNKNOWN,
        type = this.type,
        spectralType = this.spectralType,
    )
}