package com.merkost.drawablepreview.factories

import com.android.ide.common.rendering.api.ResourceNamespace
import com.android.ide.common.rendering.api.ResourceValue
import com.android.ide.common.rendering.api.ResourceValueImpl
import com.android.ide.common.resources.ResourceResolver
import com.android.ide.common.vectordrawable.VdPreview
import com.android.resources.ResourceType
import com.android.resources.ResourceUrl
import com.android.tools.idea.configurations.ConfigurationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiFile
import org.jetbrains.android.facet.AndroidFacet
import com.merkost.drawablepreview.drawables.DrawableInflater
import com.merkost.drawablepreview.drawables.Utils
import com.merkost.drawablepreview.drawables.dom.Drawable
import com.merkost.drawablepreview.drawables.forEach
import com.merkost.drawablepreview.settings.SettingsUtils
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.awt.image.BufferedImage
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

object XmlImageFactory {

    private val LOG = Logger.getInstance(XmlImageFactory::class.java)

    fun createXmlImage(path: String): BufferedImage? {
        val document = parseDocument(path) ?: return null

        getDrawableImage(document.documentElement)?.let { return it }

        // Vector drawables we don't model in our DOM (e.g. plain <vector>) fall
        // through to Android's reference renderer.
        val targetSize = VdPreview.TargetSize.createFromMaxDimension(SettingsUtils.getPreviewSize())
        val errors = StringBuilder()
        return VdPreview.getPreviewFromVectorDocument(targetSize, document, errors)
    }

    fun getDrawable(path: String): Drawable? = parseDocument(path)?.let { DrawableInflater.getDrawable(it.documentElement) }

    private fun parseDocument(path: String): Document? {
        if (!path.endsWith(Constants.XML_TYPE, ignoreCase = true) || !path.isInResourceFolder()) {
            return null
        }

        val document = parseXmlFile(File(path))
        val root = document?.documentElement ?: return null
        val resolver = getResourceResolver(Utils.getPsiFileFromPath(path))
        if (resolver != null) {
            replaceResourceReferences(root, resolver)
        }
        return document
    }

    fun parseXmlFile(file: File): Document? = try {
        DocumentBuilderFactory.newInstance().apply {
            // Harden against XXE attacks.
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            isXIncludeAware = false
            isExpandEntityReferences = false
        }.newDocumentBuilder().parse(file)
    } catch (e: Exception) {
        LOG.warn("Failed to parse XML drawable: ${file.path}", e)
        null
    }

    private fun getDrawableImage(rootElement: Element): BufferedImage? {
        return DrawableInflater.getDrawable(rootElement)?.let { drawable ->
            return@let BufferedImage(SettingsUtils.getPreviewSize(), SettingsUtils.getPreviewSize(), BufferedImage.TYPE_INT_ARGB).also { image ->
                drawable.draw(image)
            }
        }
    }

    private fun replaceResourceReferences(node: Node, resolver: ResourceResolver) {
        if (node.nodeType == Node.ELEMENT_NODE) {
            node.attributes.forEach { attribute ->
                val value = attribute.nodeValue
                if (isReference(value)) {
                    val resolvedValue = resolveStringValue(resolver, value)
                    if (!isReference(resolvedValue)) {
                        attribute.nodeValue = resolvedValue
                    }
                }
            }
        }

        var newNode = node.firstChild
        while (newNode != null) {
            replaceResourceReferences(newNode, resolver)
            newNode = newNode.nextSibling
        }
    }

    private fun resolveStringValue(resolver: ResourceResolver, value: String): String {
        val resValue = findResValue(resolver, value) ?: return value
        return resolver.resolveResValue(resValue)?.value ?: value
    }

    private fun findResValue(resolver: ResourceResolver, value: String): ResourceValue? {
        // Use the actual resource type from the parsed reference. The earlier
        // hardcoded ResourceType.ID coerced @drawable/@color/@dimen lookups
        // through ID, which silently failed to resolve cross-type references.
        val url = ResourceUrl.parse(value) ?: return null
        return resolver.dereference(
            ResourceValueImpl(ResourceNamespace.RES_AUTO, url.type, url.name, value)
        )
    }

    private fun isReference(attributeValue: String) = ResourceUrl.parse(attributeValue) != null

    /**
     * True if the file lives directly inside a resource folder we recognise.
     * Matches path segments rather than substrings so paths like
     * "/Users/foo/drawable_helpers/x.xml" don't false-match.
     *
     * Recognised:
     *  - Android: ".../drawable/" or ".../mipmap/" with optional config
     *    qualifiers ("-night", "-anydpi-v26", etc.).
     *  - Compose Multiplatform: ".../composeResources/drawable*\/" (any source
     *    set; CMP allows the same qualifier suffixes for locale variants).
     */
    internal fun String.isInResourceFolder(): Boolean {
        val segments = split('/').filter { it.isNotEmpty() }
        if (segments.size < 2) return false
        val parent = segments[segments.size - 2]
        val parentPrefix = parent.substringBefore('-')

        if (parentPrefix in Constants.SUPPORTED_FOLDER_PREFIXES) return true

        if (parentPrefix == "drawable" &&
            segments.size >= 3 &&
            segments[segments.size - 3] == "composeResources"
        ) return true

        return false
    }

    private fun getResourceResolver(element: PsiFile?): ResourceResolver? {
        if (element == null) return null
        val virtualFile = element.virtualFile ?: return null

        val module = ProjectRootManager.getInstance(element.project)
            .fileIndex.getModuleForFile(virtualFile)
            ?: return null

        // ConfigurationManager / ResourceResolver only work for modules with
        // an Android facet. Compose Multiplatform commonMain (and any plain
        // Kotlin/JVM module) has none — getConfiguration throws AssertionError
        // there. Without a resolver we just skip @-reference resolution; the
        // raw drawable still renders correctly for self-contained vectors.
        if (AndroidFacet.getInstance(module) == null) return null

        return try {
            ConfigurationManager.getOrCreateInstance(module)
                .getConfiguration(virtualFile)
                .resourceResolver
        } catch (e: Throwable) {
            LOG.debug("Could not obtain ResourceResolver for ${virtualFile.path}", e)
            null
        }
    }
}