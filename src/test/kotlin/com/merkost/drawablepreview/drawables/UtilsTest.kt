package com.merkost.drawablepreview.drawables

import android.view.Gravity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.awt.Color

class UtilsTest {

    @Test
    fun `parseAttributeAsInt extracts leading int from dp value`() {
        assertEquals(16, Utils.parseAttributeAsInt("16dp", -1))
    }

    @Test
    fun `parseAttributeAsInt returns default for null`() {
        assertEquals(-1, Utils.parseAttributeAsInt(null, -1))
    }

    @Test
    fun `parseAttributeAsInt returns default for non-numeric string`() {
        assertEquals(-1, Utils.parseAttributeAsInt("abc", -1))
    }

    @Test
    fun `parseAttributeAsFloat parses decimal value`() {
        assertEquals(3.14f, Utils.parseAttributeAsFloat("3.14", 0f), 0.0001f)
    }

    @Test
    fun `parseAttributeAsFloat returns default for garbage`() {
        assertEquals(0f, Utils.parseAttributeAsFloat("xx", 0f), 0.0001f)
    }

    @Test
    fun `parseAttributeAsPercent parses 50 percent`() {
        assertEquals(0.5f, Utils.parseAttributeAsPercent("50%", -1f), 0.0001f)
    }

    @Test
    fun `parseAttributeAsPercent returns default on garbage`() {
        assertEquals(-1f, Utils.parseAttributeAsPercent("abc", -1f), 0.0001f)
    }

    @Test
    fun `parseAttributeAsColor parses 6-digit hex with implicit alpha`() {
        val color = Utils.parseAttributeAsColor("#FF0000", null)
        assertNotNull(color)
        assertEquals(255, color!!.red)
        assertEquals(0, color.green)
        assertEquals(0, color.blue)
        assertEquals(255, color.alpha)
    }

    @Test
    fun `parseAttributeAsColor parses 8-digit hex with explicit alpha`() {
        val color = Utils.parseAttributeAsColor("#80FF0000", null)
        assertNotNull(color)
        assertEquals(0x80, color!!.alpha)
        assertEquals(0xFF, color.red)
    }

    @Test
    fun `parseAttributeAsColor returns default on null`() {
        assertNull(Utils.parseAttributeAsColor(null, null))
        assertEquals(Color.BLUE, Utils.parseAttributeAsColor(null, Color.BLUE))
    }

    @Test
    fun `parseAttributeAsColor returns default on garbage`() {
        assertEquals(Color.BLUE, Utils.parseAttributeAsColor("not a color", Color.BLUE))
    }

    @Test
    fun `parseAttributeAsGravity recognises single value`() {
        assertEquals(Gravity.LEFT, Utils.parseAttributeAsGravity("left", 0))
    }

    @Test
    fun `parseAttributeAsGravity ORs piped values`() {
        val expected = Gravity.LEFT or Gravity.CENTER_VERTICAL
        assertEquals(expected, Utils.parseAttributeAsGravity("left|center_vertical", 0))
    }

    @Test
    fun `parseAttributeAsGravity returns default for unknown token`() {
        assertEquals(42, Utils.parseAttributeAsGravity("unknown_value", 42))
    }

    @Test
    fun `parseAttributeAsGravity returns default for null`() {
        assertEquals(42, Utils.parseAttributeAsGravity(null, 42))
    }
}
