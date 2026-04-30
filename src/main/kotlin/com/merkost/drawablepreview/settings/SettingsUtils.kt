package com.merkost.drawablepreview.settings

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.ProjectManager
import com.merkost.drawablepreview.factories.Constants
import com.merkost.drawablepreview.factories.IconPreviewFactory

object SettingsUtils {

    const val MIN_PREVIEW_SIZE = 16
    const val MAX_PREVIEW_SIZE = 256

    // Property keys kept in the original (mistamek) namespace for backwards
    // compatibility with users migrating from the upstream fork — changing it
    // would silently reset everyone's settings.
    private const val PROPERTIES_SIZE = "com.mistamek.drawablepreview.settings.PropertiesSize"
    private const val PROPERTIES_ENABLED = "com.merkost.drawablepreview.settings.Enabled"

    // Per-thread render size override. Lets one-off renders (the right-click
    // preview popup, the hover tooltip) request a different size than the
    // project-view default without wiring a parameter through every drawable.
    private val sizeOverride = ThreadLocal<Int?>()

    fun getPreviewSize(): Int =
        sizeOverride.get()
            ?: PropertiesComponent.getInstance().getInt(PROPERTIES_SIZE, Constants.ICON_SIZE)

    /** Render `block` with [size] in place of the configured preview size. */
    fun <T> withRenderSize(size: Int, block: () -> T): T {
        val previous = sizeOverride.get()
        sizeOverride.set(size)
        return try {
            block()
        } finally {
            if (previous == null) sizeOverride.remove() else sizeOverride.set(previous)
        }
    }

    fun isEnabled(): Boolean =
        PropertiesComponent.getInstance().getBoolean(PROPERTIES_ENABLED, true)

    fun isModified(previewSize: Int, enabled: Boolean): Boolean =
        previewSize.clampToValidRange() != getPreviewSize() || enabled != isEnabled()

    fun apply(previewSize: Int, enabled: Boolean) {
        val clamped = previewSize.clampToValidRange()
        val props = PropertiesComponent.getInstance()
        props.setValue(PROPERTIES_SIZE, clamped, Constants.ICON_SIZE)
        props.setValue(PROPERTIES_ENABLED, enabled, true)
        IconPreviewFactory.invalidateAll()
        refreshOpenProjectViews()
    }

    private fun refreshOpenProjectViews() {
        ProjectManager.getInstance().openProjects.forEach {
            ProjectView.getInstance(it).refresh()
        }
    }

    private fun Int.clampToValidRange(): Int = coerceIn(MIN_PREVIEW_SIZE, MAX_PREVIEW_SIZE)
}