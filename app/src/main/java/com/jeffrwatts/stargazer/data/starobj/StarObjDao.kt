package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.starobj

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StarObjDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStars(stars: List<StarObj>)

    @Query("SELECT * FROM stars ORDER BY magnitude ASC")
    fun getStars(): Flow<List<StarObj>>

    @Query("DELETE FROM stars")
    fun deleteAll()
}
