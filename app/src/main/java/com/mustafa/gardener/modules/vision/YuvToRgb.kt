package com.mustafa.gardener.modules.vision

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

/**
 * Minimal YUV_420_888 → ARGB_8888 converter for CameraX analysis frames.
 * Output size matches the ImageProxy width/height (no rotation applied).
 */
object YuvToRgb {
    fun toArgb(image: ImageProxy, out: IntArray? = null): IntArray {
        val width = image.width
        val height = image.height
        require(image.format == android.graphics.ImageFormat.YUV_420_888) {
            "Expected YUV_420_888, got ${image.format}"
        }
        val pixels = out?.takeIf { it.size == width * height } ?: IntArray(width * height)

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuf = yPlane.buffer.duplicate()
        val uBuf = uPlane.buffer.duplicate()
        val vBuf = vPlane.buffer.duplicate()
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        val uRowStride = uPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vRowStride = vPlane.rowStride
        val vPixelStride = vPlane.pixelStride

        var outIndex = 0
        for (row in 0 until height) {
            val yRow = row * yRowStride
            val uvRow = (row shr 1) * uRowStride
            val vvRow = (row shr 1) * vRowStride
            for (col in 0 until width) {
                val y = (yBuf.get(yRow + col * yPixelStride).toInt() and 0xFF)
                val u = (uBuf.get(uvRow + (col shr 1) * uPixelStride).toInt() and 0xFF) - 128
                val v = (vBuf.get(vvRow + (col shr 1) * vPixelStride).toInt() and 0xFF) - 128
                val r = (y + (1.370705f * v)).toInt().coerceIn(0, 255)
                val g = (y - (0.337633f * u) - (0.698001f * v)).toInt().coerceIn(0, 255)
                val b = (y + (1.732446f * u)).toInt().coerceIn(0, 255)
                pixels[outIndex++] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return pixels
    }

    /** Unused helper kept for clarity if ByteBuffer RGB packing is needed later. */
    @Suppress("unused")
    fun packRgb888(argb: IntArray, width: Int, height: Int, dst: ByteBuffer) {
        dst.rewind()
        for (i in 0 until width * height) {
            val c = argb[i]
            dst.put(((c shr 16) and 0xFF).toByte())
            dst.put(((c shr 8) and 0xFF).toByte())
            dst.put((c and 0xFF).toByte())
        }
        dst.rewind()
    }
}
