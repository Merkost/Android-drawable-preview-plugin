package com.merkost.drawablepreview.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.COLUMNS_TINY
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel

class SettingsConfigurable : BoundConfigurable("Android drawable preview") {

    private var previewSize: Int = SettingsUtils.getPreviewSize()

    override fun createPanel(): DialogPanel = panel {
        row("Preview size (px):") {
            intTextField(SettingsUtils.MIN_PREVIEW_SIZE..SettingsUtils.MAX_PREVIEW_SIZE)
                .bindIntText({ previewSize }, { previewSize = it })
                .columns(COLUMNS_TINY)
        }
    }

    override fun isModified(): Boolean {
        super.isModified()
        return SettingsUtils.isModified(previewSize)
    }

    override fun apply() {
        super.apply()
        SettingsUtils.apply(previewSize)
    }

    override fun reset() {
        super.reset()
        previewSize = SettingsUtils.getPreviewSize()
    }
}
