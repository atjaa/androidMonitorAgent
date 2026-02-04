package com.atjaa.myapplication.worker

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.atjaa.myapplication.bean.ConstConfig
import com.atjaa.myapplication.service.MonitorService
import java.net.InetSocketAddress
import java.net.Socket

/**
 * 保活worker
 */
class ServiceCheckWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {
    val TAG: String = "MyAccessibilityService"
    override fun doWork(): Result {
        Log.i(TAG, "保活Worker触发")
        val context = applicationContext
        // 检查服务是否正在运行（2026 年标准做法是检查静态变量或进程状态）
        if (!isPortOpen("127.0.0.1", ConstConfig.PORT)) {
            Log.i(TAG, "保活检查失败，重新拉起服务")
            val intent = Intent(context, MonitorService::class.java)
            // 注意：Android 14+ 后台启动前台服务有限制，需捕获异常
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // 如果应用在后台且无权限，可能会启动失败
                return Result.retry()
            }
        } else {
            Log.i(TAG, "保活检查成功")
        }
        return Result.success()
    }

    private fun isPortOpen(ip: String, port: Int, timeout: Int = 200): Boolean {
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