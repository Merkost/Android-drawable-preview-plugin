package com.merkost.drawablepreview.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.COLUMNS_TINY
import com.intellij.ui.dsl.builder.bindIntText
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.layout.selected
import javax.swing.JCheckBox

class SettingsConfigurable : BoundConfigurable("Android drawable preview") {

    private var previewSize: Int = SettingsUtils.getPreviewSize()
    private var enabled: Boolean = SettingsUtils.isEnabled()

    override fun createPanel(): DialogPanel = panel {
        lateinit var enabledCheckbox: JCheckBox
        row {
            enabledCheckbox = checkBox("Show drawable previews in Project View")
                .bindSelected({ enabled }, { enabled = it })
                .component
        }
        row("Preview size (px):") {
            intTextField(SettingsUtils.MIN_PREVIEW_SIZE..SettingsUtils.MAX_PREVIEW_SIZE)
                .bindIntText({ previewSize }, { previewSize = it })
                .columns(COLUMNS_TINY)
                .enabledIf(enabledCheckbox.selected)
        }
    }

    override fun isModified(): Boolean {
        super.isModified()
        return SettingsUtils.isModified(previewSize, enabled)
    }

    override fun apply() {
        super.apply()
        SettingsUtils.apply(previewSize, enabled)
    }

    override fun reset() {
        super.reset()
        previewSize = SettingsUtils.getPreviewSize()
        enabled = SettingsUtils.isEnabled()
    }
}
