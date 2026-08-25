package com.yagnik.birdrepeller

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.compose.foundation.Canvas
import kotlin.math.max
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.IntSize
import com.yagnik.birdrepeller.detection.BirdDetector
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var birdDetector: BirdDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        birdDetector = BirdDetector(this)
        
        // Initialize detector on the same background thread it will be used on
        cameraExecutor.execute {
            birdDetector.setup()
        }

        setContent {
            BirdRepellerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionGateway {
                        CameraScreen(cameraExecutor, birdDetector)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        birdDetector.close()
    }
}

@Composable
fun BirdRepellerTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

@Composable
fun PermissionGateway(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var permissionStatus by remember {
        mutableStateOf(
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                PermissionStatus.Granted
            } else {
                PermissionStatus.Initial
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            permissionStatus = if (isGranted) {
                PermissionStatus.Granted
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(context as Activity, Manifest.permission.CAMERA)) {
                    PermissionStatus.Denied
                } else {
                    PermissionStatus.PermanentlyDenied
                }
            }
        }
    )

    LaunchedEffect(Unit) {
        if (permissionStatus == PermissionStatus.Initial) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    when (permissionStatus) {
        PermissionStatus.Granted -> content()
        PermissionStatus.Initial -> { /* Waiting for launcher */ }
        PermissionStatus.Denied -> {
            PermissionDeniedScreen(
                message = "Camera access is essential for detecting birds on your solar panels. Please grant permission to continue.",
                onRetry = { launcher.launch(Manifest.permission.CAMERA) }
            )
        }
        PermissionStatus.PermanentlyDenied -> {
            PermissionDeniedScreen(
                message = "Camera access has been permanently denied. Please enable it in the system settings to use the app.",
                buttonText = "Open Settings",
                onRetry = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}

enum class PermissionStatus {
    Initial, Granted, Denied, PermanentlyDenied
}

@Composable
fun PermissionDeniedScreen(
    message: String,
    buttonText: String = "Try Again",
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text(text = buttonText)
        }
    }
}

@Composable
fun CameraScreen(cameraExecutor: ExecutorService, birdDetector: BirdDetector) {
    val context = LocalContext.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    var lastLatency by remember { mutableStateOf<Long?>(null) }
    var detections by remember { mutableStateOf<List<BirdDetector.DetectionResult>>(emptyList()) }
    var bitmapSize by remember { mutableStateOf<IntSize?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(imageCapture)

        if (!birdDetector.isInitialized()) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.align(Alignment.TopCenter).padding(16.dp)
            ) {
                Text(
                    "Error: Model failed to load",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        DetectionOverlay(detections, bitmapSize)

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            lastLatency?.let {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    shape = MaterialTheme.shapes.small
                ) {
                    val statusText = if (detections.isEmpty()) {
                        "Last capture: ${it}ms | No objects found"
                    } else {
                        val topLabels = detections.flatMap { d -> d.categories }.take(3).joinToString { "${it.first} (${(it.second * 100).toInt()}%)" }
                        "Last capture: ${it}ms | Seen: $topLabels"
                    }
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    capturePhoto(context, imageCapture, cameraExecutor) { latency, file ->
                        // All detector work MUST happen on the same thread (cameraExecutor)
                        var bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        if (bitmap != null) {
                            bitmap = rotateBitmapIfRequired(bitmap, file)
                            
                            val maxDimension = 1024
                            if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
                                val scale = maxDimension.toFloat() / max(bitmap.width, bitmap.height)
                                bitmap = Bitmap.createScaledBitmap(
                                    bitmap, 
                                    (bitmap.width * scale).toInt(), 
                                    (bitmap.height * scale).toInt(), 
                                    true
                                )
                            }
                            
                            val results = birdDetector.detect(bitmap)
                            val size = IntSize(bitmap.width, bitmap.height)

                            // Update UI state on the main thread
                            (context as Activity).runOnUiThread {
                                lastLatency = latency
                                bitmapSize = size
                                detections = results
                            }
                        }
                    }
                }
            ) {
                Text("Capture & Detect")
            }
        }
    }
}

@Composable
fun DetectionOverlay(detections: List<BirdDetector.DetectionResult>, bitmapSize: IntSize?) {
    if (bitmapSize == null) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        detections.forEach { detection ->
            val box = detection.boundingBox
            
            // Map bitmap pixels to canvas coordinates
            val scaleX = size.width / bitmapSize.width
            val scaleY = size.height / bitmapSize.height
            
            val left = box.left * scaleX
            val top = box.top * scaleY
            val width = box.width() * scaleX
            val height = box.height() * scaleY

            drawRect(
                color = if (detection.isBird) Color.Red else Color.Green,
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = 4f)
            )
        }
    }
}

private fun rotateBitmapIfRequired(bitmap: android.graphics.Bitmap, file: File): android.graphics.Bitmap {
    val ei = ExifInterface(file.absolutePath)
    val orientation = ei.getAttributeInt(
        ExifInterface.TAG_ORIENTATION,
        ExifInterface.ORIENTATION_NORMAL
    )

    return when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
        else -> bitmap
    }
}

private fun rotateImage(source: android.graphics.Bitmap, angle: Float): android.graphics.Bitmap {
    val matrix = Matrix()
    matrix.postRotate(angle)
    return android.graphics.Bitmap.createBitmap(
        source, 0, 0, source.width, source.height,
        matrix, true
    )
}

private fun capturePhoto(
    context: android.content.Context,
    imageCapture: ImageCapture,
    executor: ExecutorService,
    onResult: (Long, File) -> Unit
) {
    val startTime = System.currentTimeMillis()
    val name = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
        .format(startTime)
    val file = File(context.filesDir, "capture_$name.jpg")

    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val latency = System.currentTimeMillis() - startTime
                Log.d("MainActivity", "Photo saved to: ${file.absolutePath} (Size: ${file.length()} bytes, Latency: ${latency}ms)")
                onResult(latency, file)
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("MainActivity", "Photo capture failed: ${exception.message}", exception)
            }
        }
    )
}

@Composable
fun CameraPreview(imageCapture: ImageCapture) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                } catch (e: Exception) {
                    Log.e("CameraPreview", "Use case binding failed", e)
                }
            }, executor)
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}
