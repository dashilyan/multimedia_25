package com.example.artefactdetect

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.core.MatOfPoint

fun detectArtifacts(bitmap: Bitmap): List<Rect> {
    val mat = BitmapToMat(bitmap)
    val grayMat = Mat()
    Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_BGR2GRAY)

    // Простая обработка для выделения контуров
    val thresholdMat = Mat()
    Imgproc.threshold(grayMat, thresholdMat, 127.0, 255.0, Imgproc.THRESH_BINARY)

    // Находим контуры
    val contours = mutableListOf<MatOfPoint>()
    Imgproc.findContours(thresholdMat, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

    val boundingBoxes = mutableListOf<Rect>()

    // Фильтруем только большие прямоугольники
    contours.forEach { contour ->
        val rect = Imgproc.boundingRect(contour)
        // Фильтруем слишком маленькие прямоугольники
        if (rect.width > 50 && rect.height > 50) {
            boundingBoxes.add(rect)
        }
    }

    return boundingBoxes
}


fun BitmapToMat(bitmap: Bitmap): Mat {
    val mat = Mat(bitmap.height, bitmap.width, CvType.CV_8UC4)
    Utils.bitmapToMat(bitmap, mat)
    return mat
}

fun drawBoundingBoxes(mat: Mat, boundingBoxes: List<Rect>) {
    // Рисуем прямоугольники вокруг найденных артефактов
    for (rect in boundingBoxes) {
        Imgproc.rectangle(mat, rect.tl(), rect.br(), Scalar(0.0, 255.0, 0.0), 2)
    }
}
