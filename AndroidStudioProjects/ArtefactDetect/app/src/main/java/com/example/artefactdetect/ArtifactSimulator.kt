package com.example.artefactdetect

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.random.Random
import android.util.Log
import kotlin.math.abs

class ArtifactSimulator {
    private val activeArtifacts = mutableListOf<RectArtifact>()

    data class RectArtifact(
        val rect: Rect,
        val color: Scalar,
        val artifactType: ArtifactType
    )

    enum class ArtifactType {
        SOLID_COLOR, NOISE, PIXELATED
    }

    fun addRandomRectArtifact(frameSize: Size) {
        // Случайные параметры прямоугольника
        val width = Random.nextInt(50, 200)
        val height = Random.nextInt(50, 200)
        val x = Random.nextInt(0, frameSize.width.toInt() - width)
        val y = Random.nextInt(0, frameSize.height.toInt() - height)

        val artifactType = ArtifactType.values().random()
        val color = when(artifactType) {
            ArtifactType.SOLID_COLOR -> Scalar(
                Random.nextDouble(0.0, 255.0),
                Random.nextDouble(0.0, 255.0),
                Random.nextDouble(0.0, 255.0)
            )
            else -> Scalar(0.0, 0.0, 0.0) // Для других типов цвет не важен
        }

        activeArtifacts.add(RectArtifact(
            rect = Rect(x, y, width, height),
            color = color,
            artifactType = artifactType
        ))
        Log.d("ArtifactSimulator", "Added rect artifact at ($x,$y) ${width}x$height")
    }

    fun applyAllArtifacts(input: Mat): Mat {
        val output = input.clone()

        activeArtifacts.forEach { artifact ->
            when(artifact.artifactType) {
                ArtifactType.SOLID_COLOR -> {
                    Imgproc.rectangle(
                        output,
                        artifact.rect.tl(),
                        artifact.rect.br(),
                        artifact.color,
                        -1 // Заливка
                    )
                }
                ArtifactType.NOISE -> {
                    val roi = output.submat(artifact.rect)
                    val noise = Mat(roi.size(), roi.type())
                    Core.randn(noise, 0.0, 50.0)
                    Core.add(roi, noise, roi)
                    noise.release()
                }
                ArtifactType.PIXELATED -> {
                    val roi = output.submat(artifact.rect)
                    val pixelSize = 10
                    for (i in 0 until roi.rows() step pixelSize) {
                        for (j in 0 until roi.cols() step pixelSize) {
                            val block = roi.submat(
                                i, minOf(i + pixelSize, roi.rows()),
                                j, minOf(j + pixelSize, roi.cols())
                            )
                            Core.mean(block).let { block.setTo(it) }
                        }
                    }
                }
            }
        }

        return output
    }

    fun clearArtifacts() {
        activeArtifacts.clear()
    }

    fun getArtifactsCount(): Int = activeArtifacts.size
}