package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.Telescope

@Dao
interface TelescopeDao {
    @Insert
    suspend fun insert(telescope: Telescope)

    @Query("SELECT * FROM telescopes")
    suspend fun getAllTelescopes(): List<Telescope>
}