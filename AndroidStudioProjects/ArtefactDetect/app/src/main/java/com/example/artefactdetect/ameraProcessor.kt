package com.example.artefactdetect

import android.graphics.Bitmap
import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer

class CameraProcessor(
    private val artifactSimulator: ArtifactSimulator,
    private val onFrameProcessed: (Bitmap) -> Unit,
    private var artifactIntensity: Float = 0.5f
) : ImageAnalysis.Analyzer {

    override fun analyze(image: ImageProxy) {
        try {
            val mat = image.toMat()
            val processedMat = artifactSimulator.applyArtifacts(mat, artifactIntensity)
            val resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(processedMat, resultBitmap)

            onFrameProcessed(resultBitmap)

            mat.release()
            processedMat.release()
        } finally {
            image.close()
        }
    }

    fun updateIntensity(newIntensity: Float) {
        artifactIntensity = newIntensity.coerceIn(0f, 1f)
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