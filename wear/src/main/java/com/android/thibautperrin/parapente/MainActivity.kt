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

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Criteria
import android.location.LocationListener
import android.location.LocationManager
import android.location.OnNmeaMessageListener
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.android.thibautperrin.parapente.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {

    private var isRecording = false
    private val LOG_TAG = MainActivity::class.java.simpleName
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd_hh-mm-ss", Locale.FRANCE)
    private lateinit var binding: ActivityMainBinding
    private lateinit var textPressure: TextView
    private lateinit var textAlt: TextView
    private lateinit var textSpeed: TextView
    private lateinit var textVerticalSpeed: TextView
    private lateinit var button: Button
    private lateinit var layout: ViewGroup

    private val kalmanFilter = KalmanFilter()
    private lateinit var sensorManager: SensorManager
    private lateinit var locationManager: LocationManager
    private lateinit var pressureSensor: Sensor
    private lateinit var heartRateSensor: Sensor
    private lateinit var inertialSensorGrav: Sensor
    private lateinit var inertialSensorLinAc: Sensor
    private lateinit var inertialSensorAcc: Sensor
    private lateinit var locationProvider: String
    private var locationOutputStream: FileOutputStream? = null
    private var pressureOutputStream: FileOutputStream? = null
    private var nmeaOutputStream: FileOutputStream? = null
    private var heartRateOutputStream: FileOutputStream? = null
    private var inertialOutputStream: FileOutputStream? = null
    private var locationFile: File? = null
    private var pressureFile: File? = null
    private var nmeaFile: File? = null
    private var heartRateFile: File? = null
    private var inertialFile: File? = null
    private var currentColorSpeedZone = ColorSpeedZone.WHITE


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
            Log.i(LOG_TAG, "onSensorChanged $sensor $accuracy")
        }
    }

    private val heartRateEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            when (event?.sensor?.type) {
                Sensor.TYPE_HEART_RATE -> {
                    Log.v(LOG_TAG, "Heart-rate ${event.timestamp} ${event.accuracy} ${event.values[0]}\n")
                    if (isRecording && event.accuracy == SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
                        heartRateOutputStream?.write("${event.timestamp}: ${event.values[0]}".toByteArray(charset("UTF-8")))
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            Log.i(LOG_TAG, "onSensorChanged $sensor $accuracy")
        }
    }

    private val sensorEventListener = object : SensorEventListener {
        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            Log.d(LOG_TAG, "onAccuracyChanged: $p1")
        }

        override fun onSensorChanged(p0: SensorEvent?) {
            //   //   Log.d("TO_DELETE", "Sensor: $pressureSensor fifoMaxEventCount: ${pressureSensor.fifoMaxEventCount} highestDirectReportRateLevel: ${pressureSensor.highestDirectReportRateLevel} isWakeUpSensor: ${pressureSensor.isWakeUpSensor} isAdditionalInfoSupported: ${pressureSensor.isAdditionalInfoSupported}")
            //      val floatVal = p0!!.values[0]
            //      val intVal = (floatVal/pressureSensor.resolution).roundToInt()
            //      Log.d("TO_DELETE", "FloatVal: $floatVal IntVal: $intVal")
            val newPressure = p0?.values?.get(0)
            if (newPressure != null) {
                if (isRecording) {
                    val string = String.format(Locale.US, "%d\t%.5f\t%d\r\n", p0.timestamp, p0.values[0], p0.accuracy)
                    pressureOutputStream?.write(string.toByteArray(charset("UTF-8")))
                }
                val timestampNs = p0.timestamp
                kalmanFilter.onNewMeasure(timestampNs, newPressure)
                textPressure.text = String.format("%.2f hPa", kalmanFilter.pressure)
                val verticalSpeed = -9.7f * kalmanFilter.pressureDerivative
                textVerticalSpeed.text = String.format("%.1f m/s", verticalSpeed)
                handleVerticalSpeed(verticalSpeed)
            }
        }
    }

    private enum class ColorSpeedZone(val color: Int) {
        BLUE(0xFF4444FF.toInt()),
        SOFT_BLUE(0xFF9999FF.toInt()),
        WHITE(0xFFFFFFFF.toInt()),
        GREEN(0xFF88FF88.toInt()),
        YELLOW(0xFFFFFF66.toInt()),
        ORANGE(0xFFFFFF00.toInt()),
        PINK(0xFFFF8888.toInt()),
        RED(0xFFFF0000.toInt());
    }

    private fun getVerticalSpeedZone(verticalSpeed: Float): ColorSpeedZone {
        return if (verticalSpeed > 1.5f) {
            if (verticalSpeed > 2.5f) {
                if (verticalSpeed > 4.0f) {
                    ColorSpeedZone.RED
                } else {
                    ColorSpeedZone.PINK
                }
            } else {
                if (verticalSpeed > 2.0f) {
                    ColorSpeedZone.ORANGE
                } else {
                    ColorSpeedZone.YELLOW
                }
            }
        } else {
            if (verticalSpeed > -0.8f) {
                if (verticalSpeed > 0.5f) {
                    ColorSpeedZone.GREEN
                } else {
                    ColorSpeedZone.WHITE
                }
            } else {
                if (verticalSpeed > -1.2f) {
                    ColorSpeedZone.SOFT_BLUE
                } else {
                    ColorSpeedZone.BLUE
                }
            }
        }
    }

    private fun handleVerticalSpeed(verticalSpeed: Float) {
        val colorSpeedZone = getVerticalSpeedZone(verticalSpeed)
        if (colorSpeedZone != currentColorSpeedZone) {
            currentColorSpeedZone = colorSpeedZone
            layout.setBackgroundColor(colorSpeedZone.color)
        }
    }

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
        if (it.hasVerticalAccuracy() && it.hasAltitude() && kalmanFilter.hasData) {
            textAlt.text = String.format("%.0f m", it.altitude)
        }
        if (it.hasSpeed()) {
            textSpeed.text = String.format("%.1f km/h", it.speed * 3.6)
        }
    }

    private val onNmeaMessageListener = OnNmeaMessageListener { message, timestamp ->
        if (isRecording) {
            nmeaOutputStream?.write("${dateFormatter.format(Date(timestamp))}: $message".toByteArray(charset("UTF-8")))
        }
    }
    private val onClickListener = View.OnClickListener {
        if (isRecording) {
            Log.d(LOG_TAG, "stopping recording")
            button.text = "START"
        } else {
            Log.d(LOG_TAG, "starting recording")
            button.text = "STOP"
        }
        val thread = object : java.lang.Thread() {
            override fun run() {
                Log.d(LOG_TAG, "recording thread: wasRecording=$isRecording")
                if (isRecording) {
                    isRecording = false
                    locationOutputStream?.close()
                    pressureOutputStream?.close()
                } else {
                    isRecording = true
                    val date = dateFormatter.format(Date())
                    locationFile = File(filesDir, "${date}_location.file")
                    pressureFile = File(filesDir, "${date}_pressure.file")
                    nmeaFile = File(filesDir, "${date}_nmea.file")
                    heartRateFile = File(filesDir, "${date}_heartRate.file")
                    inertialFile = File(filesDir, "${date}_inertial.file")
                    locationOutputStream = FileOutputStream(locationFile)
                    pressureOutputStream = FileOutputStream(pressureFile)
                    nmeaOutputStream = FileOutputStream(nmeaFile)
                    heartRateOutputStream = FileOutputStream(heartRateFile)
                    inertialOutputStream = FileOutputStream(inertialFile)
                    val stringLocation =
                        "time\telapsedRealtimeNanos\tlatitude\tlongitude\taltitude\tspeed\tbearing\taccuracy\tverticalAccuracyMeters\tspeedAccuracyMetersPerSecond\tbearingAccuracyDegrees\r\n"
                    locationOutputStream?.write(stringLocation.toByteArray(charset("UTF-8")))
                    val stringPressure = "timestamp\tvalue\taccuracy\r\n"
                    pressureOutputStream?.write(stringPressure.toByteArray(charset("UTF-8")))
                }
            }
        }
        thread.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        textPressure = findViewById(R.id.textPressure)
        textAlt = findViewById(R.id.textAlt)
        textSpeed = findViewById(R.id.textSpeed)
        textVerticalSpeed = findViewById(R.id.textVerticalSpeed)
        button = findViewById(R.id.button)
        layout = findViewById(R.id.layout)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        inertialSensorLinAc = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        inertialSensorAcc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        inertialSensorGrav = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
        val criteria = Criteria()
        criteria.accuracy = Criteria.ACCURACY_FINE
        criteria.isBearingRequired = true
        criteria.isAltitudeRequired = true
        criteria.isSpeedRequired = true
        criteria.isCostAllowed = true
        criteria.powerRequirement = Criteria.POWER_HIGH
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 42)
            return
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BODY_SENSORS), 42)
            return
        }
        locationProvider = locationManager.getBestProvider(criteria, true)!!
        Log.d(LOG_TAG, "Location provider: $locationProvider")

        val lastLocation = locationManager.getLastKnownLocation(locationProvider)
        val handler = Handler(Looper.getMainLooper())
        locationManager.addNmeaListener(onNmeaMessageListener, handler)
        locationManager.requestLocationUpdates(locationProvider, 0, 0.0f, locationListener)
        Log.d(LOG_TAG, "lastLocation: $lastLocation")
        Log.d(LOG_TAG, "PressureSensor: ${pressureSensor.name}")
        button.setOnClickListener(onClickListener)
        layout.setBackgroundColor(ColorSpeedZone.WHITE.color)
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            sensorEventListener,
            pressureSensor,
            SensorManager.SENSOR_DELAY_FASTEST
        )
        sensorManager.registerListener(heartRateEventListener, heartRateSensor, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(inertialEventListener, inertialSensorGrav, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(inertialEventListener, inertialSensorLinAc, SensorManager.SENSOR_DELAY_UI)
        sensorManager.registerListener(inertialEventListener, inertialSensorAcc, SensorManager.SENSOR_DELAY_UI)
    }

    override fun onPause() {
        sensorManager.unregisterListener(sensorEventListener)
        sensorManager.unregisterListener(heartRateEventListener)
        sensorManager.unregisterListener(inertialEventListener)
        sensorManager.unregisterListener(inertialEventListener)
        sensorManager.unregisterListener(inertialEventListener)
        super.onPause()
    }
}