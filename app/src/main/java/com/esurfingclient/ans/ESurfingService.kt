package com.esurfingclient.ans

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

enum class ServiceStatus {
    STOPPED, RUNNING, STOPPING
}

class ESurfingService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        copyPortalAssets()

        val notification = createNotification()
        startForeground(1, notification)
        
        startNative(filesDir.absolutePath)
        startNativeMonitor()
        
        return START_STICKY
    }

    private fun copyPortalAssets() {
        val portalDir = File(filesDir, "portal")
        try {
            val assetManager = assets
            fun copyRecursive(assetPath: String, targetDir: File) {
                val files = assetManager.list(assetPath) ?: return
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                for (filename in files) {
                    val outFile = File(targetDir, filename)
                    val subPath = "$assetPath/$filename"
                    val subFiles = assetManager.list(subPath)
                    if (!subFiles.isNullOrEmpty()) {
                        outFile.mkdirs()
                        copyRecursive(subPath, outFile)
                    } else {
                        assetManager.open(subPath).use { input ->
                            outFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
            copyRecursive("portal", portalDir)
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        serviceJob.cancel()
        stopNative()
        super.onDestroy()
    }

    private fun startNativeMonitor() {
        serviceScope.launch {
            var hasRun = false
            val startTime = System.currentTimeMillis()
            while (System.currentTimeMillis() - startTime < 10000) {
                if (getServiceStatus() == ServiceStatus.RUNNING) {
                    hasRun = true
                    break
                }
                delay(200.milliseconds)
            }

            if (!hasRun) {
                stopSelf()
                return@launch
            }

            while (true) {
                delay(500.milliseconds)
                if (getServiceStatus() == ServiceStatus.STOPPED) {
                    stopSelf()
                    break
                }
            }
        }
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
            } catch (_: Exception) {
                ServiceStatus.STOPPED
            }
        }
        
        init {
            System.loadLibrary("ESurfingClient")
        }
    }
}
