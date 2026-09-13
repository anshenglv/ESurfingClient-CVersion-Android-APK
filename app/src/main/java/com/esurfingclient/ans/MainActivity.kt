package com.esurfingclient.ans

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextObfuscationMode
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.esurfingclient.ans.ui.theme.ESurfingTheme
import kotlin.math.roundToInt
import androidx.core.net.toUri

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
        channel = viewModel.channel,
        logLv = viewModel.logLv,
        onChannelChange = { viewModel.channel = it },
        onLogLvChange = { viewModel.logLv = it },
        onSaveClick = {
            viewModel.saveConfig()
            Toast.makeText(context, R.string.config_saved, Toast.LENGTH_SHORT).show()
        },
        onHistoryLogsClick = {
            context.startActivity(Intent(context, LogHistoryActivity::class.java))
        },
        logContent = viewModel.logContent,
        logFontSize = viewModel.logFontSize,
        onClearLogsClick = {
            viewModel.clearLogs()
            Toast.makeText(context, R.string.logs_cleared, Toast.LENGTH_SHORT).show()
        },
        onLogFontSizeChange = { viewModel.logFontSize = it },
        onLogFontSizeSave = { viewModel.saveLogFontSize(it) },
        serviceStatus = viewModel.serviceStatus,
        fabPositionX = viewModel.fabPositionX,
        fabPositionY = viewModel.fabPositionY,
        selectedItem = viewModel.selectedItem,
        onSelectedItemSave = { selected -> viewModel.saveSelected(selected) },
        onFabPositionSave = { x, y -> viewModel.saveFabPosition(x, y) },
        wifiOnly = viewModel.wifiOnly,
        onWifiOnlyChange = { viewModel.updateWifiOnly(it) }
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
    channel: String,
    logLv: String,
    onLogLvChange: (String) -> Unit,
    onChannelChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onHistoryLogsClick: () -> Unit,
    logContent: String,
    logFontSize: Float,
    onClearLogsClick: () -> Unit,
    onLogFontSizeChange: (Float) -> Unit,
    onLogFontSizeSave: (Float) -> Unit,
    serviceStatus: ServiceStatus,
    fabPositionX: Float,
    fabPositionY: Float,
    selectedItem: Int,
    onSelectedItemSave: (Int) -> Unit,
    onFabPositionSave: (Float, Float) -> Unit,
    wifiOnly: Boolean,
    onWifiOnlyChange: (Boolean) -> Unit
) {
    var selectedItem by remember { mutableIntStateOf(selectedItem) }

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
                            onClick = { selectedItem = 0; onSelectedItemSave(0) }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Filled.Terminal, contentDescription = null) },
                            label = { Text(stringResource(R.string.nav_logs)) },
                            selected = selectedItem == 1,
                            onClick = { selectedItem = 1; onSelectedItemSave(1) }
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
                            channel = channel,
                            onChannelChange = onChannelChange,
                            onStartClick = onStartClick,
                            onStopClick = onStopClick,
                            onSaveClick = onSaveClick,
                            onClearLogsClick = onClearLogsClick,
                            onHistoryLogsClick = onHistoryLogsClick,
                            serviceStatus = serviceStatus,
                            isWideScreen = true,
                            logLv = logLv,
                            onLogLvChange = onLogLvChange,
                            wifiOnly = wifiOnly,
                            onWifiOnlyChange = onWifiOnlyChange,
                            logFontSize = logFontSize,
                            onLogFontSizeChange = onLogFontSizeChange,
                            onLogFontSizeSave = onLogFontSizeSave
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
                            channel = channel,
                            onChannelChange = onChannelChange,
                            onStartClick = onStartClick,
                            onStopClick = onStopClick,
                            onSaveClick = onSaveClick,
                            onClearLogsClick = onClearLogsClick,
                            onHistoryLogsClick = onHistoryLogsClick,
                            serviceStatus = serviceStatus,
                            isWideScreen = false,
                            logLv = logLv,
                            onLogLvChange = onLogLvChange,
                            wifiOnly = wifiOnly,
                            onWifiOnlyChange = onWifiOnlyChange,
                            logFontSize = logFontSize,
                            onLogFontSizeChange = onLogFontSizeChange,
                            onLogFontSizeSave = onLogFontSizeSave
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

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    username: String,
    onUsernameChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    channel: String,
    logLv: String,
    onLogLvChange: (String) -> Unit,
    onChannelChange: (String) -> Unit,
    onStartClick: () -> Unit,
    onStopClick: () -> Unit,
    onSaveClick: () -> Unit,
    onClearLogsClick: () -> Unit,
    onHistoryLogsClick: () -> Unit,
    serviceStatus: ServiceStatus,
    isWideScreen: Boolean,
    wifiOnly: Boolean,
    onWifiOnlyChange: (Boolean) -> Unit,
    logFontSize: Float,
    onLogFontSizeChange: (Float) -> Unit,
    onLogFontSizeSave: (Float) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var expanded2 by remember { mutableStateOf(false) }
    val channels = listOf("1","2","3","4", "5")
    val channelLabels = mapOf(
        "1" to "Windows(暂未实现,回退至Android)",
        "2" to "Linux",
        "3" to "Android",
        "4" to "iOS",
        "5" to "MacOS"
    )
    val logLvs = listOf("1","2","3","4","5","6")
    val logLvLabels = mapOf(
        "1" to "FATAL",
        "2" to "ERROR",
        "3" to "WARN",
        "4" to stringResource(R.string.log_lv_4),
        "5" to "DEBUG",
        "6" to "VERBOSE"
    )

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isPreview = LocalInspectionMode.current
    var isPasswordVisible by remember { mutableStateOf(false) }
    val passwordState = rememberTextFieldState(password)
    onPasswordChange(passwordState.text.toString())
    
    var isIgnoringBatteryOptimizations by remember {
        mutableStateOf(
            if (isPreview) false else {
                (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                    .isIgnoringBatteryOptimizations(context.packageName)
            }
        )
    }

    DisposableEffect(lifecycleOwner, isPreview) {
        if (isPreview) return@DisposableEffect onDispose {}
        
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isIgnoringBatteryOptimizations = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                    .isIgnoringBatteryOptimizations(context.packageName)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
        //(false){
        if (!isIgnoringBatteryOptimizations) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                shape = RoundedCornerShape(24.dp),
                onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = "package:${context.packageName}".toUri()
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)}
                    context.startActivity(intent)
                },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null,modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.battery_opt_title),modifier = Modifier.padding(start = 4.dp))
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ArrowOutward, contentDescription = null, modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            //modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
            onClick = {}
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text(stringResource(R.string.hint_username)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 12.dp, end = 12.dp, bottom = 6.dp)
            )

            OutlinedSecureTextField(
                state = passwordState,
                textObfuscationMode = if (isPasswordVisible) { TextObfuscationMode.Visible }
                    else { TextObfuscationMode.System },
                label = { Text(stringResource(R.string.hint_password)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            },
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = channelLabels[channel] ?: channel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.channel_use)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    channels.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(channelLabels[selectionOption] ?: selectionOption) },
                            onClick = {
                                onChannelChange(selectionOption)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = expanded2,
                onExpandedChange = { expanded2 = !expanded2 },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                OutlinedTextField(
                    value = logLvLabels[logLv] ?: logLv,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.log_lv)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded2) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded2,
                    onDismissRequest = { expanded2 = false }
                ) {
                    logLvs.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(logLvLabels[selectionOption] ?: selectionOption) },
                            onClick = {
                                onLogLvChange(selectionOption)
                                expanded2 = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 8.dp)
            ) {
                Text(stringResource(R.string.btn_save))
            }
        }

        if(isWideScreen) {
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

        OutlinedButton(
            onClick = onClearLogsClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_clear_logs))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            onClick = {}
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "日志字体默认大小: ${logFontSize.toInt()}")
                }
                Slider(
                    value = logFontSize,
                    onValueChange = onLogFontSizeChange,
                    onValueChangeFinished = { onLogFontSizeSave(logFontSize) },
                    valueRange = 4f..20f,
                    steps = 15
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            onClick = {onWifiOnlyChange(!wifiOnly)}
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "绑定认证请求至 WiFi")
                Switch(
                    checked = wifiOnly,
                    onCheckedChange = onWifiOnlyChange
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            onClick = onHistoryLogsClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.history_logs))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Text(
            text = "关于",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            onClick = {}
            )
        {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ){
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ){
                    Text("当前版本",fontSize = 14.sp)
                    val versionName = context.packageManager
                        .getPackageInfo(context.packageName, 0)
                        .versionName
                    val versionCode = context.packageManager
                        .getPackageInfo(context.packageName, 0)
                        .longVersionCode
                    Text(text = "$versionName ($versionCode)",fontSize = 14.sp)
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Column(modifier = Modifier.fillMaxWidth()){
                    Text(text = "项目地址", fontSize = 14.sp, lineHeight = 16.sp, modifier = Modifier.fillMaxWidth())
                    Text(
                        buildAnnotatedString {
                            withLink(
                                LinkAnnotation.Url(
                                    "https://github.com/anshenglv/ESurfingClient-CVersion-Android-APK",
                                    TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)),
                                )
                            ) {
                                append("https://github.com/anshenglv/ESurfingClient-CVersion-Android-APK")
                            }
                        },lineHeight = 13.sp, fontSize = 11.sp
                    )
                }
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Column(modifier = Modifier.fillMaxWidth()){
                    Text(text = "上游 C 版", fontSize = 14.sp, lineHeight = 16.sp, modifier = Modifier.fillMaxWidth())
                    Text(
                        buildAnnotatedString {
                            withLink(
                                LinkAnnotation.Url(
                                    "https://github.com/BadGhost520/ESurfingClient-CVersion",
                                    TextLinkStyles(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)),
                                )
                            ) {
                                append("https://github.com/BadGhost520/ESurfingClient-CVersion")
                            }
                        },lineHeight = 13.sp, fontSize = 11.sp
                    )
                }
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

    val currentLogFontSize by rememberUpdatedState(logFontSize)
    val currentOnLogFontSizeChange by rememberUpdatedState(onLogFontSizeChange)

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
                        currentOnLogFontSizeChange((currentLogFontSize * zoom).coerceIn(4f, 20f))
                    }
                }
                .verticalScroll(scrollState)
                .padding(8.dp)
        ) {
            Text(
                text = logContent.ifEmpty { stringResource(R.string.no_logs) },
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
            ServiceStatus.STOPPING -> Icons.Filled.Block
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
            channel = "1",
            onChannelChange = {},
            onSaveClick = {},
            logContent = "Log line 1\nLog line 2\nLog line 3",
            logFontSize = 8f,
            onClearLogsClick = {},
            onLogFontSizeChange = {},
            serviceStatus = ServiceStatus.RUNNING,
            fabPositionX = -1f,
            fabPositionY = -1f,
            onFabPositionSave = { _, _ -> },
            selectedItem = 0,
            onSelectedItemSave = {},
            onHistoryLogsClick = {},
            logLv = "4",
            onLogLvChange = {},
            wifiOnly = false,
            onWifiOnlyChange = {},
            onLogFontSizeSave = {}
        )
    }
}
