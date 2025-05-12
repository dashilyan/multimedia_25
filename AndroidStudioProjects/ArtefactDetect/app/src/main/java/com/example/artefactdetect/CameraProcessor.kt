package com.example.artefactdetect

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

class CameraProcessor(
    private val artifactSimulator: ArtifactSimulator,
    private val onFrameProcessed: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    private var shouldAddArtifact = false
    private val detector = ArtifactDetect()

    fun requestArtifact() {
        shouldAddArtifact = true
    }

    override fun analyze(image: ImageProxy) {
        try {
            val mat = image.toMat()
            val rotatedMat = Mat()

            // Поворот изображения в зависимости от ориентации
            when (image.imageInfo.rotationDegrees) {
                90 -> Core.rotate(mat, rotatedMat, Core.ROTATE_90_CLOCKWISE)
                180 -> Core.rotate(mat, rotatedMat, Core.ROTATE_180)
                270 -> Core.rotate(mat, rotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE)
                else -> mat.copyTo(rotatedMat)
            }

            // Зеркальное отражение для фронтальной камеры
            Core.flip(rotatedMat, rotatedMat, 1)
            mat.release()

            // Добавление артефактов если нужно
            if (shouldAddArtifact) {
                artifactSimulator.addRandomRectArtifact(rotatedMat.size())
                shouldAddArtifact = false
            }

            val processedMat = artifactSimulator.applyAllArtifacts(rotatedMat)

            // Детекция артефактов
            val detected = detector.detect(processedMat)

            // Отображение результатов детекции
            for (artifact in detected) {
                val color = when (artifact.label) {
                    "Pixelization" -> Scalar(255.0, 0.0, 0.0) // Красный для пикселизации
                    "Noise" -> Scalar(0.0, 255.0, 255.0) // Желтый для шума
                    "Blur" -> Scalar(0.0, 0.0, 255.0) // Синий для размытия
                    else -> Scalar(0.0, 255.0, 0.0) // Зеленый для остальных
                }

                // Для пикселизации рисуем рамку вокруг всего изображения
                if (artifact.label.contains("Pixelization")) {
                    Imgproc.rectangle(
                        processedMat,
                        Point(0.0, 0.0),
                        Point(processedMat.width().toDouble(), processedMat.height().toDouble()),
                        color,
                        4
                    )
                } else {
                    Imgproc.rectangle(
                        processedMat,
                        artifact.rect.tl(),
                        artifact.rect.br(),
                        color,
                        2
                    )
                }

                Imgproc.putText(
                    processedMat,
                    artifact.label,
                    Point(artifact.rect.x.toDouble(), artifact.rect.y.toDouble() - 10),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.7,
                    color,
                    2
                )
            }

            // Конвертация в Bitmap и передача результата
            val resultBitmap = Bitmap.createBitmap(
                processedMat.cols(),
                processedMat.rows(),
                Bitmap.Config.ARGB_8888
            )
            Utils.matToBitmap(processedMat, resultBitmap)
            onFrameProcessed(resultBitmap)

            rotatedMat.release()
            processedMat.release()
        } catch (e: Exception) {
            Log.e("CameraProcessor", "Error processing image", e)
        } finally {
            image.close()
        }
    }

    private fun ImageProxy.toMat(): Mat {
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)

        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val mat = Mat(height + height / 2, width, CvType.CV_8UC1)
        mat.put(0, 0, nv21)

        val rgbMat = Mat()
        Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_YUV2RGB_NV21, 3)

        mat.release()
        return rgbMat
    }
}