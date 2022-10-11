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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log

private const val LOG_TAG = "CoreForegroundService"

class CoreForegroundService : Service() {


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action?.equals(START_SERVICE) == true) {
            Log.d(LOG_TAG, "onStartCommand --> start service in foreground")
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_MAX
            val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)

            val notification: Notification = Notification.Builder(this, mChannel.id)
                .setContentTitle(getText(R.string.notification_title))
                .setContentText(getText(R.string.notification_message))
                .setSmallIcon(R.drawable.icon)
                .build()

            startForeground(NOTIFICATION_ID, notification)
        } else if (intent?.action?.equals(STOP_SERVICE) == true) {
            Log.d(LOG_TAG, "onStartCommand --> stop service in foreground")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelfResult(startId)
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val START_SERVICE = "com.android.thibautperrin.parapente.startService"
        const val STOP_SERVICE = "com.android.thibautperrin.parapente.stopService"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "CHANNEL_SERVICE"

        fun startService(context: Context) {
            val startIntent = Intent(context, CoreForegroundService::class.java)
            startIntent.action = START_SERVICE
            context.startService(startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, CoreForegroundService::class.java)
            stopIntent.action = STOP_SERVICE
            context.startService(stopIntent)
        }
    }
}