package com.example.artefactdetect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import org.opencv.android.OpenCVLoader
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val context = LocalContext.current
            val opencvInitialized = remember { mutableStateOf(false) }
            val cameraProcessor = remember { mutableStateOf<CameraProcessor?>(null) }

            LaunchedEffect(Unit) {
                // Инициализация OpenCV
                val success = OpenCVLoader.initDebug()
                opencvInitialized.value = success
            }

            if (!opencvInitialized.value) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Инициализация OpenCV...",
                        color = Color.White,
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f))
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        onImageProcessorCreated = { processor ->
                            cameraProcessor.value = processor
                        }
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 48.dp)
                    ) {
                        Text(
                            text = "Рабочая Д.А. Вариант 19",
                            color = Color.White,
                            fontSize = 24.sp,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}
