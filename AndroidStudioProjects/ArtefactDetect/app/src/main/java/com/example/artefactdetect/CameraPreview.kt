package com.example.artefactdetect

import android.graphics.Bitmap
import android.util.Log
import android.view.Surface
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.camera.core.ImageProxy
import androidx.lifecycle.LifecycleOwner
import org.opencv.android.Utils
import org.opencv.core.Mat
import org.opencv.core.Rect

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalContext.current as LifecycleOwner  // Получаем LifecycleOwner через LocalContext
    val previewView = remember { PreviewView(context) }
    val boundingBoxes = remember { mutableStateListOf<Rect>() }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(1280, 720))
                .build()

            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                // Получаем Bitmap из ImageProxy
                val bitmap = imageProxy.toBitmap()

                // Обрабатываем кадр и получаем найденные артефакты
                boundingBoxes.clear()
                boundingBoxes.addAll(detectArtifacts(bitmap))

                // Закрываем imageProxy после обработки
                imageProxy.close()
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
            } catch (exc: Exception) {
                Log.e("CameraPreview", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            cameraProviderFuture.get().unbindAll()
        }
    }

    Box(modifier = modifier) {
        // Отображаем видео поток
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Отображаем рамки поверх видео
        Box(modifier = Modifier.fillMaxSize()) {
            // Рисуем прямоугольники вокруг артефактов
            Canvas(modifier = Modifier.fillMaxSize()) {
                boundingBoxes.forEach { rect ->
                    drawRect(
                        color = Color.Green,  // Цвет рамки
                        topLeft = androidx.compose.ui.geometry.Offset(rect.x.toFloat(), rect.y.toFloat()),
                        size = androidx.compose.ui.geometry.Size(rect.width.toFloat(), rect.height.toFloat()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 5f)  // Толщина линии
                    )
                }
            }
        }
    }
}


