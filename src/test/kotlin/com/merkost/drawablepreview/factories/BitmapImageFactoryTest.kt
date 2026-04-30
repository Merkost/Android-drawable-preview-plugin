package com.merkost.drawablepreview.factories

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.awt.Color
import java.awt.image.BufferedImage

class BitmapImageFactoryTest {

    @Test
    fun `9-patch border strip removes one pixel from each edge`() {
        val source = BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)
        val stripped = BitmapImageFactory.stripNinePatchBorders(source)!!
        assertEquals(8, stripped.width)
        assertEquals(8, stripped.height)
    }

    @Test
    fun `9-patch border strip returns null for too-small images`() {
        assertNull(BitmapImageFactory.stripNinePatchBorders(BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB)))
        assertNull(BitmapImageFactory.stripNinePatchBorders(BufferedImage(1, 5, BufferedImage.TYPE_INT_ARGB)))
        assertNull(BitmapImageFactory.stripNinePatchBorders(BufferedImage(5, 1, BufferedImage.TYPE_INT_ARGB)))
    }

    @Test
    fun `9-patch strip preserves inner pixel content`() {
        // Border = black, inner = red. After stripping, every pixel should be red.
        val source = BufferedImage(6, 6, BufferedImage.TYPE_INT_ARGB)
        val g = source.createGraphics()
        g.color = Color.BLACK
        g.fillRect(0, 0, 6, 6)
        g.color = Color.RED
        g.fillRect(1, 1, 4, 4)
        g.dispose()

        val stripped = BitmapImageFactory.stripNinePatchBorders(source)!!
        for (x in 0 until stripped.width) {
            for (y in 0 until stripped.height) {
                assertEquals("pixel ($x,$y) should be red", Color.RED.rgb, stripped.getRGB(x, y))
            }
        }
        // Sanity: original has black at the border.
        assertNotEquals(Color.RED.rgb, source.getRGB(0, 0))
    }
}
