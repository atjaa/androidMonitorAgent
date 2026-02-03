package com.atjaa.myapplication.service

import android.accounts.Account
import android.app.Service
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.Intent
import android.content.SyncResult
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import com.atjaa.myapplication.bean.ConstConfig
import java.net.InetSocketAddress
import java.net.Socket

/**
 * 同步服务：这是系统“复活”入口
 */
class SyncService : Service() {
    val TAG = "SyncService"
    override fun onBind(intent: Intent): IBinder? {
        val syncAdapter = object : AbstractThreadedSyncAdapter(this, true) {
            override fun onPerformSync(
                account: Account?,
                extras: Bundle?,
                authority: String?,
                provider: ContentProviderClient?,
                syncResult: SyncResult?
            ) {
                Log.i(TAG, "账号同步触发，检查MonitorService 存活状态")
                if (!isPortOpen("127.0.0.1", ConstConfig.PORT)) {
                    Log.i(TAG, "MonitorService 已死，执行拉起操作")
                    // 启动你的服务
                    val serviceIntent = Intent(context, MonitorService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } else {
                    Log.i(TAG, "MonitorService 存活，无需处理")
                }
            }
        }

        // 关键点：改用这个方法名，它是最稳健的 Java 映射方法
        return syncAdapter.syncAdapterBinder
        // 如果 syncAdapterBinder 还报错，请强制使用 syncAdapter.getSyncAdapterBinder()
    }

    private fun isPortOpen(ip: String, port: Int, timeout: Int = 300): Boolean {
        Log.i(TAG, "触发保活检查")
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeout)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}