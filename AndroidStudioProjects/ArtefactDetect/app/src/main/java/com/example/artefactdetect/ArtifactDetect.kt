package com.example.artefactdetect

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs

class ArtifactDetect {

    data class DetectedArtifact(
        val rect: Rect,
        val label: String
    )

    fun detect(frame: Mat): List<DetectedArtifact> {
        val artifacts = mutableListOf<DetectedArtifact>()

        val gray = Mat()
        Imgproc.cvtColor(frame, gray, Imgproc.COLOR_RGB2GRAY)

        // === NOISE DETECTION (very high local variance) ===
        val noiseMask = Mat()
        Imgproc.Laplacian(gray, noiseMask, CvType.CV_64F)
        Core.convertScaleAbs(noiseMask, noiseMask)

        val threshold = 60.0
        val binaryNoise = Mat()
        Imgproc.threshold(noiseMask, binaryNoise, threshold, 255.0, Imgproc.THRESH_BINARY)

        val contours = mutableListOf<MatOfPoint>()
        Imgproc.findContours(binaryNoise, contours, Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        for (cnt in contours) {
            val rect = Imgproc.boundingRect(cnt)
            if (rect.area() > 1000) {
                artifacts.add(DetectedArtifact(rect, "Noise"))
            }
        }

        // === BLUR DETECTION (low variance of Laplacian) ===
        val laplacian = Mat()
        Imgproc.Laplacian(gray, laplacian, CvType.CV_64F)
        val stddev = MatOfDouble()
        Core.meanStdDev(laplacian, MatOfDouble(), stddev)

        if (stddev.toArray()[0] < 10) {
            // Low sharpness: maybe whole image is blurred
            artifacts.add(DetectedArtifact(Rect(0, 0, frame.cols(), frame.rows()), "Blur"))
        }

        // === PIXELIZATION DETECTION ===
        detectPixelization(gray, frame.size()).forEach {
            artifacts.add(it)
        }

        gray.release()
        noiseMask.release()
        binaryNoise.release()
        laplacian.release()

        return artifacts
    }

    private fun detectPixelization(gray: Mat, size: Size): List<DetectedArtifact> {
        val artifacts = mutableListOf<DetectedArtifact>()

        // 1. Detect block patterns using edge detection
        val edges = Mat()
        Imgproc.Canny(gray, edges, 50.0, 150.0)

        // 2. Apply Hough transform to find lines (block edges)
        val lines = Mat()
        Imgproc.HoughLinesP(edges, lines, 1.0, Math.PI/180, 50, 30.0, 10.0)

        // 3. Analyze lines to find grid patterns
        val horizontalLines = mutableListOf<Double>()
        val verticalLines = mutableListOf<Double>()

        for (i in 0 until lines.rows()) {
            val line = lines.get(i, 0)
            val x1 = line[0]
            val y1 = line[1]
            val x2 = line[2]
            val y2 = line[3]

            // Classify as horizontal or vertical line
            if (abs(y2 - y1) < 5) { // horizontal line
                horizontalLines.add(y1.toDouble())
            } else if (abs(x2 - x1) < 5) { // vertical line
                verticalLines.add(x1.toDouble())
            }
        }

        // 4. Find regular grid patterns (pixelization blocks)
        if (horizontalLines.size > 5 && verticalLines.size > 5) {
            // Calculate average block size
            horizontalLines.sort()
            verticalLines.sort()

            val avgHorizontalSpacing = calculateAverageSpacing(horizontalLines)
            val avgVerticalSpacing = calculateAverageSpacing(verticalLines)

            // If we have regular spacing, it's likely pixelization
            if (avgHorizontalSpacing in 5.0..30.0 && avgVerticalSpacing in 5.0..30.0 &&
                abs(avgHorizontalSpacing - avgVerticalSpacing) < 5) {
                artifacts.add(DetectedArtifact(
                    Rect(0, 0, size.width.toInt(), size.height.toInt()),
                    "Pixelization (${avgHorizontalSpacing.toInt()}x${avgVerticalSpacing.toInt()} blocks)"
                ))
            }
        }

        edges.release()
        lines.release()

        return artifacts
    }

    private fun calculateAverageSpacing(sortedValues: List<Double>): Double {
        if (sortedValues.size < 2) return 0.0

        val spacings = mutableListOf<Double>()
        for (i in 1 until sortedValues.size) {
            spacings.add(sortedValues[i] - sortedValues[i-1])
        }

        return spacings.average()
    }
}