package com.merkost.drawablepreview.drawables.dom

import com.merkost.drawablepreview.factories.XmlImageFactory
import org.w3c.dom.Element
import java.awt.image.BufferedImage

/**
 * Static-frame preview of `<animated-vector>`. The tag wraps a regular
 * `<vector>` (referenced via `android:drawable="@drawable/foo"`) plus
 * `<target>` animator declarations we don't simulate. We render the
 * referenced vector at its rest position — close enough for project-view
 * previews where motion isn't observable anyway.
 */
class AnimatedVectorDrawable : Drawable() {

    companion object {
        private const val DRAWABLE_ATTR = "android:drawable"
    }

    private var inner: Drawable? = null

    override fun inflate(element: Element) {
        val ref = element.getAttribute(DRAWABLE_ATTR)
        if (ref.isNullOrEmpty()) return
        inner = XmlImageFactory.getDrawable(ref)
    }

    override fun draw(outputImage: BufferedImage) {
        inner?.draw(outputImage)
    }
}
