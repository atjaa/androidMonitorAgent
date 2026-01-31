package com.atjaa.myapplication.bean

import android.app.usage.UsageStats
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import kotlin.jvm.Throws
import kotlin.properties.Delegates

/**
 * 监控数据实体类
 */
class AppInforBean {
    lateinit var usageStats: UsageStats //应用信息存储包对象
    lateinit var packageName: String //包名称
    var icon: Drawable? = null //应用图标
    lateinit var appName: String //应用名称
    var beginPlayTime: Long = 0 //最近一次使用时间
    var usedTimes: Long = 0L //运行时长
    var usedNumbers: Int = 0 //本次开机操作次数
    var timeStampMoveToForeground: Long = -1 //开始时间戳
    var timeStampMoveToBackGround: Long = -1 //结束时间戳
    lateinit var context: Context
    var isSuccess: Boolean = true

    constructor(usageStats: UsageStats, context: Context?) {
        this.usageStats = usageStats
        if (context != null) {
            this.context = context
        }
        try {
            generateInfor()
        } catch (e: Exception) {
            isSuccess = false
            println("$e")
        }
    }

    fun generateInfor() {
        var packageManager = context.packageManager
        this.packageName = usageStats.packageName
        if (!packageName.isNullOrEmpty()) {
            var info = packageManager.getApplicationInfo(packageName, 0)
            this.appName = packageManager.getApplicationLabel(info) as String
            this.usedTimes = usageStats.totalTimeInForeground
            // 反射获取，可能有bug
            this.usedNumbers =
                usageStats.javaClass.getDeclaredField("mLaunchCount").get(usageStats) as Int
            this.beginPlayTime = usageStats.lastTimeUsed
            this.icon = info.loadIcon(packageManager)
        }
    }

    /**
     * 计算运行时长
     */
    fun calculateRunningTime() {
        if (timeStampMoveToForeground < 0 || timeStampMoveToBackGround < 0) {
            return;
        }

        if (timeStampMoveToBackGround > timeStampMoveToForeground) {
            usedTimes += (timeStampMoveToBackGround - timeStampMoveToForeground);
            timeStampMoveToForeground = -1;
            timeStampMoveToBackGround = -1;
        }
    }

    fun timesPlusPlus() {
        usedNumbers++
    }
}