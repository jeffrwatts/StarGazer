package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.celestialobjectimage

data class CelestialObjImage (
    val objectId: String,
    val crop: Int,
    val cropX: Int,
    val cropY: Int,
    val resize: Int,
    val url: String
)