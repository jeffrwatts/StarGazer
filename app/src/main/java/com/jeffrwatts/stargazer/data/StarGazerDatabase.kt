package com.jeffrwatts.stargazer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjDao

@Database(entities = [CelestialObj::class], version = 1, exportSchema = false)
abstract class StarGazerDatabase : RoomDatabase() {
    abstract fun celestialObjDao(): CelestialObjDao

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