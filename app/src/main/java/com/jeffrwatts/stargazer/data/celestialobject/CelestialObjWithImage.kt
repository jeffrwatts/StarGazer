package com.jeffrwatts.stargazer.data.celestialobject

import androidx.room.Embedded
import androidx.room.Relation
import com.jeffrwatts.stargazer.data.celestialobjectimage.CelestialObjImage

data class CelestialObjWithImage(
    @Embedded val celestialObj: CelestialObj,
    @Relation(
        parentColumn = "objectId",
        entityColumn = "objectId"
    )
    val image: CelestialObjImage?
)