package com.jeffrwatts.stargazer.data.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class CompassRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var sensorAccuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE


    private val _compassData = MutableStateFlow(CompassData(0.0, SensorManager.SENSOR_STATUS_UNRELIABLE))
    val compassData = _compassData.asStateFlow()

    fun setupSensors() {
        accelerometerSensor?.also { accel ->
            sensorManager.registerListener(sensorEventListener, accel, SensorManager.SENSOR_DELAY_NORMAL)
        }

        magnetometerSensor?.also { magnetic ->
            sensorManager.registerListener(sensorEventListener, magnetic, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            logSensorData(event)
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, gravity, 0, event.values.size)
                Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
            }

            SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
            SensorManager.getOrientation(rotationMatrix, orientationAngles)

            val azimuthRadians = orientationAngles[0]
            var azimuthDegrees = Math.toDegrees(azimuthRadians.toDouble())
            azimuthDegrees = (azimuthDegrees + 360) % 360

            _compassData.value = CompassData(azimuthDegrees, sensorAccuracy)
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                sensorAccuracy = accuracy
            }
        }
    }

    fun logSensorData(event: SensorEvent) {
        val sensor = event.sensor
        val sensorType = sensor.type
        val sensorName = sensor.name
        val sensorVendor = sensor.vendor
        val sensorVersion = sensor.version
        val sensorAccuracy = event.accuracy
        val sensorValues = event.values.joinToString(separator = ", ") { it.toString() }

        Log.d("SensorData", "Sensor Information")
        Log.d("SensorData", "Type: $sensorType")
        Log.d("SensorData", "Name: $sensorName")
        Log.d("SensorData", "Vendor: $sensorVendor")
        Log.d("SensorData", "Version: $sensorVersion")
        Log.d("SensorData", "Accuracy: $sensorAccuracy")
        Log.d("SensorData", "Values: $sensorValues")
    }
}