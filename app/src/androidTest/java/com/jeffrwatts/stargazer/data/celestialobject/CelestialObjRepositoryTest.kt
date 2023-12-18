package com.jeffrwatts.stargazer.data.celestialobject

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jeffrwatts.stargazer.R
import com.jeffrwatts.stargazer.data.StarGazerDatabase
import com.jeffrwatts.stargazer.data.planetaryposition.PlanetPos
import com.jeffrwatts.stargazer.data.planetaryposition.PlanetPosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyDouble
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

class CelestialObjRepositoryTest {

    private lateinit var database: StarGazerDatabase
    private lateinit var celestialObjDao: CelestialObjDao
    private val planetPosRepository: PlanetPosRepository = mock(PlanetPosRepository::class.java)
    private lateinit var repository: CelestialObjRepository

    @Before
    fun setup() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            appContext,
            StarGazerDatabase::class.java
        ).allowMainThreadQueries().build()
        celestialObjDao = database.celestialObjDao()
        populateDao(appContext)

        `when`(planetPosRepository.getPlanetPosition(anyString(), anyDouble()))
            .thenAnswer(object : Answer<Flow<PlanetPos>> {
                override fun answer(invocation: InvocationOnMock): Flow<PlanetPos> {
                    val planetName = invocation.getArgument<String>(0)
                    val time = invocation.getArgument<Double>(1)
                    val planetPos = createPlanetPosForName(planetName, time)
                    return flowOf(planetPos)
                }
            })


        repository = CelestialObjRepository(
            celestialObjDao,
            planetPosRepository,
            appContext,
            Dispatchers.IO // Replace with a test dispatcher if needed
        )
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun findByRaDecTest_NonWrap() = runBlocking {
        val raThreshold = 2.0
        val decThreshold = 2.0

        val result = repository.getCelestialObjsByRaDec(90.0, 45.0, 0.0, raThreshold, decThreshold).first()

        // Expected names
        val expectedNames = setOf("Menkalinan", "Venus")

        // Verify results
        assertEquals(2, result.size)
        assertTrue(result.all { it.friendlyName in expectedNames })

        // Additional check to ensure each name appears only once
        val resultNames = result.map { it.friendlyName }.toSet()
        assertEquals(expectedNames, resultNames)
    }

    @Test
    fun findByRaDecTest_ZeroWrapPos() = runBlocking {
        val raThreshold = 2.0
        val decThreshold = 2.0

        val result = repository.getCelestialObjsByRaDec(359.0, 16.0, 0.0, raThreshold, decThreshold).first()

        val expectedNames = setOf("NGC 7814", "Jupiter")

        // Verify results
        assertEquals(2, result.size)
        assertTrue(result.all { it.friendlyName in expectedNames })

        // Additional check to ensure each name appears only once
        val resultNames = result.map { it.friendlyName }.toSet()
        assertEquals(expectedNames, resultNames)
    }
    @Test
    fun findByRaDecTest_ZeroWrapNeg() = runBlocking {
        val raThreshold = 7.0
        val decThreshold = 2.0

        val result = repository.getCelestialObjsByRaDec(1.0, 77.0, 0.0, raThreshold, decThreshold).first()

        val expectedNames = setOf("Alrai", "Saturn")

        // Verify results
        assertEquals(2, result.size)
        assertTrue(result.all { it.friendlyName in expectedNames })

        // Additional check to ensure each name appears only once
        val resultNames = result.map { it.friendlyName }.toSet()
        assertEquals(expectedNames, resultNames)
    }

    private fun populateDao(context: Context) = runBlocking {
        val jsonString = context.resources.openRawResource(R.raw.items).bufferedReader().use { it.readText() }

        val gson = Gson()
        val itemType = object : TypeToken<List<CelestialObjJson>>() {}.type
        val jsonItems: List<CelestialObjJson> = gson.fromJson(jsonString, itemType)
        val celestialObjs = jsonItems.map { it.toCelestialObjEntity() }
        celestialObjDao.insertAll(celestialObjs)
    }

    private fun createPlanetPosForName(planetName: String, time: Double): PlanetPos {
        return when(planetName) {
            // Venus will be example of non wrap.
            "Venus" -> PlanetPos(500, planetName, 0.0, 89.0, 44.0)

            // Jupiter will be example of wrap pos
            "Jupiter" -> PlanetPos(501, planetName, 0.0, 0.3, 15.5)

            // Saturn will be example of wrap pos
            "Saturn" -> PlanetPos(502, planetName, 0.0, 358.3, 77.5)

            // Set other planets to be outside of search params by making dec negative.
            else -> PlanetPos(503, planetName, 0.0, 180.0, -25.0)
        }
    }
}