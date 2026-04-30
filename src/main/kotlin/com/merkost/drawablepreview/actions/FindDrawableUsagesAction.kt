package com.merkost.drawablepreview.actions

import com.intellij.find.FindManager
import com.intellij.find.findInProject.FindInProjectManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.vfs.VirtualFile
import com.merkost.drawablepreview.factories.Constants

/**
 * Right-click on a drawable file and pick "Find Drawable Usages" to open
 * IntelliJ's native Find-in-Files panel preloaded with a regex that catches
 * Kotlin/Java code references and Android XML layout references:
 *
 *   - `R.drawable.foo`            (Java/Kotlin Android)
 *   - `Res.drawable.foo`          (Compose Multiplatform)
 *   - `@drawable/foo`             (Android layout/style XML)
 *   - `painterResource(.foo)`     (loose CMP — caught by the @drawable
 *                                  pattern only via the literal name)
 */
class FindDrawableUsagesAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = file != null && file.isDrawable()
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val name = baseName(file.name)

        val findManager = FindManager.getInstance(project)
        val model = findManager.findInProjectModel.clone().apply {
            stringToFind = """\b(R|Res)\.drawable\.${Regex.escape(name)}\b|@drawable/${Regex.escape(name)}\b"""
            isRegularExpressions = true
            isCaseSensitive = true
            isWholeWordsOnly = false
            isProjectScope = true
        }
        FindInProjectManager.getInstance(project).findInProject(e.dataContext, model)
    }

    private fun VirtualFile.isDrawable(): Boolean {
        if (isDirectory) return false
        val n = name
        return n.endsWith(Constants.XML_TYPE, ignoreCase = true) ||
                n.endsWith(Constants.SVG_TYPE, ignoreCase = true) ||
                BITMAP_EXTENSIONS.any { n.endsWith(it, ignoreCase = true) }
    }

    private fun baseName(filename: String): String {
        val lower = filename.lowercase()
        return when {
            lower.endsWith(".9.png") -> filename.substring(0, filename.length - ".9.png".length)
            else -> filename.substringBeforeLast('.')
        }
    }

    private companion object {
        private val BITMAP_EXTENSIONS = listOf(".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp")
    }
}
