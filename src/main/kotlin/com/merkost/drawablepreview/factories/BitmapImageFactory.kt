package com.merkost.drawablepreview.factories

import com.merkost.drawablepreview.drawables.Utils
import com.merkost.drawablepreview.settings.SettingsUtils
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object BitmapImageFactory {

    private const val NINE_PATCH_SUFFIX = ".9.png"

    fun createBitmapImage(path: String): BufferedImage? {
        val raw = ImageIO.read(File(path)) ?: return null
        val source = if (path.endsWith(NINE_PATCH_SUFFIX, ignoreCase = true)) {
            stripNinePatchBorders(raw) ?: raw
        } else {
            raw
        }
        val output = BufferedImage(
            SettingsUtils.getPreviewSize(),
            SettingsUtils.getPreviewSize(),
            BufferedImage.TYPE_INT_ARGB,
        )
        Utils.drawResizedIcon(source, output)
        return output
    }

    /**
     * Strip the 1-pixel border that 9-patch PNGs use to encode stretch and
     * content markers. Without this the project-view preview shows the
     * dotted black markers as ugly border lines.
     *
     * Returns null when the image is too small to be a valid 9-patch.
     */
    internal fun stripNinePatchBorders(image: BufferedImage): BufferedImage? {
        if (image.width <= 2 || image.height <= 2) return null
        return image.getSubimage(1, 1, image.width - 2, image.height - 2)
    }
}
