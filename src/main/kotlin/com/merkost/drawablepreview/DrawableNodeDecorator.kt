package com.merkost.drawablepreview

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.merkost.drawablepreview.factories.Constants
import com.merkost.drawablepreview.settings.SettingsUtils
import java.io.IOException
import javax.imageio.ImageIO

/**
 * Adds a richer tooltip on Project View nodes for previewable drawables: an
 * inline 96px thumbnail, dimensions, density bucket, and file size. The
 * thumbnail is base64-embedded so the tooltip's HTML renderer doesn't need
 * file access at hover time.
 */
class DrawableNodeDecorator : ProjectViewNodeDecorator {

    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
        if (!SettingsUtils.isEnabled()) return
        val file = node.virtualFile ?: return
        if (file.isDirectory || !file.isPreviewable()) return

        val tooltip = buildTooltip(file) ?: return
        data.tooltip = tooltip
    }

    private fun buildTooltip(file: VirtualFile): String? = try {
        val parts = mutableListOf<String>()
        dimensionsOf(file)?.let(parts::add)
        densityBucketOf(file)?.let(parts::add)
        parts += humanSize(file.length)

        val name = htmlEscape(file.name)
        val meta = parts.joinToString("&nbsp;&nbsp;•&nbsp;&nbsp;")
        val thumb = TooltipThumbnail.dataUriFor(file)?.let {
            "<img src='$it' width='96' height='96'/><br/>"
        } ?: ""

        """
        <html><body style='margin:6px'>
        $thumb<b>$name</b><br/>
        <span style='color:#888'>$meta</span>
        </body></html>
        """.trimIndent()
    } catch (e: Exception) {
        LOG.debug("Tooltip metadata failed for ${file.path}", e)
        null
    }

    private fun dimensionsOf(file: VirtualFile): String? {
        val name = file.name
        return when {
            name.endsWith(Constants.XML_TYPE, ignoreCase = true) -> "vector (xml)"
            name.endsWith(Constants.SVG_TYPE, ignoreCase = true) -> "vector (svg)"
            else -> readRasterDimensions(file)
        }
    }

    /**
     * Reads only the image header — much cheaper than full decode for a tooltip
     * that may fire dozens of times per second during scroll.
     */
    private fun readRasterDimensions(file: VirtualFile): String? = try {
        file.inputStream.use { input ->
            val stream = ImageIO.createImageInputStream(input) ?: return null
            stream.use {
                val readers = ImageIO.getImageReaders(it)
                if (!readers.hasNext()) return null
                val reader = readers.next()
                reader.input = it
                val w = reader.getWidth(0)
                val h = reader.getHeight(0)
                reader.dispose()
                "$w × $h px"
            }
        }
    } catch (e: IOException) {
        null
    }

    private fun densityBucketOf(file: VirtualFile): String? {
        val parent = file.parent?.name ?: return null
        if (!parent.startsWith("drawable") && !parent.startsWith("mipmap")) return null
        val qualifiers = parent.substringAfter('-', missingDelimiterValue = "")
        if (qualifiers.isEmpty()) return null
        return qualifiers
    }

    private fun humanSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        return "%.1f MB".format(kb / 1024.0)
    }

    private fun htmlEscape(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

    private fun VirtualFile.isPreviewable(): Boolean {
        val n = name
        return n.endsWith(Constants.XML_TYPE, ignoreCase = true) ||
                n.endsWith(Constants.SVG_TYPE, ignoreCase = true) ||
                BITMAP_EXTENSIONS.any { n.endsWith(it, ignoreCase = true) }
    }

    private companion object {
        private val LOG = Logger.getInstance(DrawableNodeDecorator::class.java)
        private val BITMAP_EXTENSIONS = listOf(".png", ".jpg", ".jpeg", ".webp", ".gif", ".bmp")
    }
}
