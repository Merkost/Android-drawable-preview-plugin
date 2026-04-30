package com.merkost.drawablepreview.actions

import java.awt.Color
import java.awt.image.BufferedImage

/**
 * Extracts the dominant colors of a [BufferedImage] using a coarse
 * 4-bit-per-channel histogram (so similar colors collapse together).
 * Skips fully transparent pixels and grayscale-near-white/black neutrals
 * to surface the actually-distinctive brand colors.
 */
object ColorPalette {

    /**
     * Returns up to [maxColors] dominant Colors, ordered by descending
     * pixel count. Empty list when the image is fully transparent or
     * uninteresting.
     */
    fun extract(image: BufferedImage, maxColors: Int = 6): List<Color> {
        val histogram = HashMap<Int, Int>()
        val width = image.width
        val height = image.height
        // Subsample large images — the dominant colors aren't sensitive
        // to a perfect pixel count.
        val step = maxOf(1, minOf(width, height) / 96)
        for (y in 0 until height step step) {
            for (x in 0 until width step step) {
                val argb = image.getRGB(x, y)
                val alpha = (argb ushr 24) and 0xFF
                if (alpha < 32) continue  // mostly transparent
                val bucket = quantize(argb)
                histogram[bucket] = (histogram[bucket] ?: 0) + 1
            }
        }
        return histogram.entries
            .sortedByDescending { it.value }
            .map { Color(it.key) }
            .distinctBy { it.rgb }
            .take(maxColors)
    }

    /** Snap each channel to the upper 4 bits — collapses near-identical shades. */
    private fun quantize(argb: Int): Int {
        val r = ((argb ushr 16) and 0xFF) and 0xF0
        val g = ((argb ushr 8) and 0xFF) and 0xF0
        val b = (argb and 0xFF) and 0xF0
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    fun toHex(color: Color): String =
        "#%02X%02X%02X".format(color.red, color.green, color.blue)
}
