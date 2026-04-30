package com.merkost.drawablepreview.actions

import com.intellij.openapi.diagnostic.Logger
import com.merkost.drawablepreview.drawables.ItemDrawableInflater
import com.merkost.drawablepreview.drawables.forEachAsElement
import com.merkost.drawablepreview.factories.XmlImageFactory
import com.merkost.drawablepreview.settings.SettingsUtils
import org.w3c.dom.Element
import java.awt.image.BufferedImage
import java.io.File

/** A single state-conditioned variant inside a `<selector>`. */
data class SelectorState(
    val label: String,
    val image: BufferedImage?,
)

object SelectorStates {

    private val LOG = Logger.getInstance(SelectorStates::class.java)

    private const val SELECTOR = "selector"
    private const val ANIMATED_SELECTOR = "animated-selector"
    private const val ITEM = "item"
    private const val STATE_PREFIX = "android:state_"
    private const val STATE_PREFIX_LEN = STATE_PREFIX.length

    /**
     * If [path] is a selector drawable, return one [SelectorState] per
     * `<item>` child with that item's drawable rendered at [renderSize].
     * Returns null when the file isn't a selector.
     */
    fun extract(path: String, renderSize: Int): List<SelectorState>? {
        val document = XmlImageFactory.parseXmlFile(File(path)) ?: return null
        val root = document.documentElement
        val tag = root.tagName
        if (tag != SELECTOR && tag != ANIMATED_SELECTOR) return null

        val states = mutableListOf<SelectorState>()
        root.childNodes?.forEachAsElement { child ->
            if (child.tagName != ITEM) return@forEachAsElement
            val label = labelFor(child)
            val image = renderItem(child, renderSize)
            states += SelectorState(label, image)
        }
        return states
    }

    private fun labelFor(item: Element): String {
        val activeStates = mutableListOf<String>()
        val attrs = item.attributes ?: return "Default"
        for (i in 0 until attrs.length) {
            val attr = attrs.item(i)
            val name = attr.nodeName
            if (!name.startsWith(STATE_PREFIX)) continue
            val short = name.substring(STATE_PREFIX_LEN)
            val value = attr.nodeValue
            activeStates += if (value == "true") short else "$short=$value"
        }
        return if (activeStates.isEmpty()) "Default" else activeStates.joinToString(" + ")
    }

    private fun renderItem(item: Element, renderSize: Int): BufferedImage? = try {
        SettingsUtils.withRenderSize(renderSize) {
            val drawable = ItemDrawableInflater.getDrawableWithInflate(item) ?: return@withRenderSize null
            BufferedImage(renderSize, renderSize, BufferedImage.TYPE_INT_ARGB).also(drawable::draw)
        }
    } catch (e: Throwable) {
        LOG.warn("Failed to render selector state ${labelFor(item)}", e)
        null
    }
}
