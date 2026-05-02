package com.merkost.drawablepreview.toolwindow

import org.junit.Assert.assertEquals
import org.junit.Test

class ToolWindowPreferencesTest {

    @Test
    fun `loadQuery returns empty string when no IntelliJ Application is running`() {
        assertEquals("", ToolWindowPreferences.loadQuery())
    }

    @Test
    fun `loadKinds defaults to all kinds when nothing persisted`() {
        val expected = DrawableEntry.Kind.values().toSet()
        assertEquals(expected, ToolWindowPreferences.loadKinds())
    }

    @Test
    fun `loadGroupBy defaults to NONE`() {
        assertEquals(GroupBy.NONE, ToolWindowPreferences.loadGroupBy())
    }

    @Test
    fun `loadSortBy defaults to NAME_ASC`() {
        assertEquals(SortBy.NAME_ASC, ToolWindowPreferences.loadSortBy())
    }

    @Test
    fun `save methods are no-ops in test context (no NPE)`() {
        ToolWindowPreferences.saveQuery("anything")
        ToolWindowPreferences.saveKinds(setOf(DrawableEntry.Kind.PNG))
        ToolWindowPreferences.saveGroupBy(GroupBy.KIND)
        ToolWindowPreferences.saveSortBy(SortBy.SIZE)
    }
}
