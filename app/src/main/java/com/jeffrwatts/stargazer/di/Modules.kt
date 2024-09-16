package com.jeffrwatts.stargazer.di

import android.content.Context
import androidx.work.WorkManager
import com.jeffrwatts.stargazer.BuildConfig
import com.jeffrwatts.stargazer.StarGazerApplication
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.CameraDao
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.EquipmentRepository
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.OpticalElementDao
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.data.equipment.TelescopeDao
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.network.SkyViewApi
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.network.StarGazerApi
import com.jeffrwatts.stargazer.data.StarGazerDatabase
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjDao
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjRepository
import com.jeffrwatts.stargazer.data.celestialobjectimage.CelestialObjImageDao
import com.jeffrwatts.stargazer.data.orientation.OrientationRepository
import com.jeffrwatts.stargazer.data.location.LocationRepository
import com.jeffrwatts.stargazer.data.timeoffset.TimeOffsetRepository
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjDao
import com.jeffrwatts.stargazer.data.variablestarobject.VariableStarObjRepository
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
    fun provideStarGazerApi(): StarGazerApi {
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

    @Provides
    @Singleton
    fun provideSkyViewApi(): SkyViewApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        val okHttpClient = OkHttpClient.Builder()
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://skyview.gsfc.nasa.gov/current/cgi/")
            .client(okHttpClient)
            .build()

        return retrofit.create(SkyViewApi::class.java)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
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

    @Singleton
    @Provides
    fun provideTelescopeDao(@ApplicationContext context: Context): TelescopeDao {
        return StarGazerDatabase.getDatabase(context).telescopeDao()
    }

    @Singleton
    @Provides
    fun provideCameraDao(@ApplicationContext context: Context): CameraDao {
        return StarGazerDatabase.getDatabase(context).cameraDao()
    }

    @Singleton
    @Provides
    fun provideOpticalElementDao(@ApplicationContext context: Context): OpticalElementDao {
        return StarGazerDatabase.getDatabase(context).opticalElementDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Singleton
    @Provides
    fun provideCelestialObjRepository(
        celestialObjDao: CelestialObjDao
    ): CelestialObjRepository {
        return CelestialObjRepository(celestialObjDao)
    }

    @Singleton
    @Provides
    fun provideVariableStarObjRepository(
        variableStarObjDao: VariableStarObjDao
    ): VariableStarObjRepository {
        return VariableStarObjRepository(variableStarObjDao)
    }

    @Singleton
    @Provides
    fun provideEquipmentRepository(
        telescopeDao: TelescopeDao,
        cameraDao: CameraDao,
        opticalElementDao: OpticalElementDao
    ): EquipmentRepository {
        return EquipmentRepository(telescopeDao, cameraDao, opticalElementDao)
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

    @Singleton
    @Provides
    fun provideTimeOffsetRepository(): TimeOffsetRepository {
        return TimeOffsetRepository()
    }

    @Singleton
    @Provides
    fun provideApplicationScope(
        @ApplicationContext context: Context
    ): CoroutineScope {
        val app = context.applicationContext as StarGazerApplication
        return app.applicationScope
    }
}


