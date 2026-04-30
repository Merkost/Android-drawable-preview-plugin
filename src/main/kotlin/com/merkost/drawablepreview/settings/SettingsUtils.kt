package com.merkost.drawablepreview.settings

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.ProjectManager
import com.merkost.drawablepreview.factories.Constants
import com.merkost.drawablepreview.factories.IconPreviewFactory

object SettingsUtils {

    const val MIN_PREVIEW_SIZE = 16
    const val MAX_PREVIEW_SIZE = 256

    // Property key kept in the original (mistamek) namespace for backwards
    // compatibility with users migrating from the upstream fork — changing it
    // would silently reset everyone's preview size.
    private const val PROPERTIES_SIZE = "com.mistamek.drawablepreview.settings.PropertiesSize"

    fun getPreviewSize() = PropertiesComponent.getInstance().getInt(PROPERTIES_SIZE, Constants.ICON_SIZE)

    fun isModified(previewSize: Int): Boolean {
        return previewSize.clampToValidRange() != getPreviewSize()
    }

    fun apply(previewSize: Int) {
        val clamped = previewSize.clampToValidRange()
        PropertiesComponent.getInstance().setValue(PROPERTIES_SIZE, clamped, Constants.ICON_SIZE)
        IconPreviewFactory.invalidateAll()
        ProjectManager.getInstance().openProjects.forEach {
            ProjectView.getInstance(it).refresh()
        }
    }

    private fun Int.clampToValidRange(): Int = coerceIn(MIN_PREVIEW_SIZE, MAX_PREVIEW_SIZE)
}