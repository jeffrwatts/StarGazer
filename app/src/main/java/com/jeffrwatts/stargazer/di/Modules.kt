package com.jeffrwatts.stargazer.di

import android.content.Context
import androidx.work.WorkManager
import com.jeffrwatts.stargazer.BuildConfig
import com.jeffrwatts.stargazer.StarGazerApplication
import com.jeffrwatts.stargazer.data.StarGazerDatabase
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjDao
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobjectimage.CelestialObjImageDao
import com.jeffrwatts.stargazer.data.orientation.OrientationRepository
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.data.solarsystem.EphemerisDao
import com.jeffrwatts.stargazer.data.solarsystem.SolarSystemRepository
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjDao
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjRepository
import com.jeffrwatts.stargazer.network.EphemerisApi
import com.jeffrwatts.stargazer.network.ImageApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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
object AppModule {

    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
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

    @Provides
    @Singleton
    fun provideImageApi(): ImageApi {
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

        return retrofit.create(ImageApi::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Singleton
    @Provides
    fun provideEphermerisDao(@ApplicationContext context: Context): EphemerisDao {
        return StarGazerDatabase.getDatabase(context).ephemerisDao()
    }

    @Singleton
    @Provides
    fun provideCelestialObjDao(@ApplicationContext context: Context): CelestialObjDao {
        return StarGazerDatabase.getDatabase(context).celestialObjDao()
    }

    @Singleton
    @Provides
    fun provideVariableStarObjDao(@ApplicationContext context: Context): VariableStarObjDao {
        return StarGazerDatabase.getDatabase(context).variableStarObjDao()
    }

    @Singleton
    @Provides
    fun provideCelestiaLObjImageDao(@ApplicationContext context: Context): CelestialObjImageDao {
        return StarGazerDatabase.getDatabase(context).celestialObjImageDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Singleton
    @Provides
    fun provideCelestialObjRepository(
        @ApplicationContext context: Context,
        celestialObjDao: CelestialObjDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): CelestialObjRepository {
        return CelestialObjRepository(celestialObjDao, context, ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideSolarSystemRepository(
        dao: EphemerisDao,
        ephemerisApi: EphemerisApi,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): SolarSystemRepository {
        return SolarSystemRepository(dao, ephemerisApi, ioDispatcher)
    }

    @Singleton
    @Provides
    fun provideVariableStarObjRepository(
        @ApplicationContext context: Context,
        variableStarObjDao: VariableStarObjDao,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): VariableStarObjRepository {
        return VariableStarObjRepository(variableStarObjDao, context, ioDispatcher)
    }

    // Provide CoroutineScope separately
    @Singleton
    @Provides
    fun provideApplicationScope(
        @ApplicationContext context: Context
    ): CoroutineScope {
        val app = context.applicationContext as StarGazerApplication
        return app.applicationScope
    }

    @Singleton
    @Provides
    fun provideLocationRepository(
        @ApplicationContext context: Context,
        applicationScope: CoroutineScope
    ): LocationRepository {
        return LocationRepository(context, applicationScope)
    }

    @Singleton
    @Provides
    fun provideOrientationRepository(
        @ApplicationContext context: Context
    ): OrientationRepository {
        return OrientationRepository(context)
    }
}


