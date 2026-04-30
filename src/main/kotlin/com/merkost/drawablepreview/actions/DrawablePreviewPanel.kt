package com.merkost.drawablepreview.actions

import com.intellij.psi.PsiFile
import com.intellij.util.ui.JBUI
import com.merkost.drawablepreview.drawables.MaskShape
import com.merkost.drawablepreview.drawables.dom.AdaptiveIconDrawable
import com.merkost.drawablepreview.factories.IconPreviewFactory
import com.merkost.drawablepreview.factories.XmlImageFactory
import com.merkost.drawablepreview.settings.SettingsUtils
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JToggleButton

/**
 * Popup panel that renders [psiFile] at [renderSize] over a switchable
 * background. Backgrounds and (for adaptive icons) mask shapes are toggled
 * live; we re-render only when the underlying drawable settings change.
 */
class DrawablePreviewPanel(
    private val psiFile: PsiFile,
    private val renderSize: Int = DEFAULT_SIZE,
) : JPanel(BorderLayout()) {

    private val isAdaptiveIcon: Boolean = isAdaptiveIcon(psiFile)
    private var maskShape: MaskShape = SettingsUtils.getPersistedMaskShape()

    // Lazily-evaluated selector states; null if the file isn't a selector.
    private val selectorStates: List<SelectorState>? = psiFile.virtualFile?.path?.let {
        SelectorStates.extract(it, renderSize)
    }

    private val canvas = PreviewCanvas(initialImage(), renderSize)
    private val palette = PaletteStrip(initialImage())

    init {
        border = JBUI.Borders.empty(8)
        add(canvas, BorderLayout.CENTER)
        add(buildControls(), BorderLayout.SOUTH)
    }

    private fun initialImage(): BufferedImage? = selectorStates?.firstOrNull()?.image
        ?: renderImage()

    private fun renderImage(): BufferedImage? = SettingsUtils.withRenderSize(renderSize) {
        SettingsUtils.withMaskShape(maskShape) {
            IconPreviewFactory.getImage(psiFile)
        }
    }

    private fun buildControls(): JPanel {
        val column = JPanel()
        column.layout = javax.swing.BoxLayout(column, javax.swing.BoxLayout.Y_AXIS)
        if (selectorStates != null && selectorStates.size > 1) {
            column.add(buildSelectorStateChooser(selectorStates))
            column.add(Box.createVerticalStrut(4))
        }
        column.add(buildBackgroundChooser())
        if (isAdaptiveIcon) {
            column.add(Box.createVerticalStrut(4))
            column.add(buildMaskChooser())
        }
        if (palette.hasColors()) {
            column.add(Box.createVerticalStrut(4))
            column.add(palette)
        }
        column.add(Box.createVerticalStrut(4))
        column.add(buildActionsRow())
        return column
    }

    private fun buildActionsRow(): JPanel {
        val row = JPanel(FlowLayout(FlowLayout.CENTER, 4, 4))
        row.add(JButton("Copy as PNG").apply {
            isFocusable = false
            addActionListener { copyToClipboard() }
        })
        return row
    }

    private fun copyToClipboard() {
        val image = canvas.currentImage() ?: return
        val transferable = ImageTransferable(image)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
    }

    /**
     * Horizontal row of clickable color chips extracted from the rendered
     * image. Click a chip to copy its hex value to the clipboard.
     */
    private class PaletteStrip(initialImage: BufferedImage?) : JPanel(FlowLayout(FlowLayout.CENTER, 4, 4)) {
        init {
            setImage(initialImage)
        }

        fun hasColors(): Boolean = componentCount > 1  // > 1 because of the leading label

        fun setImage(image: BufferedImage?) {
            removeAll()
            val colors = if (image != null) ColorPalette.extract(image) else emptyList()
            if (colors.isEmpty()) {
                revalidate(); repaint(); return
            }
            add(JLabel("Palette:"))
            colors.forEach { color -> add(buildChip(color)) }
            revalidate(); repaint()
        }

        private fun buildChip(color: Color): JPanel {
            val hex = ColorPalette.toHex(color)
            return object : JPanel() {
                init {
                    preferredSize = Dimension(24, 24)
                    background = color
                    border = BorderFactory.createLineBorder(JBUI.CurrentTheme.Label.foreground(), 1)
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    toolTipText = "Copy $hex"
                    addMouseListener(object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent) {
                            Toolkit.getDefaultToolkit().systemClipboard
                                .setContents(StringSelection(hex), null)
                            toolTipText = "Copied $hex"
                        }
                    })
                }
            }
        }
    }

    private class ImageTransferable(private val image: BufferedImage) : Transferable {
        override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(DataFlavor.imageFlavor)
        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == DataFlavor.imageFlavor
        override fun getTransferData(flavor: DataFlavor): Any {
            if (flavor != DataFlavor.imageFlavor) throw UnsupportedFlavorException(flavor)
            return image
        }
    }

    private fun buildSelectorStateChooser(states: List<SelectorState>): JPanel = chooserRow(
        label = "State",
        items = states,
        default = states.first(),
        labelOf = { it.label },
    ) {
        canvas.setImage(it.image)
        palette.setImage(it.image)
    }

    private fun buildBackgroundChooser(): JPanel = chooserRow(
        label = "Background",
        items = PreviewBackground.values().asList(),
        default = PreviewBackground.DEFAULT,
        labelOf = { it.displayName },
    ) { canvas.setBackground(it) }

    private fun buildMaskChooser(): JPanel = chooserRow(
        label = "Mask",
        items = MaskShape.values().asList(),
        default = maskShape,
        labelOf = { it.displayName },
    ) {
        maskShape = it
        SettingsUtils.setPersistedMaskShape(it)
        val rendered = renderImage()
        canvas.setImage(rendered)
        palette.setImage(rendered)
    }

    private fun <T> chooserRow(
        label: String,
        items: List<T>,
        default: T,
        labelOf: (T) -> String,
        onSelected: (T) -> Unit,
    ): JPanel {
        val row = JPanel(FlowLayout(FlowLayout.CENTER, 4, 4))
        row.add(JLabel("$label:"))
        val group = ButtonGroup()
        items.forEach { item ->
            val button = JToggleButton(labelOf(item), item == default).apply {
                isFocusable = false
                addActionListener { onSelected(item) }
            }
            group.add(button)
            row.add(button)
        }
        return row
    }

    private class PreviewCanvas(
        private var image: BufferedImage?,
        renderSize: Int,
    ) : JPanel() {

        private var bgChoice: PreviewBackground = PreviewBackground.DEFAULT

        init {
            preferredSize = Dimension(renderSize + 32, renderSize + 32)
            border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
            isOpaque = false
        }

        fun setBackground(choice: PreviewBackground) {
            bgChoice = choice
            repaint()
        }

        fun setImage(image: BufferedImage?) {
            this.image = image
            repaint()
        }

        fun currentImage(): BufferedImage? = image

        override fun paintComponent(g: Graphics) {
            val g2 = g.create() as Graphics2D
            try {
                g2.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                )
                g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON,
                )
                bgChoice.paint(g2, width, height)
                val current = image
                if (current != null) {
                    val x = (width - current.width) / 2
                    val y = (height - current.height) / 2
                    g2.drawImage(current, x, y, null)
                } else {
                    val message = "Could not render preview"
                    val fm = g2.fontMetrics
                    g2.color = JBUI.CurrentTheme.Label.foreground()
                    g2.drawString(
                        message,
                        (width - fm.stringWidth(message)) / 2,
                        height / 2,
                    )
                }
            } finally {
                g2.dispose()
            }
        }
    }

    companion object {
        const val DEFAULT_SIZE = 256

        private fun isAdaptiveIcon(file: PsiFile): Boolean {
            val path = file.virtualFile?.path ?: return false
            if (!path.endsWith(".xml", ignoreCase = true)) return false
            return XmlImageFactory.getDrawable(path) is AdaptiveIconDrawable
        }
    }
}
