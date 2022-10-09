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

import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

object GpxUtils {

    private val formatter = SimpleDateFormat("yyyy-MM-dd\'T\'HH:mm:ss\'Z\'", Locale.FRANCE)

    fun buildGpx(fileInput: File, fileOutput: File) {
        val printWriter = fileOutput.printWriter()
        printWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<gpx version=\"1.1\" \n" +
                "creator=\"ViewRanger/8.5.02 http://www.viewranger.com\"\n" +
                "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                "xmlns=\"http://www.topografix.com/GPX/1/1\"\n" +
                "xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\"\n" +
                "xmlns:viewranger=\"http://www.viewranger.com/xmlschemas/GpxExtensions/v2\"\n" +
                "xmlns:gpx_style=\"http://www.topografix.com/GPX/gpx_style/0/2\"\n" +
                "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.topografix.com/GPX/gpx_style/0/2\n" +
                "http://www.topografix.com/GPX/gpx_style/0/2/gpx_style.xsd http://www.viewranger.com/xmlschemas/GpxExtensions/v2 http://www.viewranger.com/xmlschemas/GpxExtensions/v2/GpxExtensionsV2.xsd\">\n" +
                "<trk>\n" +
                "<name><![CDATA[Tracé 27 mai 2018 11:29:27]]></name>\n" +
                "<trkseg>\n") // TODO
        var isFirstLine = true
        fileInput.forEachLine { line: String ->
            run {
                if (isFirstLine) {
                    isFirstLine = false
                } else {
                    val items = line.split("\t")
                    val timestamp = items[0].trim().toLong()
                    val elapsedTime = items[1].trim().toLong()
                    val latitude = items[2].trim().toDouble()
                    val longitude = items[3].trim().toDouble()
                    val altitude = items[4].trim().toInt()
                    val speed = items[5].trim().toDouble()
                    val bearing = items[6].trim().toDouble()
                    val accuracy = items[7].trim().toDouble()
                    val verticalAccuracyMeters = items[8].trim().toDouble()
                    val speedAccuracyMetersPerSecond = items[9].trim().toDouble()
                    val bearingAccuracyDegrees = items[10].trim().toDouble()
                    printWriter.write("<trkpt lat=\"$latitude\" lon=\"$longitude\"><ele>$altitude</ele><time>${formatter.format(Date(timestamp))}</time></trkpt>\n")
                }
            }
        }
        printWriter.write("</trkseg>\n" +
                "</trk>\n" +
                "</gpx>\n")
        printWriter.close()
    }
}