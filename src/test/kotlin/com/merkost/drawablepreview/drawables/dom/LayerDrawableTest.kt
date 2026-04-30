package com.merkost.drawablepreview.drawables.dom

import com.merkost.drawablepreview.drawables.DrawableInflater
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Regression tests for the two LayerDrawable crash paths fixed in
 * fix(harden LayerDrawable…): zero padding (was a NaN/Infinity divide
 * producing Int.MIN_VALUE coordinates) and explicit android:width on
 * items (was UnsupportedOperationException killing the whole render).
 */
class LayerDrawableTest {

    private fun rootOf(xml: String): Element {
        val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        return builder.parse(ByteArrayInputStream(xml.toByteArray())).documentElement
    }

    private fun newOutput(size: Int = 64) =
        BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)

    @Test
    fun `layer-list with no padding does not crash on draw`() {
        val drawable = DrawableInflater.getDrawable(
            rootOf(
                """
                <layer-list xmlns:android="http://schemas.android.com/apk/res/android">
                    <item><color android:color="#FF0000"/></item>
                </layer-list>
                """.trimIndent()
            )
        )
        assertNotNull(drawable)
        // Was: divide-by-zero on maxPaddingArg=0F → NaN → Int.MIN_VALUE coords.
        drawable!!.draw(newOutput())
    }

    @Test
    fun `layer-list with explicit width on item does not crash`() {
        val drawable = DrawableInflater.getDrawable(
            rootOf(
                """
                <layer-list xmlns:android="http://schemas.android.com/apk/res/android">
                    <item android:width="20dp" android:height="20dp">
                        <color android:color="#00FF00"/>
                    </item>
                </layer-list>
                """.trimIndent()
            )
        )
        assertNotNull(drawable)
        // Was: UnsupportedOperationException blew up the whole render path.
        drawable!!.draw(newOutput())
    }

    @Test
    fun `layer-list with one padded item renders without error`() {
        val drawable = DrawableInflater.getDrawable(
            rootOf(
                """
                <layer-list xmlns:android="http://schemas.android.com/apk/res/android">
                    <item><color android:color="#0000FF"/></item>
                    <item android:left="10dp" android:top="10dp">
                        <color android:color="#FFFFFF"/>
                    </item>
                </layer-list>
                """.trimIndent()
            )
        )
        assertTrue(drawable is LayerDrawable)
        drawable!!.draw(newOutput())
    }

    @Test
    fun `transition tag dispatches to LayerDrawable`() {
        val drawable = DrawableInflater.getDrawable(
            rootOf(
                """
                <transition xmlns:android="http://schemas.android.com/apk/res/android">
                    <item><color android:color="#FF0000"/></item>
                    <item><color android:color="#00FF00"/></item>
                </transition>
                """.trimIndent()
            )
        )
        assertTrue(drawable is LayerDrawable)
        drawable!!.draw(newOutput())
    }
}
