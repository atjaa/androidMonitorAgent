package com.atjaa.myapplication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atjaa.myapplication.utils.SpCacheUtils

class AutoStartActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_auto_start)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

    }

    override fun onResume() {
        super.onResume()
        SpCacheUtils.init(this)
        if ("true".equals(SpCacheUtils.get("autoStart", "false"))) {
            val intent = Intent(this, SplashActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    /**
     * 拉起自启动授权页面
     */
    fun autoStart(view: View) {
        requestAutoStart(this)
    }

    /**
     * 进入下一个页面
     */
    fun autoSkip(view: View) {
        SpCacheUtils.init(this)
        SpCacheUtils.put("autoStart", "true")
        val intent = Intent(this, SplashActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun requestAutoStart(context: Context) {
        val brand = Build.BRAND.lowercase()
        val intent = Intent()

        try {
            when {
                brand.contains("xiaomi") -> {
                    intent.component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                }

                brand.contains("huawei") || brand.contains("honor") -> {
                    // 华为通常在手机管家的启动管理中
                    intent.component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.optimize.process.ProtectActivity"
                    )
                }

                brand.contains("oppo") -> {
                    intent.component = ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                    )
                }

                brand.contains("vivo") -> {
                    intent.component = ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                }

                else -> {
                    // 其他品牌跳转到应用详情页，让用户手动找
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    intent.data = Uri.fromParts("package", context.packageName, null)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // 如果跳转失败（系统版本变化），兜底跳转到设置详情页
            val detailIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            detailIntent.data = Uri.fromParts("package", context.packageName, null)
            detailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(detailIntent)
        }
    }
}