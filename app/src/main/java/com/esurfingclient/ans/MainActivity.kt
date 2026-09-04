package com.esurfingclient.ans

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.esurfingclient.ans.databinding.ActivityMainBinding
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val handler = Handler(Looper.getMainLooper())
    private var logFontSize = 8f
    private lateinit var scaleGestureDetector: ScaleGestureDetector

    private val logUpdater = object : Runnable {
        override fun run() {
            updateLogs()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Allow drawing behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get Action Bar Height from attributes
        val tv = TypedValue()
        val actionBarHeight = if (theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            TypedValue.complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        } else {
            0
        }

        // Handle Window Insets for Notch/Status bar
        ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout) { _, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val navBarHeight = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            
            // Adjust spacer for Log page
            binding.statusBarSpacer.layoutParams.height = statusBarHeight
            binding.statusBarSpacer.requestLayout()
            
            // Adjust toolbar padding for Home page (instant visibility management)
            binding.toolbar.setPadding(0, statusBarHeight, 0, 0)
            binding.toolbar.layoutParams.height = actionBarHeight + statusBarHeight
            binding.toolbar.requestLayout()
            
            // Adjust padding for bottom nav to avoid overlapping with gesture bar/nav bar
            binding.bottomNav.setPadding(0, 0, 0, navBarHeight)
            
            insets
        }

        // Setup Bottom Navigation
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showHome()
                    true
                }
                R.id.nav_logs -> {
                    showLogs()
                    true
                }
                else -> false
            }
        }

        binding.btnSave.setOnClickListener {
            saveConfig()
        }

        binding.btnStartStop.setOnClickListener {
            if (isNativeRunning()) {
                stopService()
            } else {
                checkPermissionAndStart()
            }
        }

        binding.btnClearLogs.setOnClickListener {
            clearLogs()
        }

        setupPinchToZoom()
        binding.tvLogs.textSize = logFontSize
        loadConfigToUI()
        
        // Update button state periodically
        handler.post(object : Runnable {
            override fun run() {
                updateButtonState()
                handler.postDelayed(this, 500)
            }
        })
        
        // Default view
        showHome()
    }

    private fun updateButtonState() {
        val running = isNativeRunning()
        if (binding.pbLoading.visibility == View.VISIBLE) {
            // We are in the middle of stopping
            if (!running) {
                binding.pbLoading.visibility = View.GONE
                binding.btnStartStop.isEnabled = true
                binding.btnStartStop.text = getString(R.string.btn_start)
            }
        } else {
            binding.btnStartStop.text = if (running) getString(R.string.btn_stop) else getString(R.string.btn_start)
        }
    }

    private fun stopService() {
        binding.btnStartStop.isEnabled = false
        binding.btnStartStop.text = ""
        binding.pbLoading.visibility = View.VISIBLE
        
        val intent = Intent(this, ESurfingService::class.java)
        stopService(intent)
    }

    private fun showHome() {
        binding.toolbar.visibility = View.VISIBLE
        binding.containerHome.visibility = View.VISIBLE
        binding.containerLogs.visibility = View.GONE
        
        updateStatusBar(isLogPage = false)
    }

    private fun showLogs() {
        binding.toolbar.visibility = View.GONE
        binding.containerHome.visibility = View.GONE
        binding.containerLogs.visibility = View.VISIBLE
        
        updateStatusBar(isLogPage = true)
    }

    private fun updateStatusBar(isLogPage: Boolean) {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (isLogPage) {
            window.statusBarColor = Color.BLACK
            windowInsetsController.isAppearanceLightStatusBars = false // White icons
        } else {
            window.statusBarColor = Color.TRANSPARENT
            
            val isDarkMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            // Use White icons in Light mode, Black icons in Dark mode as requested
            windowInsetsController.isAppearanceLightStatusBars = isDarkMode
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(logUpdater)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(logUpdater)
    }

    private fun clearLogs() {
        val logDir = File(filesDir, "logs")
        if (logDir.exists() && logDir.isDirectory) {
            val files = logDir.listFiles()
            files?.forEach { file ->
                if (file.name == "run.log") {
                    // Clear the content of run.log
                    try {
                        file.writeText("")
                        binding.tvLogs.text = getString(R.string.no_logs)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    // Delete other files
                    file.delete()
                }
            }
            Toast.makeText(this, R.string.logs_cleared, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupPinchToZoom() {
        scaleGestureDetector = ScaleGestureDetector(this, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                logFontSize *= detector.scaleFactor
                logFontSize = logFontSize.coerceIn(4f, 20f)
                binding.tvLogs.textSize = logFontSize
                return true
            }
        })

        binding.scrollLogs.setOnTouchListener { v, event ->
            scaleGestureDetector.onTouchEvent(event)
            v.performClick()
            false 
        }
    }

    private fun updateLogs() {
        val logFile = File(filesDir, "logs/run.log")
        if (logFile.exists()) {
            try {
                val lines = logFile.readLines().takeLast(200)
                val logContent = lines.joinToString("\n")
                if (binding.tvLogs.text.toString() != logContent) {
                    binding.tvLogs.text = logContent
                    binding.scrollLogs.post {
                        binding.scrollLogs.fullScroll(View.FOCUS_DOWN)
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun saveConfig() {
        val username = binding.etUsername.text.toString()
        val password = binding.etPassword.text.toString()

        val config = JSONObject()
        config.put("enabled", true)
        config.put("log_lv", 4)

        val accounts = JSONArray()
        val account = JSONObject()
        account.put("username", username)
        account.put("password", password)
        account.put("channel", "phone")
        account.put("mark", "")
        accounts.put(account)

        config.put("accounts", accounts)

        val configFile = File(filesDir, "ESurfingClient.json")
        configFile.writeText(config.toString(4))
        Toast.makeText(this, R.string.config_saved, Toast.LENGTH_SHORT).show()
    }

    private fun loadConfigToUI() {
        val configFile = File(filesDir, "ESurfingClient.json")
        if (configFile.exists()) {
            try {
                val config = JSONObject(configFile.readText())
                val accounts = config.getJSONArray("accounts")
                if (accounts.length() > 0) {
                    val account = accounts.getJSONObject(0)
                    binding.etUsername.setText(account.getString("username"))
                    binding.etPassword.setText(account.getString("password"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun checkPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                return
            }
        }
        startService()
    }

    private fun startService() {
        val intent = Intent(this, ESurfingService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startService()
        }
    }

    private external fun isNativeRunning(): Boolean

    external fun stringFromJNI(): String

    companion object {
        init {
            System.loadLibrary("ans")
        }
    }
}
