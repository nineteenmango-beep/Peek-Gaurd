package com.peekguard.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.peekguard.app.MainActivity
import com.peekguard.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Alert Manager for PeekGuard
 * Handles notifications, sounds, and vibrations when unauthorized viewers are detected
 */
class AlertManager(private val context: Context) {
    
    private val tag = "AlertManager"
    
    // Notification configuration
    private val channelId = "peekguard_alerts"
    private val notificationId = 1001
    
    // Alert settings
    var alertMode: AlertMode = AlertMode.NOTIFICATION_SOUND
    var soundEnabled: Boolean = true
    var vibrationEnabled: Boolean = true
    
    // System services
    private val notificationManager = NotificationManagerCompat.from(context)
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    // Tone generator for alert sounds
    private var toneGenerator: ToneGenerator? = null
    
    // Throttling to prevent spam alerts
    private var lastAlertTime: Long = 0
    private val alertThrottleMs = 3000 // 3 seconds minimum between alerts
    
    init {
        createNotificationChannel()
        initializeToneGenerator()
    }
    
    /**
     * Trigger alert when unauthorized viewer is detected
     */
    fun triggerAlert(faceCount: Int, onAlertShown: () -> Unit = {}) {
        val currentTime = System.currentTimeMillis()
        
        // Throttle alerts to prevent spam
        if (currentTime - lastAlertTime < alertThrottleMs) {
            Log.d(tag, "Alert throttled")
            return
        }
        
        lastAlertTime = currentTime
        
        CoroutineScope(Dispatchers.Main).launch {
            when (alertMode) {
                AlertMode.NOTIFICATION_SOUND -> {
                    showNotification(faceCount)
                    playAlertSound()
                    triggerVibration()
                }
                AlertMode.VIBRATION_ONLY -> {
                    triggerVibration()
                }
                AlertMode.SILENT_ICON -> {
                    showSilentNotification(faceCount)
                }
                AlertMode.POPUP_ONLY -> {
                    showPopupAlert(faceCount)
                }
            }
            
            onAlertShown()
            Log.i(tag, "Alert triggered for $faceCount faces, mode: $alertMode")
        }
    }
    
    /**
     * Show notification with alert message
     */
    private fun showNotification(faceCount: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_shield_alert) // You'll need to add this icon
            .setContentTitle(context.getString(R.string.unauthorized_viewer_detected))
            .setContentText(context.getString(R.string.someone_watching))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .build()
        
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.e(tag, "Notification permission not granted", e)
        }
    }
    
    /**
     * Show silent notification (icon only)
     */
    private fun showSilentNotification(faceCount: Int) {
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_shield_alert)
            .setContentTitle("Privacy Alert")
            .setContentText("$faceCount viewer(s) detected")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setAutoCancel(true)
            .build()
        
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.e(tag, "Notification permission not granted", e)
        }
    }
    
    /**
     * Show popup alert dialog (system overlay)
     */
    private fun showPopupAlert(faceCount: Int) {
        // This would require SYSTEM_ALERT_WINDOW permission
        // For now, we'll use a notification with high priority
        showNotification(faceCount)
    }
    
    /**
     * Play alert sound
     */
    private fun playAlertSound() {
        if (!soundEnabled) return
        
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000)
        } catch (e: Exception) {
            Log.e(tag, "Error playing alert sound", e)
        }
    }
    
    /**
     * Trigger vibration pattern
     */
    private fun triggerVibration() {
        if (!vibrationEnabled) return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(
                    longArrayOf(0, 300, 200, 300, 200, 300), // Pattern
                    -1 // Don't repeat
                )
                vibrator.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 300, 200, 300, 200, 300), -1)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error triggering vibration", e)
        }
    }
    
    /**
     * Create notification channel for alerts
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Privacy Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for unauthorized viewer detection"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                setShowBadge(true)
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Initialize tone generator for alert sounds
     */
    private fun initializeToneGenerator() {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 80)
        } catch (e: Exception) {
            Log.e(tag, "Error initializing tone generator", e)
        }
    }
    
    /**
     * Clear all active notifications
     */
    fun clearNotifications() {
        notificationManager.cancel(notificationId)
    }
    
    /**
     * Test alert functionality
     */
    fun testAlert() {
        triggerAlert(2) {
            Log.d(tag, "Test alert completed")
        }
    }
    
    /**
     * Check if notification permission is granted
     */
    fun isNotificationPermissionGranted(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }
    
    /**
     * Release resources
     */
    fun release() {
        try {
            toneGenerator?.release()
            toneGenerator = null
            clearNotifications()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing alert manager", e)
        }
    }
}

/**
 * Alert modes for different notification types
 */
enum class AlertMode {
    NOTIFICATION_SOUND,  // Notification + Sound + Vibration
    VIBRATION_ONLY,      // Vibration only
    SILENT_ICON,         // Silent notification icon
    POPUP_ONLY           // Popup overlay (requires special permission)
}