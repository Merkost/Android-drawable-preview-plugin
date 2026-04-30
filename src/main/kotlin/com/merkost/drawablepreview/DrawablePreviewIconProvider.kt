package com.merkost.drawablepreview

import com.intellij.ide.IconProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.ui.IconDeferrer
import com.intellij.util.PlatformIcons
import com.merkost.drawablepreview.factories.Constants
import com.merkost.drawablepreview.factories.IconPreviewFactory
import com.merkost.drawablepreview.settings.SettingsUtils
import javax.swing.Icon

class DrawablePreviewIconProvider : IconProvider() {

    override fun getIcon(element: PsiElement, flags: Int): Icon? {
        if (!SettingsUtils.isEnabled()) return null
        if (element !is PsiFile) return null
        val virtualFile = element.virtualFile ?: return null
        val path = virtualFile.path

        // Cheap up-front filter so we don't pay deferral cost for every PSI file
        // in the project — only files we could actually render previews for.
        if (!path.endsWithAnyOf(Constants.XML_TYPE, Constants.SVG_TYPE) &&
            !looksLikeBitmap(path)) {
            return null
        }

        // Defer the heavy rendering to a background thread; the placeholder is
        // shown until the evaluator returns.
        return IconDeferrer.getInstance().defer(
            FILE_PLACEHOLDER,
            DeferralKey(path, virtualFile.modificationStamp, SettingsUtils.getPreviewSize()),
        ) { _ ->
            ApplicationManager.getApplication().runReadAction<Icon?> {
                runCatching { IconPreviewFactory.createIcon(element) }
                    .onFailure { LOG.warn("Deferred icon render failed for $path", it) }
                    .getOrNull()
            }
        }
    }

    private fun looksLikeBitmap(path: String): Boolean =
        path.endsWithAnyOf(".png", ".jpg", ".jpeg", ".webp", ".gif")

    private fun String.endsWithAnyOf(vararg suffixes: String): Boolean =
        suffixes.any { endsWith(it, ignoreCase = true) }

    private data class DeferralKey(val path: String, val modificationStamp: Long, val previewSize: Int)

    private companion object {
        private val LOG = Logger.getInstance(DrawablePreviewIconProvider::class.java)
        private val FILE_PLACEHOLDER: Icon = PlatformIcons.FILE_ICON
    }
}
