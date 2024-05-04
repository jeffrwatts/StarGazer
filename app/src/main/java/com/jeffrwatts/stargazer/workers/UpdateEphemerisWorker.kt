package com.jeffrwatts.stargazer.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.jeffrwatts.stargazer.BuildConfig
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.network.StarGazerApi
import com.jeffrwatts.stargazer.data.StarGazerDatabase
import com.jeffrwatts.stargazer.data.solarsystem.EphemerisDao
import com.jeffrwatts.stargazer.data.solarsystem.toEphemerisEntry
import com.jeffrwatts.stargazer.utils.Utils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class UpdateEphemerisWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams)
{
    private companion object {
        const val EPHEMERIS_STEP = 2      // Get in increments of 2 days.
        const val EPHEMERIS_RANGE = 14    // Get 14 days at a time.
    }

    private val starGazerApi = provideStarGazerApi()
    private val ephemerisDao = provideEphemerisDao(context)

    private fun provideEphemerisDao(@ApplicationContext context: Context): EphemerisDao {
        return StarGazerDatabase.getDatabase(context).ephemerisDao()
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
        try {
            val startDate = LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT);
            val startJulianDate = Utils.calculateJulianDateFromLocal(startDate)

            ephemerisDao.deleteAll()
            for (i in 0..EPHEMERIS_RANGE step EPHEMERIS_STEP) {
                val ephemeris = starGazerApi.getEphemeris(startJulianDate+i, EPHEMERIS_STEP.toDouble()).map {
                    it.toEphemerisEntry()
                }
                ephemerisDao.insertAll(ephemeris)
                val percentage = ((i + EPHEMERIS_STEP) * 100.0 / EPHEMERIS_RANGE).roundToInt()
                setProgressAsync(buildStatusUpdate("Updating ephemeris: ${percentage}%"))
            }
        } catch (e: Exception) {
            Log.e("WorkManager", "Failed to update ephemeris", e)
            setProgressAsync(buildStatusUpdate("Failed to update ephemeris ${e.message}"))
        }

        Result.success(buildStatusUpdate("Updates Complete"))
    }

    private fun buildStatusUpdate(statusUpdate: String): Data {
        return Data.Builder()
            .putString("Status", statusUpdate)
            .putString("UpdateType", UpdateType.EPHEMERIS.name)
            .build()
    }
}