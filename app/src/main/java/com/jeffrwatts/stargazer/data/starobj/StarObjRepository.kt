package com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.starobj

import kotlinx.coroutines.flow.Flow

class StarObjRepository(private val starDao: StarObjDao) {

    suspend fun insertStars(stars: List<StarObj>) {
        starDao.insertStars(stars)
    }

    fun getStars(): Flow<List<StarObj>> {
        return starDao.getStars()
    }
}
