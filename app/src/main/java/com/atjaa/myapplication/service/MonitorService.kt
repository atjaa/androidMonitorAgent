package com.atjaa.myapplication.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.text.format.DateUtils
import android.util.Log

import androidx.core.app.NotificationCompat
import com.atjaa.myapplication.bean.AppInforBean

import com.atjaa.myapplication.bean.ConstConfig
import com.atjaa.myapplication.receiver.InstallReceiver
import com.atjaa.myapplication.utils.CommonUtils
import com.atjaa.myapplication.utils.MonitorUtils
import com.atjaa.myapplication.utils.SpCacheUtils
import com.atjaa.myapplication.utils.SystemInforUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import es.dmoral.toasty.Toasty
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * 监控侧后台服务
 * 需要被保活拉起
 */
class MonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val installReceiver = InstallReceiver()
    private lateinit var wakeLock: PowerManager.WakeLock
    private lateinit var wifiLock: WifiManager.WifiLock

    // 提供http服务
    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? =
        null
    private var targetService: PhotoService? = null
    private var isBound = false
    val TAG: String = "MonitorService"

    // 拍照服务连接器
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
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

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager?
        if (powerManager != null) {
            // 参数一：保持 CPU 运行，但允许屏幕熄灭
            // 参数二：自定义标签，建议包含包名便于调试
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Phone Assistant:wakeLock"
            )
        }
        val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock =
            wifiManager.createWifiLock(
                WifiManager.WIFI_MODE_FULL_HIGH_PERF,
                "Phone Assistant:wifiLock"
            )

        Log.i(TAG,"【onCreate】立即创建通知并启动前台服务 (适配 Android 8.0+)")
        startForeground(1, createNotification())

        Log.i(TAG,"【onCreate】开启HTTP线程监听端口")
        startTcpServer(ConstConfig.PORT)

        Log.i(TAG,"【onCreate】绑定目标 拍照服务")
        val intent = Intent(this, PhotoService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)

        Log.i(TAG,"【onCreate】创建手机消息通道")
        CommonUtils.createNotificationChannel(this)

        Log.i(TAG,"【onCreate】启动Worker能力保活服务 15分钟保活一次")
        CommonUtils.scheduleServiceCheck(this)
        Log.i(TAG,"【onCreate】启动Worker信息上报服务 20分钟保活一次")
        CommonUtils.scheduleReportWork(this)

        Log.i(TAG,"【onCreate】动态注册软件安装广播接收器")
        val filter = IntentFilter(Intent.ACTION_PACKAGE_ADDED).apply {
            addDataScheme("package")
        }
        // installReceiver 是类成员变量，确保全局唯一
        registerReceiver(installReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 启动前台服务，需要创建notification
        startForeground(1, createNotification())

        // 重启判断
        if (intent == null) {
            // 在这里重新启动你的 Socket 监听或消息接收逻辑
            Log.i(TAG,"【onStartCommand】启动HTTP线程监听端口")
            startTcpServer(ConstConfig.PORT)
        }
        try {
            // 熄屏保持 开启电源白名单这里没有太大意义了，电源白名单后续优化可以关闭
//            暂时注释，原因是小米手机
//            (packageName=com.atjaa.myapplication, userId=0)'s appop state for runtime op android:nearby_wifi_devices should not be set directly.
//            可能有关
//            wakeLock?.acquire(60 * 60 * 1000L)
//            wifiLock?.acquire()
        } catch (e: Exception) {
            Log.e(TAG, "Lock启动异常" + e.message)
        }
        //返回 START_STICKY 告知系统：若被杀，请尝试重启我
        return START_STICKY;
    }


    override fun onDestroy() {
        try {
            unregisterReceiver(installReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "注销异常: ${e.message}")
        }

//        if (wakeLock?.isHeld == true) {
//            wakeLock?.release()
//        }
//        if (wifiLock?.isHeld == true) {
//            wifiLock?.release()
//        }
        server?.stop(1000, 2000)
        serviceScope.cancel()
        super.onDestroy()
    }

    /**
     * 创建前台服务需要发一个通知
     */
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

    /**
     * 启动HTTP服务监听
     */
    private fun startTcpServer(port: Int) {
        serviceScope.launch(Dispatchers.IO) {
            // 测试wifi切换不影响服务，只要保活
            var isStarted = false
            var retryCount = 0
            val maxRetries = 5

            while (!isStarted && retryCount < maxRetries) {
                try {
                    // 每次重试时重新构建 server 实例（防止内部状态残留）
                    server = embeddedServer(CIO, port = port, host = "0.0.0.0") {
                        routing {
                            get("/") {
                                call.respondText("ok#" + CommonUtils.getCustomDeviceName(applicationContext))
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

                            get("/monitor/current") {
                                call.respondText("ok#" + getPhoneInfo())
                            }

                            get("/monitor/photo") {
                                call.respondText("ok#" + targetService?.takePhoto())
                            }
                            get("/monitor/message") {
                                val message = call.request.queryParameters["m"]
                                if (null != message) {
                                    CommonUtils.showNotification(
                                        "家长通知",
                                        message,
                                        this@MonitorService
                                    )
                                }
                                call.respondText("ok#")
                            }
                            post("/monitor/voice") {
                                Log.i(TAG, "收到音频文件请求，开始读流")
                                try {
                                    val multipart = call.receiveMultipart()
                                    var file = CommonUtils.fileVoice(this@MonitorService, multipart)
                                    if (file != null) {
                                        Log.i(TAG, "音频文件下载完成准备播放")
                                        playReceivedVoice(file!!.absolutePath)
                                        call.respondText("ok#")
                                        // TODO 是否需要删除音频文件？
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "音频文件下载完播放异常" + e.message)
                                }
                            }
                        }
                    }

                    // 这里 wait 必须设为 false 才能继续后续逻辑判断
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

    /**
     * 播放语音文件
     */
    fun playReceivedVoice(path: String) {
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setDataSource(path)
        // 建议设置为语音模式，这样声音会走媒体通道
        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_ALARM) // 强制甚至在静音时可能响起的通道，或者用 USAGE_MEDIA
                .build()
        )
        mediaPlayer.prepare()
        mediaPlayer.start()

        mediaPlayer.setOnCompletionListener {
            it.stop()
            it.release() // 播完后释放内存
        }
        mediaPlayer.setOnErrorListener { mp, _, _ ->
            mp.release()
            true
        }
    }

    /**
     * 获取APP监控信息
     */
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


    fun getPhoneInfo(): String? {
        val map: MutableMap<String, MutableList<Map<String, Any>>> =
            HashMap<String, MutableList<Map<String, Any>>>()
        var beanMap = getForegroundApp()
        if (null != beanMap) {
            map.put("currentApp", beanMap)
        }
        var appAdd = getAppAddInfo()
        if (null != appAdd) {
            map.put("appAddInfo", appAdd)
        }
        return Gson().toJson(map)
    }

    /**
     * 获取当前运行APP
     */
    fun getForegroundApp(): MutableList<Map<String, Any>>? {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 // 查询最近 1 分钟
        // 获取统计列表
        val stats =
            usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        if (stats != null && stats.isNotEmpty()) {
            // 按最后使用时间排序
            val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
            var appInforBean = AppInforBean(sortedStats[0], this)
            if (appInforBean.isSuccess) {
                var data: MutableList<Map<String, Any>> = ArrayList<Map<String, Any>>()
                var beanMap = MonitorUtils.getData(appInforBean, 1)
                data.add(beanMap)
                return data
            }
        }
        return null
    }

    /**
     * 获取应用安装日志
     */
    fun getAppAddInfo(): MutableList<Map<String, Any>>? {
        SpCacheUtils.init(this)
        var appAddInfo = SpCacheUtils.get("appAddInfo")
        if ("".equals(appAddInfo)) {
            return null
        } else {
            val type = object : TypeToken<MutableList<Map<String, Any>>>() {}.type
            val data: MutableList<Map<String, Any>> = Gson().fromJson(appAddInfo, type)
            return data
        }
    }


}