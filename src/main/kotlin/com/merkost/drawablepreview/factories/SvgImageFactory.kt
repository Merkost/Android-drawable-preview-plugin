package com.merkost.drawablepreview.factories

import com.intellij.openapi.diagnostic.Logger
import com.merkost.drawablepreview.settings.SettingsUtils
import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

object SvgImageFactory {

    private val LOG = Logger.getInstance(SvgImageFactory::class.java)

    fun createSvgImage(path: String): BufferedImage? {
        val size = SettingsUtils.getPreviewSize().toFloat()
        val transcoder = PNGTranscoder().apply {
            addTranscodingHint(PNGTranscoder.KEY_WIDTH, size)
            addTranscodingHint(PNGTranscoder.KEY_HEIGHT, size)
        }

        return try {
            val bytes = File(path).inputStream().use { input ->
                ByteArrayOutputStream().use { output ->
                    transcoder.transcode(TranscoderInput(input), TranscoderOutput(output))
                    output.toByteArray()
                }
            }
            ImageIO.read(ByteArrayInputStream(bytes))
        } catch (e: Exception) {
            LOG.warn("Failed to render SVG: $path", e)
            null
        }
    }
}
