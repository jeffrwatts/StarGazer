import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jeffrwatts.stargazer.BuildConfig
import com.jeffrwatts.stargazer.com.jeffrwatts.stargazer.network.ImageApi
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
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val imageApi = provideImageApi()

    // Determine why dependency injection wasn't working here.
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
            val imageUpdates = imageApi.getImages() // Assume this returns a list of image data with URLs and catalogIds
            imageUpdates.forEach { image ->
                try {
                    val imageUrl = image.url
                    val imageStream = URL(imageUrl).openStream()
                    val outputFile = File(applicationContext.cacheDir, "${image.objectId}.webp")

                    outputFile.outputStream().use { fileOutputStream ->
                        imageStream.copyTo(fileOutputStream)
                    }
                } catch (e: IOException) {
                    Log.e("ImageDownload", "Failed to download image for catalogId: ${image.objectId}", e)
                    // Optionally continue with other images or fail the work
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("WorkManager", "Failed during image download work", e)
            Result.failure()
        }
    }
}
