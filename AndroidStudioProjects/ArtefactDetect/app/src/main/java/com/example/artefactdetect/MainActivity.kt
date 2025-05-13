// MainActivity.kt
package com.example.artefactdetect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val context = LocalContext.current
                var opencvInitialized by remember { mutableStateOf(false) }
                var cameraProcessor by remember { mutableStateOf<CameraProcessor?>(null) }
                var fixEnabled by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    // Инициализация OpenCV
                    opencvInitialized = OpenCVLoader.initDebug()
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    if (!opencvInitialized) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Инициализация OpenCV...",
                                color = Color.White
                            )
                        }
                    } else {
                        // Камерный превью с получением обработчика
                        CameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            onImageProcessorCreated = { processor ->
                                cameraProcessor = processor
                                processor.setFixEnabled(fixEnabled)
                            }
                        )

                        // Заголовок сверху
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 48.dp)
                                .background(
                                    color = Color(0xFFFFC0CB),
                                    shape = RoundedCornerShape(50)
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Рабочая Д.А. Вариант 19",
                                color = Color.Black,
                                fontSize = 18.sp
                            )
                        }

                        // Кнопка включения/отключения фиксации внизу по центру
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 32.dp)
                        ) {
                            Button(
                                onClick = {
                                    fixEnabled = !fixEnabled
                                    cameraProcessor?.setFixEnabled(fixEnabled)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFC0CB)
                                )
                            ) {
                                Text(
                                    text = if (fixEnabled) "Отключить исправление" else "Включить исправление",
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
