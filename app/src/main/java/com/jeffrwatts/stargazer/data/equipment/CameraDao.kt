package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.Camera

@Dao
interface CameraDao {
    @Insert
    suspend fun insert(camera: Camera)

    @Query("SELECT * FROM cameras")
    suspend fun getAllCameras(): List<Camera>
}