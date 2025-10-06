package com.peekguard.app.utils

import android.content.Context
import android.os.BatteryManager
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Performance Monitor for PeekGuard
 * Monitors battery usage, CPU usage, and performance metrics
 * Automatically adjusts settings to optimize battery life
 */
class PerformanceMonitor(private val context: Context) {
    
    private val tag = "PerformanceMonitor"
    
    // Monitoring job
    private var monitoringJob: Job? = null
    
    // Performance metrics
    private var batteryLevel: Float = 100f
    private var isLowPowerMode: Boolean = false
    private var cpuUsageLevel: CpuUsageLevel = CpuUsageLevel.NORMAL
    
    // Optimization callbacks
    private var onOptimizationNeeded: ((OptimizationSuggestion) -> Unit)? = null
    
    // Battery and power managers
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    fun startMonitoring(onOptimization: (OptimizationSuggestion) -> Unit) {
        onOptimizationNeeded = onOptimization
        
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    // Check battery status
                    checkBatteryStatus()
                    
                    // Check power save mode
                    checkPowerSaveMode()
                    
                    // Monitor performance
                    checkPerformanceMetrics()
                    
                    // Apply optimizations if needed
                    evaluateOptimizations()
                    
                    // Check every 30 seconds
                    delay(30000)
                    
                } catch (e: Exception) {
                    Log.e(tag, "Error in performance monitoring", e)
                    delay(60000) // Wait longer on error
                }
            }
        }
        
        Log.i(tag, "Performance monitoring started")
    }
    
    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        Log.i(tag, "Performance monitoring stopped")
    }
    
    private fun checkBatteryStatus() {
        try {
            val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            batteryLevel = level.toFloat()
            
            Log.d(tag, "Battery level: $batteryLevel%")
            
        } catch (e: Exception) {
            Log.e(tag, "Error checking battery status", e)
        }
    }
    
    private fun checkPowerSaveMode() {
        try {
            isLowPowerMode = powerManager.isPowerSaveMode
            Log.d(tag, "Power save mode: $isLowPowerMode")
            
        } catch (e: Exception) {
            Log.e(tag, "Error checking power save mode", e)
        }
    }
    
    private fun checkPerformanceMetrics() {
        // Monitor CPU usage (simplified estimation)
        // In a real implementation, you would use more sophisticated methods
        val currentTime = System.currentTimeMillis()
        val memoryInfo = Runtime.getRuntime()
        
        val usedMemory = memoryInfo.totalMemory() - memoryInfo.freeMemory()
        val maxMemory = memoryInfo.maxMemory()
        val memoryUsagePercent = (usedMemory.toFloat() / maxMemory.toFloat()) * 100
        
        cpuUsageLevel = when {
            memoryUsagePercent > 80f -> CpuUsageLevel.HIGH
            memoryUsagePercent > 60f -> CpuUsageLevel.MEDIUM
            else -> CpuUsageLevel.NORMAL
        }
        
        Log.d(tag, "Memory usage: ${memoryUsagePercent.toInt()}%, CPU level: $cpuUsageLevel")
    }
    
    private fun evaluateOptimizations() {
        val suggestions = mutableListOf<OptimizationAction>()
        
        // Battery-based optimizations
        when {
            batteryLevel <= 15f -> {
                suggestions.add(OptimizationAction.REDUCE_FRAME_RATE)
                suggestions.add(OptimizationAction.LOWER_RESOLUTION)
                suggestions.add(OptimizationAction.INCREASE_DETECTION_INTERVAL)
            }
            batteryLevel <= 30f -> {
                suggestions.add(OptimizationAction.REDUCE_FRAME_RATE)
                suggestions.add(OptimizationAction.INCREASE_DETECTION_INTERVAL)
            }
        }
        
        // Power save mode optimizations
        if (isLowPowerMode) {
            suggestions.add(OptimizationAction.ENABLE_POWER_SAVE_MODE)
            suggestions.add(OptimizationAction.REDUCE_FRAME_RATE)
        }
        
        // CPU usage optimizations
        if (cpuUsageLevel == CpuUsageLevel.HIGH) {
            suggestions.add(OptimizationAction.LOWER_RESOLUTION)
            suggestions.add(OptimizationAction.REDUCE_PROCESSING_FREQUENCY)
        }
        
        if (suggestions.isNotEmpty()) {
            val optimization = OptimizationSuggestion(
                batteryLevel = batteryLevel,
                isLowPowerMode = isLowPowerMode,
                cpuUsageLevel = cpuUsageLevel,
                actions = suggestions
            )
            
            onOptimizationNeeded?.invoke(optimization)
        }
    }
    
    /**
     * Get current performance metrics
     */
    fun getCurrentMetrics(): PerformanceMetrics {
        return PerformanceMetrics(
            batteryLevel = batteryLevel,
            isLowPowerMode = isLowPowerMode,
            cpuUsageLevel = cpuUsageLevel,
            timestamp = System.currentTimeMillis()
        )
    }
    
    /**
     * Get optimization recommendations
     */
    fun getOptimizationRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()
        
        if (batteryLevel <= 20f) {
            recommendations.add("Consider reducing detection sensitivity to save battery")
            recommendations.add("Lower camera resolution to improve performance")
        }
        
        if (isLowPowerMode) {
            recommendations.add("Power save mode detected - optimizing for battery life")
        }
        
        if (cpuUsageLevel == CpuUsageLevel.HIGH) {
            recommendations.add("High CPU usage detected - reducing processing load")
        }
        
        if (recommendations.isEmpty()) {
            recommendations.add("Performance is optimal")
        }
        
        return recommendations
    }
}

/**
 * Performance optimization suggestion
 */
data class OptimizationSuggestion(
    val batteryLevel: Float,
    val isLowPowerMode: Boolean,
    val cpuUsageLevel: CpuUsageLevel,
    val actions: List<OptimizationAction>
)

/**
 * Current performance metrics
 */
data class PerformanceMetrics(
    val batteryLevel: Float,
    val isLowPowerMode: Boolean,
    val cpuUsageLevel: CpuUsageLevel,
    val timestamp: Long
)

/**
 * CPU usage levels
 */
enum class CpuUsageLevel {
    LOW,
    NORMAL,
    MEDIUM,
    HIGH
}

/**
 * Optimization actions that can be taken
 */
enum class OptimizationAction {
    REDUCE_FRAME_RATE,          // Lower camera frame rate
    LOWER_RESOLUTION,           // Reduce camera resolution
    INCREASE_DETECTION_INTERVAL, // Check for faces less frequently
    REDUCE_PROCESSING_FREQUENCY, // Process fewer frames
    ENABLE_POWER_SAVE_MODE,     // Enable app-specific power saving
    PAUSE_BACKGROUND_TASKS      // Reduce background processing
}