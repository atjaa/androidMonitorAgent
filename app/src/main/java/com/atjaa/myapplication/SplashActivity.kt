package com.atjaa.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.PowerManager
import android.provider.Settings
import android.view.View

import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atjaa.myapplication.databinding.ActivitySplashBinding
import com.atjaa.myapplication.utils.PermissionUtils
import es.dmoral.toasty.Toasty

/**
 * 进入系统的倒计时页面
 * 主要进行授权申请操作
 * 使用无障碍进行自动点击自动授权
 */
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

        var sleepTime: Long = 3000
        //  无障碍授权后，下面获取权限全部自动
        // 电源白名单，防止杀死
        if (!PermissionUtils.isIgnoringBatteryOptimizations(this)) {
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
        if (!PermissionUtils.isUsageCompat(this)) {
            // 权限未授予，需要请求权限
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                101
            )
        }

        // 申请发送系统消息权限 (只兼容小米手机)
        if(!PermissionUtils.isNotificationPermission(this)) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                1
            )

            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            }
            startActivity(intent)

        }
        // 倒计时进入登录页
        val intent = Intent(this, LoginActivity::class.java)
        val countDownTimer = object : CountDownTimer(sleepTime, 1000) {
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


}