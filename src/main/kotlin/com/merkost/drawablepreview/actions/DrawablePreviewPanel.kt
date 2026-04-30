package com.merkost.drawablepreview.actions

import com.intellij.psi.PsiFile
import com.intellij.util.ui.JBUI
import com.merkost.drawablepreview.factories.IconPreviewFactory
import com.merkost.drawablepreview.settings.SettingsUtils
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import javax.swing.BorderFactory
import javax.swing.ButtonGroup
import javax.swing.JPanel
import javax.swing.JToggleButton

/**
 * Popup panel that renders [psiFile] at [renderSize] over a switchable
 * background. The render is performed once on construction (cheap because we
 * already have a cached entry for the project-view size and a few extra
 * one-shot renders won't hurt) and reused as the user toggles backgrounds.
 */
class DrawablePreviewPanel(
    psiFile: PsiFile,
    private val renderSize: Int = DEFAULT_SIZE,
) : JPanel(BorderLayout()) {

    private val rendered: BufferedImage? = SettingsUtils.withRenderSize(renderSize) {
        IconPreviewFactory.getImage(psiFile)
    }

    private val canvas = PreviewCanvas(rendered, renderSize)

    init {
        border = JBUI.Borders.empty(8)
        add(canvas, BorderLayout.CENTER)
        add(buildBackgroundChooser(), BorderLayout.SOUTH)
    }

    private fun buildBackgroundChooser(): JPanel {
        val row = JPanel(FlowLayout(FlowLayout.CENTER, 4, 4))
        val group = ButtonGroup()
        PreviewBackground.values().forEach { bg ->
            val button = JToggleButton(bg.displayName, bg == PreviewBackground.DEFAULT).apply {
                isFocusable = false
                addActionListener { canvas.setBackground(bg) }
            }
            group.add(button)
            row.add(button)
        }
        return row
    }

    private class PreviewCanvas(
        private val image: BufferedImage?,
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
                if (image != null) {
                    val x = (width - image.width) / 2
                    val y = (height - image.height) / 2
                    g2.drawImage(image, x, y, null)
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
    }
}
