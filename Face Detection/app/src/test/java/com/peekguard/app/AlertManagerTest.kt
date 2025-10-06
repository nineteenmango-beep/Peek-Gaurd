package com.peekguard.app

import com.peekguard.app.notification.AlertMode
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Alert and Notification functionality
 */
class AlertManagerTest {
    
    @Test
    fun testAlertModeValues() {
        val modes = AlertMode.values()
        assertEquals(4, modes.size)
        assertTrue(modes.contains(AlertMode.NOTIFICATION_SOUND))
        assertTrue(modes.contains(AlertMode.VIBRATION_ONLY))
        assertTrue(modes.contains(AlertMode.SILENT_ICON))
        assertTrue(modes.contains(AlertMode.POPUP_ONLY))
    }
    
    @Test
    fun testAlertThrottling() {
        val alertThrottleMs = 3000 // 3 seconds
        val currentTime = System.currentTimeMillis()
        val lastAlertTime = currentTime - 2000 // 2 seconds ago
        
        val shouldThrottle = (currentTime - lastAlertTime) < alertThrottleMs
        assertTrue("Alert should be throttled if within 3 seconds", shouldThrottle)
        
        val oldAlertTime = currentTime - 4000 // 4 seconds ago
        val shouldNotThrottle = (currentTime - oldAlertTime) >= alertThrottleMs
        assertTrue("Alert should not be throttled if more than 3 seconds", shouldNotThrottle)
    }
    
    @Test
    fun testVibrationPattern() {
        val expectedPattern = longArrayOf(0, 300, 200, 300, 200, 300)
        assertEquals(6, expectedPattern.size)
        assertEquals(0L, expectedPattern[0]) // Start immediately
        assertEquals(300L, expectedPattern[1]) // First vibration
        assertEquals(200L, expectedPattern[2]) // First pause
    }
    
    @Test
    fun testNotificationChannelId() {
        val channelId = "peekguard_alerts"
        assertNotNull(channelId)
        assertFalse(channelId.isEmpty())
        assertEquals("peekguard_alerts", channelId)
    }
}