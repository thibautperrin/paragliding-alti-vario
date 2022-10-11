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

interface SensorDataManagerListener {


    fun onIsRecordingChanged(isRecording: Boolean)

    /**
     * Horizontal speed from locationManager (GPS) in m/s
     */
    fun onNewHorizontalSpeed(speed: Float)

    /**
     * Elevation/Altitude above sea level (or more exactly above geoid WGS84) from locationManager (GPS) in meters
     */
    fun onNewElevation(elevation: Float)

    /**
     * Pressure from the dedicated micro-electro-mechanical-system (MEMS) (=Harware in the android Wear device), in hPa (hecto Pascal)
     */
    fun onNewPressure(pressure: Float)

    /**
     * Vertical speed estimated from the pressure variation using a conversion factor of 9,7 m / hPa
     * This approximation can be particularly wrong at very high altitude (> 3000m) or with very low/high temperature)
     * TODO : A better approximation, taking these variables into account will be used in a close future.
     */
    fun onNewVerticalSpeed(verticalSpeed: Float)
}