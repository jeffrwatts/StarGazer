package com.jeffrwatts.stargazer.data.location

import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY

class LocationRepository(private val context: Context) {
    private val TAG = LocationRepository::class.java.simpleName
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    private val _locationFlow = MutableStateFlow<Location?>(null)
    val locationFlow = _locationFlow.asStateFlow()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let {
                Log.d(TAG, "Location result: ${it.accuracy}, ${it.isComplete}")
                _locationFlow.value = it
            }
        }
    }

    fun startLocationUpdates(scope: CoroutineScope) {
        val locationRequest = LocationRequest.Builder(5000L).apply {
            setPriority(PRIORITY_HIGH_ACCURACY)
            setMinUpdateDistanceMeters(25f)
        }.build()


        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            scope.launch {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
