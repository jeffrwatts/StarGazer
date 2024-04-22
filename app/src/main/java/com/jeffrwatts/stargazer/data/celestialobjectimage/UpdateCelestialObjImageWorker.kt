package com.jeffrwatts.stargazer.data.celestialobjectimage

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.jeffrwatts.stargazer.BuildConfig
import com.jeffrwatts.stargazer.data.StarGazerDatabase
import com.jeffrwatts.stargazer.network.ImageApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit

@HiltWorker
class UpdateCelestialObjImageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    //private val celestialObjImageDao: CelestialObjImageDao
) : CoroutineWorker(context, workerParams) {

    private val imageApi = provideImageApi()
    private val celestialObjImageDao = provideCelestiaLObjImageDao(context)

    // Determine why dependency injection wasn't working here.
    private fun provideCelestiaLObjImageDao(context: Context): CelestialObjImageDao {
        return StarGazerDatabase.getDatabase(context).celestialObjImageDao()
    }

    private fun provideImageApi(): ImageApi {
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

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            var succeeded = 0
            setProgressAsync(buildStatusUpdate("Getting Images..."))
            val imageUpdates = imageApi.getImages() // Assume this returns a list of image data with URLs and catalogIds
            setProgressAsync(buildStatusUpdate("Downloading ${imageUpdates.size} images"))
            celestialObjImageDao.deleteAllImages()
            imageUpdates.forEachIndexed() { index, image ->
                try {
                    val imageUrl = image.url
                    val imageStream = URL(imageUrl).openStream()
                    val outputFile = File(applicationContext.cacheDir, "${image.objectId}.webp")

                    outputFile.outputStream().use { fileOutputStream ->
                        imageStream.copyTo(fileOutputStream)
                    }
                    setProgressAsync(buildStatusUpdate("Downloaded ${index + 1} of ${imageUpdates.size}: ${image.objectId}"))
                    celestialObjImageDao.insertImage(CelestialObjImage(objectId = image.objectId, crop = image.crop, filename = outputFile.toString()))
                    succeeded+=1
                } catch (e: IOException) {
                    Log.e("ImageDownload", "Failed to download image for catalogId: ${image.objectId}", e)
                    setProgressAsync(buildStatusUpdate("${image.objectId} failed"))
                }
            }
            Result.success(buildStatusUpdate("Downloaded $succeeded of ${imageUpdates.size}"))
        } catch (e: Exception) {
            Log.e("WorkManager", "Failed during image download work", e)
            Result.failure(buildStatusUpdate("Failed to get images: ${e.message}"))
        }
    }

    private fun buildStatusUpdate(statusUpdate: String): Data {
        return Data.Builder().putString("Status", statusUpdate).build()
    }
}
