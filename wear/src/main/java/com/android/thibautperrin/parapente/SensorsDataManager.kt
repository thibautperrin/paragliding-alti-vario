/*
 * MIT License
 *
 * Copyright (c) 2022 Thibaut PERRIN
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.android.thibautperrin.parapente

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Criteria
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val LOG_TAG = "SensorsDataManager"

class SensorsDataManager(
    private val sensorManager: SensorManager,
    private val locationManager: LocationManager,
    private val filesDir: File
) {

    private var listener: SensorDataManagerListener? = null
    private var locationOutputStream: FileOutputStream? = null
    private var pressureOutputStream: FileOutputStream? = null
    private var heartRateOutputStream: FileOutputStream? = null
    private var inertialOutputStream: FileOutputStream? = null
    private var locationFile: File? = null
    private var pressureFile: File? = null
    private var nmeaFile: File? = null
    private var heartRateFile: File? = null
    private var inertialFile: File? = null

    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.FRANCE)

    private val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    private val heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    private val inertialSensorLinAc = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
    private val inertialSensorAcc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val inertialSensorGrav = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
    private val kalmanFilter = KalmanFilter()
    private var isRecording = false
    private var isListenerActive = false

    private val locationListener = LocationListener {
        if (isRecording) {
            val string = String.format(
                Locale.US,
                "%d\t%d\t%.6f\t%.6f\t%4.0f\t%.1f\t%.1f\t%.2f\t%.2f\t%.2f\t%.2f\r\n",
                it.time,
                it.elapsedRealtimeNanos,
                it.latitude,
                it.longitude,
                it.altitude,
                it.speed,
                it.bearing,
                it.accuracy,
                it.verticalAccuracyMeters,
                it.speedAccuracyMetersPerSecond,
                it.bearingAccuracyDegrees
            )
            locationOutputStream?.write(string.toByteArray(charset("UTF-8")))
        }
        if (it.hasVerticalAccuracy() && it.hasAltitude()) {
            listener?.onNewElevation(it.altitude.toFloat())
        }
        if (it.hasSpeed()) {
            listener?.onNewHorizontalSpeed(it.speed)
        }
    }

    private val pressureListener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent?) {
            val newPressure = event?.values?.get(0)
            if (newPressure != null) {
                if (isRecording) {
                    val string = String.format(Locale.US, "%d\t%.5f\t%d\r\n", event.timestamp, event.values[0], event.accuracy)
                    pressureOutputStream?.write(string.toByteArray(charset("UTF-8")))
                }
                val timestampNs = event.timestamp
                kalmanFilter.onNewMeasure(timestampNs, newPressure)
                listener?.onNewPressure(kalmanFilter.pressure)
                val verticalSpeed = -9.7f * kalmanFilter.pressureDerivative
                listener?.onNewVerticalSpeed(verticalSpeed)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }
    }

    private val heartRateEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            when (event?.sensor?.type) {
                Sensor.TYPE_HEART_RATE -> {
                    if (isRecording && event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
                        heartRateOutputStream?.write("${event.timestamp}: ${event.values[0]}".toByteArray(charset("UTF-8")))
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    private val inertialEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            when (event?.sensor?.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    inertialOutputStream?.write(
                        "${event.timestamp}:\tINERT_ACCELEROMETER\t${event.values[0]}\t${event.values[1]}\t${event.values[2]}\n".toByteArray(
                            charset("UTF-8")
                        )
                    )
                }
                Sensor.TYPE_GRAVITY -> {
                    inertialOutputStream?.write(
                        "${event.timestamp}:\tINERT_GRAVITY\t${event.values[0]}\t${event.values[1]}\t${event.values[2]}\n".toByteArray(
                            charset("UTF-8")
                        )
                    )

                }
                Sensor.TYPE_LINEAR_ACCELERATION -> {
                    inertialOutputStream?.write(
                        "${event.timestamp}:\tINERT_LINEARACC\t${event.values[0]}\t${event.values[1]}\t${event.values[2]}\n".toByteArray(
                            charset("UTF-8")
                        )
                    )

                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        }
    }

    init {
        registerGPS()
        registerPressure()
        registerInertial()
        registerHeartRate()
    }

    private fun registerGPS() {
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_FINE
        criteria.isAltitudeRequired = true
        criteria.isSpeedRequired = true
        criteria.powerRequirement = Criteria.POWER_HIGH
        val locationProvider = locationManager.getBestProvider(criteria, true)
        if (locationProvider != null) {
            locationManager.requestLocationUpdates(locationProvider, 0, 0.0f, locationListener)
        }
    }

    private fun registerPressure() {
        sensorManager.registerListener(
            pressureListener,
            pressureSensor,
            SensorManager.SENSOR_DELAY_FASTEST
        )
    }

    private fun registerInertial() {
        sensorManager.registerListener(inertialEventListener, inertialSensorGrav, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(inertialEventListener, inertialSensorLinAc, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(inertialEventListener, inertialSensorAcc, SensorManager.SENSOR_DELAY_UI)
    }

    private fun registerHeartRate() {
        sensorManager.registerListener(heartRateEventListener, heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    fun startRecording() {
        if (isRecording) {
            return
        }
        val date = dateFormatter.format(Date())
        locationFile = File(filesDir, "${date}_location.file")
        pressureFile = File(filesDir, "${date}_pressure.file")
        nmeaFile = File(filesDir, "${date}_nmea.file")
        heartRateFile = File(filesDir, "${date}_heartRate.file")
        inertialFile = File(filesDir, "${date}_inertial.file")
        locationOutputStream = FileOutputStream(locationFile)
        pressureOutputStream = FileOutputStream(pressureFile)
        heartRateOutputStream = FileOutputStream(heartRateFile)
        inertialOutputStream = FileOutputStream(inertialFile)
        val stringLocation =
            "time\telapsedRealtimeNanos\tlatitude\tlongitude\taltitude\tspeed\tbearing\taccuracy\tverticalAccuracyMeters\tspeedAccuracyMetersPerSecond\tbearingAccuracyDegrees\r\n"
        locationOutputStream?.write(stringLocation.toByteArray(charset("UTF-8")))
        val stringPressure = "timestamp\tvalue\taccuracy\r\n"
        pressureOutputStream?.write(stringPressure.toByteArray(charset("UTF-8")))

        isRecording = true
        listener?.onIsRecordingChanged(true)
    }

    fun stopRecording() {
        if (!isRecording) {
            return
        }
        isRecording = false
        locationOutputStream?.close()
        locationOutputStream = null
        pressureOutputStream?.close()
        pressureOutputStream = null
        heartRateOutputStream?.close()
        heartRateOutputStream = null
        inertialOutputStream?.close()
        inertialOutputStream = null
        listener?.onIsRecordingChanged(false)
    }

    fun setListener(listener: SensorDataManagerListener) {
        this.listener = listener
        listener.onIsRecordingChanged(isRecording)
    }

    fun setIsListenerActive(isListenerActive: Boolean) {
        this.isListenerActive = isListenerActive
        if (isRecording) {
            Log.d(LOG_TAG, "setIsListenerActive ignored because recording is in progress")
        } else if (isListenerActive) {
            registerPressure()
            registerGPS()
            registerInertial()
            registerHeartRate()
        } else {
            sensorManager.unregisterListener(pressureListener)
            sensorManager.unregisterListener(inertialEventListener)
            sensorManager.unregisterListener(heartRateEventListener)
            locationManager.removeUpdates(locationListener)
        }
    }
}