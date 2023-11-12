package com.jeffrwatts.stargazer.data.planetaryposition

import kotlinx.coroutines.flow.Flow

interface PlanetPosRepository {
    fun getPlanetPositionForDate(planetName: String, date: Double): Flow<PlanetPos>
}