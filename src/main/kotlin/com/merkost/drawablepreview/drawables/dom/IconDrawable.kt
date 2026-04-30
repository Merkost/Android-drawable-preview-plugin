package com.merkost.drawablepreview.drawables.dom

import com.merkost.drawablepreview.drawables.Utils
import java.awt.image.BufferedImage

class IconDrawable : Drawable() {

    var childImage: BufferedImage? = null

    override fun draw(outputImage: BufferedImage) {
        childImage?.also { childImage ->
            Utils.drawResizedIcon(childImage, outputImage)
        }
    }
}
