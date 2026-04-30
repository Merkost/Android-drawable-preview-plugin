@file:OptIn(org.jetbrains.jewel.foundation.ExperimentalJewelApi::class)

package com.merkost.drawablepreview.toolwindow

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiManager
import com.merkost.drawablepreview.factories.IconPreviewFactory
import com.merkost.drawablepreview.settings.SettingsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.ui.component.CheckboxRow
import org.jetbrains.jewel.ui.component.Text
import org.jetbrains.jewel.ui.component.TextField
import androidx.compose.foundation.Image as ComposeImage

private const val THUMB_SIZE_DP = 72
private const val CELL_SIZE_DP = 96

enum class GroupBy(val displayName: String) {
    NONE("None"),
    SOURCE_FOLDER("Source folder"),
    KIND("Kind"),
}

enum class SortBy(val displayName: String) {
    NAME_ASC("Name ↑"),
    NAME_DESC("Name ↓"),
    KIND("Kind"),
    SIZE("Size"),
}

@Composable
fun ResourceManagerPanel(project: Project) {
    var entries by remember { mutableStateOf<List<DrawableEntry>>(emptyList()) }
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var enabledKinds by remember { mutableStateOf(DrawableEntry.Kind.values().toSet()) }
    var groupBy by remember { mutableStateOf(GroupBy.NONE) }
    var sortBy by remember { mutableStateOf(SortBy.NAME_ASC) }
    val scope = rememberCoroutineScope()

    suspend fun rescan() {
        entries = withContext(Dispatchers.Default) {
            ApplicationManager.getApplication().runReadAction<List<DrawableEntry>> {
                DrawableScanner.scan(project)
            }
        }
    }

    LaunchedEffect(project) { rescan() }

    // Subscribe to VFS bulk events; debounce a re-scan so a multi-file
    // operation (e.g. Move Resource) doesn't fire dozens of scans in a row.
    DisposableEffect(project) {
        var pending: Job? = null
        val connection = project.messageBus.connect()
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                if (!events.any { it.isInDrawableFolder() }) return
                pending?.cancel()
                pending = scope.launch {
                    delay(250)  // debounce
                    rescan()
                }
            }
        })
        onDispose { connection.disconnect() }
    }

    val filtered by remember {
        derivedStateOf {
            val needle = query.text.trim()
            val sorted = entries
                .filter { entry ->
                    entry.kind in enabledKinds &&
                            (needle.isEmpty() || entry.baseName.contains(needle, ignoreCase = true))
                }
                .sortedWith(sortBy.comparator())
            sorted
        }
    }

    val grouped by remember {
        derivedStateOf {
            when (groupBy) {
                GroupBy.NONE -> listOf(null to filtered)
                GroupBy.SOURCE_FOLDER -> filtered
                    .groupBy { it.sourceFolder }
                    .toSortedMap()
                    .map { (k, v) -> k to v }
                GroupBy.KIND -> filtered
                    .groupBy { it.kind.label }
                    .toSortedMap()
                    .map { (k, v) -> k to v }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        TextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            placeholder = { Text("Filter by name") },
        )

        FilterChipsRow(
            kinds = DrawableEntry.Kind.values().toList(),
            selected = enabledKinds,
            onToggle = { kind ->
                enabledKinds = if (kind in enabledKinds) enabledKinds - kind else enabledKinds + kind
            },
        )

        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EnumChooser(
                label = "Group:",
                values = GroupBy.values().toList(),
                selected = groupBy,
                labelOf = GroupBy::displayName,
                onSelected = { groupBy = it },
            )
            EnumChooser(
                label = "Sort:",
                values = SortBy.values().toList(),
                selected = sortBy,
                labelOf = SortBy::displayName,
                onSelected = { sortBy = it },
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            text = when {
                entries.isEmpty() -> "Scanning…"
                filtered.size == entries.size -> "${entries.size} drawables"
                else -> "${filtered.size} of ${entries.size} drawables"
            },
            modifier = Modifier.padding(bottom = 6.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = CELL_SIZE_DP.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            grouped.forEach { (header, list) ->
                if (header != null) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = "$header  ·  ${list.size}",
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                        )
                    }
                }
                items(list, key = { it.file.path }) { entry ->
                    DrawableCell(project, entry)
                }
            }
        }
    }
}

@Composable
private fun <T> EnumChooser(
    label: String,
    values: List<T>,
    selected: T,
    labelOf: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$label ", modifier = Modifier.padding(end = 4.dp))
        values.forEach { value ->
            Text(
                text = labelOf(value),
                modifier = Modifier
                    .clickable { onSelected(value) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                    .background(
                        if (value == selected) JewelTheme.globalColors.borders.normal
                        else JewelTheme.globalColors.panelBackground
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}

private fun SortBy.comparator(): Comparator<DrawableEntry> = when (this) {
    SortBy.NAME_ASC -> compareBy { it.baseName.lowercase() }
    SortBy.NAME_DESC -> compareByDescending { it.baseName.lowercase() }
    SortBy.KIND -> compareBy({ it.kind.ordinal }, { it.baseName.lowercase() })
    SortBy.SIZE -> compareByDescending<DrawableEntry> { it.file.length }
        .then(compareBy { it.baseName.lowercase() })
}

@Composable
private fun FilterChipsRow(
    kinds: List<DrawableEntry.Kind>,
    selected: Set<DrawableEntry.Kind>,
    onToggle: (DrawableEntry.Kind) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        kinds.forEach { kind ->
            CheckboxRow(
                text = kind.label,
                checked = kind in selected,
                onCheckedChange = { onToggle(kind) },
            )
        }
    }
}

private val DrawableEntry.Kind.label: String
    get() = when (this) {
        DrawableEntry.Kind.XML -> "XML"
        DrawableEntry.Kind.SVG -> "SVG"
        DrawableEntry.Kind.PNG -> "PNG"
        DrawableEntry.Kind.JPG -> "JPG"
        DrawableEntry.Kind.WEBP -> "WebP"
        DrawableEntry.Kind.GIF -> "GIF"
        DrawableEntry.Kind.NINE_PATCH -> "9-patch"
    }

@Composable
private fun DrawableCell(project: Project, entry: DrawableEntry) {
    val bitmap = remember(entry.file.path, entry.file.modificationStamp) {
        loadThumbnail(project, entry)
    }
    var menuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .size(CELL_SIZE_DP.dp)
            .clickable { openFile(project, entry) }
            .pointerInput(entry.file.path) {
                awaitEachGesture {
                    val event = awaitPointerEvent()
                    if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press &&
                        event.buttons.isSecondaryPressed
                    ) {
                        menuExpanded = true
                        event.changes.forEach { it.consume() }
                    }
                }
            }
            .padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (menuExpanded) {
            Popup(
                onDismissRequest = { menuExpanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                Column(
                    modifier = Modifier
                        .background(JewelTheme.globalColors.panelBackground)
                        .border(1.dp, JewelTheme.globalColors.borders.normal)
                        .padding(4.dp),
                ) {
                    CellMenuItem("Open in Editor") {
                        menuExpanded = false; openFile(project, entry)
                    }
                    CellMenuItem("Reveal in Project View") {
                        menuExpanded = false; revealInProjectView(project, entry)
                    }
                    CellMenuItem("Find Drawable Usages") {
                        menuExpanded = false; findUsages(project, entry)
                    }
                    CellMenuItem("Copy Path") {
                        menuExpanded = false
                        java.awt.Toolkit.getDefaultToolkit().systemClipboard
                            .setContents(java.awt.datatransfer.StringSelection(entry.file.path), null)
                    }
                }
            }
        }
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

private fun revealInProjectView(project: Project, entry: DrawableEntry) {
    com.intellij.ide.projectView.ProjectView.getInstance(project).select(null, entry.file, true)
}

private fun findUsages(project: Project, entry: DrawableEntry) {
    val baseName = baseNameFor(entry.file.name)
    val findManager = com.intellij.find.FindManager.getInstance(project)
    val model = findManager.findInProjectModel.clone().apply {
        stringToFind = """\b(R|Res)\.drawable\.${Regex.escape(baseName)}\b|@drawable/${Regex.escape(baseName)}\b"""
        isRegularExpressions = true
        isCaseSensitive = true
        isWholeWordsOnly = false
        isProjectScope = true
    }
    com.intellij.find.findInProject.FindInProjectManager.getInstance(project)
        .findInProject(com.intellij.openapi.actionSystem.DataContext.EMPTY_CONTEXT, model)
}

private fun baseNameFor(filename: String): String {
    val lower = filename.lowercase()
    return when {
        lower.endsWith(".9.png") -> filename.substring(0, filename.length - ".9.png".length)
        else -> filename.substringBeforeLast('.')
    }
}

@Composable
private fun CellMenuItem(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/**
 * Cheap path check — matches segments named drawable* / mipmap*. We accept
 * false positives here (e.g. a file in /drawable_helpers/ would re-trigger
 * a scan); the rescan itself filters properly via DrawableScanner so the
 * worst case is one wasted re-walk.
 */
private fun VFileEvent.isInDrawableFolder(): Boolean {
    val p = path
    return p.contains("/drawable", ignoreCase = false) || p.contains("/mipmap", ignoreCase = false)
}
