package com.jeffrwatts.stargazer.data.orientation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class OrientationRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var currentAccuracy: Int = SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM

    val orientationData = callbackFlow {
        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val orientation = calculateOrientation(event)
                    trySend(orientation.copy(accuracy = currentAccuracy)).isSuccess
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                currentAccuracy = accuracy
            }
        }

        // Register the listener with the sensor manager
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        sensorManager.registerListener(sensorEventListener, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL)

        // Handle the closing of the flow
        awaitClose {
            sensorManager.unregisterListener(sensorEventListener)
        }
    }

    private fun calculateOrientation(event: SensorEvent): OrientationData {
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)

        val adjustedRotationMatrix = FloatArray(9)
        SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, adjustedRotationMatrix)

        val orientationAngles = FloatArray(3)
        SensorManager.getOrientation(adjustedRotationMatrix, orientationAngles)

        val azimuth = Math.toDegrees(orientationAngles[0].toDouble()) // Azimuth (degrees)
        var pitch = Math.toDegrees(orientationAngles[1].toDouble()) // Pitch (degrees)

        // Invert the pitch
        pitch *= -1

        // Adjust pitch range from -180 to 180 to -90 to 90
        if (pitch > 90) {
            pitch = 180 - pitch
        } else if (pitch < -90) {
            pitch = -180 - pitch
        }

        return OrientationData(pitch, azimuth, SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM)
    }



}
