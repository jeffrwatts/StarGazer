package com.jeffrwatts.stargazer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObj
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjDao
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjDao
import com.jeffrwatts.stargazer.data.planetaryposition.PlanetPos
import com.jeffrwatts.stargazer.data.planetaryposition.PlanetPosDao

@Database(entities = [CelestialObj::class, PlanetPos::class, VariableStarObj::class], version = 1, exportSchema = false)
abstract class StarGazerDatabase : RoomDatabase() {
    abstract fun celestialObjDao(): CelestialObjDao
    abstract fun planetPosDao(): PlanetPosDao
    abstract fun variableStarObjDao(): VariableStarObjDao

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