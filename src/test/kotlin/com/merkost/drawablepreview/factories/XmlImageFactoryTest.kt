package com.merkost.drawablepreview.factories

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XmlImageFactoryTest {

    @Test
    fun `Android drawable folder is recognised`() = with(XmlImageFactory) {
        assertTrue("/proj/app/src/main/res/drawable/icon.xml".isInResourceFolder())
    }

    @Test
    fun `Android mipmap folder is recognised`() = with(XmlImageFactory) {
        assertTrue("/proj/app/src/main/res/mipmap/icon.xml".isInResourceFolder())
    }

    @Test
    fun `Android drawable with config qualifiers is recognised`() = with(XmlImageFactory) {
        assertTrue("/proj/app/src/main/res/drawable-night/icon.xml".isInResourceFolder())
        assertTrue("/proj/app/src/main/res/mipmap-anydpi-v26/icon.xml".isInResourceFolder())
        assertTrue("/proj/app/src/main/res/drawable-xxhdpi/icon.xml".isInResourceFolder())
    }

    @Test
    fun `Compose Multiplatform drawable folder is recognised`() = with(XmlImageFactory) {
        assertTrue("/proj/shared/src/commonMain/composeResources/drawable/icon.xml".isInResourceFolder())
        assertTrue("/proj/shared/src/desktopMain/composeResources/drawable-en/icon.xml".isInResourceFolder())
    }

    @Test
    fun `unrelated folder named drawable_helpers does not match`() = with(XmlImageFactory) {
        assertFalse("/Users/foo/drawable_helpers/icon.xml".isInResourceFolder())
    }

    @Test
    fun `arbitrary directory does not match`() = with(XmlImageFactory) {
        assertFalse("/Users/foo/projects/app/Main.kt".isInResourceFolder())
        assertFalse("/etc/icon.xml".isInResourceFolder())
    }

    @Test
    fun `composeResources files folder is not treated as drawable`() = with(XmlImageFactory) {
        // CMP also has composeResources/files for raw assets — only drawable*
        // should engage the XML drawable parser.
        assertFalse("/proj/shared/src/commonMain/composeResources/files/data.xml".isInResourceFolder())
    }
}
