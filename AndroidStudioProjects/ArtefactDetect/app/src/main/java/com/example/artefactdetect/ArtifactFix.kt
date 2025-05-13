package com.example.artefactdetect

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.max

object ArtifactFix {

    fun correctNoise(mat: Mat, rect: Rect) {
        val roi = Mat(mat, rect)
        val denoised = Mat()
        Imgproc.GaussianBlur(roi, denoised, Size(5.0, 5.0), 0.0)
        denoised.copyTo(roi)
        denoised.release()
        roi.release()
    }

    fun correctBlur(mat: Mat, rect: Rect) {
        val roi = Mat(mat, rect)
        val blurred = Mat()
        val sharpened = Mat()

        Imgproc.GaussianBlur(roi, blurred, Size(0.0, 0.0), 3.0)
        Core.addWeighted(roi, 1.5, blurred, -0.5, 0.0, sharpened)

        sharpened.copyTo(roi)
        blurred.release()
        sharpened.release()
        roi.release()
    }

    fun correctPixelization(mat: Mat, rect: Rect) {
        val roi = Mat(mat, rect)
        val upscaled = Mat()
        val smoothed = Mat()

        // Увеличиваем изображение
        Imgproc.resize(roi, upscaled, Size(), 2.0, 2.0, Imgproc.INTER_LINEAR)

        // Применяем размытие для сглаживания пикселизации
        Imgproc.GaussianBlur(upscaled, smoothed, Size(3.0, 3.0), 0.0)

        // Возвращаем обратно в исходный размер
        Imgproc.resize(smoothed, roi, Size(rect.width.toDouble(), rect.height.toDouble()), 0.0, 0.0, Imgproc.INTER_LINEAR)

        upscaled.release()
        smoothed.release()
        roi.release()
    }
}
