package com.atjaa.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.Html
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
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

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
            val intent = Intent(this, AdminMonitorListActivity::class.java).apply {
                putExtra("ip", id)
            }
            startActivity(intent)
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
            Toasty.info(this, "当前系统版本:" + CommonUtils.getAppVersion(this).second).show()
        }
    }

    fun back(view: View) {
        onBackPressedDispatcher.onBackPressed()
    }

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
                var localIp = getLocalIp()
                if (null != localIp) {
                    var ipPrefix = localIp.substring(0, localIp.lastIndexOf('.') + 1)
                    var openIps = scanSubnet(ipPrefix, ConstConfig.PORT)
                    println("扫描完成，开放设备数: ${openIps.size}")
                    var datalist = getInfoList(openIps, localIp)
                    val
                    // 创建SimpleAdapter
                            simpleAdapter = SimpleAdapter(
                        context,  // 上下文
                        datalist,  // 数据源
                        R.layout.item_ip_list,  // 每项的布局文件
                        arrayOf<String>(
                            "ip",
                            "name"
                        ),  // 数据源中Map的key
                        intArrayOf(
                            R.id.txt_ip,
                            R.id.txt_name
                        ) // 对应布局中的控件ID
                    )
                    simpleAdapter.viewBinder =
                        SimpleAdapter.ViewBinder { view, data, textRepresentation ->
                            if (view.id == R.id.txt_ip && data is String) {
                                val textView = view as TextView
                                // 将字符串解析为 HTML 并设置给 TextView
                                textView.text = Html.fromHtml(data, Html.FROM_HTML_MODE_LEGACY)
                                true // 返回 true 表示我们已经手动处理了该 View，SimpleAdapter 不需要再处理
                            } else {
                                false // 返回 false 让 SimpleAdapter 按默认方式处理其他 View
                            }
                        }
                    runOnUiThread {
                        list.adapter = simpleAdapter
                    }
                }
            } catch (e: Exception) {
                println("扫描异常" + e)
            } finally {
                // 3. 任务完成后（无论成功失败），切回主线程恢复按钮
                runOnUiThread {
                    bScan.isEnabled = true
                    bScan.text = "扫描"
                }
            }
        }
    }

    suspend fun getInfoList(openIps: List<String>, localIp: String): List<Map<String, Any>> {
        var dataList: MutableList<Map<String, Any>> = ArrayList<Map<String, Any>>()
        if (null != openIps && openIps.size > 0) {
            for (ip in openIps) {
                var url = "http://" + ip + ":" + ConstConfig.PORT + "/"
                var result = HttpUtils.fetchUrlContent(url)
                if (null != result && result.startsWith("ok#")) {
                    var info: HashMap<String, String> = HashMap<String, String>()

                    // TODO 着急，逻辑可以优化
                    if (ip.startsWith(localIp)) {
                        info.put(
                            "ip",
                            "IP地址：" + ip + "(本机)" + result.substring(3).split("#")
                                .get(1) + "#" + result.substring(3).split("#").get(2)
                        )
                    } else {
                        info.put(
                            "ip",
                            "IP地址：" + ip + result.substring(3).split("#")
                                .get(1) + "#" + result.substring(3).split("#").get(2)
                        )
                    }
                    info.put("name", "机器名：" + result.substring(3).split("#").get(0))
                    info.put("id", ip)
                    dataList.add(info)
                }
            }
        }
        return dataList
    }

    /**
     * 获取本机ip
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

    suspend fun scanSubnet(prefix: String, port: Int): List<String> = withContext(Dispatchers.IO) {
        val openIps = mutableListOf<String>()
        val jobs = (1..254).map { i ->
            async {
                val testIp = "$prefix$i"
                if (isPortOpen(testIp, port)) {
                    synchronized(openIps) { openIps.add(testIp) }
                }
            }
        }
        jobs.awaitAll() // 等待所有 IP 扫描结束
        return@withContext openIps // 此时 openIps 已填充完毕
    }

    private fun isPortOpen(ip: String, port: Int, timeout: Int = 200): Boolean {
        return try {
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), timeout)
            socket.close()
            true
        } catch (e: Exception) {
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