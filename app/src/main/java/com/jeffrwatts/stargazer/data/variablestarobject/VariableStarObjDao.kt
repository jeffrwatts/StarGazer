package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.variablestarobject

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VariableStarObjDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(variableStarObjs: List<VariableStarObj>)

    @Query("SELECT * FROM variable_star_objects")
    fun getAll(): Flow<List<VariableStarObj>>

    @Query("SELECT COUNT(*) FROM variable_star_objects")
    fun getCount(): Int
}
