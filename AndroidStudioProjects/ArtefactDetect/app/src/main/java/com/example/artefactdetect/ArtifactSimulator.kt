package com.example.artefactdetect

import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.random.Random

class ArtifactSimulator {
    // Основной метод применения всех артефактов
    fun applyArtifacts(input: Mat, intensity: Float): Mat {
        var output = input.clone()

        // Нормализуем интенсивность (0.0 - 1.0) к конкретным параметрам эффектов
        val artifactsIntensity = (intensity * 10).toInt()

        // Применяем эффекты в зависимости от интенсивности
        if (artifactsIntensity > 0) {
            output = applyCompressionArtifacts(output, artifactsIntensity)
            output = applyNoise(output, artifactsIntensity * 2.5)
            if (artifactsIntensity > 5) {
                output = applyBlur(output, artifactsIntensity / 2)
                output = applyColorDistortion(output, artifactsIntensity * 0.1f)
            }
            if (artifactsIntensity > 7) {
                output = applyJitter(output, artifactsIntensity / 3)
            }
        }

        return output
    }

    // Блочные артефакты сжатия
    private fun applyCompressionArtifacts(input: Mat, blockSize: Int): Mat {
        val output = input.clone()
        val size = maxOf(2, minOf(blockSize, 16)) // Ограничиваем размер блоков

        for (i in 0 until output.rows() step size) {
            for (j in 0 until output.cols() step size) {
                val block = output.submat(
                    i, minOf(i + size, output.rows()),
                    j, minOf(j + size, output.cols())
                )
                Core.mean(block).let { mean ->
                    block.setTo(mean)
                }
            }
        }
        return output
    }

    // Добавление шума
    private fun applyNoise(input: Mat, strength: Double): Mat {
        val noise = Mat(input.size(), input.type())
        Core.randn(noise, 0.0, strength)
        val output = Mat()
        Core.add(input, noise, output)
        return output
    }

    // Размытие изображения
    private fun applyBlur(input: Mat, radius: Int): Mat {
        val output = Mat()
        val kernelSize = maxOf(1, radius) * 2 + 1 // Делаем нечетным
        Imgproc.GaussianBlur(input, output, Size(kernelSize.toDouble(), kernelSize.toDouble()), 0.0)
        return output
    }

    // Дрожание камеры
    private fun applyJitter(input: Mat, maxOffset: Int): Mat {
        if (maxOffset <= 0) return input.clone()

        val output = Mat.zeros(input.size(), input.type())
        val dx = Random.nextInt(-maxOffset, maxOffset)
        val dy = Random.nextInt(-maxOffset, maxOffset)

        val srcRect = Rect(
            maxOf(0, dx),
            maxOf(0, dy),
            input.cols() - abs(dx),
            input.rows() - abs(dy)
        )

        val dstRect = Rect(
            maxOf(0, -dx),
            maxOf(0, -dy),
            input.cols() - abs(dx),
            input.rows() - abs(dy)
        )

        input.submat(srcRect).copyTo(output.submat(dstRect))
        return output
    }

    // Искажение цветов
    private fun applyColorDistortion(input: Mat, factor: Float): Mat {
        val output = Mat()
        val channels = ArrayList<Mat>()
        Core.split(input, channels)

        // Искажаем каналы по-разному
        channels[0] = distortChannel(channels[0], factor * 0.9) // Blue
        channels[1] = distortChannel(channels[1], factor * 1.1) // Green
        channels[2] = distortChannel(channels[2], factor * 1.3) // Red

        Core.merge(channels, output)
        channels.forEach { it.release() }
        return output
    }

    private fun distortChannel(channel: Mat, factor: Double): Mat {
        val output = Mat()
        Core.multiply(channel, Scalar(1.0 - factor * 0.5), output)
        return output
    }
}