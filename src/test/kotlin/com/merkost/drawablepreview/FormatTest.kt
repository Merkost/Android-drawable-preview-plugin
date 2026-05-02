package com.merkost.drawablepreview

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun `bytes under 1024 are shown verbatim`() {
        assertEquals("0 B", Format.humanSize(0))
        assertEquals("1023 B", Format.humanSize(1023))
    }

    @Test
    fun `kilobytes shown to one decimal place`() {
        assertEquals("1.0 KB", Format.humanSize(1024))
        assertEquals("1.5 KB", Format.humanSize(1536))
        assertEquals("1023.0 KB", Format.humanSize(1024L * 1023))
    }

    @Test
    fun `megabytes shown to one decimal place`() {
        assertEquals("1.0 MB", Format.humanSize(1024L * 1024))
        assertEquals("12.4 MB", Format.humanSize((12.4 * 1024 * 1024).toLong()))
    }

    @Test
    fun `gigabytes shown to two decimal places`() {
        assertEquals("1.00 GB", Format.humanSize(1024L * 1024 * 1024))
        assertEquals("2.50 GB", Format.humanSize((2.5 * 1024 * 1024 * 1024).toLong()))
    }
}
