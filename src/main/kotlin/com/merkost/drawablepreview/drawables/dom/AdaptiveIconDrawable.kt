package com.merkost.drawablepreview.drawables.dom

import com.merkost.drawablepreview.drawables.ItemDrawableInflater
import com.merkost.drawablepreview.drawables.forEachAsElement
import org.w3c.dom.Element
import java.awt.AlphaComposite
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage

class AdaptiveIconDrawable : Drawable() {

    companion object {
        private const val BACKGROUND = "background"
        private const val FOREGROUND = "foreground"

        // Android adaptive icons: 108dp canvas, 72dp safe zone. Foreground/
        // background extend into a 33% bleed area that is masked off by the
        // launcher. We mimic this by rendering at canvas size, then center-
        // cropping the safe zone into the output.
        private const val SAFE_ZONE_FRACTION = 72f / 108f
    }

    private var background: Drawable? = null
    private var foreground: Drawable? = null

    override fun inflate(element: Element) {
        super.inflate(element)
        element.childNodes?.forEachAsElement { childNode ->
            ItemDrawableInflater.getDrawableWithInflate(childNode)?.also { drawable ->
                when (childNode.tagName) {
                    BACKGROUND -> background = drawable
                    FOREGROUND -> foreground = drawable
                }
            }
        }
    }

    override fun draw(outputImage: BufferedImage) {
        super.draw(outputImage)
        val outputSize = minOf(outputImage.width, outputImage.height)
        if (outputSize <= 0) return

        // Render bg+fg into an oversized buffer so the safe-zone crop has the
        // bleed area to pull from. Working size is outputSize / SAFE_ZONE,
        // matching the 108dp:72dp ratio.
        val workingSize = (outputSize / SAFE_ZONE_FRACTION).toInt().coerceAtLeast(1)
        val composed = BufferedImage(workingSize, workingSize, BufferedImage.TYPE_INT_ARGB)
        background?.draw(composed)
        foreground?.draw(composed)

        // Clip to a circular mask — the most common launcher shape, and a
        // closer visual to what users see at runtime than a raw square.
        val masked = BufferedImage(outputSize, outputSize, BufferedImage.TYPE_INT_ARGB)
        masked.createGraphics().apply {
            setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            clip = Ellipse2D.Float(0f, 0f, outputSize.toFloat(), outputSize.toFloat())

            // Center-crop the safe zone of `composed` into the masked output.
            val offset = (workingSize - outputSize) / 2
            drawImage(composed, -offset, -offset, null)
            dispose()
        }

        outputImage.createGraphics().apply {
            composite = AlphaComposite.Src
            drawImage(masked, 0, 0, null)
            dispose()
        }
    }
}
