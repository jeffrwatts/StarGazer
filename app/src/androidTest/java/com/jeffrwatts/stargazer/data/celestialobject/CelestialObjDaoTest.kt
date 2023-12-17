package com.jeffrwatts.stargazer.data.celestialobject

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.data.StarGazerDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CelestialObjDaoTest {

    private lateinit var database: StarGazerDatabase
    private lateinit var dao: CelestialObjDao

    @Before
    fun createDb() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            appContext,
            StarGazerDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.celestialObjDao()

        populateDao(appContext)
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun findByRaDecTest_NonWrap() = runBlocking {
        val raThreshold = 2.0
        val decThreshold = 2.0
        val types = listOf(ObjectType.STAR, ObjectType.GALAXY, ObjectType.NEBULA, ObjectType.CLUSTER)
        val result = dao.findByRaDec(types, 90.0, 45.0, raThreshold, decThreshold).first()

        // Verify results
        assertEquals(1, result.size)
        assertEquals("Menkalinan", result[0].friendlyName)
    }

    @Test
    fun findByRaDecTest_ZeroWrapPos() = runBlocking {
        val raThreshold = 2.0
        val decThreshold = 2.0
        val types = listOf(ObjectType.STAR, ObjectType.GALAXY, ObjectType.NEBULA, ObjectType.CLUSTER)
        val result = dao.findByRaDec(types, 359.0, 16.0, raThreshold, decThreshold).first()

        // Verify results
        assertEquals(1, result.size)
        assertEquals("NGC 7814", result[0].friendlyName)
    }
    @Test
    fun findByRaDecTest_ZeroWrapNeg() = runBlocking {
        val raThreshold = 7.0
        val decThreshold = 2.0
        val types = listOf(ObjectType.STAR, ObjectType.GALAXY, ObjectType.NEBULA, ObjectType.CLUSTER)
        val result = dao.findByRaDec(types, 1.0, 77.0, raThreshold, decThreshold).first()

        // Verify results
        assertEquals(1, result.size)
        assertEquals("Alrai", result[0].friendlyName)
    }

    private fun populateDao(context: Context) = runBlocking {
        val jsonString = context.resources.openRawResource(R.raw.items).bufferedReader().use { it.readText() }

        val gson = Gson()
        val itemType = object : TypeToken<List<CelestialObjJson>>() {}.type
        val jsonItems: List<CelestialObjJson> = gson.fromJson(jsonString, itemType)
        val celestialObjs = jsonItems.map { it.toCelestialObjEntity() }
        dao.insertAll(celestialObjs)
    }

}