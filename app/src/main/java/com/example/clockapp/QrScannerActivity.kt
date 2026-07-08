package com.example.clockapp

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class QrScannerActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private var targetBarcode: String? = null
    private var isSettingMode = false
    
    private lateinit var scanLine: View
    private lateinit var tvScanStatus: TextView
    private lateinit var fabFlashlight: FloatingActionButton
    
    private var camera: Camera? = null
    private var isFlashOn = false
    private var scanLineAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        // Initialize views
        viewFinder = findViewById(R.id.viewFinder)
        scanLine = findViewById(R.id.scanLine)
        tvScanStatus = findViewById(R.id.tvScanStatus)
        fabFlashlight = findViewById(R.id.fabFlashlight)
        
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Get intent data
        targetBarcode = intent.getStringExtra("TARGET_BARCODE")
        isSettingMode = intent.getBooleanExtra("IS_SETTING_MODE", false)

        // Set initial status text
        if (isSettingMode) {
            tvScanStatus.text = "MISSION: SET TARGET QR"
        } else {
            tvScanStatus.text = "MISSION: SCAN TARGET QR"
        }

        fabFlashlight.setOnClickListener {
            toggleFlashlight()
        }

        startCamera()
        startScanLineAnimation()
    }

    private fun startScanLineAnimation() {
        // The overlay is 260dp. We move the line within that area.
        val distance = 230f * resources.displayMetrics.density
        scanLineAnimator = ObjectAnimator.ofFloat(scanLine, "translationY", 0f, distance).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = LinearInterpolator()
            start()
        }
    }

    private fun toggleFlashlight() {
        camera?.let {
            isFlashOn = !isFlashOn
            it.cameraControl.enableTorch(isFlashOn)
            
            // Visual feedback on the button
            fabFlashlight.supportBackgroundTintList = ContextCompat.getColorStateList(
                this, if (isFlashOn) android.R.color.holo_orange_light else android.R.color.darker_gray
            )
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "Camera error: ${exc.message}", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val value = barcodes[0].rawValue ?: return@addOnSuccessListener
                        handleBarcodeDetected(value)
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun handleBarcodeDetected(value: String) {
        if (isSettingMode) {
            runOnUiThread {
                tvScanStatus.text = "TARGET SET SUCCESS!"
                tvScanStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            }
            val resultIntent = Intent().apply {
                putExtra("SCANNED_BARCODE", value)
            }
            setResult(RESULT_OK, resultIntent)
            // Small delay to show success UI before closing
            viewFinder.postDelayed({ finish() }, 1000)
        } else {
            if (value == targetBarcode) {
                runOnUiThread {
                    tvScanStatus.text = "MATCH FOUND!"
                    tvScanStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
                }
                stopAlarmService()
                viewFinder.postDelayed({ finish() }, 1000)
            } else {
                runOnUiThread {
                    tvScanStatus.text = "WRONG QR! SEARCHING..."
                    tvScanStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                }
                // Reset status after a delay
                tvScanStatus.postDelayed({
                    if (!isFinishing) {
                        tvScanStatus.text = "MISSION: SCAN TARGET QR"
                        tvScanStatus.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    }
                }, 2000)
            }
        }
    }

    private fun stopAlarmService() {
        val intent = Intent(this, AlarmService::class.java)
        stopService(intent)
        runOnUiThread {
            Toast.makeText(this, "Good Morning! Mission Accomplished.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scanLineAnimator?.cancel()
        cameraExecutor.shutdown()
    }

    override fun onBackPressed() {
        if (!isSettingMode) {
            Toast.makeText(this, "Complete Mission to Stop Alarm!", Toast.LENGTH_SHORT).show()
        } else {
            super.onBackPressed()
        }
    }
}
