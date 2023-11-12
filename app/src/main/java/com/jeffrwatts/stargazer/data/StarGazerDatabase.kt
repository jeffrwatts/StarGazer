package com.jeffrwatts.stargazer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjDao
import com.jeffrwatts.stargazer.data.planetaryposition.PlanetPos
import com.jeffrwatts.stargazer.data.planetaryposition.PlanetPosDao

@Database(entities = [CelestialObj::class, PlanetPos::class], version = 1, exportSchema = false)
abstract class StarGazerDatabase : RoomDatabase() {
    abstract fun celestialObjDao(): CelestialObjDao
    abstract fun planetPosDao(): PlanetPosDao

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