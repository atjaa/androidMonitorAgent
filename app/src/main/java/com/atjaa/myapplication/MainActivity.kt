package com.atjaa.myapplication

import android.accounts.Account
import android.accounts.AccountManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atjaa.myapplication.bean.ConstConfig
import com.atjaa.myapplication.utils.PermissionUtils
import com.atjaa.myapplication.utils.SpCacheUtils
import com.atjaa.myapplication.worker.AtjaaKeepAliveService

/**
 * 程序主入口
 * 包括无障碍权限申请
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val context = this
        // 账号注册
        initAccountSync(this)


        // 无障碍保活权限  之后进入下一个页面，自动申请其他权限
        if (!PermissionUtils.isAccessibilitySettingsOn(this, AtjaaKeepAliveService::class.java)) {
            val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            // 注册监听器
            val stateChangeListener = AccessibilityManager.AccessibilityStateChangeListener { enabled ->
                if (enabled) {
                    // 检查你的服务是否真的被开启了（因为其他应用开启也会触发此回调）
                    if (PermissionUtils.isAccessibilitySettingsOn(this, AtjaaKeepAliveService::class.java)) {
                        Log.d("Accessibility", "无障碍权限已激活！")
                        // 执行跳转回 App 的逻辑
                        val intent = Intent(context, AutoStartActivity::class.java).apply {
                            // 关键：如果 Activity 已在后台，则将其移至前台，而不是创建新实例
                            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                            // 如果是在 Service 或 Listener 中启动，必须加这个
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            // 选配：传递一个参数告诉 Activity 是从权限页回来的
                            putExtra("FROM_ACCESSIBILITY_SETTING", true)
                        }
                        context.startActivity(intent)
                        finish()
                    }
                }
            }
            accessibilityManager.addAccessibilityStateChangeListener(stateChangeListener)
            // 授权操作
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            try {
                startActivity(intent)
            } catch (e: Exception) {
            }
        } else {
            // 权限已开启
            Log.d("Accessibility", "服务已就绪")
            SpCacheUtils.init(this)
            if(!ConstConfig.SKIP_KEY.equals(SpCacheUtils.get("autoStart", "false"))) {
                val intent = Intent(context, AutoStartActivity::class.java)
                context.startActivity(intent)
                finish()
            }else{
                val intent = Intent(context, SplashActivity::class.java)
                context.startActivity(intent)
                finish()
            }
        }


    }

    fun initAccountSync(context: Context) {
        val accountType = "com.atjaa.monitor.account"
        val authority = "com.atjaa.monitor.provider"
        val accountName = "系统服务核心"

        val am = AccountManager.get(context)
        // 1. 获取该类型下所有已注册账号
        val accounts = am.getAccountsByType(accountType)

        // 检查是否已存在目标账号
        val existingAccount = accounts.find { it.name == accountName }
        val targetAccount = existingAccount ?: Account(accountName, accountType)

        if (existingAccount == null) {
            // 2. 如果不存在，则添加
            val success = am.addAccountExplicitly(targetAccount, null, null)
            Log.i("AccountSync", "账号注册结果: $success")
        }

        // 3. 无论是否新注册，都重新强制激活同步设置（确保开关是开着的）
        ContentResolver.setIsSyncable(targetAccount, authority, 1)
        ContentResolver.setSyncAutomatically(targetAccount, authority, true)

        // 4. 检查是否已有该频率的同步任务，没有则添加
        ContentResolver.addPeriodicSync(targetAccount, authority, Bundle.EMPTY, 15 * 60L)

        Log.i("AccountSync", "同步状态已激活")
    }


}