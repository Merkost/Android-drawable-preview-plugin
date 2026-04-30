package com.merkost.drawablepreview.factories

import com.intellij.util.ui.UIUtil

object Constants {
    // Recomputed on each access so monitor changes (retina ↔ non-retina) are
    // picked up without restarting the IDE.
    val ICON_SIZE: Int get() = if (UIUtil.isRetina()) 36 else 16
    const val XML_TYPE = ".xml"
    const val SVG_TYPE = ".svg"

    // Resource folder prefixes — match path segments like "drawable" or
    // "drawable-night-v21", but not arbitrary substrings like "drawable_helpers".
    val SUPPORTED_FOLDER_PREFIXES = setOf("drawable", "mipmap")
}