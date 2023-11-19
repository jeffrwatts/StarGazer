package com.jeffrwatts.stargazer.data.planetaryposition

import kotlinx.coroutines.flow.Flow

interface PlanetPosRepository {
    fun getPlanetPosition(planetName: String, time: Double): Flow<PlanetPos>
}