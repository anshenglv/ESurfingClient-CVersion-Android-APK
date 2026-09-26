package com.esurfingclient.ans

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

enum class ServiceStatus {
    STOPPED, RUNNING, STOPPING
}

class ESurfingService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)
        
        startNative(filesDir.absolutePath)
        
        return START_STICKY
    }

    override fun onDestroy() {
        stopNative()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            "认证服务通道",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("广东校园重制版")
            .setContentText("认证服务正在运行")
            .setSmallIcon(R.mipmap.icon)
            .build()
    }

    private external fun startNative(baseDir: String)
    private external fun stopNative()
    private external fun getNativeStatus(): Int

    companion object {
        const val CHANNEL_ID = "ESurfingServiceChannel"

        fun getServiceStatus(): ServiceStatus {
            // Use a dummy instance to call native method if necessary, 
            // but since it's a static JNI call in C++, any instance works.
            // Better to make the JNI method static if possible, but this works for now.
            return try {
                when (ESurfingService().getNativeStatus()) {
                    1 -> ServiceStatus.RUNNING
                    2 -> ServiceStatus.STOPPING
                    else -> ServiceStatus.STOPPED
                }
            } catch (e: Exception) {
                ServiceStatus.STOPPED
            }
        }
        
        init {
            System.loadLibrary("ans")
        }
    }
}
