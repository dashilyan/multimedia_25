package com.example.artefactdetect

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.roundToInt

class ArtifactDetect {

    data class DetectedArtifact(
        val rect: Rect,
        val label: String,
        val confidence: Double = 1.0
    )

    private val noiseThreshold = 60.0
    private val minNoiseArea = 2000
    private val blurThreshold = 15.0
    private val minPixelizationLines = 10

    fun detect(frame: Mat): List<DetectedArtifact> {
        val artifacts = mutableListOf<DetectedArtifact>()
        val gray = Mat()
        try {
            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGB2GRAY)
            artifacts.addAll(detectNoise(gray, frame.size()))
            artifacts.addAll(detectBlur(gray, frame.size()))
            artifacts.addAll(detectPixelization(gray, frame.size()))
            return artifacts
        } finally {
            gray.release()
        }
    }

    private fun detectNoise(gray: Mat, size: Size): List<DetectedArtifact> {
        val artifacts = mutableListOf<DetectedArtifact>()
        val noiseMask = Mat()
        val frameArea = size.width * size.height
        try {
            Imgproc.Laplacian(gray, noiseMask, CvType.CV_64F)
            Core.convertScaleAbs(noiseMask, noiseMask)
            val binaryNoise = Mat()
            Imgproc.threshold(noiseMask, binaryNoise, noiseThreshold, 255.0, Imgproc.THRESH_BINARY)
            val contours = mutableListOf<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(binaryNoise, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)
            for (cnt in contours) {
                val rect = Imgproc.boundingRect(cnt)
                val area = rect.area()
                if (area > minNoiseArea && area < frameArea * 0.4) {
                    val roi = noiseMask.submat(rect)
                    val mean = MatOfDouble()
                    val stddev = MatOfDouble()
                    Core.meanStdDev(roi, mean, stddev)
                    val stddevValue = stddev.toArray()[0]
                    roi.release()
                    if (stddevValue > noiseThreshold * 0.7) {
                        artifacts.add(DetectedArtifact(
                            rect,
                            "Noise (\${stddevValue.roundToInt()})",
                            stddevValue / noiseThreshold
                        ))
                    }
                }
            }
            return artifacts
        } finally {
            noiseMask.release()
        }
    }

    private fun detectBlur(gray: Mat, size: Size): List<DetectedArtifact> {
        val artifacts = mutableListOf<DetectedArtifact>()
        val blockSize = 80
        val minBlockStdDev = 5.0
        val laplacianFull = Mat()
        Imgproc.Laplacian(gray, laplacianFull, CvType.CV_64F)
        val fullStdDev = MatOfDouble()
        val fullMean = MatOfDouble()
        Core.meanStdDev(laplacianFull, fullMean, fullStdDev)
        laplacianFull.release()
        val dynamicBlurThreshold = fullStdDev.toArray()[0] * 0.7
        if (dynamicBlurThreshold <= 0.0 || dynamicBlurThreshold.isNaN()) return artifacts
        for (y in 0 until gray.height() step blockSize) {
            for (x in 0 until gray.width() step blockSize) {
                val rect = Rect(x, y, blockSize, blockSize)
                if (rect.x + rect.width > gray.width() || rect.y + rect.height > gray.height()) continue
                val subMat = gray.submat(rect)
                val localStd = MatOfDouble()
                val localMean = MatOfDouble()
                Core.meanStdDev(subMat, localMean, localStd)
                if (localStd.toArray()[0] < minBlockStdDev) {
                    subMat.release()
                    continue
                }
                val laplacian = Mat()
                Imgproc.Laplacian(subMat, laplacian, CvType.CV_64F)
                val stddev = MatOfDouble()
                val mean = MatOfDouble()
                Core.meanStdDev(laplacian, mean, stddev)
                val stddevValue = stddev.toArray()[0]
                subMat.release()
                laplacian.release()
                if (stddevValue < dynamicBlurThreshold) {
                    val confidence = 1.0 - (stddevValue / dynamicBlurThreshold).coerceIn(0.0, 1.0)
                    artifacts.add(
                        DetectedArtifact(
                            rect,
                            "Blur (\${stddevValue.roundToInt()})",
                            confidence
                        )
                    )
                }
            }
        }
        return artifacts
    }

    private fun detectPixelization(gray: Mat, size: Size): List<DetectedArtifact> {
        val artifacts = mutableListOf<DetectedArtifact>()
        val edges = Mat()
        Imgproc.Canny(gray, edges, 50.0, 150.0)
        val lines = Mat()
        Imgproc.HoughLinesP(edges, lines, 1.0, Math.PI/180, 50, 30.0, 10.0)
        val horizontalLines = mutableListOf<Double>()
        val verticalLines = mutableListOf<Double>()
        for (i in 0 until lines.rows()) {
            val line = lines.get(i, 0)
            val x1 = line[0]; val y1 = line[1]; val x2 = line[2]; val y2 = line[3]
            if (abs(y2 - y1) < 5) horizontalLines.add(y1)
            else if (abs(x2 - x1) < 5) verticalLines.add(x1)
        }
        if (horizontalLines.size > minPixelizationLines && verticalLines.size > minPixelizationLines) {
            horizontalLines.sort(); verticalLines.sort()
            val avgH = calculateAverageSpacing(horizontalLines)
            val avgV = calculateAverageSpacing(verticalLines)
            if (avgH in 5.0..30.0 && avgV in 5.0..30.0 && abs(avgH - avgV) < 5) {
                artifacts.add(DetectedArtifact(
                    Rect(0, 0, size.width.toInt(), size.height.toInt()),
                    "Pixelization (\${avgH.roundToInt()}x\${avgV.roundToInt()} blocks)"
                ))
            }
        }
        edges.release(); lines.release()
        return artifacts
    }

    private fun calculateAverageSpacing(sortedValues: List<Double>): Double {
        if (sortedValues.size < 2) return 0.0
        val spacings = sortedValues.zipWithNext { a, b -> b - a }.sorted().drop(1).dropLast(1)
        return if (spacings.isNotEmpty()) spacings.average() else 0.0
    }
}
