package com.esurfingclient.ans

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotInterested
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.esurfingclient.ans.ui.theme.ESurfingTheme
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startESurfingService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            ESurfingTheme {
                MainScreen(
                    onStartClick = { checkPermissionAndStart() },
                    onStopClick = { stopESurfingService() }
                )
            }
        }
    }

    private fun checkPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }
        startESurfingService()
    }

    private fun startESurfingService() {
        val intent = Intent(this, ESurfingService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopESurfingService() {
        val intent = Intent(this, ESurfingService::class.java)
        stopService(intent)
    }

    external fun stringFromJNI(): String

    companion object {
        init {
            System.loadLibrary("ans")
        }
    }
}

@Composable
fun MainScreen(
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    MainScreenContent(
        onStartClick = onStartClick,
        onStopClick = onStopClick,
        username = viewModel.username,
        onUsernameChange = { viewModel.username = it },
        password = viewModel.password,
        onPasswordChange = { viewModel.password = it },
        isPcChannel = viewModel.isPcChannel,
        onPcChannelChange = { viewModel.isPcChannel = it },
        onSaveClick = {
            viewModel.saveConfig()
            Toast.makeText(context, R.string.config_saved, Toast.LENGTH_SHORT).show()
        },
        logContent = viewModel.logContent,
        logFontSize = viewModel.logFontSize,
        onClearLogsClick = {
            viewModel.clearLogs()
            Toast.makeText(context, R.string.logs_cleared, Toast.LENGTH_SHORT).show()
        },
        onLogFontSizeChange = { viewModel.logFontSize = it },
        serviceStatus = viewModel.serviceStatus,
        fabPositionX = viewModel.fabPositionX,
        fabPositionY = viewModel.fabPositionY,
        onFabPositionSave = { x, y -> viewModel.saveFabPosition(x, y) }
    )
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPcChannel: Boolean,
    onPcChannelChange: (Boolean) -> Unit,
    onSaveClick: () -> Unit,
    logContent: String,
    logFontSize: Float,
    onClearLogsClick: () -> Unit,
    onLogFontSizeChange: (Float) -> Unit,
    serviceStatus: ServiceStatus,
    fabPositionX: Float,
    fabPositionY: Float,
    onFabPositionSave: (Float, Float) -> Unit
) {
    var selectedItem by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 800.dp

        Scaffold(
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_home)) },
                            selected = selectedItem == 0,
                            onClick = { selectedItem = 0 }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_logs)) },
                            selected = selectedItem == 1,
                            onClick = { selectedItem = 1 }
                        )
                    }
                }
            }
        ) { innerPadding ->
            if (isWideScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .widthIn(max = 450.dp)
                            .fillMaxHeight()
                    ) {
                        HomeScreenContent(
                            username = username,
                            onUsernameChange = onUsernameChange,
                            password = password,
                            onPasswordChange = onPasswordChange,
                            isPcChannel = isPcChannel,
                            onPcChannelChange = onPcChannelChange,
                            onStartClick = onStartClick,
                            onStopClick = onStopClick,
                            onSaveClick = onSaveClick,
                            onClearLogsClick = onClearLogsClick,
                            serviceStatus = serviceStatus
                        )
                    }
                    VerticalDivider(
                        modifier = Modifier.fillMaxHeight(),
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        LogScreenContent(
                            logContent = logContent,
                            logFontSize = logFontSize,
                            onLogFontSizeChange = onLogFontSizeChange,
                            serviceStatus = serviceStatus,
                            onStartClick = onStartClick,
                            onStopClick = onStopClick,
                            fabPositionX = fabPositionX,
                            fabPositionY = fabPositionY,
                            onFabPositionSave = onFabPositionSave,
                            isWideScreen = true
                        )
                    }
                }
            } else {
                Column(modifier = Modifier.padding(innerPadding)) {
                    if (selectedItem == 0) {
                        HomeScreenContent(
                            username = username,
                            onUsernameChange = onUsernameChange,
                            password = password,
                            onPasswordChange = onPasswordChange,
                            isPcChannel = isPcChannel,
                            onPcChannelChange = onPcChannelChange,
                            onStartClick = onStartClick,
                            onStopClick = onStopClick,
                            onSaveClick = onSaveClick,
                            onClearLogsClick = onClearLogsClick,
                            serviceStatus = serviceStatus
                        )
                    } else {
                        LogScreenContent(
                            logContent = logContent,
                            logFontSize = logFontSize,
                            onLogFontSizeChange = onLogFontSizeChange,
                            serviceStatus = serviceStatus,
                            onStartClick = onStartClick,
                            onStopClick = onStopClick,
                            fabPositionX = fabPositionX,
                            fabPositionY = fabPositionY,
                            onFabPositionSave = onFabPositionSave,
                            isWideScreen = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeScreenContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isPcChannel: Boolean,
    onPcChannelChange: (Boolean) -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onSaveClick: () -> Unit,
    onClearLogsClick: () -> Unit,
    serviceStatus: ServiceStatus
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, end = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 16.dp)
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            onClick = {}
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text(stringResource(R.string.hint_username)) },
                modifier = Modifier.fillMaxWidth().padding(top= 10.dp, start = 12.dp, end = 12.dp, bottom = 6.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.hint_password)) },
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 6.dp)
                    .toggleable(
                        value = isPcChannel,
                        onValueChange = onPcChannelChange
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.channel_use))
                Switch(
                    checked = isPcChannel,
                    onCheckedChange = onPcChannelChange
                )
            }

            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 8.dp)
            ) {
                Text(stringResource(R.string.btn_save))
            }
        }
        OutlinedButton(
            onClick = onClearLogsClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_clear_logs))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onStartClick,
                modifier = Modifier.weight(1f),
                enabled = serviceStatus == ServiceStatus.STOPPED,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.btn_start))
            }
            Button(
                onClick = onStopClick,
                modifier = Modifier.weight(1f),
                enabled = serviceStatus == ServiceStatus.RUNNING,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.btn_stop))
            }
        }
    }
}

@Composable
fun LogScreenContent(
    logContent: String,
    logFontSize: Float,
    onLogFontSizeChange: (Float) -> Unit,
    serviceStatus: ServiceStatus,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    fabPositionX: Float,
    fabPositionY: Float,
    onFabPositionSave: (Float, Float) -> Unit,
    isWideScreen: Boolean
) {
    val scrollState = rememberScrollState()
    val isDark = isSystemInDarkTheme()
    
    // Auto-scroll to bottom when logs change
    LaunchedEffect(logContent) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                        onLogFontSizeChange((logFontSize * zoom).coerceIn(4f, 20f))
                    }
                }
                .verticalScroll(scrollState)
                .padding(8.dp)
        ) {
            Text(
                text = if (logContent.isEmpty()) stringResource(R.string.no_logs) else logContent,
                fontSize = logFontSize.sp,
                lineHeight = 1.2.em,
                fontFamily = FontFamily.Monospace,
                color = if (isDark) Color.White else Color.Black
            )
        }

        DraggableFAB(
            serviceStatus = serviceStatus,
            onStartClick = onStartClick,
            onStopClick = onStopClick,
            initialX = fabPositionX,
            initialY = fabPositionY,
            onPositionSave = onFabPositionSave,
            isWideScreen = isWideScreen
        )
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun DraggableFAB(
    serviceStatus: ServiceStatus,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    initialX: Float,
    initialY: Float,
    onPositionSave: (Float, Float) -> Unit,
    isWideScreen: Boolean
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val fabSize = with(density) { 56.dp.toPx() }
        val padding16 = with(density) { 16.dp.toPx() }
        val padding80 = with(density) { 80.dp.toPx() }

        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }

        var offsetX by remember {
            mutableFloatStateOf(
                if (initialX >= 0) initialX.coerceIn(0f, maxWidthPx - fabSize)
                else maxWidthPx - fabSize - padding16
            )
        }
        var offsetY by remember {
            mutableFloatStateOf(
                if (initialY >= 0) initialY.coerceIn(0f, maxHeightPx - fabSize)
                else maxHeightPx - fabSize - padding80
            )
        }

        val icon = when (serviceStatus) {
            ServiceStatus.STOPPED -> Icons.Filled.PlayArrow
            ServiceStatus.RUNNING -> Icons.Filled.Stop
            ServiceStatus.STOPPING -> Icons.Filled.NotInterested
        }

        val containerColor = when (serviceStatus) {
            ServiceStatus.STOPPED -> MaterialTheme.colorScheme.primaryContainer
            ServiceStatus.RUNNING -> MaterialTheme.colorScheme.errorContainer
            ServiceStatus.STOPPING -> Color.Gray.copy(alpha = 0.5f)
        }

        val enabled = serviceStatus != ServiceStatus.STOPPING

        if(!isWideScreen) {
            FloatingActionButton(
                onClick = { if (serviceStatus == ServiceStatus.STOPPED) onStartClick() else onStopClick() },
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { onPositionSave(offsetX, offsetY) }
                        ) { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxWidthPx - fabSize)
                            offsetY = (offsetY + dragAmount.y).coerceIn(0f, maxHeightPx - fabSize)
                        }
                    },
                containerColor = if (enabled) containerColor else Color.LightGray,
                contentColor = if (enabled) contentColorFor(containerColor) else Color.DarkGray,
                shape = CircleShape
            ) {
                Icon(icon, contentDescription = null)
            }
        }
    }
}
@Preview(showBackground = true, locale = "zh")
@Composable
fun MainScreenPreview() {
    ESurfingTheme {
        MainScreenContent(
            onStartClick = {},
            onStopClick = {},
            username = "test_user",
            onUsernameChange = {},
            password = "password123",
            onPasswordChange = {},
            isPcChannel = false,
            onPcChannelChange = {},
            onSaveClick = {},
            logContent = "Log line 1\nLog line 2\nLog line 3",
            logFontSize = 12f,
            onClearLogsClick = {},
            onLogFontSizeChange = {},
            serviceStatus = ServiceStatus.STOPPING,
            fabPositionX = -1f,
            fabPositionY = -1f,
            onFabPositionSave = { _, _ -> }
        )
    }
}
