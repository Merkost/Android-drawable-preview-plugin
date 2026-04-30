package com.merkost.drawablepreview.drawables

import java.awt.Shape
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

/**
 * Mask shape applied to adaptive icons. Real Android launchers pick one of
 * these (each OEM may differ) when displaying adaptive icons.
 */
enum class MaskShape(val displayName: String) {
    CIRCLE("Circle") {
        override fun shapeFor(size: Int): Shape =
            Ellipse2D.Float(0f, 0f, size.toFloat(), size.toFloat())
    },
    SQUIRCLE("Squircle") {
        override fun shapeFor(size: Int): Shape = squirclePath(size, exponent = 4.0)
    },
    ROUNDED_SQUARE("Rounded square") {
        override fun shapeFor(size: Int): Shape {
            val arc = size * 0.25f
            return RoundRectangle2D.Float(0f, 0f, size.toFloat(), size.toFloat(), arc, arc)
        }
    },
    SQUARE("Square") {
        override fun shapeFor(size: Int): Shape =
            Rectangle2D.Float(0f, 0f, size.toFloat(), size.toFloat())
    };

    abstract fun shapeFor(size: Int): Shape

    companion object {
        val DEFAULT = CIRCLE

        /**
         * Approximate a superellipse (squircle): |x/r|^n + |y/r|^n = 1.
         * n=4 looks close to the iOS / Pixel Launcher squircle.
         */
        private fun squirclePath(size: Int, exponent: Double, segments: Int = 96): Path2D {
            val r = size / 2.0
            val path = Path2D.Float()
            for (i in 0..segments) {
                val theta = 2.0 * Math.PI * i / segments
                val cosT = Math.cos(theta)
                val sinT = Math.sin(theta)
                val x = r + r * sign(cosT) * abs(cosT).pow(2.0 / exponent)
                val y = r + r * sign(sinT) * abs(sinT).pow(2.0 / exponent)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.closePath()
            return path
        }
    }
}
