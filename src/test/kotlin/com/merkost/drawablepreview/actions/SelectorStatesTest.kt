package com.merkost.drawablepreview.actions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SelectorStatesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun writeXml(name: String, content: String): File {
        // Selector parsing requires the file to live inside a recognised
        // resource folder (drawable / mipmap / composeResources/drawable*).
        val resDir = tempFolder.newFolder("res", "drawable")
        return File(resDir, name).apply { writeText(content) }
    }

    @Test
    fun `non-selector returns null`() {
        val file = writeXml(
            "shape.xml",
            """<shape xmlns:android="http://schemas.android.com/apk/res/android"
                       android:shape="rectangle"/>""",
        )
        assertNull(SelectorStates.extract(file.absolutePath, renderSize = 32))
    }

    @Test
    fun `selector with default and one state produces two entries`() {
        val file = writeXml(
            "btn.xml",
            """
            <selector xmlns:android="http://schemas.android.com/apk/res/android">
                <item android:state_pressed="true"><color android:color="#FF0000"/></item>
                <item><color android:color="#00FF00"/></item>
            </selector>
            """.trimIndent(),
        )
        val states = SelectorStates.extract(file.absolutePath, renderSize = 16)
        assertNotNull(states)
        assertEquals(2, states!!.size)
        assertEquals("pressed", states[0].label)
        assertEquals("Default", states[1].label)
    }

    @Test
    fun `multi-state item is labelled with all active states`() {
        val file = writeXml(
            "btn.xml",
            """
            <selector xmlns:android="http://schemas.android.com/apk/res/android">
                <item android:state_pressed="true" android:state_focused="true">
                    <color android:color="#FF0000"/>
                </item>
            </selector>
            """.trimIndent(),
        )
        val states = SelectorStates.extract(file.absolutePath, renderSize = 16)!!
        assertEquals(1, states.size)
        // Order of attributes from DOM is implementation-defined, so just
        // check both names appear.
        assertTrue(states[0].label.contains("pressed"))
        assertTrue(states[0].label.contains("focused"))
    }

    @Test
    fun `false-valued state is shown with explicit value`() {
        val file = writeXml(
            "btn.xml",
            """
            <selector xmlns:android="http://schemas.android.com/apk/res/android">
                <item android:state_enabled="false"><color android:color="#888888"/></item>
            </selector>
            """.trimIndent(),
        )
        val states = SelectorStates.extract(file.absolutePath, renderSize = 16)!!
        assertEquals("enabled=false", states[0].label)
    }

    @Test
    fun `animated-selector is treated as selector`() {
        val file = writeXml(
            "btn.xml",
            """
            <animated-selector xmlns:android="http://schemas.android.com/apk/res/android">
                <item><color android:color="#000000"/></item>
            </animated-selector>
            """.trimIndent(),
        )
        val states = SelectorStates.extract(file.absolutePath, renderSize = 16)
        assertNotNull(states)
        assertEquals(1, states!!.size)
    }
}
