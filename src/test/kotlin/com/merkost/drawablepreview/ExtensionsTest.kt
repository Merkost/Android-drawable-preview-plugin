package com.merkost.drawablepreview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExtensionsTest {

    @Test
    fun `getDigits extracts digits from mixed string`() {
        assertEquals(24, "size 24dp".getDigits())
    }

    @Test
    fun `getDigits returns int when string is all digits`() {
        assertEquals(42, "42".getDigits())
    }

    @Test
    fun `getDigits returns null when no digits present`() {
        assertNull("abc".getDigits())
    }

    @Test
    fun `getDigits returns null on empty string`() {
        assertNull("".getDigits())
    }

    @Test
    fun `getDigits joins digits across non-digit groups`() {
        assertEquals(123, "1a2b3".getDigits())
    }
}
