package com.atjaa.myapplication.utils


import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.pm.PackageInfoCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.atjaa.myapplication.worker.ServiceCheckWorker
import java.util.concurrent.TimeUnit

class CommonUtils {
    companion object {
        /**
         * 保活检查
         */
        fun scheduleServiceCheck(context: Context) {
            Log.i("Atjaa","启动保活任务，15分钟保活一次")
            val checkRequest = PeriodicWorkRequestBuilder<ServiceCheckWorker>(
                15, TimeUnit.MINUTES // 系统允许的最小间隔是 15 分钟
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED) // 仅在有网时检查，更省电
                        .build())
                .build()
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(
                "AtjaaServiceKeepAlive",
                ExistingPeriodicWorkPolicy.KEEP, // 如果任务已存在则保留，不重复创建
                checkRequest
            )
        }
        fun getAppVersion(context: Context): Pair<Long, String> {
            val manager = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13 (API 33) 及以上使用新 API
                manager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                // 旧版本 API
                manager.getPackageInfo(context.packageName, 0)
            }

            // longVersionCode 对应 build.gradle 中的 versionCode
            // versionName 对应 build.gradle 中的 versionName
            val versionCode = PackageInfoCompat.getLongVersionCode(info)
            val versionName = info.versionName ?: "1.0.0"

            return Pair(versionCode, versionName)
        }
    }
}