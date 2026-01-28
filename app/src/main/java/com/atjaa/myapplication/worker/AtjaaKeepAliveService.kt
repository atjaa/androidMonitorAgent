package com.atjaa.myapplication.worker

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.atjaa.myapplication.utils.CommonUtils


class AtjaaKeepAliveService : AccessibilityService() {

    val TAG: String = "MyAccessibilityService"
    private var lastCheckTime = 0L
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val currentTime = System.currentTimeMillis()
        // 间隔少于 5 分钟 (300,000 毫秒) 则直接跳过
        if (currentTime - lastCheckTime < 1000 * 60 * 2) {
            return
        }

        lastCheckTime = currentTime
        Log.d(TAG, "触发一次无障碍检查")
        CommonUtils.scheduleServiceCheck(this)
    }

    override fun onInterrupt() {
        Log.d(TAG, "无障碍服务被中断")
        Toast.makeText(this, "服务中断", Toast.LENGTH_SHORT).show()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "无障碍服务已连接")
        Toast.makeText(this, "服务启动成功", Toast.LENGTH_SHORT).show()
    }
}