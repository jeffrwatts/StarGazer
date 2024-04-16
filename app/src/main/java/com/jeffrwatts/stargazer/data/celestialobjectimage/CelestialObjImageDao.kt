package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.celestialobjectimage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CelestialObjImageDao {
    // Insert a new image record
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: CelestialObjImage): Long

    // Retrieve all images based on the object ID
    @Query("SELECT * FROM celestial_object_images WHERE objectId = :objectId")
    suspend fun getImagesByObjectId(objectId: String): List<CelestialObjImage>

    // Delete all records from the table
    @Query("DELETE FROM celestial_object_images")
    suspend fun deleteAllImages()
}