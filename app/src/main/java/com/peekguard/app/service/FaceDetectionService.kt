package com.peekguard.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.peekguard.app.MainActivity
import com.peekguard.app.R
import com.peekguard.app.camera.CameraManager
import com.peekguard.app.detection.FaceDetectionManager
import com.peekguard.app.notification.AlertManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Face Detection Service
 * Runs in the background to continuously monitor for unauthorized viewers
 */
class FaceDetectionService : Service(), LifecycleOwner {
    
    private val tag = "FaceDetectionService"
    private val serviceId = 1001
    private val channelId = "face_detection_service"
    
    // Service components
    private lateinit var faceDetectionManager: FaceDetectionManager
    private lateinit var cameraManager: CameraManager
    private lateinit var alertManager: AlertManager
    
    // Lifecycle management
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle get() = lifecycleRegistry
    
    // Coroutine job for service operations
    private var serviceJob: Job? = null
    
    // Detection state
    private var isMonitoring = false
    private var detectionCount = 0
    
    override fun onCreate() {
        super.onCreate()
        
        lifecycleRegistry.currentState = androidx.lifecycle.Lifecycle.State.CREATED
        
        initializeComponents()
        createNotificationChannel()
        
        Log.i(tag, "Face Detection Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(tag, "Starting Face Detection Service")
        
        lifecycleRegistry.currentState = androidx.lifecycle.Lifecycle.State.STARTED
        
        startForeground(serviceId, createForegroundNotification())
        startMonitoring()
        
        return START_STICKY // Restart service if killed
    }
    
    override fun onDestroy() {
        Log.i(tag, "Destroying Face Detection Service")
        
        stopMonitoring()
        releaseResources()
        
        lifecycleRegistry.currentState = androidx.lifecycle.Lifecycle.State.DESTROYED
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null // This is a started service, not bound
    }
    
    /**
     * Initialize all service components
     */
    private fun initializeComponents() {
        // Initialize face detection manager
        faceDetectionManager = FaceDetectionManager(this) { faceCount ->
            onFaceDetected(faceCount)
        }
        
        // Initialize camera manager
        cameraManager = CameraManager(this, this, faceDetectionManager)
        
        // Initialize alert manager
        alertManager = AlertManager(this)
        
        Log.d(tag, "Service components initialized")
    }
    
    /**
     * Start monitoring for faces
     */
    private fun startMonitoring() {
        if (isMonitoring) {
            Log.w(tag, "Monitoring already active")
            return
        }
        
        serviceJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                // Start camera with face detection
                cameraManager.startCamera(
                    onSuccess = {
                        isMonitoring = true
                        Log.i(tag, "Face monitoring started successfully")
                        updateForegroundNotification("Monitoring active")
                    },
                    onError = { error ->
                        Log.e(tag, "Failed to start camera", error)
                        stopSelf()
                    }
                )
                
                // Keep service alive and monitor performance
                monitorServiceHealth()
                
            } catch (e: Exception) {
                Log.e(tag, "Error starting monitoring", e)
                stopSelf()
            }
        }
    }
    
    /**
     * Stop monitoring
     */
    private fun stopMonitoring() {
        if (!isMonitoring) {
            Log.w(tag, "Monitoring not active")
            return
        }
        
        isMonitoring = false
        serviceJob?.cancel()
        
        try {
            cameraManager.stopCamera()
            Log.i(tag, "Face monitoring stopped")
            
        } catch (e: Exception) {
            Log.e(tag, "Error stopping monitoring", e)
        }
    }
    
    /**
     * Handle face detection event
     */
    private fun onFaceDetected(faceCount: Int) {
        detectionCount++
        
        Log.d(tag, "Faces detected: $faceCount (Total detections: $detectionCount)")
        
        // Trigger alert
        alertManager.triggerAlert(faceCount) {
            // Update notification after alert
            updateForegroundNotification("Alert triggered - $faceCount viewers")
            
            // Log detection to database (future implementation)
            // logDetection(faceCount)
        }
        
        // Update foreground notification
        updateForegroundNotification("$detectionCount detections today")
    }
    
    /**
     * Monitor service health and performance
     */
    private suspend fun monitorServiceHealth() {
        while (isMonitoring) {
            try {
                // Check every 30 seconds
                delay(30000)
                
                // Verify camera is still running
                if (!cameraManager.isRunning) {
                    Log.w(tag, "Camera stopped unexpectedly, restarting...")
                    cameraManager.restartCamera()
                }
                
                // Log performance metrics
                Log.d(tag, "Service health check - Detections: $detectionCount")
                
            } catch (e: Exception) {
                Log.e(tag, "Error in health monitoring", e)
                break
            }
        }
    }
    
    /**
     * Create foreground notification
     */
    private fun createForegroundNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("PeekGuard Protection Active")
            .setContentText("Monitoring for unauthorized viewers")
            .setSmallIcon(R.drawable.ic_shield_alert)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    /**
     * Update foreground notification with new text
     */
    private fun updateForegroundNotification(text: String) {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("PeekGuard Protection Active")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_shield_alert)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(serviceId, notification)
    }
    
    /**
     * Create notification channel for service
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Face Detection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background face detection monitoring"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Release all resources
     */
    private fun releaseResources() {
        try {
            cameraManager.release()
            faceDetectionManager.release()
            alertManager.release()
            
        } catch (e: Exception) {
            Log.e(tag, "Error releasing resources", e)
        }
    }
    
    companion object {
        /**
         * Check if service is running
         */
        fun isServiceRunning(context: android.content.Context): Boolean {
            val manager = context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
                if (FaceDetectionService::class.java.name == service.service.className) {
                    return true
                }
            }
            return false
        }
    }
}