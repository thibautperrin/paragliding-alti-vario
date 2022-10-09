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

import org.junit.Test
import java.io.File
import java.util.*

internal class SensorMeasureTabTest {
    @Test
    fun testSensorMeasureTab() {
        val fileInput = File("/data/data/com.android.thibautperrin.parapente/files/2022-09-04_10-44-39_pressure.file")
        val fileOutput = File("/data/data/com.android.thibautperrin.parapente/files/2022-09-04_10-44-39_VSPEED.test.file")
        val printWriter = fileOutput.printWriter()
        var isFirstLine = true
        var index = 0
       // val sensorMeasureTab = SensorMeasureTab(1 / 4096.0f)
        val kalmanFilter = KalmanFilter()
        fileInput.forEachLine { line: String ->
            run {
                if (isFirstLine) {
                    isFirstLine = false
                } else {
                    index++
                    val items = line.split("\t")
                    val timestamp = items[0].trim().toLong()
                    val pressure = items[1].trim().toFloat()
                    val accuracy = items[2].trim().toInt()
                    kalmanFilter.onNewMeasure(timestamp, pressure)
                    if (kalmanFilter.hasData) {
                        val verticalSpeed = -9.7f * kalmanFilter.pressureDerivative
                        printWriter.write(
                            "$timestamp\t$pressure\t${kalmanFilter.pressure}\t$verticalSpeed\n"
                        )
                    }
                }
            }
        }
        printWriter.close()
    }
}