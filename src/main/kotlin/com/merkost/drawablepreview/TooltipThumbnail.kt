package com.merkost.drawablepreview

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.merkost.drawablepreview.factories.IconPreviewFactory
import com.merkost.drawablepreview.settings.SettingsUtils
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.Collections
import javax.imageio.ImageIO

/**
 * Renders a base64 PNG data URI for the tooltip thumbnail. Cached by
 * (path, modificationStamp) so each hover doesn't re-render and re-encode.
 */
object TooltipThumbnail {

    private const val SIZE = 96
    private const val MAX_CACHE_ENTRIES = 200

    private data class Key(val path: String, val modificationStamp: Long)

    private val LOG = Logger.getInstance(TooltipThumbnail::class.java)

    private val cache: MutableMap<Key, String?> = Collections.synchronizedMap(
        object : LinkedHashMap<Key, String?>(MAX_CACHE_ENTRIES, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<Key, String?>?): Boolean =
                size > MAX_CACHE_ENTRIES
        }
    )

    /**
     * Returns a `data:image/png;base64,…` string at [SIZE]px, or null when the
     * file can't be rendered. Safe to call from any thread.
     */
    fun dataUriFor(file: VirtualFile): String? {
        val key = Key(file.path, file.modificationStamp)
        cache[key]?.let { return it }

        val dataUri = try {
            renderAndEncode(file)
        } catch (e: Throwable) {
            LOG.debug("Tooltip thumbnail failed for ${file.path}", e)
            null
        }
        cache[key] = dataUri
        return dataUri
    }

    private fun renderAndEncode(file: VirtualFile): String? {
        val image = SettingsUtils.withRenderSize(SIZE) {
            ApplicationManager.getApplication().runReadAction<BufferedImage?> {
                val psiFile = ProjectManager.getInstance().openProjects
                    .firstNotNullOfOrNull { PsiManager.getInstance(it).findFile(file) }
                    ?: return@runReadAction null
                IconPreviewFactory.getImage(psiFile)
            }
        } ?: return null
        return "data:image/png;base64,${encodeToBase64Png(image)}"
    }

    private fun encodeToBase64Png(image: BufferedImage): String {
        val bytes = ByteArrayOutputStream(8 * 1024).use { out ->
            ImageIO.write(image, "png", out)
            out.toByteArray()
        }
        return Base64.getEncoder().encodeToString(bytes)
    }
}
