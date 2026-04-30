package com.merkost.drawablepreview.actions

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Paint
import java.awt.TexturePaint
import java.awt.image.BufferedImage

enum class PreviewBackground(val displayName: String) {
    LIGHT("Light") {
        override fun paint(g: Graphics2D, width: Int, height: Int) {
            g.color = Color(0xFA, 0xFA, 0xFA)
            g.fillRect(0, 0, width, height)
        }
    },
    DARK("Dark") {
        override fun paint(g: Graphics2D, width: Int, height: Int) {
            g.color = Color(0x20, 0x20, 0x20)
            g.fillRect(0, 0, width, height)
        }
    },
    CHECKERED("Checkered") {
        override fun paint(g: Graphics2D, width: Int, height: Int) {
            g.paint = checkerboardPaint
            g.fillRect(0, 0, width, height)
        }
    },
    TRANSPARENT("Transparent") {
        override fun paint(g: Graphics2D, width: Int, height: Int) {
            // No fill — the popup component draws on top of whatever is behind.
        }
    };

    abstract fun paint(g: Graphics2D, width: Int, height: Int)

    companion object {
        val DEFAULT = CHECKERED

        private val checkerboardPaint: Paint by lazy {
            val tileSize = 8
            val tile = BufferedImage(tileSize * 2, tileSize * 2, BufferedImage.TYPE_INT_RGB)
            tile.createGraphics().apply {
                color = Color(0xFF, 0xFF, 0xFF)
                fillRect(0, 0, tileSize * 2, tileSize * 2)
                color = Color(0xE0, 0xE0, 0xE0)
                fillRect(0, 0, tileSize, tileSize)
                fillRect(tileSize, tileSize, tileSize, tileSize)
                dispose()
            }
            TexturePaint(tile, java.awt.Rectangle(0, 0, tileSize * 2, tileSize * 2))
        }
    }
}
