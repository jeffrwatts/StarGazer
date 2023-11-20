package com.jeffrwatts.stargazer.data

import android.content.Context
import com.jeffrwatts.stargazer.BuildConfig
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.OfflineCelestialObjRepository
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.data.planetaryposition.DefaultPlanetPosRepository
import com.jeffrwatts.stargazer.data.planetaryposition.PlanetPosRepository
import com.jeffrwatts.stargazer.network.EphemerisApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

interface AppContainer {
    val celestialObjRepository: CelestialObjRepository
    val locationRepository: LocationRepository
    val planetPosRepository: PlanetPosRepository
    val ephemerisApi: EphemerisApi
}

class AppContainerImpl (private val context: Context) : AppContainer {
    override val celestialObjRepository: CelestialObjRepository by lazy {
        OfflineCelestialObjRepository(
            context = context,
            dao = StarGazerDatabase.getDatabase(context).celestialObjDao(),
            planetPosRepository)
    }

    override val locationRepository: LocationRepository by lazy {
        LocationRepository(context = context)
    }

    override val planetPosRepository: PlanetPosRepository by lazy {
        DefaultPlanetPosRepository(ephemerisApi = ephemerisApi,
            dao = StarGazerDatabase.getDatabase(context).planetPosDao())
    }

    override val ephemerisApi: EphemerisApi by lazy {
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

        retrofit.create(EphemerisApi::class.java)
    }
}

