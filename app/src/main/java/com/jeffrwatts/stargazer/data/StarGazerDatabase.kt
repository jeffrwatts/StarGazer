package com.jeffrwatts.stargazer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.Camera
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.CameraDao
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.OpticalElement
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.OpticalElementDao
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.Telescope
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.TelescopeDao
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.starobj.StarObj
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.starobj.StarObjDao
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjDao
import com.jeffrwatts.stargazer.data.celestialobjectimage.CelestialObjImage
import com.jeffrwatts.stargazer.data.celestialobjectimage.CelestialObjImageDao
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObj
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjDao

@Database(entities = [
    CelestialObj::class,
    VariableStarObj::class,
    StarObj::class,
    CelestialObjImage::class,
    Telescope::class,
    Camera::class,
    OpticalElement::class], version = 1, exportSchema = false)
abstract class StarGazerDatabase : RoomDatabase() {
    abstract fun celestialObjDao(): CelestialObjDao
    abstract fun variableStarObjDao(): VariableStarObjDao
    abstract fun starObjDao(): StarObjDao
    abstract fun celestialObjImageDao(): CelestialObjImageDao
    abstract fun telescopeDao(): TelescopeDao
    abstract fun cameraDao(): CameraDao
    abstract fun opticalElementDao(): OpticalElementDao

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