package com.jeffrwatts.stargazer.data.variablestarobject

import com.jeffrwatts.stargazer.utils.Utils

data class VariableStarObjPos (
    val variableStarObj: VariableStarObj,
    val alt: Double,
    val azm: Double,
    val timeUntilMeridian: Double,
    val observable: Boolean
) {
    companion object {
        private const val MIN_ALTITUDE = 15

        fun fromVariableStarObj(obj: VariableStarObj, julianDate: Double, lat: Double, lon: Double): VariableStarObjPos {
            val (alt, azm, timeUntilMeridian) = Utils.calculateAltAzm(obj.ra, obj.dec, lat, lon, julianDate)

            return VariableStarObjPos(obj, alt, azm, timeUntilMeridian, observable = (alt> MIN_ALTITUDE))
        }
    }
}