package com.merkost.drawablepreview.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.jetbrains.jewel.bridge.JewelComposePanel

/**
 * Hosts the Compose-based Resource Manager panel inside an IntelliJ tool
 * window. The Compose runtime + Jewel UI library are loaded from the IDE's
 * own modules (declared as `bundledModule` in build.gradle.kts), so this
 * adds zero binary weight to the plugin.
 */
class ResourceManagerToolWindowFactory : ToolWindowFactory, com.intellij.openapi.project.DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JewelComposePanel { ResourceManagerPanel(project) }
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
