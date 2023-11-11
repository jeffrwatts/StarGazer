package com.jeffrwatts.stargazer.data.celestialobject

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.jeffrwatts.stargazer.R

enum class ObjectType {
    STAR, GALAXY, NEBULA, CLUSTER, UNKNOWN
}

enum class ObservationStatus(val priority: Int) {
    GREAT(1),
    SUGGESTED(2),
    NOT_OBSERVED(3),
    POOR(4);
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
    val catalogId: String?,
    val ngcId: String?,
    val ra: Double,
    val dec: Double,
    val type: ObjectType,
    val subType: String?,
    val constellation: String,
    val magnitude: Double?,
    val observationNotes: String?,
    val observationStatus: ObservationStatus
)

fun CelestialObj.getImageResource(): Int {
    return when (this.type) {
        ObjectType.STAR -> R.drawable.star
        ObjectType.GALAXY -> R.drawable.galaxy
        ObjectType.NEBULA -> R.drawable.nebula
        ObjectType.CLUSTER -> R.drawable.cluster
        else -> R.drawable.star
    }
}