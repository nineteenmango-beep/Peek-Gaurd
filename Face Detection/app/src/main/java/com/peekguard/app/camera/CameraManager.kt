package com.peekguard.app.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.peekguard.app.detection.FaceDetectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Camera Manager for PeekGuard
 * Handles front camera access and frame processing for face detection
 */
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val faceDetectionManager: FaceDetectionManager
) {
    
    private val tag = "CameraManager"
    
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var camera: Camera? = null
    
    // Camera executor for background processing
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    // Camera configuration
    private val targetResolution = Size(640, 480) // Lower resolution for better performance
    private val targetFrameRate = 15 // FPS - balance between detection speed and battery
    
    var isRunning: Boolean = false
        private set
    
    /**
     * Initialize camera and start face detection
     */
    fun startCamera(onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {}) {
        if (!hasCameraPermission()) {
            onError(SecurityException("Camera permission not granted"))
            return
        }
        
        if (isRunning) {
            Log.w(tag, "Camera is already running")
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProvider = cameraProviderFuture.get()
                
                setupCamera()
                isRunning = true
                onSuccess()
                Log.i(tag, "Camera started successfully")
                
            } catch (e: Exception) {
                Log.e(tag, "Failed to start camera", e)
                onError(e)
            }
        }
    }
    
    /**
     * Stop camera and release resources
     */
    fun stopCamera() {
        if (!isRunning) {
            Log.w(tag, "Camera is not running")
            return
        }
        
        try {
            cameraProvider?.unbindAll()
            camera = null
            imageAnalysis = null
            isRunning = false
            Log.i(tag, "Camera stopped successfully")
            
        } catch (e: Exception) {
            Log.e(tag, "Error stopping camera", e)
        }
    }
    
    /**
     * Setup camera with front-facing lens selector and image analysis
     */
    private fun setupCamera() {
        val cameraProvider = this.cameraProvider ?: return
        
        // Select front camera
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
            .build()
        
        // Configure image analysis
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(targetResolution)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor, faceDetectionManager.imageAnalyzer)
            }
        
        try {
            // Unbind any existing use cases
            cameraProvider.unbindAll()
            
            // Bind camera to lifecycle with image analysis
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageAnalysis
            )
            
            Log.i(tag, "Camera bound to lifecycle successfully")
            
        } catch (e: Exception) {
            Log.e(tag, "Failed to bind camera to lifecycle", e)
            throw e
        }
    }
    
    /**
     * Check if camera permission is granted
     */
    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Get camera capabilities and info
     */
    fun getCameraInfo(): CameraInfo? {
        return camera?.cameraInfo
    }
    
    /**
     * Check if front camera is available
     */
    fun isFrontCameraAvailable(): Boolean {
        return try {
            val cameraProvider = this.cameraProvider ?: return false
            cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
        } catch (e: Exception) {
            Log.e(tag, "Error checking front camera availability", e)
            false
        }
    }
    
    /**
     * Enable/disable torch (flash) if available
     */
    fun setTorchEnabled(enabled: Boolean) {
        try {
            camera?.cameraControl?.enableTorch(enabled)
        } catch (e: Exception) {
            Log.e(tag, "Error controlling torch", e)
        }
    }
    
    /**
     * Get current torch state
     */
    fun isTorchEnabled(): Boolean {
        return try {
            camera?.cameraInfo?.torchState?.value == TorchState.ON
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Restart camera with new configuration
     */
    fun restartCamera(onSuccess: () -> Unit = {}, onError: (Exception) -> Unit = {}) {
        stopCamera()
        
        // Small delay to ensure camera is fully released
        CoroutineScope(Dispatchers.Main).launch {
            kotlinx.coroutines.delay(100)
            startCamera(onSuccess, onError)
        }
    }
    
    /**
     * Release all resources
     */
    fun release() {
        stopCamera()
        cameraExecutor.shutdown()
        Log.i(tag, "Camera manager released")
    }
    
    /**
     * Camera state callback interface
     */
    interface CameraStateCallback {
        fun onCameraStarted()
        fun onCameraStopped()
        fun onCameraError(error: Exception)
    }
    
    companion object {
        /**
         * Check if device has a front camera
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
    }
}