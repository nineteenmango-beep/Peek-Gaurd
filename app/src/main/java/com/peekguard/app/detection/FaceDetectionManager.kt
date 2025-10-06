package com.peekguard.app.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.media.Image
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * Face Detection Manager using ML Kit
 * Handles real-time face detection from camera frames
 */
class FaceDetectionManager(
    private val context: Context,
    private val onFaceDetected: (Int) -> Unit
) {
    
    private val tag = "FaceDetectionManager"
    
    // Face detector configuration
    private val faceDetectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
        .setMinFaceSize(0.15f) // Minimum face size (15% of image)
        .enableTracking()
        .build()
    
    private val faceDetector: FaceDetector = FaceDetection.getClient(faceDetectorOptions)
    
    // Detection settings
    var sensitivityLevel: SensitivityLevel = SensitivityLevel.MEDIUM
        set(value) {
            field = value
            updateDetectionThreshold()
        }
    
    private var detectionThreshold: Float = 0.7f
    private var minFaceSize: Float = 0.15f
    
    // Image analyzer for CameraX
    val imageAnalyzer = ImageAnalysis.Analyzer { imageProxy ->
        processImageProxy(imageProxy)
    }
    
    private fun updateDetectionThreshold() {
        when (sensitivityLevel) {
            SensitivityLevel.LOW -> {
                detectionThreshold = 0.8f
                minFaceSize = 0.20f
            }
            SensitivityLevel.MEDIUM -> {
                detectionThreshold = 0.7f
                minFaceSize = 0.15f
            }
            SensitivityLevel.HIGH -> {
                detectionThreshold = 0.6f
                minFaceSize = 0.10f
            }
        }
        
        // Update detector options
        val newOptions = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(minFaceSize)
            .enableTracking()
            .build()
    }
    
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            
            faceDetector.process(inputImage)
                .addOnSuccessListener { faces ->
                    processFaceDetectionResult(faces)
                }
                .addOnFailureListener { e ->
                    Log.e(tag, "Face detection failed", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
    
    private fun processFaceDetectionResult(faces: List<Face>) {
        val validFaces = faces.filter { face ->
            // Filter faces based on confidence and size
            val faceSize = face.boundingBox.width() * face.boundingBox.height()
            val imageSize = 640 * 480 // Approximate camera resolution
            val relativeFaceSize = faceSize.toFloat() / imageSize.toFloat()
            
            relativeFaceSize >= minFaceSize && isValidFace(face)
        }
        
        Log.d(tag, "Detected ${validFaces.size} valid faces")
        
        // Trigger alert if more than one face detected
        if (validFaces.size > 1) {
            onFaceDetected(validFaces.size)
        }
    }
    
    private fun isValidFace(face: Face): Boolean {
        // Additional validation for face quality
        val boundingBox = face.boundingBox
        
        // Check if face is reasonably sized and positioned
        val minDimension = 50 // Minimum pixels for face width/height
        return boundingBox.width() >= minDimension && 
               boundingBox.height() >= minDimension &&
               boundingBox.left >= 0 && 
               boundingBox.top >= 0
    }
    
    /**
     * Process bitmap directly (for testing or manual image processing)
     */
    fun processBitmap(bitmap: Bitmap, onResult: (Int) -> Unit) {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        
        faceDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                val validFaces = faces.filter { isValidFace(it) }
                onResult(validFaces.size)
            }
            .addOnFailureListener { e ->
                Log.e(tag, "Bitmap face detection failed", e)
                onResult(0)
            }
    }
    
    /**
     * Release resources
     */
    fun release() {
        try {
            faceDetector.close()
        } catch (e: Exception) {
            Log.e(tag, "Error releasing face detector", e)
        }
    }
    
    /**
     * Convert Image to Bitmap (utility function)
     */
    private fun imageToBitmap(image: Image): Bitmap? {
        return try {
            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            
            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()
            
            val nv21 = ByteArray(ySize + uSize + vSize)
            
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)
            
            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val outputStream = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, outputStream)
            val imageBytes = outputStream.toByteArray()
            BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
        } catch (e: Exception) {
            Log.e(tag, "Error converting image to bitmap", e)
            null
        }
    }
}

/**
 * Sensitivity levels for face detection
 */
enum class SensitivityLevel {
    LOW,    // Less sensitive, fewer false positives
    MEDIUM, // Balanced sensitivity
    HIGH    // More sensitive, may have more false positives
}