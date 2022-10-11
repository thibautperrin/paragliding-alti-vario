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
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.android.thibautperrin.parapente.databinding.ActivityMainBinding
import org.koin.android.ext.android.inject


class MainActivity : Activity() {

    private val sensorsDataManager: SensorsDataManager by inject()

    private lateinit var binding: ActivityMainBinding
    private lateinit var textPressure: TextView
    private lateinit var textAlt: TextView
    private lateinit var textSpeed: TextView
    private lateinit var textVerticalSpeed: TextView
    private lateinit var button: Button
    private lateinit var layout: ViewGroup
    private var currentColorSpeedZone = ColorSpeedZone.WHITE
    private var isRecording = false

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

        val listener = object : SensorDataManagerListener {
            override fun onIsRecordingChanged(isRecording: Boolean) {
                this@MainActivity.isRecording = isRecording
                if (isRecording) {
                    button.text = "STOP"
                } else {
                    button.text = "START"
                }
            }

            override fun onNewHorizontalSpeed(speed: Float) {
                textSpeed.text = String.format("%.1f km/h", speed * 3.6)
            }

            override fun onNewElevation(elevation: Float) {
                textAlt.text = String.format("%.0f m", elevation)
            }

            override fun onNewPressure(pressure: Float) {
                textPressure.text = String.format("%.2f hPa", pressure)
            }

            override fun onNewVerticalSpeed(verticalSpeed: Float) {
                textVerticalSpeed.text = String.format("%.1f m/s", verticalSpeed)
                handleVerticalSpeed(verticalSpeed)
            }
        }
        sensorsDataManager.setListener(listener)

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
        button.setOnClickListener {
            if (isRecording) {
                sensorsDataManager.stopRecording()
                CoreForegroundService.stopService(this)
            } else {
                sensorsDataManager.startRecording()
                CoreForegroundService.startService(this)
            }
        }
        layout.setBackgroundColor(ColorSpeedZone.WHITE.color)
    }

    override fun onResume() {
        super.onResume()
        sensorsDataManager.setIsListenerActive(true)
    }

    override fun onPause() {
        sensorsDataManager.setIsListenerActive(false)
        super.onPause()
    }
}