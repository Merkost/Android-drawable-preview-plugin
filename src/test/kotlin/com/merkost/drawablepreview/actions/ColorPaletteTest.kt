package com.merkost.drawablepreview.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage

class ColorPaletteTest {

    private fun solid(color: Color, size: Int = 32): BufferedImage =
        BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB).apply {
            createGraphics().apply {
                this.color = color
                fillRect(0, 0, size, size)
                dispose()
            }
        }

    @Test
    fun `solid red image extracts red as dominant`() {
        val palette = ColorPalette.extract(solid(Color.RED), maxColors = 4)
        assertTrue("at least one color extracted", palette.isNotEmpty())
        // Quantization snaps to upper 4 bits, so the dominant color may be
        // 0xFFFFFF00 — what matters is red dominates green/blue channels.
        val first = palette.first()
        assertTrue(first.red > first.green)
        assertTrue(first.red > first.blue)
    }

    @Test
    fun `fully transparent image produces empty palette`() {
        val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        // BufferedImage starts fully transparent.
        val palette = ColorPalette.extract(image)
        assertTrue(palette.isEmpty())
    }

    @Test
    fun `two-color image extracts two distinct colors`() {
        val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().apply {
            color = Color.BLUE
            fillRect(0, 0, 16, 32)
            color = Color.GREEN
            fillRect(16, 0, 16, 32)
            dispose()
        }
        val palette = ColorPalette.extract(image, maxColors = 4)
        assertTrue("at least 2 colors", palette.size >= 2)
        // Each top color should be predominantly its respective channel.
        val hasBlueDominant = palette.any { it.blue > it.red && it.blue > it.green }
        val hasGreenDominant = palette.any { it.green > it.red && it.green > it.blue }
        assertTrue("blue is in palette", hasBlueDominant)
        assertTrue("green is in palette", hasGreenDominant)
    }

    @Test
    fun `palette is capped at maxColors`() {
        // Make an image with many distinct hues.
        val image = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
        for (i in 0 until 8) {
            image.createGraphics().apply {
                color = Color.getHSBColor(i / 8f, 1f, 1f)
                fillRect(i * 8, 0, 8, 64)
                dispose()
            }
        }
        val palette = ColorPalette.extract(image, maxColors = 3)
        assertTrue(palette.size <= 3)
    }

    @Test
    fun `toHex formats canonical 6-digit uppercase`() {
        assertEquals("#FF0000", ColorPalette.toHex(Color(0xFF, 0, 0)))
        assertEquals("#00AABB", ColorPalette.toHex(Color(0, 0xAA, 0xBB)))
    }

    @Test
    fun `tiny alpha pixels are ignored`() {
        val image = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().apply {
            color = Color(255, 0, 0, 16)  // alpha < 32, ignored
            fillRect(0, 0, 16, 16)
            dispose()
        }
        assertTrue(ColorPalette.extract(image).isEmpty())

        // Sanity: same color but opaque is detected.
        val opaque = solid(Color.RED)
        assertFalse(ColorPalette.extract(opaque).isEmpty())
    }
}
