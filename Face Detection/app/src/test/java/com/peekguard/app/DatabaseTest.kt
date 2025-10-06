package com.peekguard.app

import com.peekguard.app.data.DetectionEvent
import com.peekguard.app.data.UserPreferences
import com.peekguard.app.detection.SensitivityLevel
import com.peekguard.app.notification.AlertMode
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Database entities and data models
 */
class DatabaseTest {
    
    @Test
    fun testDetectionEventCreation() {
        val timestamp = System.currentTimeMillis()
        val detection = DetectionEvent(
            id = 1,
            faceCount = 2,
            timestamp = timestamp,
            duration = 5000,
            confidence = 0.85f
        )
        
        assertEquals(1L, detection.id)
        assertEquals(2, detection.faceCount)
        assertEquals(timestamp, detection.timestamp)
        assertEquals(5000L, detection.duration)
        assertEquals(0.85f, detection.confidence, 0.01f)
    }
    
    @Test
    fun testUserPreferencesDefaults() {
        val preferences = UserPreferences()
        
        assertEquals(1, preferences.id)
        assertEquals(SensitivityLevel.MEDIUM, preferences.sensitivityLevel)
        assertEquals(AlertMode.NOTIFICATION_SOUND, preferences.alertMode)
        assertTrue(preferences.soundEnabled)
        assertTrue(preferences.vibrationEnabled)
        assertFalse(preferences.darkModeEnabled)
        assertFalse(preferences.isProtectionEnabled)
    }
    
    @Test
    fun testDetectionEventValidation() {
        val validDetection = DetectionEvent(
            faceCount = 2,
            timestamp = System.currentTimeMillis(),
            confidence = 0.9f
        )
        
        assertTrue("Face count should be positive", validDetection.faceCount > 0)
        assertTrue("Timestamp should be valid", validDetection.timestamp > 0)
        assertTrue("Confidence should be between 0 and 1", 
                  validDetection.confidence >= 0.0f && validDetection.confidence <= 1.0f)
    }
    
    @Test
    fun testPreferencesValidation() {
        val preferences = UserPreferences(
            sensitivityLevel = SensitivityLevel.HIGH,
            alertMode = AlertMode.VIBRATION_ONLY,
            soundEnabled = false
        )
        
        assertEquals(SensitivityLevel.HIGH, preferences.sensitivityLevel)
        assertEquals(AlertMode.VIBRATION_ONLY, preferences.alertMode)
        assertFalse(preferences.soundEnabled)
    }
    
    @Test
    fun testTimestampLogic() {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000)
        val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000)
        
        assertTrue("One day ago should be less than now", oneDayAgo < now)
        assertTrue("One week ago should be less than one day ago", oneWeekAgo < oneDayAgo)
        
        val daysDifference = (now - oneDayAgo) / (24 * 60 * 60 * 1000)
        assertEquals(1L, daysDifference)
    }
}