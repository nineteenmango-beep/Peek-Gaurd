package com.peekguard.app.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Permission utilities for PeekGuard
 */
object PermissionUtils {
    
    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if notification permission is granted (Android 13+)
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Not required before Android 13
        }
    }
    
    /**
     * Check if all required permissions are granted
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasCameraPermission(context) && hasNotificationPermission(context)
    }
    
    /**
     * Get list of missing permissions
     */
    fun getMissingPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()
        
        if (!hasCameraPermission(context)) {
            missing.add(android.Manifest.permission.CAMERA)
        }
        
        if (!hasNotificationPermission(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                missing.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        return missing
    }
}

/**
 * Device capability utilities
 */
object DeviceUtils {
    
    /**
     * Check if device has front camera
     */
    fun hasFrontCamera(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
    }
    
    /**
     * Check if device has camera hardware
     */
    fun hasCamera(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
    
    /**
     * Get device model information
     */
    fun getDeviceInfo(): String {
        return "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
    }
    
    /**
     * Check if device supports vibration
     */
    fun hasVibrator(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_VIBRATE)
    }
}

/**
 * Formatting utilities
 */
object FormatUtils {
    
    /**
     * Format timestamp to readable string
     */
    fun formatTimestamp(timestamp: Long): String {
        val formatter = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault())
        return formatter.format(java.util.Date(timestamp))
    }
    
    /**
     * Format duration in milliseconds to readable string
     */
    fun formatDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }
    
    /**
     * Format battery percentage
     */
    fun formatBatteryLevel(level: Float): String {
        return "${level.toInt()}%"
    }
}