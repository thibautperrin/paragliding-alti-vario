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

import kotlin.math.pow

object ComputationUtils {

    val M_CSTE = 0.0289644 // kg/mol
    val G_CSTE = 9.80665 // m/s²
    val R_CSTE = 8.31446261815 // J/K/mol
    val T0_CSTE = 293 // K (let's say 20°C)
    val A_CSTE = 0.00984 // K/m

    fun computePressureAtSeaLevel(altitude: Float, pressure: Float): Float {
        return (pressure / (1 - A_CSTE * altitude / T0_CSTE).pow(M_CSTE * G_CSTE / ( R_CSTE * A_CSTE))).toFloat()
    }

    /**
     * dh/dt = dh/dP * dP/dt
     * varPressure is dP/dt (Pa/s)
     */
    fun computeVerticalSpeed(varPressure: Float, pressure: Float, pressureH0: Float): Float {
        val pressurePa = pressure * 100.0
        val pressureH0Pa = pressureH0 * 100.0

        // coeff is dh/dP
        val coeff = -T0_CSTE * R_CSTE / (M_CSTE * G_CSTE * pressureH0Pa) * (pressurePa / pressureH0Pa).pow(R_CSTE * A_CSTE / M_CSTE * G_CSTE - 1)

        return (coeff * varPressure).toFloat()
    }
}