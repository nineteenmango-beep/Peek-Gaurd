package com.peekguard.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Detection Events
 */
@Dao
interface DetectionEventDao {
    
    @Query("SELECT * FROM detection_events ORDER BY timestamp DESC")
    fun getAllDetections(): Flow<List<DetectionEvent>>
    
    @Query("SELECT * FROM detection_events WHERE timestamp >= :startTime ORDER BY timestamp DESC")
    fun getDetectionsSince(startTime: Long): Flow<List<DetectionEvent>>
    
    @Query("SELECT COUNT(*) FROM detection_events WHERE timestamp >= :startTime")
    suspend fun getDetectionCountSince(startTime: Long): Int
    
    @Query("SELECT COUNT(*) FROM detection_events WHERE date(timestamp/1000, 'unixepoch') = date('now')")
    suspend fun getTodayDetectionCount(): Int
    
    @Query("SELECT COUNT(*) FROM detection_events WHERE date(timestamp/1000, 'unixepoch') >= date('now', '-7 days')")
    suspend fun getWeekDetectionCount(): Int
    
    @Query("SELECT * FROM detection_events WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getDetectionsBetween(startTime: Long, endTime: Long): List<DetectionEvent>
    
    @Insert
    suspend fun insertDetection(detection: DetectionEvent): Long
    
    @Insert
    suspend fun insertDetections(detections: List<DetectionEvent>)
    
    @Delete
    suspend fun deleteDetection(detection: DetectionEvent)
    
    @Query("DELETE FROM detection_events WHERE timestamp < :cutoffTime")
    suspend fun deleteOldDetections(cutoffTime: Long): Int
    
    @Query("DELETE FROM detection_events")
    suspend fun deleteAllDetections()
}

/**
 * Data Access Object for User Preferences
 */
@Dao
interface UserPreferencesDao {
    
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getPreferences(): Flow<UserPreferences?>
    
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    suspend fun getPreferencesSync(): UserPreferences?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(preferences: UserPreferences)
    
    @Update
    suspend fun updatePreferences(preferences: UserPreferences)
    
    @Query("UPDATE user_preferences SET isProtectionEnabled = :enabled WHERE id = 1")
    suspend fun updateProtectionStatus(enabled: Boolean)
    
    @Query("UPDATE user_preferences SET sensitivityLevel = :level WHERE id = 1")
    suspend fun updateSensitivityLevel(level: String)
    
    @Query("UPDATE user_preferences SET alertMode = :mode WHERE id = 1")
    suspend fun updateAlertMode(mode: String)
}

/**
 * Data Access Object for Detection Statistics
 */
@Dao
interface DetectionStatsDao {
    
    @Query("SELECT * FROM detection_stats ORDER BY date DESC")
    fun getAllStats(): Flow<List<DetectionStats>>
    
    @Query("SELECT * FROM detection_stats WHERE date = :date")
    suspend fun getStatsForDate(date: String): DetectionStats?
    
    @Query("SELECT * FROM detection_stats WHERE date >= :startDate ORDER BY date DESC")
    suspend fun getStatsFrom(startDate: String): List<DetectionStats>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: DetectionStats)
    
    @Update
    suspend fun updateStats(stats: DetectionStats)
    
    @Query("DELETE FROM detection_stats WHERE date < :cutoffDate")
    suspend fun deleteOldStats(cutoffDate: String): Int
}