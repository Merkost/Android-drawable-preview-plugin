package com.merkost.drawablepreview.drawables

import com.merkost.drawablepreview.drawables.dom.ColorDrawable
import com.merkost.drawablepreview.drawables.dom.Drawable
import com.merkost.drawablepreview.drawables.dom.IconDrawable
import com.merkost.drawablepreview.factories.IconPreviewFactory
import com.merkost.drawablepreview.factories.XmlImageFactory
import org.w3c.dom.Element

object ItemDrawableInflater {

    private const val DRAWABLE = "android:drawable"

    /**
     * Result of looking up the drawable referenced by an `<item>` element.
     *
     *  - [Direct] — `<item android:drawable="...">`. The drawable came from
     *    an attribute lookup and is already inflated; no element to inflate
     *    against.
     *  - [Wrapped] — `<item><shape/></item>`. The drawable is a child of the
     *    item element and must be inflated against [element].
     *  - [None] — neither attribute nor child; nothing to render.
     */
    sealed class Result {
        data class Direct(val drawable: Drawable) : Result()
        data class Wrapped(val element: Element, val drawable: Drawable) : Result()
        object None : Result()

        val drawableOrNull: Drawable?
            get() = when (this) {
                is Direct -> drawable
                is Wrapped -> drawable
                None -> null
            }
    }

    fun resolve(element: Element): Result = when {
        element.hasAttribute(DRAWABLE) ->
            getDrawableFromAttribute(element)?.let(Result::Direct) ?: Result.None
        element.hasChildNodes() ->
            firstChildElementDrawable(element)?.let { (childElement, drawable) ->
                Result.Wrapped(childElement, drawable)
            } ?: Result.None
        else -> Result.None
    }

    /** Resolve and inflate against the wrapped element when one is present. */
    fun getDrawableWithInflate(element: Element): Drawable? = when (val r = resolve(element)) {
        is Result.Direct -> r.drawable
        is Result.Wrapped -> r.drawable.also { it.inflate(r.element) }
        Result.None -> null
    }

    private fun getDrawableFromAttribute(element: Element): Drawable? {
        val ref = element.getAttribute(DRAWABLE)
        if (ref.startsWith("#")) return ColorDrawable(ref)
        return XmlImageFactory.getDrawable(ref)
            ?: Utils.getPsiFileFromPath(ref)?.let {
                IconDrawable().apply { childImage = IconPreviewFactory.getImage(it) }
            }
    }

    private fun firstChildElementDrawable(element: Element): Pair<Element, Drawable>? {
        element.childNodes?.forEachAsElement { child ->
            return child to (DrawableInflater.getDrawable(child) ?: return@forEachAsElement)
        }
        return null
    }
}
