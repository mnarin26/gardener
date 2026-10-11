package com.mustafa.gardener.modules.vision

/**
 * Rotate row-major ARGB pixels by CameraX rotationDegrees (0/90/180/270).
 * Shared utility for detect + seg engines (YUV path does not rotate).
 */
object FrameOrientation {
    data class Oriented(
        val pixels: IntArray,
        val width: Int,
        val height: Int,
    )

    fun rotateArgb(
        src: IntArray,
        width: Int,
        height: Int,
        rotationDegrees: Int,
    ): Oriented {
        val deg = ((rotationDegrees % 360) + 360) % 360
        return when (deg) {
            0 -> Oriented(src, width, height)
            90 -> {
                val out = IntArray(width * height)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val nx = height - 1 - y
                        val ny = x
                        out[ny * height + nx] = src[y * width + x]
                    }
                }
                Oriented(out, height, width)
            }
            180 -> {
                val out = IntArray(width * height)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        out[(height - 1 - y) * width + (width - 1 - x)] = src[y * width + x]
                    }
                }
                Oriented(out, width, height)
            }
            270 -> {
                val out = IntArray(width * height)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val nx = y
                        val ny = width - 1 - x
                        out[ny * height + nx] = src[y * width + x]
                    }
                }
                Oriented(out, height, width)
            }
            else -> Oriented(src, width, height)
        }
    }

    /**
     * Letterbox [src] into [dstW]×[dstH] with gray pad (114), matching Ultralytics.
     * Returns scale and pad offsets to undo after inference.
     */
    data class LetterboxMeta(
        val scale: Float,
        val padX: Float,
        val padY: Float,
        val srcW: Int,
        val srcH: Int,
    )

    fun letterboxRgbFloat(
        srcArgb: IntArray,
        srcW: Int,
        srcH: Int,
        dstFloat: FloatArray,
        dstW: Int,
        dstH: Int,
    ): LetterboxMeta {
        val scale = minOf(dstW.toFloat() / srcW, dstH.toFloat() / srcH)
        val newW = (srcW * scale).toInt().coerceAtLeast(1)
        val newH = (srcH * scale).toInt().coerceAtLeast(1)
        val padX = (dstW - newW) / 2f
        val padY = (dstH - newH) / 2f
        val padColor = 114f / 255f

        // Fill pad
        java.util.Arrays.fill(dstFloat, padColor)

        for (y in 0 until newH) {
            val sy = (y / scale).toInt().coerceIn(0, srcH - 1)
            for (x in 0 until newW) {
                val sx = (x / scale).toInt().coerceIn(0, srcW - 1)
                val c = srcArgb[sy * srcW + sx]
                val dx = (x + padX.toInt())
                val dy = (y + padY.toInt())
                if (dx in 0 until dstW && dy in 0 until dstH) {
                    val i = (dy * dstW + dx) * 3
                    dstFloat[i] = ((c shr 16) and 0xFF) / 255f
                    dstFloat[i + 1] = ((c shr 8) and 0xFF) / 255f
                    dstFloat[i + 2] = (c and 0xFF) / 255f
                }
            }
        }
        return LetterboxMeta(scale, padX, padY, srcW, srcH)
    }
}
