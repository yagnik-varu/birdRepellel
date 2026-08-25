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
            .setScoreThreshold(0.10f)
            .setMaxResults(15)

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
            // Since the device is on a rooftop solar panel, 'person' or 'teddy bear' 
            // detections are almost certainly misidentified birds.
            val isBirdMatch = detection.categories.any { 
                it.label.contains("bird", ignoreCase = true) || 
                it.label.contains("pigeon", ignoreCase = true) || 
                it.label.contains("crow", ignoreCase = true) ||
                it.label.contains("person", ignoreCase = true) ||
                it.label.contains("teddy bear", ignoreCase = true) ||
                it.label.contains("kite", ignoreCase = true)
            }

            val normalizedBox = android.graphics.RectF(
                detection.boundingBox.left / bitmap.width,
                detection.boundingBox.top / bitmap.height,
                detection.boundingBox.right / bitmap.width,
                detection.boundingBox.bottom / bitmap.height
            )
            
            DetectionResult(
                boundingBox = detection.boundingBox,
                normalizedBoundingBox = normalizedBox,
                categories = categories,
                isBird = isBirdMatch
            )
        }
    }

    data class DetectionResult(
        val boundingBox: android.graphics.RectF,
        val normalizedBoundingBox: android.graphics.RectF,
        val categories: List<Pair<String, Float>>,
        val isBird: Boolean,
        val isInRoi: Boolean = false
    )
    
    fun close() {
        detector?.close()
    }
}
