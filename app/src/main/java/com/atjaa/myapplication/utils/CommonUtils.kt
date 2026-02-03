package com.atjaa.myapplication.utils


import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.atjaa.myapplication.NotificationMessageActivity
import com.atjaa.myapplication.R
import com.atjaa.myapplication.worker.ReportWorker
import com.atjaa.myapplication.worker.ServiceCheckWorker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.http.content.MultiPartData
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.LinkedList
import java.util.concurrent.TimeUnit
import kotlin.collections.iterator

/**
 * 通用工具类
 */
class CommonUtils {
    companion object {
        val TAG = "CommonUtils"


        /**
         * 删除安装记录指定某一条
         */
        fun delAddDetail(packageName: String?, context: Context) {
            SpCacheUtils.init(context)
            var appAddInfo = SpCacheUtils.get("appAddInfo")
            if (!"".equals(appAddInfo)) {
                val type = object : TypeToken<LinkedList<MutableMap<String, Any>>>() {}.type
                val data: LinkedList<MutableMap<String, Any>> = Gson().fromJson(appAddInfo, type)
                data.removeIf { map ->
                    map["packageName"] == packageName
                }
                SpCacheUtils.put("appAddInfo", Gson().toJson(data))
            }
        }

        /**
         * 保活检查
         */
        fun scheduleServiceCheck(context: Context) {
            Log.i("Atjaa", "启动保活任务，15分钟保活一次")
            val checkRequest = PeriodicWorkRequestBuilder<ServiceCheckWorker>(
                15, TimeUnit.MINUTES // 系统允许的最小间隔是 15 分钟
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED) // 仅在有网时检查，更省电
                        .build()
                )
                .build()
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(
                "AtjaaServiceKeepAlive",
                ExistingPeriodicWorkPolicy.KEEP, // 如果任务已存在则保留，不重复创建
                checkRequest
            )
        }

        fun scheduleReportWork(context: Context) {
            Log.i("Atjaa", "启动信息上报任务，20分钟触发一次")
            val checkRequest = PeriodicWorkRequestBuilder<ReportWorker>(
                20, TimeUnit.MINUTES // 系统允许的最小间隔是 15 分钟
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED) // 仅在有网时检查，更省电
                        .build()
                )
                .build()
            val workManager = WorkManager.getInstance(context)
            workManager.enqueueUniquePeriodicWork(
                "AtjaaReportInfo",
                ExistingPeriodicWorkPolicy.KEEP, // 如果任务已存在则保留，不重复创建
                checkRequest
            )
        }

        /**
         * 获取APP的版本号
         * （构建版本号）
         */
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

        /**
         * 创建通知渠道
         * 渠道号暂时写死
         */
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = "新消息通知"
                val descriptionText = "用于显示 App 的实时管理提醒"
                val importance = NotificationManager.IMPORTANCE_HIGH // 必须是 HIGH 才能悬浮
                val channel = NotificationChannel("MSG_CHANNEL_ID_ATJAA", name, importance).apply {
                    enableVibration(true)
                    enableLights(true)
                    lightColor = Color.GREEN
                    vibrationPattern = longArrayOf(100, 200, 300, 400, 500)
                    description = descriptionText
                    setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
                }
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
            }
        }

        /**
         * 往通知渠道发送消息
         */
        fun showNotification(title: String, message: String, context: Context) {
            try {
                Log.i(TAG, "往安卓消息渠道发送消息：" + message)
                // 1. 点击通知后的跳转意图
                val intent = Intent(context, NotificationMessageActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                // 2. 构建通知
                val builder = NotificationCompat.Builder(context, "MSG_CHANNEL_ID_ATJAA")
                    .setSmallIcon(R.drawable.splash)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH) // 兼容旧版的高优先级
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE) // 声明为消息类
                    .setAutoCancel(true) // 点击后自动消失
                    .setContentIntent(pendingIntent) // 设置跳转
                    .setDefaults(NotificationCompat.DEFAULT_ALL)
                    .setFullScreenIntent(pendingIntent, true) // 关键：强制弹出横幅（慎用，通常用于来电）

                // 3. 发送
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    )
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    // 权限已授予，执行逻辑
                    NotificationManagerCompat.from(context)
                        .notify(System.currentTimeMillis().toInt(), builder.build())
                } else {
                    // 权限未授予，去申请权限
                    Log.e(TAG, "没有发送消息的权限")

                }

            } catch (e: Exception) {
                Log.e(TAG, "消息处理异常" + e.message)
            }
        }

        /**
         * 接受音频文件并存储
         * 返回存储文件对象
         */
        suspend fun fileVoice(context: Context, multipart: MultiPartData): File? {
            var file: File? = null
            multipart.forEachPart { part ->
                when (part) {
                    is PartData.FileItem -> {
                        val input = part.provider()
                        val fileName = "voice_${System.currentTimeMillis()}.m4a"
                        file = File(context.cacheDir, fileName)
                        input.copyTo(file.outputStream())
                    }

                    else -> part.dispose()
                }
            }
            return file
        }

        @SuppressLint("HardwareIds")
        fun getPhoneUuid(context: Context): String {
            val androidId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            return androidId
        }

        /**
         * 获取本机IP
         */
        fun getLocalIp(): String? {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                for (intf in interfaces) {
                    val addrs = intf.inetAddresses
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            val host = addr.hostAddress // 例如 192.168.1.50
                            return host
                        }
                    }
                }
            } catch (e: Exception) {
                println("获取本机IP异常" + e)
            }
            return null
        }

        /**
         * 获取设备名称
         */
        fun getCustomDeviceName(context: Context): String {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val isScreenOn = pm.isInteractive // 屏幕是否亮着
            var screenStr: String
            if (isScreenOn) {
                screenStr = "#<font color='red'>(亮屏)</font>"
            } else {
                screenStr = "#(熄屏)"
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                (Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
                    ?: "Unknown Device") + screenStr
            } else {
                // 旧版本通过蓝牙或特定字段获取
                (Settings.Secure.getString(context.contentResolver, "bluetooth_name")
                    ?: "Unknown Device") + screenStr
            }
        }

        fun getCustomDeviceNameSimple(context: Context): String {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                (Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
                    ?: "Unknown Device")
            } else {
                // 旧版本通过蓝牙或特定字段获取
                (Settings.Secure.getString(context.contentResolver, "bluetooth_name")
                    ?: "Unknown Device")
            }
        }

    }

}