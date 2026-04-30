package com.merkost.drawablepreview.actions

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.merkost.drawablepreview.factories.IconPreviewFactory
import com.merkost.drawablepreview.settings.SettingsUtils
import java.awt.image.BufferedImage
import java.io.File

/** A density bucket for a particular drawable resource. */
data class DensityVariant(
    val bucket: String,
    val file: File,
    val image: BufferedImage?,
    val intrinsicWidth: Int?,
    val intrinsicHeight: Int?,
)

object DensityVariants {

    private val LOG = Logger.getInstance(DensityVariants::class.java)

    // Ordered low → high density. Anything else (anydpi, nodpi, tvdpi, …)
    // is included if a file exists, in trailing order.
    private val KNOWN_BUCKETS = listOf("ldpi", "mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")

    /**
     * If [path] is a raster drawable inside a `drawable-<density>/` folder,
     * find sibling files of the same name in every other density bucket and
     * render each at [renderSize]. Returns null when there are no other
     * variants (so the caller can hide the row).
     */
    fun extract(path: String, renderSize: Int): List<DensityVariant>? {
        val file = File(path)
        val parent = file.parentFile ?: return null
        val parentName = parent.name
        if (!parentName.startsWith("drawable-")) return null

        val resDir = parent.parentFile ?: return null
        val name = file.name

        val buckets = mutableListOf<String>()
        buckets += KNOWN_BUCKETS
        // Also include the source's own bucket if it's not already in the list
        // (covers edge cases like drawable-xxxhdpi-v24).
        val ownBucket = parentName.removePrefix("drawable-")
        if (ownBucket !in KNOWN_BUCKETS) buckets += ownBucket

        val variants = buckets.mapNotNull { bucket ->
            val candidate = File(resDir, "drawable-$bucket/$name")
            if (!candidate.exists()) null
            else renderVariant(bucket, candidate, renderSize)
        }
        // Hide the row when there's only the file the user already opened.
        return if (variants.size <= 1) null else variants
    }

    private fun renderVariant(bucket: String, file: File, renderSize: Int): DensityVariant? = try {
        val (image, w, h) = renderViaIconFactory(file, renderSize)
        DensityVariant(bucket, file, image, w, h)
    } catch (e: Throwable) {
        LOG.debug("Density variant render failed for ${file.path}", e)
        null
    }

    private fun renderViaIconFactory(file: File, renderSize: Int): Triple<BufferedImage?, Int?, Int?> {
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(file)
            ?: return Triple(null, null, null)
        return SettingsUtils.withRenderSize(renderSize) {
            ApplicationManager.getApplication().runReadAction<Triple<BufferedImage?, Int?, Int?>> {
                val psiFile = ProjectManager.getInstance().openProjects
                    .firstNotNullOfOrNull { PsiManager.getInstance(it).findFile(virtualFile) }
                    ?: return@runReadAction Triple(null, null, null)
                val image = IconPreviewFactory.getImage(psiFile)
                val (w, h) = readIntrinsicDimensions(file)
                Triple(image, w, h)
            }
        }
    }

    private fun readIntrinsicDimensions(file: File): Pair<Int?, Int?> = try {
        file.inputStream().use { input ->
            val stream = javax.imageio.ImageIO.createImageInputStream(input) ?: return null to null
            stream.use {
                val readers = javax.imageio.ImageIO.getImageReaders(it)
                if (!readers.hasNext()) return null to null
                val reader = readers.next()
                reader.input = it
                val w = reader.getWidth(0)
                val h = reader.getHeight(0)
                reader.dispose()
                w to h
            }
        }
    } catch (e: Exception) {
        null to null
    }
}
