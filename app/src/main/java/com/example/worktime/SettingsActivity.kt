package com.example.worktime

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.worktime.databinding.ActivitySettingsBinding
import com.google.gson.Gson

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = getSharedPreferences("worktime_data", MODE_PRIVATE)
        
        setupUI()
        loadSettings()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "自动打卡设置"
        
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        
        // 启用自动打卡开关
        binding.enableAutoCheckSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (!isAccessibilityEnabled()) {
                    showAccessibilityDialog()
                    binding.enableAutoCheckSwitch.isChecked = false
                } else {
                    Toast.makeText(this, "自动打卡已启用", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "自动打卡已禁用", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 公司打卡 App 包名设置
        binding.targetAppPackageLayout.setOnClickListener {
            showPackageInputDialog()
        }
        
        // 打卡按钮文字设置
        binding.checkButtonTextLayout.setOnClickListener {
            showButtonTextInputDialog()
        }
    }

    private fun loadSettings() {
        val targetPackage = prefs.getString("targetAppPackage", "com.alibaba.android.rimet") ?: "com.alibaba.android.rimet"
        val buttonTexts = prefs.getString("checkButtonTexts", "上班打卡，下班打卡，打卡") ?: "上班打卡，下班打卡，打卡"
        val autoCheckEnabled = prefs.getBoolean("autoCheckEnabled", false)
        
        binding.targetAppPackageValue.text = targetPackage
        binding.checkButtonTextValue.text = buttonTexts
        binding.enableAutoCheckSwitch.isChecked = autoCheckEnabled
        
        // 同步到静态变量
        AutoCheckAccessibilityService.targetPackageName = targetPackage
        AutoCheckAccessibilityService.checkButtonTexts = buttonTexts.split(",", "，").map { it.trim() }
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as android.accessibilityservice.AccessibilityServiceManager
        val enabledServices = am.getEnabledAccessibilityServiceList(android.accessibilityservice.AccessibilityServiceInfo.FEEDBACK_GENERIC)
        
        for (service in enabledServices) {
            if (service.resolveInfo.serviceInfo.packageName == packageName) {
                return true
            }
        }
        return false
    }

    private fun showAccessibilityDialog() {
        AlertDialog.Builder(this)
            .setTitle("需要开启无障碍服务")
            .setMessage("自动打卡功能需要开启无障碍服务权限。\n\n点击「去开启」后：\n1. 找到「自动打卡服务」\n2. 开启开关\n3. 确认授权")
            .setPositiveButton("去开启") { _, _ ->
                openAccessibilitySettings()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun openAccessibilitySettings() {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开设置，请手动进入无障碍设置", Toast.LENGTH_LONG).show()
        }
    }

    private fun showPackageInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("公司打卡 App 包名")
        
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null)
        val editText = view.findViewById<EditText>(R.id.editText)
        editText.hint = "例如：com.alibaba.android.rimet"
        editText.setText(prefs.getString("targetAppPackage", "com.alibaba.android.rimet") ?: "")
        
        builder.setView(view)
        builder.setPositiveButton("保存") { dialog, _ ->
            val text = editText.text.toString().trim()
            if (text.isNotEmpty()) {
                prefs.edit().putString("targetAppPackage", text).apply()
                binding.targetAppPackageValue.text = text
                AutoCheckAccessibilityService.targetPackageName = text
                Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("取消", null)
        builder.show()
    }

    private fun showButtonTextInputDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("打卡按钮文字")
        .setMessage("多个文字用逗号分隔，系统会依次尝试点击")
        
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null)
        val editText = view.findViewById<EditText>(R.id.editText)
        editText.hint = "例如：上班打卡，下班打卡，打卡"
        editText.setText(prefs.getString("checkButtonTexts", "上班打卡，下班打卡，打卡") ?: "")
        
        builder.setView(view)
        builder.setPositiveButton("保存") { dialog, _ ->
            val text = editText.text.toString().trim()
            if (text.isNotEmpty()) {
                prefs.edit().putString("checkButtonTexts", text).apply()
                binding.checkButtonTextValue.text = text
                AutoCheckAccessibilityService.checkButtonTexts = text.split(",", "，").map { it.trim() }
                Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("取消", null)
        builder.show()
    }
}
