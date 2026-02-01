package com.atjaa.myapplication.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.atjaa.myapplication.service.MonitorService


/**
 * 广播监听服务
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 广播：加电、更新
        if (Intent.ACTION_BOOT_COMPLETED == intent.getAction() || Intent.ACTION_MY_PACKAGE_REPLACED == intent.getAction()) {
            val serviceIntent: Intent = Intent(context, MonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent) // Android 8.0+ 需启动前台服务
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}