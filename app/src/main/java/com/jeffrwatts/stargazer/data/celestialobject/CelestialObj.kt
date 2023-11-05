package com.jeffrwatts.stargazer.data.celestialobject

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.jeffrwatts.stargazer.R

enum class ObjectType {
    STAR, MESSIER, CALDWELL, UNKNOWN
}

enum class ObservationStatus {
    NOT_OBSERVED, POOR, GOOD, GREAT
}

class Converters {

    @TypeConverter
    fun toObjectType(value: String) = enumValueOf<ObjectType>(value)

    @TypeConverter
    fun fromObjectType(value: ObjectType) = value.name

    @TypeConverter
    fun toObservationStatus(value: String) = enumValueOf<ObservationStatus>(value)

    @TypeConverter
    fun fromObservationStatus(value: ObservationStatus) = value.name
}

@Entity(tableName = "celestial_objects")
@TypeConverters(Converters::class)
data class CelestialObj(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val friendlyName: String,
    val ngcId: String?,
    val catalogId: String?,
    val ra: Double,
    val dec: Double,
    val mag: Double?,
    val desc: String?,
    val constellation: String,
    val type: ObjectType,
    val defaultImage: String,
    val observationStatus: ObservationStatus
)

fun CelestialObj.getImageResource(): Int {
    return when (this.defaultImage) {
        "star" -> R.drawable.star
        "galaxy" -> R.drawable.galaxy
        "nebula" -> R.drawable.nebula
        "cluster" -> R.drawable.cluster
        else -> R.drawable.star
    }
}