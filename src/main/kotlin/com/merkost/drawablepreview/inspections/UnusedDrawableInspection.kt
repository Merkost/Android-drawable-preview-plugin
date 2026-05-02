package com.merkost.drawablepreview.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.UsageSearchContext
import com.merkost.drawablepreview.factories.Constants

/**
 * Flags drawable files (XML / SVG / PNG / JPG / WebP / GIF) that have no
 * R.drawable / Res.drawable / @drawable references anywhere in the project.
 * Likely-unused resources are dead weight in the APK / artifact.
 *
 * Detection is conservative: a single textual occurrence of the base name
 * inside a code or XML file is enough to mark the drawable "in use", so
 * false positives are rare. False negatives can happen for reflective /
 * dynamic lookups (`getIdentifier("foo", "drawable", ...)`) which we don't
 * try to resolve.
 */
class UnusedDrawableInspection : LocalInspectionTool() {

    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean,
    ): Array<ProblemDescriptor>? {
        val virtualFile = file.virtualFile ?: return null
        if (!virtualFile.isPreviewableDrawable()) return null
        if (!virtualFile.isInDrawableFolder()) return null

        val baseName = baseName(virtualFile.name)
        if (baseName.isEmpty()) return null

        val project = file.project
        val helper = PsiSearchHelper.getInstance(project)
        val scope = GlobalSearchScope.projectScope(project)

        val foundReference = runReadAction {
            var found = false
            // We search for the bare name and then verify it's used in a
            // drawable context. processElementsWithWord short-circuits on the
            // first match returning false from the processor.
            helper.processElementsWithWord(
                { element, _ ->
                    val text = element.containingFile?.text ?: return@processElementsWithWord true
                    val offset = element.textRange.startOffset
                    if (looksLikeDrawableRef(text, offset, baseName)) {
                        found = true
                        return@processElementsWithWord false
                    }
                    true
                },
                scope,
                baseName,
                (UsageSearchContext.IN_CODE.toInt() or
                        UsageSearchContext.IN_FOREIGN_LANGUAGES.toInt() or
                        UsageSearchContext.IN_STRINGS.toInt()).toShort(),
                /* caseSensitive = */ true,
            )
            found
        }

        if (foundReference) return null

        val descriptor = manager.createProblemDescriptor(
            file,
            "Drawable '$baseName' is not referenced from any code or layout XML",
            isOnTheFly,
            emptyArray(),
            ProblemHighlightType.LIKE_UNUSED_SYMBOL,
        )
        return arrayOf(descriptor)
    }

    /** Look for `R.drawable.foo`, `Res.drawable.foo`, or `@drawable/foo` near [offset]. */
    private fun looksLikeDrawableRef(text: String, offset: Int, baseName: String): Boolean {
        val end = offset + baseName.length
        if (end > text.length) return false
        // Verify the matched word IS our base name (helper search may match
        // sub-words in some edge cases — defensive double-check).
        if (text.regionMatches(offset, baseName, 0, baseName.length, ignoreCase = false).not()) {
            return false
        }
        val before = if (offset > 0) text.substring(maxOf(0, offset - 16), offset) else ""
        return DRAWABLE_PREFIX_REGEX.containsMatchIn(before + baseName)
    }

    private fun VirtualFile.isPreviewableDrawable(): Boolean {
        val n = name.lowercase()
        return n.endsWith(Constants.XML_TYPE) ||
                n.endsWith(Constants.SVG_TYPE) ||
                BITMAP_EXTENSIONS.any { n.endsWith(it) }
    }

    private fun VirtualFile.isInDrawableFolder(): Boolean {
        val parentName = parent?.name ?: return false
        return parentName.startsWith("drawable") || parentName.startsWith("mipmap")
    }

    private fun baseName(filename: String): String {
        val lower = filename.lowercase()
        return when {
            lower.endsWith(".9.png") -> filename.substring(0, filename.length - ".9.png".length)
            else -> filename.substringBeforeLast('.')
        }
    }

    override fun getDisplayName(): String = "Unused drawable resource"
    override fun getGroupDisplayName(): String = "Android"
    override fun getShortName(): String = "DrawablePreviewUnusedDrawable"

    private companion object {
        private val BITMAP_EXTENSIONS = listOf(".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp")
        private val DRAWABLE_PREFIX_REGEX = Regex(
            """(R\.drawable\.|Res\.drawable\.|@drawable/)[A-Za-z_][A-Za-z0-9_]*$""",
        )
    }
}
