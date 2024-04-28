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
import com.jeffrwatts.stargazer.data.celestialobject.CelestialObjDao
import com.jeffrwatts.stargazer.data.celestialobject.toCelestialObjEntity
import com.jeffrwatts.stargazer.data.celestialobjectimage.CelestialObjImage
import com.jeffrwatts.stargazer.data.celestialobjectimage.CelestialObjImageDao
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
import java.io.File
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit


class UpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val starGazerApi = provideStarGazerApi()
    private val celestialObjDao = provideCelestialObjDao(context)
    private val celestialObjImageDao = provideCelestiaLObjImageDao(context)
    private val variableStarObjDao = provideVariableStarObjDao(context)

    private fun provideCelestialObjDao(@ApplicationContext context: Context): CelestialObjDao {
        return StarGazerDatabase.getDatabase(context).celestialObjDao()
    }

    private fun provideVariableStarObjDao(@ApplicationContext context: Context): VariableStarObjDao {
        return StarGazerDatabase.getDatabase(context).variableStarObjDao()
    }

    fun provideCelestiaLObjImageDao(@ApplicationContext context: Context): CelestialObjImageDao {
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
        updateDSOObjects()
        updateVariableStarObjects()
        updateImages()
        Result.success(buildStatusUpdate("Updates Complete"))
    }

    private suspend fun updateDSOObjects() {
        try {
            setProgressAsync(buildStatusUpdate("Getting DSO Objects"))
            val dsoObjects = starGazerApi.getDso()
            setProgressAsync(buildStatusUpdate("Updating DSO DB"))
            val celestialObjs = dsoObjects.map { it.toCelestialObjEntity() }
            celestialObjDao.deleteAll()
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
        return Data.Builder().putString("Status", statusUpdate).build()
    }

    private suspend fun updateImages() {
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

                    val croppedBitmap = if (image.crop > 0 && originalBitmap.height > image.crop) {
                        val aspectRatio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
                        val targetWidth = (image.crop * aspectRatio).toInt()

                        val xOffset = (originalBitmap.width - targetWidth) / 2
                        val yOffset = (originalBitmap.height - image.crop) / 2
                        Bitmap.createBitmap(originalBitmap, xOffset, yOffset, targetWidth, image.crop)
                    } else {
                        originalBitmap
                    }

                    val finalBitmap = if (croppedBitmap.height != 1080) {
                        val aspectRatio = croppedBitmap.width.toFloat() / croppedBitmap.height.toFloat()
                        val targetWidth = (1080 * aspectRatio).toInt()
                        Bitmap.createScaledBitmap(croppedBitmap, targetWidth, 1080, true)
                    } else {
                        croppedBitmap
                    }

                    val outputFile = File(applicationContext.cacheDir, "${image.objectId}.webp")
                    outputFile.outputStream().use { fos ->
                        finalBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, fos)
                    }

                    setProgressAsync(buildStatusUpdate("Downloaded ${index + 1} of ${imageUpdates.size}: ${image.objectId}"))
                    celestialObjImageDao.insertImage(CelestialObjImage(objectId = image.objectId, crop = image.crop, filename = outputFile.toString()))
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
    }
}