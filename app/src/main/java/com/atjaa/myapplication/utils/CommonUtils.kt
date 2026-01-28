package com.atjaa.myapplication.utils


import android.content.Context
import android.util.Log
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
            Log.i("Atjaa","启动保活进程")
            val checkRequest = PeriodicWorkRequestBuilder<ServiceCheckWorker>(
                15, TimeUnit.MINUTES // 系统允许的最小间隔是 15 分钟
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED) // 仅在有网时检查，更省电
                        .build())
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "AtjaaServiceKeepAlive",
                ExistingPeriodicWorkPolicy.KEEP, // 如果任务已存在则保留，不重复创建
                checkRequest
            )
        }
    }
}