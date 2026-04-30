package com.merkost.drawablepreview.drawables

import com.merkost.drawablepreview.drawables.dom.AdaptiveIconDrawable
import com.merkost.drawablepreview.drawables.dom.BitmapDrawable
import com.merkost.drawablepreview.drawables.dom.ColorDrawable
import com.merkost.drawablepreview.drawables.dom.GradientDrawable
import com.merkost.drawablepreview.drawables.dom.InsetDrawable
import com.merkost.drawablepreview.drawables.dom.LayerDrawable
import com.merkost.drawablepreview.drawables.dom.LevelListDrawable
import com.merkost.drawablepreview.drawables.dom.RippleDrawable
import com.merkost.drawablepreview.drawables.dom.RotateDrawable
import com.merkost.drawablepreview.drawables.dom.ScaleDrawable
import com.merkost.drawablepreview.drawables.dom.SelectorDrawable
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

class DrawableInflaterTest {

    private fun rootOf(xml: String): Element {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(ByteArrayInputStream(xml.toByteArray()))
        return doc.documentElement
    }

    @Test
    fun `selector tag inflates SelectorDrawable`() {
        val drawable = DrawableInflater.createDrawable(rootOf("<selector/>"))
        assertTrue(drawable is SelectorDrawable)
    }

    @Test
    fun `animated-selector tag inflates SelectorDrawable`() {
        val drawable = DrawableInflater.createDrawable(rootOf("<animated-selector/>"))
        assertTrue(drawable is SelectorDrawable)
    }

    @Test
    fun `level-list tag inflates LevelListDrawable`() {
        val drawable = DrawableInflater.createDrawable(rootOf("<level-list/>"))
        assertTrue(drawable is LevelListDrawable)
    }

    @Test
    fun `layer-list tag inflates LayerDrawable`() {
        val drawable = DrawableInflater.createDrawable(rootOf("<layer-list/>"))
        assertTrue(drawable is LayerDrawable)
    }

    @Test
    fun `transition tag inflates LayerDrawable`() {
        val drawable = DrawableInflater.createDrawable(rootOf("<transition/>"))
        assertTrue(drawable is LayerDrawable)
    }

    @Test
    fun `ripple tag inflates RippleDrawable`() {
        val drawable = DrawableInflater.createDrawable(rootOf("<ripple/>"))
        assertTrue(drawable is RippleDrawable)
    }

    @Test
    fun `adaptive-icon tag inflates AdaptiveIconDrawable`() {
        val drawable = DrawableInflater.createDrawable(rootOf("<adaptive-icon/>"))
        assertTrue(drawable is AdaptiveIconDrawable)
    }

    @Test
    fun `color tag inflates ColorDrawable`() {
        val drawable = DrawableInflater.createDrawable(rootOf("<color/>"))
        assertTrue(drawable is ColorDrawable)
    }

    @Test
    fun `shape tag inflates GradientDrawable`() {
        val drawable = DrawableInflater.createDrawable(rootOf("<shape/>"))
        assertTrue(drawable is GradientDrawable)
    }

    @Test
    fun `scale tag inflates ScaleDrawable`() {
        val drawable = DrawableInflater.createDrawable(rootOf("<scale/>"))
        assertTrue(drawable is ScaleDrawable)
    }

    @Test
    fun `rotate tag inflates RotateDrawable`() {
        val drawable = DrawableInflater.createDrawable(rootOf("<rotate/>"))
        assertTrue(drawable is RotateDrawable)
    }

    @Test
    fun `animated-rotate tag inflates RotateDrawable`() {
        val drawable = DrawableInflater.createDrawable(rootOf("<animated-rotate/>"))
        assertTrue(drawable is RotateDrawable)
    }

    @Test
    fun `inset tag inflates InsetDrawable`() {
        val drawable = DrawableInflater.createDrawable(rootOf("<inset/>"))
        assertTrue(drawable is InsetDrawable)
    }

    @Test
    fun `bitmap tag inflates BitmapDrawable`() {
        val drawable = DrawableInflater.createDrawable(rootOf("<bitmap/>"))
        assertTrue(drawable is BitmapDrawable)
    }

    @Test
    fun `animated-vector tag inflates AnimatedVectorDrawable`() {
        val drawable = DrawableInflater.createDrawable(rootOf("<animated-vector/>"))
        assertTrue(drawable is com.merkost.drawablepreview.drawables.dom.AnimatedVectorDrawable)
    }

    @Test
    fun `unknown tag returns null`() {
        assertNull(DrawableInflater.createDrawable(rootOf("<not-a-real-drawable/>")))
    }
}
