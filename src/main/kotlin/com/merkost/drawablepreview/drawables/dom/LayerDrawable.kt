package com.merkost.drawablepreview.drawables.dom

import com.merkost.drawablepreview.drawables.ItemDrawableInflater
import com.merkost.drawablepreview.drawables.Utils
import com.merkost.drawablepreview.drawables.forEachAsElement
import org.w3c.dom.Element
import java.awt.image.BufferedImage

class LayerDrawable : Drawable() {

    companion object {
        private const val ITEM_TAG = "item"
        private const val MAX_PADDING_FRACTION = 0.3F
    }

    private val drawables = ArrayList<LayerDrawableItem>()

    override fun inflate(element: Element) {
        super.inflate(element)

        element.childNodes?.forEachAsElement { childNode ->
            if (childNode.tagName == ITEM_TAG) {
                drawables.add(LayerDrawableItem((childNode)))
            }
        }
    }

    override fun draw(outputImage: BufferedImage) {
        super.draw(outputImage)
        resolveDimens(outputImage)
        drawables.forEach { it.draw(outputImage) }
    }

    private fun resolveDimens(image: BufferedImage) {
        // Find the largest specified padding so we can scale all of them
        // proportionally to fit inside MAX_PADDING_FRACTION of the preview.
        val maxPaddingArg = drawables
            .flatMap { listOf(it.left, it.top, it.right, it.bottom) }
            .maxOrNull()
            ?.toFloat()
            ?: 0F
        val maxPaddingWidth = image.width * MAX_PADDING_FRACTION

        drawables.forEach { item ->
            item.width = image.width
            item.height = image.height

            // Without any specified padding everything stretches edge-to-edge,
            // which matches Android's default for layer-list items.
            if (maxPaddingArg > 0F) {
                item.left = ((item.left / maxPaddingArg) * maxPaddingWidth).toInt()
                item.top = ((item.top / maxPaddingArg) * maxPaddingWidth).toInt()
                item.right = ((item.right / maxPaddingArg) * maxPaddingWidth).toInt()
                item.bottom = ((item.bottom / maxPaddingArg) * maxPaddingWidth).toInt()
            }
        }
        // Note: we ignore explicit android:width/android:height on items.
        // Implementing them properly requires per-item gravity resolution; the
        // preview is approximate by design and we'd rather render a slightly
        // wrong layer than crash the project view.
    }
}

class LayerDrawableItem(element: Element) : Drawable() {
    companion object {
        private const val WIDTH = "android:width"
        private const val HEIGHT = "android:height"
        private const val TOP = "android:top"
        private const val LEFT = "android:left"
        private const val RIGHT = "android:right"
        private const val BOTTOM = "android:bottom"
        private const val START = "android:start"
        private const val END = "android:end"
    }

    var width = 0
    var height = 0

    var top = 0
    var left = 0
    var right = 0
    var bottom = 0

    var drawable: Drawable? = null

    init {
        inflate(element)
    }

    override fun inflate(element: Element) {
        super.inflate(element)

        width = Utils.parseAttributeAsInt(element.getAttribute(WIDTH), width)
        height = Utils.parseAttributeAsInt(element.getAttribute(HEIGHT), height)

        top = Utils.parseAttributeAsInt(element.getAttribute(TOP), top)
        left = Utils.parseAttributeAsInt(element.getAttribute(LEFT), left)
        right = Utils.parseAttributeAsInt(element.getAttribute(RIGHT), right)
        bottom = Utils.parseAttributeAsInt(element.getAttribute(BOTTOM), bottom)
        left = Utils.parseAttributeAsInt(element.getAttribute(START), left)
        right = Utils.parseAttributeAsInt(element.getAttribute(END), right)

        drawable = ItemDrawableInflater.getDrawableWithInflate(element)
    }

    override fun draw(outputImage: BufferedImage) {
        super.draw(outputImage)
        drawable?.also { drawable ->
            val resolvedWidth = width - left - right
            val resolvedHeight = height - top - bottom
            if (resolvedWidth <= 0 || resolvedHeight <= 0) {
                return
            }

            BufferedImage(resolvedWidth, resolvedHeight, BufferedImage.TYPE_INT_ARGB).also { imageWithInsets ->
                drawable.draw(imageWithInsets)
                outputImage.graphics.apply {
                    drawImage(imageWithInsets, left, top, resolvedWidth, resolvedHeight, null)
                    dispose()
                }
            }
        }
    }
}