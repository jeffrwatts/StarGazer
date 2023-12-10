package com.jeffrwatts.stargazer.di

import android.content.Context
import com.jeffrwatts.stargazer.BuildConfig
import com.jeffrwatts.stargazer.data.StarGazerDatabase
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjDao
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobject.OfflineCelestialObjRepository
import com.jeffrwatts.stargazer.data.orientation.OrientationRepository
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.data.planetaryposition.DefaultPlanetPosRepository
import com.jeffrwatts.stargazer.data.planetaryposition.PlanetPosDao
import com.jeffrwatts.stargazer.data.planetaryposition.PlanetPosRepository
import com.jeffrwatts.stargazer.network.EphemerisApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    fun provideEphemerisApi(): EphemerisApi {
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

        return retrofit.create(EphemerisApi::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun providePlanetPosDao(@ApplicationContext context: Context): PlanetPosDao {
        return StarGazerDatabase.getDatabase(context).planetPosDao()
    }

    @Singleton
    @Provides
    fun provideCelestialObjDao(@ApplicationContext context: Context): CelestialObjDao {
        return StarGazerDatabase.getDatabase(context).celestialObjDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Singleton
    @Provides
    fun provideCelestialObjRepository(
        @ApplicationContext context: Context,
        dao: CelestialObjDao,
        planetPosRepository: PlanetPosRepository,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): CelestialObjRepository {
        return OfflineCelestialObjRepository(context, dao, planetPosRepository, ioDispatcher)
    }

    @Singleton
    @Provides
    fun providePlanetPosRepository(
        dao: PlanetPosDao,
        ephemerisApi: EphemerisApi,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): PlanetPosRepository {
        return DefaultPlanetPosRepository(dao, ephemerisApi, ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideLocationRepository(
        @ApplicationContext context: Context
    ): LocationRepository {
        return LocationRepository(context)
    }

    @Singleton
    @Provides
    fun provideOrientationRepository(
        @ApplicationContext context: Context
    ): OrientationRepository {
        return OrientationRepository(context)
    }
}


