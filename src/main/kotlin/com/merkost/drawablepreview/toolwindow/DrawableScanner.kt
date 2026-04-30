package com.merkost.drawablepreview.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor

/**
 * Walks every content root of [project] and collects previewable drawable
 * files in any recognised resource folder (Android `drawable*` / `mipmap*`,
 * Compose Multiplatform `composeResources/drawable*`).
 *
 * Cheap enough to call on demand — bounded by the number of source folders
 * times the number of resource files. We don't index, since Refresh re-scans.
 */
object DrawableScanner {

    fun scan(project: Project): List<DrawableEntry> {
        val results = mutableListOf<DrawableEntry>()
        val seen = HashSet<String>()
        val fileIndex = ProjectFileIndex.getInstance(project)

        fileIndex.iterateContent { root ->
            VfsUtilCore.visitChildrenRecursively(root, object : VirtualFileVisitor<Void>() {
                override fun visitFile(file: VirtualFile): Boolean {
                    if (!file.isDirectory && fileIndex.isExcluded(file)) return false
                    if (file.isDirectory) return shouldDescendInto(file)
                    classify(file)?.let { entry ->
                        if (seen.add(entry.file.path)) results += entry
                    }
                    return true
                }
            })
            true
        }
        return results.sortedWith(compareBy({ it.baseName }, { it.sourceFolder }))
    }

    private fun shouldDescendInto(dir: VirtualFile): Boolean {
        // Cheap pruning: skip out-of-source roots we know contain nothing useful.
        val name = dir.name
        if (name == "build" || name == "out" || name == ".gradle" || name == ".idea") return false
        return true
    }

    private fun classify(file: VirtualFile): DrawableEntry? {
        val name = file.name
        val parent = file.parent ?: return null
        val parentName = parent.name
        if (!isResourceFolder(parentName, parent)) return null

        val kind = kindOf(name) ?: return null
        val baseName = baseNameOf(name)
        return DrawableEntry(file, baseName, kind, parentName)
    }

    private fun isResourceFolder(parentName: String, parent: VirtualFile): Boolean {
        if (parentName.startsWith("drawable") || parentName.startsWith("mipmap")) return true
        // CMP composeResources/drawable* — same prefix shape works.
        return false
    }

    private fun kindOf(filename: String): DrawableEntry.Kind? {
        val lower = filename.lowercase()
        return when {
            lower.endsWith(".9.png") -> DrawableEntry.Kind.NINE_PATCH
            lower.endsWith(".xml") -> DrawableEntry.Kind.XML
            lower.endsWith(".svg") -> DrawableEntry.Kind.SVG
            lower.endsWith(".png") -> DrawableEntry.Kind.PNG
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> DrawableEntry.Kind.JPG
            lower.endsWith(".webp") -> DrawableEntry.Kind.WEBP
            lower.endsWith(".gif") -> DrawableEntry.Kind.GIF
            else -> null
        }
    }

    private fun baseNameOf(filename: String): String {
        val lower = filename.lowercase()
        return when {
            lower.endsWith(".9.png") -> filename.substring(0, filename.length - ".9.png".length)
            else -> filename.substringBeforeLast('.')
        }
    }
}
