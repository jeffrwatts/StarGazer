package com.jeffrwatts.stargazer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjDao
import com.jeffrwatts.stargazer.data.celestialobjectimage.CelestialObjImage
import com.jeffrwatts.stargazer.data.celestialobjectimage.CelestialObjImageDao
import com.jeffrwatts.stargazer.data.solarsystem.EphemerisDao
import com.jeffrwatts.stargazer.data.solarsystem.EphemerisEntry
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObj
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjDao

@Database(entities = [
    CelestialObj::class,
    EphemerisEntry::class,
    VariableStarObj::class,
    CelestialObjImage::class], version = 1, exportSchema = false)
abstract class StarGazerDatabase : RoomDatabase() {
    abstract fun celestialObjDao(): CelestialObjDao
    abstract fun ephemerisDao(): EphemerisDao
    abstract fun variableStarObjDao(): VariableStarObjDao
    abstract fun celestialObjImageDao(): CelestialObjImageDao

    companion object {
        @Volatile
        private var Instance: StarGazerDatabase? = null

        fun getDatabase(context: Context): StarGazerDatabase {
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context = context, StarGazerDatabase::class.java, "database")
                    .build()
                    .also {
                        Instance = it
                    }
            }
        }
    }
}