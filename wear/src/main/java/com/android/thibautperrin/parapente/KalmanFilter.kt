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

class KalmanFilter {

    private var covarianceMatrix00 = 0.01f
    private var covarianceMatrix01 = 0f
    private var covarianceMatrix10 = 0f
    private var covarianceMatrix11 = NOISE_SENSOR
    private var lastTimeStamp = 0L
    var pressure = 0.0f
    var pressureDerivative = 0.0f
    var hasData = false

    fun onNewMeasure(timestamp: Long, value: Float) {
        if (hasData) {
            val duration = (timestamp - lastTimeStamp) / 1000_000_000f
            val durationSquared = duration * duration
            // Forecast
            val predictedPressure = pressure + pressureDerivative * duration
            val predictedCovMatrix00 = covarianceMatrix00 + durationSquared * NOISE_ACCELERATION
            val predictedCovMatrix01 = covarianceMatrix01 + duration * covarianceMatrix00 + duration * durationSquared * NOISE_ACCELERATION / 2
            val predictedCovMatrix10 = covarianceMatrix10 + duration * covarianceMatrix00 + duration * durationSquared * NOISE_ACCELERATION / 2
            val predictedCovMatrix11 =
                covarianceMatrix11 + duration * (covarianceMatrix10 + covarianceMatrix01 + covarianceMatrix00 * duration) + durationSquared * durationSquared * NOISE_ACCELERATION / 4

            // Update
            val innovation = value - predictedPressure
            val innovationCovariance = predictedCovMatrix11 + NOISE_SENSOR
            val kalmanGain0 = predictedCovMatrix01 / innovationCovariance
            val kalmanGain1 = predictedCovMatrix11 / innovationCovariance
            pressureDerivative += kalmanGain0 * innovation
            pressure = predictedPressure + kalmanGain1 * innovation
            covarianceMatrix00 = predictedCovMatrix00 - kalmanGain0 * predictedCovMatrix10
            covarianceMatrix01 = predictedCovMatrix01 - kalmanGain0 * predictedCovMatrix11
            covarianceMatrix10 = predictedCovMatrix10 - kalmanGain1 * predictedCovMatrix10
            covarianceMatrix11 = predictedCovMatrix11 - kalmanGain1 * predictedCovMatrix11
        } else {
            pressure = value
            pressureDerivative = 0.0f
            hasData = true
        }
        lastTimeStamp = timestamp
    }

    companion object {
        const val NOISE_ACCELERATION = 0.06f * 0.06f // (0.6 m/s²) ~= 0.06 hPa/s²
        const val NOISE_SENSOR = 0.034f * 0.034f // standard deviation: 0.034 hPa ~= 33cm (accurately measured on 4th september 2022)
    }
}