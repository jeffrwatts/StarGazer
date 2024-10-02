package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.starobj


data class StarObjJson (
    val displayName: String,
    val ra: Double,
    val dec: Double,
    val magnitude: Double,
    val constellation: String,
    val spectralType: String?
)

fun StarObjJson.toStarEntity(): StarObj {
    return StarObj(
        0,
        this.displayName,
        this.ra,
        this.dec,
        this.magnitude,
        this.constellation,
        this.spectralType)
}

