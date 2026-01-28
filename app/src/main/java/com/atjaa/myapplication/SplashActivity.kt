package com.atjaa.myapplication

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atjaa.myapplication.databinding.ActivitySplashBinding
import com.atjaa.myapplication.worker.AtjaaKeepAliveService
import es.dmoral.toasty.Toasty

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // 获取查看其他APP状态的权限
        if (!hasUsageStatsPermission(this)) {
            val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                // 某些系统版本下可以定位到具体 App 的二级页面，但并非所有系统都支持
                // data = Uri.fromParts("package", packageName, null)
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
            }
            Toasty.error(this, "无法获取OPSTR_GET_USAGE_STATS权限").show()
        }
        // 电源白名单，防止杀死
        if (!isIgnoringBatteryOptimizations(this)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                // 某些系统版本下可以定位到具体 App 的二级页面，但并非所有系统都支持
                data = Uri.parse("package:$packageName")
            }
            try {
                startActivity(intent)
            } catch (e: Exception) {
            }
            Toasty.error(this, "无法获取ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS权限").show()
        }
        // 拍照权限
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // 权限未授予，需要请求权限
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                101
            )
        }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.FOREGROUND_SERVICE_CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.FOREGROUND_SERVICE_CAMERA),
                101
            )
        }



        // 无障碍保活权限
        if (!isAccessibilitySettingsOn(this, AtjaaKeepAliveService::class.java)) {
            // 权限未开启，弹出提示或直接跳转
            Toasty.info(this, "请在设置中开启[无障碍服务]以确保护理逻辑运行", Toasty.LENGTH_LONG)
                .show()
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        } else {
            // 权限已开启
            Log.d("Accessibility", "服务已就绪")
        }


//        Handler(Looper.getMainLooper()).postDelayed({
//            val intent = Intent(this, LoginActivity::class.java)
//            startActivity(intent)
//            finish() // 如果不希望用户按返回键回到当前页面，请调用 finish()
//        }, 5000)
        val intent = Intent(this, LoginActivity::class.java)
        val countDownTimer = object : CountDownTimer(3000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // 更新 UI，显示剩余秒数
                binding.countDownView.text = "进入系统${millisUntilFinished / 1000}s"
            }

            override fun onFinish() {
                // 倒计时结束，跳转页面
                startActivity(intent)
                finish()
            }
        }.start()
    }

    fun isAccessibilitySettingsOn(context: Context, service: Class<out AccessibilityService>): Boolean {
        val serviceName = "${context.packageName}/${service.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        // 使用冒号分割并检查
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            // 兼容性判断：匹配全路径或简写路径
            if (componentName.equals(serviceName, ignoreCase = true)) {
                return true
            }
        }

        // 二次检查：通过系统服务直接获取当前“活跃中”的服务列表（最准确）
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledAppList = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (info in enabledAppList) {
            if (info.resolveInfo.serviceInfo.name == service.name) {
                return true
            }
        }

        return false
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        // 检查当前应用包名是否在白名单中
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        // unsafeCheckOpNoThrow 是 API 29 后的推荐做法
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29 及以上使用 unsafeCheckOpNoThrow
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            // API 29 以下使用老的 checkOpNoThrow
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }

        return mode == AppOpsManager.MODE_ALLOWED
    }
}