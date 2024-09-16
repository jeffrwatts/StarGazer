package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface OpticalElementDao {
    @Insert
    suspend fun insert(opticalElement: OpticalElement)

    @Query("SELECT * FROM optical_elements")
    suspend fun getAllOpticalElements(): List<OpticalElement>
}