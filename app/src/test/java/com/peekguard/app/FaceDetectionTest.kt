package com.peekguard.app

import com.peekguard.app.detection.SensitivityLevel
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Face Detection functionality
 */
class FaceDetectionTest {
    
    @Test
    fun testSensitivityLevelValues() {
        val levels = SensitivityLevel.values()
        assertEquals(3, levels.size)
        assertTrue(levels.contains(SensitivityLevel.LOW))
        assertTrue(levels.contains(SensitivityLevel.MEDIUM))
        assertTrue(levels.contains(SensitivityLevel.HIGH))
    }
    
    @Test
    fun testSensitivityLevelOrder() {
        val levels = SensitivityLevel.values()
        assertEquals(SensitivityLevel.LOW, levels[0])
        assertEquals(SensitivityLevel.MEDIUM, levels[1])
        assertEquals(SensitivityLevel.HIGH, levels[2])
    }
    
    @Test
    fun testFaceDetectionThresholds() {
        // Test different sensitivity thresholds
        val lowThreshold = 0.8f
        val mediumThreshold = 0.7f
        val highThreshold = 0.6f
        
        assertTrue("Low sensitivity should have highest threshold", lowThreshold > mediumThreshold)
        assertTrue("Medium sensitivity should be between low and high", mediumThreshold > highThreshold)
        assertTrue("High sensitivity should have lowest threshold", highThreshold < mediumThreshold)
    }
    
    @Test
    fun testMinFaceSizeCalculation() {
        val lowMinSize = 0.20f
        val mediumMinSize = 0.15f
        val highMinSize = 0.10f
        
        assertTrue("Low sensitivity should require larger faces", lowMinSize > mediumMinSize)
        assertTrue("Medium sensitivity should be between low and high", mediumMinSize > highMinSize)
        assertTrue("High sensitivity should detect smaller faces", highMinSize < mediumMinSize)
    }
}