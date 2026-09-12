package com.esurfingclient.ans

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.esurfingclient.ans.ui.theme.ESurfingTheme
import java.io.File

class LogHistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ESurfingTheme {
                LogHistoryScreen(
                    onBack = { finish() },
                    onLogClick = { file ->
                        val intent = Intent(this@LogHistoryActivity, LogViewActivity::class.java).apply {
                            putExtra("file_path", file.absolutePath)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogHistoryScreen(onBack: () -> Unit, onLogClick: (File) -> Unit) {
    var logs by remember { mutableStateOf(emptyList<File>()) }
    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedLogs by remember { mutableStateOf(setOf<File>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val logDir = File(context.filesDir, "logs")

    fun loadLogs() {
        if (logDir.exists() && logDir.isDirectory) {
            logs = logDir.listFiles { _, name -> name.endsWith(".log") && name != "run.log" }
                ?.sortedByDescending { it.name } ?: emptyList()
        }
    }

    LaunchedEffect(Unit) {
        loadLogs()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.surfaceContainerHighest),
                title = { Text(stringResource(R.string.history_logs)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (isMultiSelectMode) {
                        IconButton(onClick = {
                            selectedLogs = if (selectedLogs.size == logs.size) {
                                emptySet()
                            } else {
                                logs.toSet()
                            }
                        }) {
                            Icon(Icons.Filled.DoneAll, contentDescription = null)
                        }
                        IconButton(
                            onClick = { if (selectedLogs.isNotEmpty()) showDeleteDialog = true },
                            enabled = selectedLogs.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                        }
                        IconButton(onClick = { isMultiSelectMode = false; selectedLogs = emptySet() }) {
                            Icon(Icons.Filled.LibraryAddCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = { isMultiSelectMode = true }) {
                            Icon(Icons.Filled.LibraryAddCheck, contentDescription = null)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_logs))
            }
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .drawVerticalScrollbar(listState)
            ) {
                items(logs) { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isMultiSelectMode) {
                                    selectedLogs = if (selectedLogs.contains(file)) {
                                        selectedLogs - file
                                    } else {
                                        selectedLogs + file
                                    }
                                } else {
                                    onLogClick(file)
                                }
                            }
                            .padding(all = if(isMultiSelectMode) 4.dp else 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isMultiSelectMode) {
                            Checkbox(
                                checked = selectedLogs.contains(file),
                                onCheckedChange = {
                                    selectedLogs = if (it) {
                                        selectedLogs + file
                                    } else {
                                        selectedLogs - file
                                    }
                                }
                            )
                        }
                        Text(file.name, modifier = Modifier.weight(1f))
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete)) },
            text = { Text(stringResource(R.string.confirm_delete)) },
            confirmButton = {
                TextButton(onClick = {
                    selectedLogs.forEach { it.delete() }
                    loadLogs()
                    selectedLogs = emptySet()
                    isMultiSelectMode = false
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

fun Modifier.drawVerticalScrollbar(
    state: LazyListState,
    color: Color = Color.Gray,
    width: Dp = 4.dp
): Modifier = drawWithContent {
    drawContent()

    val layoutInfo = state.layoutInfo
    val visibleItemsInfo = layoutInfo.visibleItemsInfo

    if (visibleItemsInfo.isEmpty()) return@drawWithContent

    val totalItemsCount = layoutInfo.totalItemsCount
    val viewportHeight = layoutInfo.viewportEndOffset - layoutInfo.viewportStartOffset

    if (viewportHeight <= 0) return@drawWithContent

    val firstItem = visibleItemsInfo.first()

    // Estimate total height based on average item size
    val averageItemSize = visibleItemsInfo.map { it.size }.average()
    val totalEstimatedHeight = averageItemSize * totalItemsCount
    
    // Calculate scroll offset
    val scrollOffset = firstItem.index * averageItemSize - firstItem.offset
    
    val visibleFraction = viewportHeight / totalEstimatedHeight
    val scrollbarHeight = (viewportHeight * visibleFraction).toFloat().coerceAtLeast(16.dp.toPx())
    
    val scrollFraction = scrollOffset / (totalEstimatedHeight - viewportHeight).coerceAtLeast(1.0)
    val scrollbarOffsetY = (viewportHeight - scrollbarHeight) * scrollFraction.toFloat()

    if (visibleItemsInfo.size < totalItemsCount) {
        drawRect(
            color = color,
            topLeft = Offset(size.width - width.toPx(), scrollbarOffsetY),
            size = Size(width.toPx(), scrollbarHeight),
            alpha = 0.5f
        )
    }
}

