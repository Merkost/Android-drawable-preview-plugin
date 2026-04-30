package com.merkost.drawablepreview.factories

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.RetinaImage
import com.intellij.util.ui.UIUtil
import com.merkost.drawablepreview.settings.SettingsUtils
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap
import javax.swing.Icon
import javax.swing.ImageIcon

object IconPreviewFactory {
    private val LOG = Logger.getInstance(IconPreviewFactory::class.java)

    // Active PSI manager for the in-flight render. ThreadLocal so concurrent
    // renders on background threads don't trample each other.
    private val activePsiManager = ThreadLocal<PsiManager?>()
    val psiManager: PsiManager? get() = activePsiManager.get()

    // (path, modificationStamp, length, previewSize) -> rendered icon (null = render failed).
    // Cached results survive across icon-provider invocations until invalidated by VFS change
    // or a settings change.
    private data class CacheKey(
        val path: String,
        val modificationStamp: Long,
        val length: Long,
        val previewSize: Int,
    )

    // ConcurrentHashMap can't hold null values, so we use NULL_ICON as a sentinel
    // for "we tried to render this and got nothing" — avoids re-attempting on every paint.
    private val NULL_ICON: Icon = ImageIcon()
    private val cache = ConcurrentHashMap<CacheKey, Icon>()

    fun createIcon(element: PsiElement): Icon? {
        if (element !is PsiFile) return null
        val virtualFile = element.virtualFile ?: return null
        val key = CacheKey(
            path = virtualFile.path,
            modificationStamp = virtualFile.modificationStamp,
            length = virtualFile.length,
            previewSize = SettingsUtils.getPreviewSize(),
        )
        cache[key]?.let { return it.takeUnless { cached -> cached === NULL_ICON } }

        val icon = try {
            activePsiManager.set(element.manager)
            renderIcon(virtualFile)
        } catch (e: Exception) {
            LOG.warn("Failed to create preview icon for ${virtualFile.path}", e)
            null
        } finally {
            activePsiManager.remove()
        }

        cache[key] = icon ?: NULL_ICON
        return icon
    }

    private fun renderIcon(virtualFile: VirtualFile): Icon? {
        val image = getImage(virtualFile) ?: return null
        if (UIUtil.isRetina()) {
            getRetinaIcon(image)?.let { return it }
        }
        return ImageIcon(image)
    }

    fun getImage(element: PsiFile): BufferedImage? = getImage(element.virtualFile)

    private fun getImage(virtualFile: VirtualFile): BufferedImage? {
        val path = virtualFile.path
        return when {
            path.endsWith(Constants.XML_TYPE, ignoreCase = true) -> XmlImageFactory.createXmlImage(path)
            path.endsWith(Constants.SVG_TYPE, ignoreCase = true) -> SvgImageFactory.createSvgImage(path)
            else -> BitmapImageFactory.createBitmapImage(path)
        }
    }

    private fun getRetinaIcon(image: BufferedImage): Icon? =
        RetinaImage.createFrom(image)?.let(::RetinaImageIcon)

    /** Drop all cached icons. Called when the user changes the preview size. */
    fun invalidateAll() {
        cache.clear()
    }
}
