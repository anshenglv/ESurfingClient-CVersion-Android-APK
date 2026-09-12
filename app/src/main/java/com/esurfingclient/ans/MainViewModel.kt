package com.esurfingclient.ans

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var channel by mutableStateOf("3")
    var logLv by mutableStateOf("4")
    var logContent by mutableStateOf("")
    var logFontSize by mutableFloatStateOf(8f)

    var serviceStatus by mutableStateOf(ServiceStatus.STOPPED)

    var fabPositionX by mutableFloatStateOf(-1f)
    var fabPositionY by mutableFloatStateOf(-1f)
    var selectedItem by mutableIntStateOf(0)

    private var logJob: Job? = null
    private val context: Context get() = getApplication()

    init {
        loadConfig()
        loadFabPosition()
        startLogUpdater()
    }

    private fun startLogUpdater() {
        logJob?.cancel()
        logJob = viewModelScope.launch {
            while (true) {
                updateLogs()
                serviceStatus = ESurfingService.getServiceStatus()
                delay(500)
            }
        }
    }

    private fun updateLogs() {
        val logFile = File(context.filesDir, "logs/run.log")
        if (logFile.exists()) {
            try {
                val lines = logFile.readLines().takeLast(200)
                val newContent = lines.joinToString("\n")
                if (logContent != newContent) {
                    logContent = newContent
                }
            } catch (e: Exception) {
            }
        }
    }

    fun clearLogs() {
        val logDir = File(context.filesDir, "logs")
        if (logDir.exists() && logDir.isDirectory) {
            val files = logDir.listFiles()
            files?.forEach { file ->
                if (file.name == "run.log") {
                    try {
                        file.writeText("")
                        logContent = ""
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun saveConfig() {
        val config = JSONObject()
        config.put("enabled", true)
        config.put("log_lv", logLv.toInt())

        val accounts = JSONArray()
        val account = JSONObject()
        val time = JSONArray()
        account.put("username", username)
        account.put("password", password)
        account.put("channel", channel)
        account.put("mark", "")
        account.put("time_windows", time)

        accounts.put(account)

        config.put("accounts", accounts)

        val configFile = File(context.filesDir, "ESurfingClient.json")
        configFile.writeText(config.toString(4))
    }

    private fun loadConfig() {
        val configFile = File(context.filesDir, "ESurfingClient.json")
        if (configFile.exists()) {
            try {
                val config = JSONObject(configFile.readText())
                logLv = config.getInt("log_lv").toString()
                val accounts = config.getJSONArray("accounts")
                if (accounts.length() > 0) {
                    val account = accounts.getJSONObject(0)
                    username = account.getString("username")
                    password = account.getString("password")
                    channel = account.optString("channel", "3")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadFabPosition() {
        val prefs = context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)
        fabPositionX = prefs.getFloat("fab_x", -1f)
        fabPositionY = prefs.getFloat("fab_y", -1f)
        selectedItem = prefs.getInt("selected_item", 0)
    }

    fun saveFabPosition(x: Float, y: Float) {
        fabPositionX = x
        fabPositionY = y
        val prefs = context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            putFloat("fab_x", x)
            putFloat("fab_y", y)
        }
    }

    fun saveSelected(selected: Int) {
        selectedItem = selected
        val prefs = context.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            putInt("selected_item", selected)
        }
    }
}
