package com.peekguard.app.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Room Database for PeekGuard
 * Handles local storage of detection events, preferences, and statistics
 */
@Database(
    entities = [
        DetectionEvent::class,
        UserPreferences::class,
        DetectionStats::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PeekGuardDatabase : RoomDatabase() {
    
    abstract fun detectionEventDao(): DetectionEventDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun detectionStatsDao(): DetectionStatsDao
    
    companion object {
        @Volatile
        private var INSTANCE: PeekGuardDatabase? = null
        
        fun getDatabase(context: Context): PeekGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PeekGuardDatabase::class.java,
                    "peekguard_database"
                )
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                
                // Initialize default preferences when database is created
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        database.userPreferencesDao().insertPreferences(
                            UserPreferences()
                        )
                    }
                }
            }
        }
    }
}

/**
 * Repository for managing data operations
 * Provides a clean API for data access across the app
 */
class PeekGuardRepository(private val database: PeekGuardDatabase) {
    
    private val detectionEventDao = database.detectionEventDao()
    private val userPreferencesDao = database.userPreferencesDao()
    private val detectionStatsDao = database.detectionStatsDao()
    
    // Detection Events
    fun getAllDetections() = detectionEventDao.getAllDetections()
    
    fun getDetectionsSince(startTime: Long) = detectionEventDao.getDetectionsSince(startTime)
    
    suspend fun getTodayDetectionCount() = detectionEventDao.getTodayDetectionCount()
    
    suspend fun getWeekDetectionCount() = detectionEventDao.getWeekDetectionCount()
    
    suspend fun insertDetection(faceCount: Int, confidence: Float = 0.0f): Long {
        val detection = DetectionEvent(
            faceCount = faceCount,
            timestamp = System.currentTimeMillis(),
            confidence = confidence
        )
        return detectionEventDao.insertDetection(detection)
    }
    
    suspend fun deleteOldDetections(daysToKeep: Int = 30): Int {
        val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
        return detectionEventDao.deleteOldDetections(cutoffTime)
    }
    
    suspend fun clearAllDetections() {
        detectionEventDao.deleteAllDetections()
    }
    
    // User Preferences
    fun getPreferences() = userPreferencesDao.getPreferences()
    
    suspend fun getPreferencesSync() = userPreferencesDao.getPreferencesSync()
    
    suspend fun updatePreferences(preferences: UserPreferences) {
        val updatedPreferences = preferences.copy(lastUpdated = System.currentTimeMillis())
        userPreferencesDao.updatePreferences(updatedPreferences)
    }
    
    suspend fun updateProtectionStatus(enabled: Boolean) {
        userPreferencesDao.updateProtectionStatus(enabled)
    }
    
    // Detection Statistics
    fun getAllStats() = detectionStatsDao.getAllStats()
    
    suspend fun updateTodayStats(additionalDetections: Int = 1) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        
        val existingStats = detectionStatsDao.getStatsForDate(today)
        
        if (existingStats != null) {
            val updatedStats = existingStats.copy(
                totalDetections = existingStats.totalDetections + additionalDetections,
                lastUpdated = System.currentTimeMillis()
            )
            detectionStatsDao.updateStats(updatedStats)
        } else {
            val newStats = DetectionStats(
                date = today,
                totalDetections = additionalDetections,
                lastUpdated = System.currentTimeMillis()
            )
            detectionStatsDao.insertStats(newStats)
        }
    }
    
    suspend fun deleteOldStats(daysToKeep: Int = 90): Int {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysToKeep)
        
        val cutoffDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(calendar.time)
        
        return detectionStatsDao.deleteOldStats(cutoffDate)
    }
}

/**
 * Database helper functions
 */
object DatabaseManager {
    
    fun getInstance(context: Context): PeekGuardRepository {
        val database = PeekGuardDatabase.getDatabase(context)
        return PeekGuardRepository(database)
    }
    
    suspend fun performMaintenance(repository: PeekGuardRepository) {
        // Clean up old data to prevent database bloat
        repository.deleteOldDetections(30) // Keep last 30 days
        repository.deleteOldStats(90) // Keep last 90 days of stats
    }
}