package com.merkost.drawablepreview

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.IconDeferrer
import com.intellij.util.PlatformIcons
import com.merkost.drawablepreview.factories.Constants
import com.merkost.drawablepreview.factories.IconPreviewFactory
import com.merkost.drawablepreview.settings.SettingsUtils
import javax.swing.Icon

/**
 * Adds a gutter icon next to drawable references in source code:
 *   - Android:  R.drawable.foo
 *   - Compose Multiplatform:  Res.drawable.foo
 *
 * The icon is the rendered preview of the referenced drawable. Clicking
 * navigates to the file.
 *
 * Detection is text-pattern based on the parent expression so we work
 * for both Java and Kotlin without depending on either language plugin's
 * PSI types.
 */
class DrawableLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        if (!SettingsUtils.isEnabled()) return null
        // Only anchor on leaf elements to avoid creating duplicate markers.
        if (element.firstChild != null) return null

        val name = element.text ?: return null
        if (!IDENTIFIER_PATTERN.matches(name)) return null

        // Walk up: parent should be a qualified expression like
        // `R.drawable.foo` or `Res.drawable.foo`.
        val parent = element.parent ?: return null
        val parentText = parent.text ?: return null
        val match = QUALIFIED_PATTERN.matchEntire(parentText) ?: return null
        // The leaf must be the trailing identifier in the qualified expression,
        // not "R", "Res", or "drawable".
        if (match.groupValues[2] != name) return null

        val targetFile = findDrawableFile(element.project, name) ?: return null
        return buildMarker(element, targetFile)
    }

    private fun buildMarker(element: PsiElement, target: VirtualFile): LineMarkerInfo<PsiElement> {
        val placeholder = PlatformIcons.FILE_ICON
        val icon: Icon = IconDeferrer.getInstance().defer(
            placeholder,
            IconKey(target.path, target.modificationStamp),
        ) { _ ->
            renderForGutter(element, target) ?: placeholder
        }
        val tooltipText = "Drawable: ${target.name}"
        return LineMarkerInfo(
            element,
            element.textRange,
            icon,
            { tooltipText },
            { _, _ -> navigateTo(element.project, target) },
            GutterIconRenderer.Alignment.LEFT,
            { tooltipText },
        )
    }

    private fun renderForGutter(element: PsiElement, target: VirtualFile): Icon? {
        val psiManager = com.intellij.psi.PsiManager.getInstance(element.project)
        val psiFile = psiManager.findFile(target) ?: return null
        return IconPreviewFactory.createIcon(psiFile)
    }

    private fun navigateTo(project: Project, target: VirtualFile) {
        OpenFileDescriptor(project, target).navigate(true)
    }

    /**
     * Look the drawable up by name in the project's filename index. Tries
     * each previewable extension; returns the first match in a recognised
     * resource folder (drawable / mipmap / composeResources/drawable*).
     */
    private fun findDrawableFile(project: Project, name: String): VirtualFile? {
        val scope = GlobalSearchScope.allScope(project)
        for (ext in CANDIDATE_EXTENSIONS) {
            val matches = FilenameIndex.getVirtualFilesByName("$name$ext", scope)
            for (file in matches) {
                if (file.isInDrawableFolder()) return file
            }
        }
        return null
    }

    private fun VirtualFile.isInDrawableFolder(): Boolean {
        val parentName = parent?.name ?: return false
        // Android: .../drawable[-cfg]/foo.xml or .../mipmap[-cfg]/foo.png
        if (parentName.startsWith("drawable") || parentName.startsWith("mipmap")) return true
        // CMP: .../composeResources/drawable[-cfg]/foo.png — same parent name
        // but grand-parent is composeResources.
        return false
    }

    private data class IconKey(val path: String, val modificationStamp: Long)

    private companion object {
        private val IDENTIFIER_PATTERN = Regex("[A-Za-z_][A-Za-z0-9_]*")
        // Group 1: R or Res. Group 2: drawable name.
        private val QUALIFIED_PATTERN = Regex("""(R|Res)\s*\.\s*drawable\s*\.\s*([A-Za-z_][A-Za-z0-9_]*)""")
        private val CANDIDATE_EXTENSIONS = listOf(
            Constants.XML_TYPE, Constants.SVG_TYPE,
            ".png", ".jpg", ".jpeg", ".webp", ".gif", ".9.png",
        )
    }
}
