package com.jeffrwatts.stargazer.data.compass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.transform
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class CompassRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var sensorAccuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE

    private val _accuracyFlow = MutableStateFlow(SensorManager.SENSOR_STATUS_UNRELIABLE)
    val accuracyFlow: Flow<Int> = _accuracyFlow.asStateFlow()

    val compassData: Flow<CompassData> = callbackFlow<Float> {
        val sensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, gravity, 0, event.values.size)
                    Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, geomagnetic, 0, event.values.size)
                }

                SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)

                val azimuthRadians = orientationAngles[0]
                trySend(azimuthRadians)
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                if (sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    _accuracyFlow.value = accuracy
                }
            }
        }

        accelerometerSensor?.also { sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_NORMAL) }
        magnetometerSensor?.also { sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_NORMAL) }

        awaitClose { sensorManager.unregisterListener(sensorEventListener) }
    }
        .smoothing(50)
        .combine(accuracyFlow) { azimuthRadians, accuracy ->
            val azimuthDegrees = (Math.toDegrees(azimuthRadians) + 360) % 360
            CompassData(azimuthDegrees, accuracy)
        }
}

fun Flow<Float>.smoothing(windowSize: Int): Flow<Double> = flow {
    val sinValues = ArrayDeque<Float>(windowSize)
    val cosValues = ArrayDeque<Float>(windowSize)

    collect { azimuthRadians ->
        if (sinValues.size >= windowSize) {
            sinValues.removeFirst()
            cosValues.removeFirst()
        }

        sinValues.addLast(sin(azimuthRadians))
        cosValues.addLast(cos(azimuthRadians))

        val averageSin = sinValues.average()
        val averageCos = cosValues.average()
        val smoothedAzimuthRadians = atan2(averageSin, averageCos)

        emit(smoothedAzimuthRadians)
    }
}

