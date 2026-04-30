package com.merkost.drawablepreview.drawables.dom

import com.merkost.drawablepreview.drawables.DrawableInflater
import com.merkost.drawablepreview.drawables.MaskShape
import com.merkost.drawablepreview.settings.SettingsUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

class AdaptiveIconDrawableTest {

    private fun rootOf(xml: String): Element {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        return builder.parse(ByteArrayInputStream(xml.toByteArray())).documentElement
    }

    private fun simpleAdaptiveIcon() = rootOf(
        """
        <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
            <background><color android:color="#FF0000"/></background>
            <foreground><color android:color="#0000FF"/></foreground>
        </adaptive-icon>
        """.trimIndent()
    )

    @Test
    fun `inflates as AdaptiveIconDrawable`() {
        val drawable = DrawableInflater.getDrawable(simpleAdaptiveIcon())
        assertNotNull(drawable)
        assertTrue(drawable is AdaptiveIconDrawable)
    }

    @Test
    fun `default circle mask leaves corners transparent`() {
        val drawable = DrawableInflater.getDrawable(simpleAdaptiveIcon())!!
        val output = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
        drawable.draw(output)

        // Corners must be transparent (clipped by the circular mask).
        assertEquals("top-left transparent", 0, alphaAt(output, 0, 0))
        assertEquals("bottom-right transparent", 0, alphaAt(output, 63, 63))
        // Center must be opaque (composed bg or fg fills it).
        assertNotEquals("center is opaque", 0, alphaAt(output, 32, 32))
    }

    @Test
    fun `square mask preserves the corners`() {
        val drawable = DrawableInflater.getDrawable(simpleAdaptiveIcon())!!
        val output = BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB)
        SettingsUtils.withMaskShape(MaskShape.SQUARE) {
            drawable.draw(output)
        }
        assertNotEquals("top-left opaque under square mask", 0, alphaAt(output, 0, 0))
        assertNotEquals("bottom-right opaque under square mask", 0, alphaAt(output, 63, 63))
    }

    private fun alphaAt(image: BufferedImage, x: Int, y: Int): Int =
        (image.getRGB(x, y) ushr 24) and 0xFF
}
