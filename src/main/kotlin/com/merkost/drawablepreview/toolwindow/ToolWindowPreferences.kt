package com.merkost.drawablepreview.toolwindow

import com.intellij.ide.util.PropertiesComponent

object ToolWindowPreferences {

    private const val KEY_QUERY = "com.merkost.drawablepreview.toolwindow.query"
    private const val KEY_KINDS = "com.merkost.drawablepreview.toolwindow.kinds"
    private const val KEY_GROUP_BY = "com.merkost.drawablepreview.toolwindow.groupBy"
    private const val KEY_SORT_BY = "com.merkost.drawablepreview.toolwindow.sortBy"

    fun loadQuery(): String =
        props()?.getValue(KEY_QUERY).orEmpty()

    fun saveQuery(query: String) {
        props()?.setValue(KEY_QUERY, query, "")
    }

    fun loadKinds(): Set<DrawableEntry.Kind> {
        val raw = props()?.getValue(KEY_KINDS)
        if (raw.isNullOrBlank()) return DrawableEntry.Kind.values().toSet()
        return raw.split(',')
            .mapNotNull { name -> runCatching { DrawableEntry.Kind.valueOf(name) }.getOrNull() }
            .toSet()
            .ifEmpty { DrawableEntry.Kind.values().toSet() }
    }

    fun saveKinds(kinds: Set<DrawableEntry.Kind>) {
        val all = DrawableEntry.Kind.values().toSet()
        if (kinds == all) {
            props()?.unsetValue(KEY_KINDS)
        } else {
            props()?.setValue(KEY_KINDS, kinds.joinToString(",") { it.name })
        }
    }

    fun loadGroupBy(): GroupBy = readEnum(KEY_GROUP_BY, GroupBy.NONE)
    fun saveGroupBy(value: GroupBy) = writeEnum(KEY_GROUP_BY, value, GroupBy.NONE)

    fun loadSortBy(): SortBy = readEnum(KEY_SORT_BY, SortBy.NAME_ASC)
    fun saveSortBy(value: SortBy) = writeEnum(KEY_SORT_BY, value, SortBy.NAME_ASC)

    private inline fun <reified E : Enum<E>> readEnum(key: String, default: E): E {
        val raw = props()?.getValue(key) ?: return default
        return runCatching { enumValueOf<E>(raw) }.getOrDefault(default)
    }

    private fun <E : Enum<E>> writeEnum(key: String, value: E, default: E) {
        if (value == default) {
            props()?.unsetValue(key)
        } else {
            props()?.setValue(key, value.name)
        }
    }

    private fun props(): PropertiesComponent? =
        runCatching { PropertiesComponent.getInstance() }.getOrNull()
}
