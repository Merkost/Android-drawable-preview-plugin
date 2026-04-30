package com.merkost.drawablepreview.toolwindow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.merkost.drawablepreview.factories.IconPreviewFactory
import com.merkost.drawablepreview.settings.SettingsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.Text
import androidx.compose.foundation.Image as ComposeImage

private const val THUMB_SIZE_DP = 72
private const val CELL_SIZE_DP = 96

@Composable
fun ResourceManagerPanel(project: Project) {
    var entries by remember { mutableStateOf<List<DrawableEntry>>(emptyList()) }

    LaunchedEffect(project) {
        entries = withContext(Dispatchers.Default) {
            ApplicationManager.getApplication().runReadAction<List<DrawableEntry>> {
                DrawableScanner.scan(project)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Text(
            text = if (entries.isEmpty()) "Scanning…" else "${entries.size} drawables",
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = CELL_SIZE_DP.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(entries, key = { it.file.path }) { entry ->
                DrawableCell(project, entry)
            }
        }
    }
}

@Composable
private fun DrawableCell(project: Project, entry: DrawableEntry) {
    val bitmap = remember(entry.file.path, entry.file.modificationStamp) {
        loadThumbnail(project, entry)
    }
    Column(
        modifier = Modifier
            .size(CELL_SIZE_DP.dp)
            .clickable { openFile(project, entry) }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(THUMB_SIZE_DP.dp)
                .background(JewelTheme.globalColors.panelBackground)
                .border(1.dp, JewelTheme.globalColors.borders.normal),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                ComposeImage(
                    painter = BitmapPainter(bitmap),
                    contentDescription = entry.baseName,
                )
            } else {
                Text("?", color = Color.Gray)
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = entry.baseName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun loadThumbnail(project: Project, entry: DrawableEntry): ImageBitmap? {
    return ApplicationManager.getApplication().runReadAction<ImageBitmap?> {
        val psiFile = PsiManager.getInstance(project).findFile(entry.file) ?: return@runReadAction null
        SettingsUtils.withRenderSize(96) {
            IconPreviewFactory.getImage(psiFile)?.toComposeImageBitmap()
        }
    }
}

private fun openFile(project: Project, entry: DrawableEntry) {
    OpenFileDescriptor(project, entry.file).navigate(true)
}
