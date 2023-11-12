package com.jeffrwatts.stargazer.data.celestialobject

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.jeffrwatts.stargazer.R

enum class ObjectType {
    STAR, GALAXY, NEBULA, CLUSTER, PLANET, UNKNOWN
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
    val constellation: String?,
    val magnitude: Double?,
    val observationNotes: String?,
    val observationStatus: ObservationStatus
)

fun CelestialObj.getImageResource(): Int {
    return when (this.catalogId) {
        "Mercury" -> R.drawable.mercury
        "Venus" -> R.drawable.venus
        "Mars" -> R.drawable.mars
        "Jupiter" -> R.drawable.jupiter
        "Saturn" -> R.drawable.saturn
        "Uranus" -> R.drawable.uranus
        "Neptune" -> R.drawable.neptune
        "Moon" -> R.drawable.moon
        "M1" -> R.drawable.m1
        "M6" -> R.drawable.m6
        "M7" -> R.drawable.m7
        "M8" -> R.drawable.m8
        "M11" -> R.drawable.m11
        "M13" -> R.drawable.m13
        "M16" -> R.drawable.m16
        "M17" -> R.drawable.m17
        "M22" -> R.drawable.m22
        "M27" -> R.drawable.m27
        "M31" -> R.drawable.m31
        "M42" -> R.drawable.m42
        "M45" -> R.drawable.m45
        "M51" -> R.drawable.m51
        "M57" -> R.drawable.m57
        "M81" -> R.drawable.m81
        "M82" -> R.drawable.m82
        "M104" -> R.drawable.m104
        "Caldwell 14" -> R.drawable.caldwell14
        "Caldwell 33" -> R.drawable.caldwell33
        "Caldwell 34" -> R.drawable.caldwell33 // intentionally the same.
        "Caldwell 49" -> R.drawable.caldwell49
        "Caldwell 64" -> R.drawable.caldwell64
        else -> when (this.type) {
            ObjectType.STAR -> R.drawable.star
            ObjectType.GALAXY -> R.drawable.galaxy
            ObjectType.NEBULA -> R.drawable.nebula
            ObjectType.CLUSTER -> R.drawable.cluster
            else -> R.drawable.star
        }
    }
}