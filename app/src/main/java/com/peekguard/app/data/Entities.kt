package com.peekguard.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.peekguard.app.detection.SensitivityLevel
import com.peekguard.app.notification.AlertMode

/**
 * Detection Event Entity for Room database
 * Stores information about each face detection event
 */
@Entity(tableName = "detection_events")
data class DetectionEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val faceCount: Int,
    val timestamp: Long,
    val duration: Long = 0, // How long the detection lasted
    val location: String? = null, // Optional location data
    val confidence: Float = 0.0f // Detection confidence level
)

/**
 * User Preferences Entity for Room database
 * Stores app settings and preferences
 */
@Entity(tableName = "user_preferences")
@TypeConverters(Converters::class)
data class UserPreferences(
    @PrimaryKey
    val id: Int = 1, // Single row for preferences
    val sensitivityLevel: SensitivityLevel = SensitivityLevel.MEDIUM,
    val alertMode: AlertMode = AlertMode.NOTIFICATION_SOUND,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val darkModeEnabled: Boolean = false,
    val isProtectionEnabled: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Detection Statistics Entity
 * Stores aggregated statistics for performance
 */
@Entity(tableName = "detection_stats")
data class DetectionStats(
    @PrimaryKey
    val date: String, // Date in YYYY-MM-DD format
    val totalDetections: Int = 0,
    val averageConfidence: Float = 0.0f,
    val peakDetectionTime: Long = 0, // Timestamp of peak activity
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Type converters for Room database
 */
class Converters {
    
    @TypeConverter
    fun fromSensitivityLevel(sensitivity: SensitivityLevel): String {
        return sensitivity.name
    }
    
    @TypeConverter
    fun toSensitivityLevel(sensitivity: String): SensitivityLevel {
        return SensitivityLevel.valueOf(sensitivity)
    }
    
    @TypeConverter
    fun fromAlertMode(alertMode: AlertMode): String {
        return alertMode.name
    }
    
    @TypeConverter
    fun toAlertMode(alertMode: String): AlertMode {
        return AlertMode.valueOf(alertMode)
    }
}