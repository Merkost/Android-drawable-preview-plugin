package com.merkost.drawablepreview

import com.intellij.ide.IconProvider
import com.intellij.psi.PsiElement
import com.merkost.drawablepreview.factories.IconPreviewFactory

class DrawablePreviewIconProvider : IconProvider() {

    override fun getIcon(element: PsiElement, flags: Int) = IconPreviewFactory.createIcon(element)
}