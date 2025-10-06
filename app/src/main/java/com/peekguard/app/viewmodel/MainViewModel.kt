package com.peekguard.app.viewmodel

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.peekguard.app.service.FaceDetectionService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for MainActivity
 * Manages privacy protection state and communication with face detection service
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val tag = "MainViewModel"
    
    // State flows for UI
    private val _isProtectionEnabled = MutableStateFlow(false)
    val isProtectionEnabled: StateFlow<Boolean> = _isProtectionEnabled.asStateFlow()
    
    private val _detectionCount = MutableStateFlow(0)
    val detectionCount: StateFlow<Int> = _detectionCount.asStateFlow()
    
    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()
    
    private val _lastDetectionTime = MutableStateFlow(0L)
    val lastDetectionTime: StateFlow<Long> = _lastDetectionTime.asStateFlow()
    
    init {
        checkPermissions()
        // Initialize detection count from preferences
        loadDetectionCount()
    }
    
    /**
     * Start privacy protection
     */
    fun startProtection() {
        if (!_hasPermissions.value) {
            Log.w(tag, "Cannot start protection without permissions")
            return
        }
        
        viewModelScope.launch {
            try {
                val intent = Intent(getApplication(), FaceDetectionService::class.java)
                getApplication<Application>().startForegroundService(intent)
                _isProtectionEnabled.value = true
                Log.i(tag, "Protection started")
                
            } catch (e: Exception) {
                Log.e(tag, "Failed to start protection", e)
            }
        }
    }
    
    /**
     * Stop privacy protection
     */
    fun stopProtection() {
        viewModelScope.launch {
            try {
                val intent = Intent(getApplication(), FaceDetectionService::class.java)
                getApplication<Application>().stopService(intent)
                _isProtectionEnabled.value = false
                Log.i(tag, "Protection stopped")
                
            } catch (e: Exception) {
                Log.e(tag, "Failed to stop protection", e)
            }
        }
    }
    
    /**
     * Update detection count when faces are detected
     */
    fun onFaceDetected(count: Int) {
        viewModelScope.launch {
            _detectionCount.value = _detectionCount.value + 1
            _lastDetectionTime.value = System.currentTimeMillis()
            
            // Save to preferences
            saveDetectionCount()
            
            Log.d(tag, "Face detection count updated: ${_detectionCount.value}")
        }
    }
    
    /**
     * Reset detection count
     */
    fun resetDetectionCount() {
        viewModelScope.launch {
            _detectionCount.value = 0
            saveDetectionCount()
        }
    }
    
    /**
     * Check if required permissions are granted
     */
    private fun checkPermissions() {
        // This would typically check actual permissions
        // For now, we'll set it to false and let MainActivity handle it
        _hasPermissions.value = false
    }
    
    /**
     * Update permissions status
     */
    fun updatePermissionsStatus(granted: Boolean) {
        _hasPermissions.value = granted
    }
    
    /**
     * Load detection count from SharedPreferences
     */
    private fun loadDetectionCount() {
        try {
            val sharedPrefs = getApplication<Application>().getSharedPreferences(
                "peekguard_prefs", 
                Application.MODE_PRIVATE
            )
            val count = sharedPrefs.getInt("detection_count", 0)
            _detectionCount.value = count
            
        } catch (e: Exception) {
            Log.e(tag, "Error loading detection count", e)
        }
    }
    
    /**
     * Save detection count to SharedPreferences
     */
    private fun saveDetectionCount() {
        try {
            val sharedPrefs = getApplication<Application>().getSharedPreferences(
                "peekguard_prefs", 
                Application.MODE_PRIVATE
            )
            sharedPrefs.edit()
                .putInt("detection_count", _detectionCount.value)
                .putLong("last_detection_time", _lastDetectionTime.value)
                .apply()
                
        } catch (e: Exception) {
            Log.e(tag, "Error saving detection count", e)
        }
    }
    
    /**
     * Get today's detection count
     */
    fun getTodayDetectionCount(): Int {
        // This would calculate detections for today only
        // For simplicity, returning total count
        return _detectionCount.value
    }
    
    /**
     * Get this week's detection count
     */
    fun getWeekDetectionCount(): Int {
        // This would calculate detections for this week only
        // For simplicity, returning total count
        return _detectionCount.value
    }
    
    override fun onCleared() {
        super.onCleared()
        Log.d(tag, "MainViewModel cleared")
    }
}