package com.atjaa.myapplication.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.atjaa.myapplication.service.MonitorService

class PgReplacedReceiver : BroadcastReceiver() {

    val TAG = "PgReplacedReceiver"
    override fun onReceive(context: Context, intent: Intent) {
        // 应用自更新
        if (Intent.ACTION_MY_PACKAGE_REPLACED == intent.getAction()) {
            Log.i(TAG,"收到应用自更新的广播")
            val serviceIntent: Intent = Intent(context, MonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent) // Android 8.0+ 需启动前台服务
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}