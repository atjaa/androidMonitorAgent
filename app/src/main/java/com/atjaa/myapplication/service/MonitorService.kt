package com.atjaa.myapplication.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateUtils
import android.util.Log

import androidx.core.app.NotificationCompat

import com.atjaa.myapplication.bean.ConstConfig
import com.atjaa.myapplication.utils.CommonUtils
import com.atjaa.myapplication.utils.MonitorUtils
import com.atjaa.myapplication.utils.SystemInforUtils
import com.google.gson.Gson
import es.dmoral.toasty.Toasty
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var wakeLock: PowerManager.WakeLock

    // 提供http服务
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? =
        null
    private var targetService: PhotoService? = null
    private var isBound = false
    val TAG: String = "MonitorService"
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            //  获取 TargetService 实例
            val binder = service as PhotoService.LocalBinder
            targetService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. 针对 Android 14+，必须在 5 秒内关联前台通知
        // 假设你已经创建了 Notification
        startForeground(1, createNotification())

        // 2. 如果是重启的情况，intent 会是 null
        if (intent == null) {
            // 在这里重新启动你的 Socket 监听或消息接收逻辑
            startTcpServer(ConstConfig.PORT)
        }

        // 3. 返回 START_STICKY 告知系统：若被杀，请尝试重启我
        return START_STICKY;
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager?
        if (powerManager != null) {
            // 参数一：保持 CPU 运行，但允许屏幕熄灭
            // 参数二：自定义标签，建议包含包名便于调试
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Phone Assistant:RemoteMessagingWakeLock"
            )
        }
        // 立即创建通知并启动前台服务 (适配 Android 8.0+)
        startForeground(1, createNotification())

        // 开启线程监听端口
        startTcpServer(ConstConfig.PORT)
        CommonUtils.scheduleServiceCheck(this)
        // 绑定目标 Service
        val intent = Intent(this, PhotoService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        // 创建手机消息通道
        CommonUtils.createNotificationChannel(this)
    }

    private fun createNotification(): Notification {
        val channelId = "tcp_service"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, "TCP Server", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("TCP 服务运行中")
            .setContentText("正在监听端口 " + ConstConfig.PORT)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()
    }

    private fun startTcpServer(port: Int) {
        serviceScope.launch(Dispatchers.IO) {
            // 测试wifi切换不影响服务，只要保活
            // 无论第一次启动，还是被杀重启，先清理服务
            try {
                server?.stop(1000, 2000)
            } catch (e: Exception) {
                Log.e(TAG, "首先尝试STOP HTTP" + e)
            }
            var isStarted = false
            var retryCount = 0
            val maxRetries = 5

            while (!isStarted && retryCount < maxRetries) {
                try {
                    // 每次重试时重新构建 server 实例（防止内部状态残留）
                    server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
                        routing {
                            get("/") {
                                call.respondText("ok#" + getCustomDeviceName(applicationContext))
                            }
                            get("/monitor/day") {
                                call.respondText("ok#" + getMonitorInfo(0))
                            }
                            get("/monitor/week") {
                                call.respondText("ok#" + getMonitorInfo(1))
                            }
                            get("/monitor/month") {
                                call.respondText("ok#" + getMonitorInfo(2))
                            }
                            get("/monitor/photo") {
                                call.respondText("ok#" + targetService?.katePhoto())
                            }
                            get("/monitor/message") {
                                val message = call.request.queryParameters["m"]
                                if (null != message) {
                                    CommonUtils.showNotification("家长通知", message, this@MonitorService)
                                }
                                call.respondText("ok#")
                            }
                        }
                    }

                    // 2. 关键点：这里 wait 必须设为 false 才能继续后续逻辑判断
                    server?.start(wait = false)

                    isStarted = true
                    Log.i(TAG, "HTTP服务在端口 $port 启动成功 (第 $retryCount 次尝试)")
                } catch (e: Exception) {
                    retryCount++
                    Log.w(TAG, "第 $retryCount 次启动失败，原因: ${e.message}")

                    server = null // 释放资源

                    if (retryCount < maxRetries) {
                        Log.w(TAG, "等待 2 秒后重试...")
                        delay(2000) // 3. 协程挂起，不阻塞主线程
                    } else {
                        Log.e(TAG, "达到最大重试次数，放弃启动。")
                    }
                }
            }
        }
    }


    fun getMonitorInfo(type: Int): String {
        val systemInfor = SystemInforUtils(this, type)
        var datalist: List<Map<String, Any>> = ArrayList<Map<String, Any>>()
        try {
            datalist =
                MonitorUtils.getDataList(systemInfor.showDataList, 1)
        } catch (e: Exception) {
            Toasty.error(this, "运行异常").show()
        }
        var result: HashMap<String, Any> = HashMap()
        if (systemInfor.allPlayTime == 0L) {
            result.put("txtAllDuration", "总运行时长:0")
        } else {
            result.put(
                "txtAllDuration",
                "总运行时长:" + DateUtils.formatElapsedTime(systemInfor.allPlayTime / 1000)
            )
        }
        result.put("txtAllNumbers", "开机操作总次数:" + systemInfor.allUsedNumber)
        result.put("datalist", datalist)
        return Gson().toJson(result)
    }

    // 获取系统全局设置中的设备名称
    fun getCustomDeviceName(context: Context): String {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = pm.isInteractive // 屏幕是否亮着
        var screenStr: String
        if (isScreenOn) {
            screenStr = "#<font color='red'>(亮屏)</font>"
        } else {
            screenStr = "#(熄屏)"
        }
        if (!isScreenOn) {

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

    override fun onDestroy() {
        super.onDestroy()
        server?.stop(1000, 2000)
        serviceScope.cancel()
    }


}