package com.merkost.drawablepreview.drawables.dom

import com.merkost.drawablepreview.drawables.ItemDrawableInflater
import com.merkost.drawablepreview.drawables.forEachAsElement
import org.w3c.dom.Element
import java.awt.image.BufferedImage

class LevelListDrawable : Drawable() {

    companion object {
        private const val ITEM_TAG = "item"
    }

    private var drawable: Drawable? = null

    override fun inflate(element: Element) {
        element.childNodes?.forEachAsElement { childElement ->
            if (childElement.tagName == ITEM_TAG) {
                drawable = ItemDrawableInflater.getDrawableWithInflate(childElement)
                return
            }
        }
    }

    override fun draw(outputImage: BufferedImage) {
        drawable?.draw(outputImage)
    }
}
