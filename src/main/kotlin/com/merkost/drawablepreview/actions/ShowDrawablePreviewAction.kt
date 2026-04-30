package com.merkost.drawablepreview.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.merkost.drawablepreview.factories.Constants

class ShowDrawablePreviewAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file != null && file.isPreviewable()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val psiFile = ApplicationManager.getApplication().runReadAction<com.intellij.psi.PsiFile?> {
            PsiManager.getInstance(project).findFile(virtualFile)
        } ?: return

        val panel = ApplicationManager.getApplication().runReadAction<DrawablePreviewPanel> {
            DrawablePreviewPanel(psiFile)
        }

        JBPopupFactory.getInstance()
            .createComponentPopupBuilder(panel, panel)
            .setTitle(virtualFile.name)
            .setResizable(true)
            .setMovable(true)
            .setRequestFocus(true)
            .setCancelOnClickOutside(true)
            .setCancelOnWindowDeactivation(false)
            .createPopup()
            .showCenteredInCurrentWindow(project)
    }

    private fun VirtualFile.isPreviewable(): Boolean {
        if (isDirectory) return false
        val name = name
        return name.endsWith(Constants.XML_TYPE, ignoreCase = true) ||
                name.endsWith(Constants.SVG_TYPE, ignoreCase = true) ||
                BITMAP_EXTENSIONS.any { name.endsWith(it, ignoreCase = true) }
    }

    private companion object {
        private val BITMAP_EXTENSIONS = listOf(".png", ".jpg", ".jpeg", ".webp", ".gif")
    }
}
