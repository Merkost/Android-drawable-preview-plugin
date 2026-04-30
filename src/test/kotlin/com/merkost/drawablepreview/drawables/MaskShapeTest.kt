package com.merkost.drawablepreview.drawables

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MaskShapeTest {

    @Test
    fun `default is circle`() {
        assertEquals(MaskShape.CIRCLE, MaskShape.DEFAULT)
    }

    @Test
    fun `circle shape contains center, excludes corners`() {
        val shape = MaskShape.CIRCLE.shapeFor(100)
        assertTrue("center is inside circle", shape.contains(50.0, 50.0))
        assertFalse("top-left corner is outside circle", shape.contains(1.0, 1.0))
        assertFalse("bottom-right corner is outside circle", shape.contains(99.0, 99.0))
    }

    @Test
    fun `square shape contains all four corners`() {
        val shape = MaskShape.SQUARE.shapeFor(100)
        assertTrue(shape.contains(1.0, 1.0))
        assertTrue(shape.contains(99.0, 1.0))
        assertTrue(shape.contains(1.0, 99.0))
        assertTrue(shape.contains(99.0, 99.0))
    }

    @Test
    fun `rounded square excludes the very corners`() {
        val shape = MaskShape.ROUNDED_SQUARE.shapeFor(100)
        // Corner at (1,1) should be inside the rounding arc and excluded.
        assertFalse("rounded corner is clipped", shape.contains(1.0, 1.0))
        // Center stays inside.
        assertTrue(shape.contains(50.0, 50.0))
    }

    @Test
    fun `squircle is between circle and rounded square`() {
        val circle = MaskShape.CIRCLE.shapeFor(100)
        val squircle = MaskShape.SQUIRCLE.shapeFor(100)
        // At a point near the corner the squircle should extend further than
        // a circle (its hallmark) but still exclude the very corner.
        val nearCorner = 90.0
        assertFalse("circle excludes near-corner", circle.contains(nearCorner, nearCorner))
        assertTrue("squircle reaches near-corner", squircle.contains(nearCorner, nearCorner))
        assertFalse("squircle still excludes the very corner", squircle.contains(99.5, 99.5))
    }
}
