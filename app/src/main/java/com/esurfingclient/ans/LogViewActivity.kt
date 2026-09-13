package com.esurfingclient.ans

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.esurfingclient.ans.ui.theme.ESurfingTheme
import java.io.File

class LogViewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filePath = intent.getStringExtra("file_path") ?: ""
        val file = File(filePath)

        if (!file.exists()) {
            Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            ESurfingTheme{
                LogViewScreen(
                    file = file,
                    onBack = { finish() }
                )
            }
        }
    }
}

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogViewScreen(file: File, onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE) }
    var fontSize by remember { mutableFloatStateOf(prefs.getFloat("log_font_size", 10f)) }
    val content = remember(file) { file.readText() }
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { outputStream ->
                outputStream.write(file.readBytes())
            }
            Toast.makeText(context, R.string.export_success, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(MaterialTheme.colorScheme.surfaceContainerHighest),
                title = { Text(file.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.share)))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = null)
                    }
                    IconButton(onClick = {
                        createDocumentLauncher.launch(file.name)
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .drawWithContent {
                    drawContent()
                    if (scrollState.maxValue > 0) {
                        val viewHeight = size.height
                        val contentHeight = scrollState.maxValue + viewHeight
                        val thumbHeight = (viewHeight / contentHeight) * viewHeight
                        val thumbOffset = (scrollState.value.toFloat() / contentHeight) * viewHeight

                        drawRoundRect(
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.3f),
                            topLeft = Offset(size.width - 6.dp.toPx(), thumbOffset),
                            size = Size(4.dp.toPx(), thumbHeight),
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        fontSize = (fontSize * zoom).coerceIn(4f, 20f)
                    }
                }
                .verticalScroll(scrollState)
                .padding(8.dp)
        ) {
            Text(
                text = content,
                fontSize = fontSize.sp,
                lineHeight = 1.2.em,
                fontFamily = FontFamily.Monospace,
                color = if (isDark) Color.White else Color.Black
            )
        }
    }
}
