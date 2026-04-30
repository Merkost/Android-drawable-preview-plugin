package com.merkost.drawablepreview.statusbar

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.util.Consumer
import com.merkost.drawablepreview.factories.IconPreviewFactory
import com.merkost.drawablepreview.settings.SettingsUtils
import java.awt.event.MouseEvent
import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.WindowManager

/**
 * Tiny status bar widget showing the current state of the master
 * "Show drawable previews" toggle. Click to flip it.
 */
class DrawablePreviewStatusBarWidget(private val project: Project) : StatusBarWidget,
    StatusBarWidget.TextPresentation {

    override fun ID(): String = WIDGET_ID

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun install(statusBar: StatusBar) = Unit

    override fun dispose() = Unit

    override fun getText(): String =
        if (SettingsUtils.isEnabled()) "🖼 Drawables: on" else "🖼 Drawables: off"

    override fun getTooltipText(): String =
        "Toggle drawable previews (currently " +
                (if (SettingsUtils.isEnabled()) "on" else "off") + ")"

    override fun getAlignment(): Float = 0f

    override fun getClickConsumer(): Consumer<MouseEvent> = Consumer {
        SettingsUtils.setEnabled(!SettingsUtils.isEnabled())
        IconPreviewFactory.invalidateAll()
        ProjectManager.getInstance().openProjects.forEach {
            ProjectView.getInstance(it).refresh()
        }
        // Force the widget to re-render its text.
        WindowManager.getInstance().getStatusBar(project)?.updateWidget(WIDGET_ID)
    }

    companion object {
        const val WIDGET_ID = "com.merkost.drawablepreview.StatusBarWidget"
    }
}

class DrawablePreviewStatusBarWidgetFactory : StatusBarWidgetFactory {
    override fun getId(): String = DrawablePreviewStatusBarWidget.WIDGET_ID
    override fun getDisplayName(): String = "Drawable Previews"
    override fun isAvailable(project: Project): Boolean = true
    override fun createWidget(project: Project): StatusBarWidget = DrawablePreviewStatusBarWidget(project)
    override fun disposeWidget(widget: StatusBarWidget) = widget.dispose()
    override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true
}
