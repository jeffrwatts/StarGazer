package com.jeffrwatts.stargazer.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.jeffrwatts.stargazer.BuildConfig
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.network.StarGazerApi
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.JUPITER
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.MARS
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.MERCURY
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.NEPTUNE
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.PLUTO
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.SATURN
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.URANUS
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.utils.VENUS
import com.jeffrwatts.stargazer.data.StarGazerDatabase
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObj
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjDao
import com.jeffrwatts.stargazer.data.celestialobject.ObjectType
import com.jeffrwatts.stargazer.data.celestialobject.toCelestialObjEntity
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjDao
import com.jeffrwatts.stargazer.data.variablestarobject.toVariableStarObjEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


class UpdateDSOVariableWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams)
{
    private val starGazerApi = provideStarGazerApi()
    private val celestialObjDao = provideCelestialObjDao(context)
    private val variableStarObjDao = provideVariableStarObjDao(context)

    private fun provideCelestialObjDao(@ApplicationContext context: Context): CelestialObjDao {
        return StarGazerDatabase.getDatabase(context).celestialObjDao()
    }

    private fun provideVariableStarObjDao(@ApplicationContext context: Context): VariableStarObjDao {
        return StarGazerDatabase.getDatabase(context).variableStarObjDao()
    }

    private fun provideStarGazerApi(): StarGazerApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

        return retrofit.create(StarGazerApi::class.java)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        updateDSOObjects()
        updateVariableStarObjects()
        Result.success(buildStatusUpdate("Updates Complete"))
    }

    private val solarSystem : List<CelestialObj> = listOf (
        CelestialObj(0, MERCURY, MERCURY, 0.0, 0.0, ObjectType.PLANET, "", 0.0, "", null, true, ""),
        CelestialObj(0, VENUS, VENUS, 0.0, 0.0, ObjectType.PLANET, "", 0.0, "", null, true, ""),
        CelestialObj(0, MARS, MARS, 0.0, 0.0, ObjectType.PLANET, "", 0.0, "", null, true, ""),
        CelestialObj(0, JUPITER, JUPITER, 0.0, 0.0, ObjectType.PLANET, "", 0.0, "", null, true, ""),
        CelestialObj(0, SATURN, SATURN, 0.0, 0.0, ObjectType.PLANET, "", 0.0, "", null, true, ""),
        CelestialObj(0, URANUS, URANUS, 0.0, 0.0, ObjectType.PLANET, "", 0.0, "", null, true, ""),
        CelestialObj(0, NEPTUNE, NEPTUNE, 0.0, 0.0, ObjectType.PLANET, "", 0.0, "", null, true, ""),
        CelestialObj(0, PLUTO, PLUTO, 0.0, 0.0, ObjectType.PLANET, "", 0.0, "", null, true, ""),
    )

    private suspend fun updateDSOObjects() {
        try {
            setProgressAsync(buildStatusUpdate("Getting DSO Objects"))
            val dsoObjects = starGazerApi.getDso()
            setProgressAsync(buildStatusUpdate("Updating DSO DB"))
            val celestialObjs = dsoObjects.map { it.toCelestialObjEntity() }
            celestialObjDao.deleteAll()
            celestialObjDao.insertAll(solarSystem)
            celestialObjDao.insertAll(celestialObjs)
            setProgressAsync(buildStatusUpdate("Updated DSO DB"))
        } catch (e: Exception) {
            Log.e("WorkManager", "Failed to get DSO Objects", e)
            setProgressAsync(buildStatusUpdate("Failed to get DSO Objects ${e.message}"))
        }
    }

    private suspend fun updateVariableStarObjects() {
        try {
            setProgressAsync(buildStatusUpdate("Getting Variable Star Objects"))
            val variableStarObjects = starGazerApi.getVariableStars()
            setProgressAsync(buildStatusUpdate("Updating Variable Star DB"))
            val variableStarObjs = variableStarObjects.map { it.toVariableStarObjEntity() }
            variableStarObjDao.deleteAll()
            variableStarObjDao.insertAll(variableStarObjs)
            setProgressAsync(buildStatusUpdate("Updated Variable Star DB"))
        } catch (e: Exception) {
            Log.e("WorkManager", "Failed to get Variable Star Objects", e)
            setProgressAsync(buildStatusUpdate("Failed to get Variable Star Objects ${e.message}"))
        }
    }

    private fun buildStatusUpdate(statusUpdate: String): Data {
        return Data.Builder()
            .putString("Status", statusUpdate)
            .putString("UpdateType", UpdateType.DSO_VARIABLE.name)
            .build()
    }
}