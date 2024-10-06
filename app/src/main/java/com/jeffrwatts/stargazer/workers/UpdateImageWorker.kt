package com.jeffrwatts.stargazer.workers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.jeffrwatts.stargazer.BuildConfig
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.network.StarGazerApi
import com.jeffrwatts.stargazer.data.StarGazerDatabase
import com.jeffrwatts.stargazer.data.celestialobjectimage.CelestialObjImage
import com.jeffrwatts.stargazer.data.celestialobjectimage.CelestialObjImageDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
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

class UpdateImageWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams)
{

    private val starGazerApi = provideStarGazerApi()
    private val celestialObjImageDao = provideCelestiaLObjImageDao(context)

    private fun provideCelestiaLObjImageDao(@ApplicationContext context: Context): CelestialObjImageDao {
        return StarGazerDatabase.getDatabase(context).celestialObjImageDao()
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
            var succeeded = 0
            setProgressAsync(buildStatusUpdate("Getting Images..."))
            val imageUpdates = starGazerApi.getImages() // Assume this returns a list of image data with URLs and catalogIds
            setProgressAsync(buildStatusUpdate("Downloading ${imageUpdates.size} images"))
            celestialObjImageDao.deleteAllImages()
            imageUpdates.forEachIndexed { index, image ->
                try {
                    val imageUrl = image.url
                    val imageStream = URL(imageUrl).openStream()
                    val originalBitmap = BitmapFactory.decodeStream(imageStream)

                    val thumbBitmap = createThumbnail(originalBitmap, image.thumbX, image.thumbY, image.thumbDim)

                    val outputFile = File(applicationContext.cacheDir, "${image.objectId}.webp")
                    outputFile.outputStream().use { fos ->
                        originalBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, fos)
                    }

                    val thumbFile = File(applicationContext.cacheDir, "${image.objectId}_thumb.webp")
                    thumbFile.outputStream().use { fos ->
                        thumbBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, fos)
                    }

                    setProgressAsync(buildStatusUpdate("Downloaded ${index + 1} of ${imageUpdates.size}: ${image.objectId}"))
                    celestialObjImageDao.insertImage(CelestialObjImage(
                        objectId = image.objectId,
                        filename = outputFile.toString(),
                        thumbFilename = thumbFile.toString()))
                    succeeded += 1
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

        Result.success(buildStatusUpdate("Updates Complete"))
    }

    private fun createThumbnail(input: Bitmap, thumbX: Int?, thumbY: Int?, thumbDim: Int?): Bitmap {
        val cropX = thumbX ?: (input.width / 2)
        val cropY = thumbY ?: (input.height / 2)
        val cropDimension = thumbDim ?: input.height

        // Ensure cropping coordinates are within bounds
        val left = (cropX - cropDimension / 2).coerceIn(0, input.width - cropDimension)
        val top = (cropY - cropDimension / 2).coerceIn(0, input.height - cropDimension)
        val right = (left + cropDimension).coerceAtMost(input.width)
        val bottom = (top + cropDimension).coerceAtMost(input.height)

        // Crop the bitmap
        return Bitmap.createBitmap(input, left, top, right - left, bottom - top)

    }

    private fun buildStatusUpdate(statusUpdate: String): Data {
        return Data.Builder()
            .putString("Status", statusUpdate)
            .putString("UpdateType", UpdateType.IMAGE.name)
            .build()
    }
}