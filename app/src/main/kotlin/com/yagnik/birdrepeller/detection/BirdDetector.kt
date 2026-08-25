package com.yagnik.birdrepeller.detection

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.detector.ObjectDetector

class BirdDetector(private val context: Context) {

    private var detector: ObjectDetector? = null

    fun isInitialized() = detector != null

    fun setup() {
        if (detector != null) return
        
        val optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
            .setScoreThreshold(0.15f)
            .setMaxResults(10)

        val baseOptionsBuilder = BaseOptions.builder().useGpu()
        optionsBuilder.setBaseOptions(baseOptionsBuilder.build())

        try {
            detector = ObjectDetector.createFromFileAndOptions(
                context,
                "ssd_mobilenet_v1.tflite",
                optionsBuilder.build()
            )
        } catch (e: Exception) {
            Log.e("BirdDetector", "TFLite failed to load model with GPU, falling back to CPU", e)
            // Fallback to CPU
            val cpuBaseOptionsBuilder = BaseOptions.builder().setNumThreads(4)
            optionsBuilder.setBaseOptions(cpuBaseOptionsBuilder.build())
            try {
                detector = ObjectDetector.createFromFileAndOptions(
                    context,
                    "ssd_mobilenet_v1.tflite",
                    optionsBuilder.build()
                )
            } catch (e2: Exception) {
                Log.e("BirdDetector", "TFLite failed to load model even on CPU", e2)
            }
        }
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        if (detector == null) {
            Log.e("BirdDetector", "Detector is null, cannot run inference")
            return emptyList()
        }
        val image = TensorImage.fromBitmap(bitmap)
        val results = detector?.detect(image) ?: emptyList()

        Log.d("BirdDetector", "Detection ran. Raw results count: ${results.size}")
        results.forEachIndexed { index, detection ->
            val labels = detection.categories.joinToString { "${it.label} (${it.score})" }
            Log.d("BirdDetector", "Detection $index: $labels")
        }

        return results.map { detection ->
            val categories = detection.categories.map { it.label to it.score }
            val isBirdMatch = detection.categories.any { 
                it.label.contains("bird", ignoreCase = true) || 
                it.label.contains("pigeon", ignoreCase = true) || 
                it.label.contains("crow", ignoreCase = true)
            }
            
            DetectionResult(
                boundingBox = detection.boundingBox,
                categories = categories,
                isBird = isBirdMatch
            )
        }
    }

    data class DetectionResult(
        val boundingBox: android.graphics.RectF,
        val categories: List<Pair<String, Float>>,
        val isBird: Boolean
    )
    
    fun close() {
        detector?.close()
    }
}
