package com.merkost.drawablepreview.toolwindow

import com.intellij.openapi.vfs.VirtualFile

/** A single previewable drawable found by [DrawableScanner]. */
data class DrawableEntry(
    val file: VirtualFile,
    /** "ic_launcher" — file name without extension(s) like .9.png */
    val baseName: String,
    /** XML, SVG, PNG, JPG, WEBP, GIF, NINE_PATCH */
    val kind: Kind,
    /** drawable / drawable-night-v21 / mipmap-anydpi-v26 / composeResources */
    val sourceFolder: String,
) {
    enum class Kind { XML, SVG, PNG, JPG, WEBP, GIF, NINE_PATCH }
}
