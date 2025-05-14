package com.example.artefactdetect

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import com.example.artefactdetect.ArtifactFix

class CameraProcessor(
    private val onFrameProcessed: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = ArtifactDetect()
    private val handler = Handler(Looper.getMainLooper())
    @Volatile private var isBusy = false
    @Volatile private var fixEnabled = true

    /**
     * Позволяет включить или отключить этап исправления артефактов.
     */
    fun setFixEnabled(enabled: Boolean) {
        fixEnabled = enabled
        // если отключаем фиксацию, снимем флаг занятости, чтобы анализ возобновился
        if (!fixEnabled) {
            isBusy = false
        }
    }

    override fun analyze(image: ImageProxy) {
        // Пропускаем, если все еще заняты показом предыдущего эффекта
        if (isBusy) {
            image.close()
            return
        }

        var rotatedMat: Mat? = null
        try {
            // Конвертация и поворот кадра
            val mat = image.toMat()
            rotatedMat = Mat()
            when (image.imageInfo.rotationDegrees) {
                90 -> Core.rotate(mat, rotatedMat, Core.ROTATE_90_CLOCKWISE)
                180 -> Core.rotate(mat, rotatedMat, Core.ROTATE_180)
                270 -> Core.rotate(mat, rotatedMat, Core.ROTATE_90_COUNTERCLOCKWISE)
                else -> mat.copyTo(rotatedMat)
            }
            mat.release()

            // Копируем оригинал для детекции и фиксации
            val originalMat = rotatedMat.clone()

            // Помечаем занятось, чтобы ждать завершения этапов
            isBusy = true

            // ЭТАП 1: детекция и отрисовка
            val detected = detector.detect(originalMat)
            val detectionMat = originalMat.clone()
            drawArtifacts(detectionMat, detected)
            onFrameProcessed(matToBitmap(detectionMat))
            detectionMat.release()

            // ЭТАП 2: исправление (если включено) и отрисовка после задержки
            if (fixEnabled) {
                handler.postDelayed({
                    try {
                        val fixedMat = originalMat.clone()
                        for (artifact in detected) {
                            when {
                                artifact.label.startsWith("Noise") -> ArtifactFix.correctNoise(fixedMat, artifact.rect)
                                artifact.label.startsWith("Blur") -> ArtifactFix.correctBlur(fixedMat, artifact.rect)
                                artifact.label.startsWith("Pixelization") -> ArtifactFix.correctPixelization(fixedMat, artifact.rect)
                            }
                        }
                        drawArtifacts(fixedMat, detected)
                        onFrameProcessed(matToBitmap(fixedMat))
                        fixedMat.release()
                    } catch (e: Exception) {
                        Log.e("CameraProcessor", "Error during fix stage", e)
                    }
                    // Удерживаем эффект фикса еще 500 мс, затем освобождаем
                    handler.postDelayed({ isBusy = false }, 250L)
                    originalMat.release()
                }, 200L)
            } else {
                // Если фиксация отключена, сбрасываем занятость сразу
                isBusy = false
                originalMat.release()
            }

        } catch (e: Exception) {
            Log.e("CameraProcessor", "Error processing image", e)
            isBusy = false
        } finally {
            rotatedMat?.release()
            image.close()
        }
    }

    private fun drawArtifacts(mat: Mat, artifacts: List<ArtifactDetect.DetectedArtifact>) {
        for (artifact in artifacts) {
            val color = when {
                artifact.label.startsWith("Pixelization") -> Scalar(255.0, 0.0, 0.0)
                artifact.label.startsWith("Noise") -> Scalar(0.0, 255.0, 255.0)
                artifact.label.startsWith("Blur") -> Scalar(0.0, 0.0, 255.0)
                else -> Scalar(0.0, 255.0, 0.0)
            }
            Imgproc.rectangle(mat, artifact.rect.tl(), artifact.rect.br(), color, 2)
            val baseLabel = artifact.label.substringBefore(' ')
            Imgproc.putText(
                mat,
                baseLabel,
                Point(artifact.rect.x.toDouble(), artifact.rect.y.toDouble() - 8),
                Imgproc.FONT_HERSHEY_SIMPLEX,
                0.6,
                color,
                1
            )
        }
    }

    private fun matToBitmap(mat: Mat): Bitmap {
        val bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bmp)
        return bmp
    }

    private fun ImageProxy.toMat(): Mat {
        // YUV -> RGB конвертация
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
        val yuvMat = Mat(height + height / 2, width, CvType.CV_8UC1)
        yuvMat.put(0, 0, nv21)
        val rgbMat = Mat()
        Imgproc.cvtColor(yuvMat, rgbMat, Imgproc.COLOR_YUV2RGB_NV21, 3)
        yuvMat.release()
        return rgbMat
    }
}