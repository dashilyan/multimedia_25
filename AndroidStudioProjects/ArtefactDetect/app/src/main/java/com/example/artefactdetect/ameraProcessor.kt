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

    fun requestArtifact() {
        shouldAddArtifact = true
    }

    override fun analyze(image: ImageProxy) {
        try {
            val mat = image.toMat()
            val rotatedMat = Mat()

            // Исправленный поворот изображения
            if (image.imageInfo.rotationDegrees == 90) {
                Core.rotate(mat, rotatedMat, Core.ROTATE_90_CLOCKWISE)
            } else {
                // Если поворот не 90 градусов, используем стандартное преобразование
                Core.rotate(mat, rotatedMat, Core.ROTATE_180)
            }

            // Зеркальное отражение по вертикали (опционально)
            Core.flip(rotatedMat, rotatedMat, 1)

            mat.release()

            if (shouldAddArtifact) {
                artifactSimulator.addRandomRectArtifact(rotatedMat.size())
                shouldAddArtifact = false
            }

            val processedMat = artifactSimulator.applyAllArtifacts(rotatedMat)

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