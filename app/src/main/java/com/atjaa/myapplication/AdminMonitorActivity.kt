package com.atjaa.myapplication

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Html
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.atjaa.myapplication.bean.ConstConfig
import com.atjaa.myapplication.databinding.ActivityAdminMonitorBinding
import com.atjaa.myapplication.utils.CommonUtils
import com.atjaa.myapplication.utils.HttpUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.lang.reflect.Type
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.net.SocketFactory


/**
 * 监控扫描页
 */
class AdminMonitorActivity : AppCompatActivity() {
    lateinit var binding: ActivityAdminMonitorBinding
    private lateinit var floatingButton: Button
    private lateinit var toUpdate: Button
    private lateinit var toAbout: Button
    private lateinit var toPermissionCheck: Button
    private lateinit var overlayLayout: LinearLayout
    private lateinit var additionalButtonsContainer: LinearLayout
    private lateinit var list: ListView
    private var isOverlayVisible = false
    private val TAG = "AdminMonitorActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminMonitorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        floatingButton = binding.floatingButton
        overlayLayout = binding.overlayLayout
        additionalButtonsContainer = binding.additionalButtonsContainer
        list = binding.list
        toUpdate = binding.toUpdate
        toPermissionCheck = binding.toPermissionCheck
        toAbout = binding.toAbout

        list.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            // position 是当前点击的索引（从 0 开始）
            // parent.getItemAtPosition(position) 获取该行对应的数据对象
            val selectedItem = parent.getItemAtPosition(position) as Map<String, Any>

            val id = selectedItem["id"].toString()
            val uuid = selectedItem["uuid"].toString()
            val type = selectedItem["type"].toString()
            if ("0".equals(type)) {
                // 展示局域网数据
                val intent = Intent(this, AdminMonitorListActivity::class.java).apply {
                    putExtra("ip", id)
                }
                startActivity(intent)
            }
            if ("1".equals(type)) {
                // 展示云端数据
                val intent = Intent(this, AdminMonitorUnlineListActivity::class.java).apply {
                    putExtra("ip", id)
                    putExtra("uuid", uuid)
                }
                startActivity(intent)
            }
        }
        floatingButton.setOnClickListener {
            toggleOverlay()
        }
        overlayLayout.setOnClickListener {
            if (isOverlayVisible) {
                hideOverlay()
            }
        }
        toUpdate.setOnClickListener {
            val intent = Intent(this, AppUpdateActivity::class.java)
            startActivity(intent)
        }
        toPermissionCheck.setOnClickListener {
            val intent = Intent(this, PermissionCheckActivity::class.java)
            startActivity(intent)
        }
        toAbout.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
        }
    }

    fun back(view: View) {
        onBackPressedDispatcher.onBackPressed()
    }

    /**
     * 获取开发监听端口的机器列表
     */
    fun scan(view: View) {
        val bScan = binding.bScan
        val list = binding.list
        // 1. 立即禁用按钮，防止重复点击
        bScan.isEnabled = false
        bScan.text = "扫描中..."
        val context = this
        // 2. 开启协程执行后台任务
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 执行你的扫描逻辑
                var localIp = CommonUtils.getLocalIp()
                if (null != localIp) {
                    var ipPrefix = localIp.substring(0, localIp.lastIndexOf('.') + 1)
                    var openIps = scanSubnet(ipPrefix, ConstConfig.PORT, 1000, context)
                    Log.i(TAG, "扫描完成，开放设备数: ${openIps.size}")
                    var datalist = getInfoList(openIps, localIp)
                    val
                    // 创建SimpleAdapter
                            simpleAdapter = SimpleAdapter(
                        context,  // 上下文
                        datalist,  // 数据源
                        R.layout.item_ip_list,  // 每项的布局文件
                        arrayOf<String>(
                            "ip",
                            "name",
                            "typeDec"
                        ),  // 数据源中Map的key
                        intArrayOf(
                            R.id.txt_ip,
                            R.id.txt_name,
                            R.id.txt_type
                        ) // 对应布局中的控件ID
                    )
                    simpleAdapter.viewBinder =
                        SimpleAdapter.ViewBinder { view, data, textRepresentation ->
                            if (view.id == R.id.txt_ip && data is String) {
                                val textView = view as TextView
                                textView.text = Html.fromHtml(data, Html.FROM_HTML_MODE_LEGACY)
                                true // 返回 true 表示我们已经手动处理了该 View，SimpleAdapter 不需要再处理
                            } else if (view.id == R.id.txt_type && data is String) {
                                val textView = view as TextView
                                textView.text = Html.fromHtml(data, Html.FROM_HTML_MODE_LEGACY)
                                true
                            } else {
                                false // 返回 false 让 SimpleAdapter 按默认方式处理其他 View
                            }
                        }
                    runOnUiThread {
                        list.adapter = simpleAdapter
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "扫描异常" + e)
            } finally {
                // 3. 任务完成后（无论成功失败），切回主线程恢复按钮
                runOnUiThread {
                    bScan.isEnabled = true
                    bScan.text = "扫描"
                }
            }
        }
    }

    /**
     * 获取主机信息
     * 局域网和云端服务两份数据整合到一起
     */
    suspend fun getInfoList(openIps: List<String>, localIp: String): List<Map<String, Any>> {
        var dataList: MutableList<Map<String, Any>> = ArrayList<Map<String, Any>>()
        // 1、先获取局域网数据
        if (null != openIps && openIps.size > 0) {
            Log.i(TAG, "准备获取主机信息 " + openIps.toString())
            try {
                for (ip in openIps) {
                    var url = "http://" + ip + ":" + ConstConfig.PORT + "/"
                    var result = HttpUtils.fetchUrlContent(url)
                    if (null != result && result.startsWith("ok#")) {
                        Log.i(TAG, "获取主机信息  " + result)
                        var info: HashMap<String, String> = HashMap<String, String>()

                        // TODO 着急，逻辑可以优化
                        if (ip.startsWith(localIp)) {
                            info.put(
                                "ip",
                                "IP地址：" + ip + "(本机)" + result.substring(3).split("#").get(1)
                            )
                        } else {
                            info.put(
                                "ip",
                                "IP地址：" + ip + result.substring(3).split("#").get(1)
                            )
                        }
                        var nowTime = Calendar.getInstance().getTimeInMillis()
                        info.put(
                            "name",
                            "机器名：" + result.substring(3).split("#")
                                .get(0) + "   " + getTimeStrings(nowTime)
                        )
                        info.put("id", ip)
                        info.put("type", "0")
                        info.put("typeDec", "数据类型：<font color='blue'>局域网实时</font>")
                        info.put("uuid", "") // 局域网的没有uuid
                        dataList.add(info)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "局域网数据处理异常 " + e.message)
            }
        }
        // 2、获取云端服务器数据
        try {
            var url =
                "http://" + ConstConfig.remoteServerIp + ":" + ConstConfig.remoteServerPort + "/api/phone/get"
            var resultStr = HttpUtils.fetchUrlContent(url)

            if (null != resultStr && resultStr.startsWith("ok#")) {
                var result = resultStr.substring(3)
                val mapType: Type? =
                    object : TypeToken<HashMap<String, HashMap<String, String>>>() {
                    }.getType()
                val phoneMap: MutableMap<String, HashMap<String, String>>? =
                    Gson().fromJson(result, mapType)
                phoneMap?.forEach { (key, innerMap) ->
                    var info: HashMap<String, String> = HashMap<String, String>()
                    info.put("type", "1")
                    info.put("typeDec", "数据类型：<b>云端离线数据</b>")
                    info.put("uuid", key) // 云端的肯定有uuid
                    info.put(
                        "name", "机器名：" +
                                (innerMap.get("name") ?: "未知设备") + "   " + (innerMap.get("time")
                            ?: "now")
                    )
                    info.put("id", innerMap.get("ip") ?: "0.0.0.0")
                    info.put("ip", "IP地址：" + (innerMap.get("ip") ?: "0.0.0.0"))
                    dataList.add(info)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "云端服务器数据处理异常 " + e.message)
        }
        return dataList
    }

    private fun getTimeStrings(t: Long): String {
        val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        return simpleDateFormat.format(t)
    }

    suspend fun scanSubnet(
        prefix: String,
        port: Int,
        timeout: Int,
        context: Context
    ): List<String> = withContext(Dispatchers.IO) {
        val openIps = mutableListOf<String>()
        // 关键：限制同时进行的扫描任务数量，防止 ARP 风暴和线程耗尽
        val semaphore = Semaphore(20)
        val jobs = (1..254).map { i ->
            async {
                val testIp = "$prefix$i"
                semaphore.withPermit {
                    if (isPortOpen(testIp, port, timeout, context)) {
                        synchronized(openIps) { openIps.add(testIp) }
                    }
                }
            }
        }
        jobs.awaitAll() // 等待所有 IP 扫描结束
        return@withContext openIps // 此时 openIps 已填充完毕
    }

    private fun isPortOpen(ip: String, port: Int, timeout: Int = 2000, context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // 1. 查找当前活跃的 Wi-Fi 网络
        val wifiNetwork = connectivityManager.allNetworks.find { network ->
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }

        return try {
            // 2. 预热 (Pre-heat / ARP Warming)
            // 即使目标禁 Ping，这一步也会强制系统内核发送 ARP 请求去获取目标的 MAC 地址。
            // 我们给预热分配 500ms，不关心它是否真的 Reachable。
            val address = InetAddress.getByName(ip)
            try {
                address.isReachable(300)
            } catch (e: Exception) {
                // 忽略预热异常，它只是为了刷新内核 ARP 表
            }

            // 3. 正式建立 Socket 连接
            val socket = Socket()

            // 强制绑定 WiFi 路由，防止流量干扰
            wifiNetwork?.bindSocket(socket)

            // 使用 InetSocketAddress 显式连接
            // 如果第一阶段预热成功，这里的三次握手将非常快
            socket.connect(InetSocketAddress(address, port), timeout)

            socket.close()
            Log.d(TAG, "连接成功: $ip:$port")
            true
        } catch (e: SocketTimeoutException) {
            // 捕获超时：通常是防火墙拦截、IP 不存在或 ARP 依然未解析
            Log.w(TAG, "连接超时 ($ip:$port): ${e.message}")
            false
        } catch (e: Exception) {
            // 捕获其他异常：如 Connection Refused (端口未监听)
            Log.e(TAG, "连接失败 ($ip:$port): ${e.message}")
            false
        }
    }

    private fun isHttpPortOpen(
        ip: String,
        port: Int,
        timeout: Int = 1000,
        context: Context
    ): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // 1. 获取 WiFi 网络对象
        val wifiNetwork = connectivityManager.allNetworks.find { network ->
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }

        // 2. 构建 OkHttpClient 并强制绑定到 WiFi
        val client = OkHttpClient.Builder()
            .connectTimeout(timeout.toLong(), TimeUnit.MILLISECONDS)
            .socketFactory(wifiNetwork?.socketFactory ?: SocketFactory.getDefault()) // 核心：强制走 WiFi
            .build()

        // 3. 发起一个轻量级的 HEAD 请求（只握手，不下载数据）
        val request = Request.Builder()
            .url("http://$ip:$port")
            .head() // 比 GET 更快，仅测试端口可达性
            .build()

        return try {
            // execute() 是同步执行，适合在协程或子线程中调用
            client.newCall(request).execute().use { response ->
                // 只要握手成功并有响应（无论是 200 还是 404），都说明端口是开的
                true
            }
        } catch (e: Exception) {
            // 如果是 Timeout，这里会捕获到 SocketTimeoutException
            // 如果是拒绝连接，会捕获到 ConnectException
            Log.e(TAG, "isHttpPortOpen 判断 " + ip + "是否可连接失败：" + e.message)
            false
        }
    }

    private fun toggleOverlay() {
        if (isOverlayVisible) {
            hideOverlay()
        } else {
            showOverlay()
        }
    }

    private fun showOverlay() {
        overlayLayout.visibility = View.VISIBLE
        additionalButtonsContainer.visibility = View.VISIBLE
        isOverlayVisible = true
    }

    private fun hideOverlay() {
        overlayLayout.visibility = View.GONE
        additionalButtonsContainer.visibility = View.GONE
        isOverlayVisible = false
    }

}