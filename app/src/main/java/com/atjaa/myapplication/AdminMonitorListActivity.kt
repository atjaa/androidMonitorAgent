package com.atjaa.myapplication

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.ImageView
import android.widget.SimpleAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.atjaa.myapplication.bean.ConstConfig
import com.atjaa.myapplication.databinding.ActivityAdminMonitorListBinding
import com.atjaa.myapplication.utils.HttpUtils
import com.atjaa.myapplication.utils.MonitorUtils
import com.atjaa.myapplication.utils.SystemInforUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.text.startsWith
import kotlin.text.substring

class AdminMonitorListActivity : AppCompatActivity() {
    lateinit var binding: ActivityAdminMonitorListBinding
    lateinit var ip: String
    var type: Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAdminMonitorListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        ip = intent.getStringExtra("ip").toString()
        binding.txtIp.text = ip
        type = SystemInforUtils.DAY
        var bottomNavView: BottomNavigationView = binding.bottomNavView
        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_day -> {
                    type = SystemInforUtils.DAY
                    initView(type)
                    true
                }

                R.id.menu_week -> {
                    type = SystemInforUtils.WEEK
                    initView(type)
                    true
                }

                R.id.menu_month -> {
                    type = SystemInforUtils.MONTH
                    initView(type)
                    true
                }

                R.id.menu_option -> {
                    Toasty.info(this, "开始截屏")
                    true
                }

                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        initView(type)
    }

    fun initView(type: Int) {
        var list = binding.list
        var txtAllDuration = binding.txtAllDuration
        var txtAllNumbers = binding.txtAllNumbers
        val context = this
        var url = "http://" + ip + ":" + ConstConfig.PORT
        when (type) {
            0 -> {
                url = url + "/monitor/day"
            }

            1 -> {
                url = url + "/monitor/week"
            }

            2 -> {
                url = url + "/monitor/month"
            }
        }
        lifecycleScope.launch(Dispatchers.IO) {
            var result = HttpUtils.fetchUrlContent(url)
            if (null != result && result.startsWith("ok#")) {
                var dataStr = result.substring(3)
                var dataList = Gson().fromJson(dataStr, Map::class.java)
                var allDuration: String = dataList.get("txtAllDuration") as String
                var allNumbers: String = dataList.get("txtAllNumbers") as String
                // 只有主线程才能操作页面
                runOnUiThread {
                    txtAllDuration.setText(allDuration)
                    txtAllNumbers.setText(allNumbers)
                }
                var datalist: List<Map<String, Any>> =
                    dataList.get("datalist") as List<Map<String, Any>>
                val
                // 创建SimpleAdapter
                        simpleAdapter = SimpleAdapter(
                    context,  // 上下文
                    datalist,  // 数据源
                    R.layout.item_info_list,  // 每项的布局文件
                    arrayOf<String>(
                        "name",
                        "usedTime",
                        "lastTime",
                        "icon",
                        "playNumber"
                    ),  // 数据源中Map的key
                    intArrayOf(
                        R.id.txt_name,
                        R.id.txt_use_time,
                        R.id.txt_duration,
                        R.id.img_icon,
                        R.id.txt_numbers
                    ) // 对应布局中的控件ID
                )
                // 2. 设置 ViewBinder 来特殊处理 icon 字段
                simpleAdapter.viewBinder =
                    SimpleAdapter.ViewBinder { view, data, textRepresentation ->
                        // 判断当前处理的是否是我们的 ImageView 控件
                        if (view.id == R.id.img_icon && data is String) {
                            val imageView = view as ImageView
                            try {
                                // 将 Base64 字符串转回 Bitmap
                                val decodedBytes =
                                    android.util.Base64.decode(data, android.util.Base64.DEFAULT)
                                val bitmap = android.graphics.BitmapFactory.decodeByteArray(
                                    decodedBytes,
                                    0,
                                    decodedBytes.size
                                )

                                if (bitmap != null) {
                                    imageView.setImageBitmap(bitmap)
                                }
                            } catch (e: Exception) {
                                println("图标处理异常" + e)
                            }
                            return@ViewBinder true // 返回 true 表示我们已经手动处理了该 View，Adapter 不需要再处理
                        }
                        false // 返回 false 表示由系统按默认逻辑处理（比如 TextView 的赋值）
                    }
                runOnUiThread {
                    list.adapter = simpleAdapter
                }
            }
        }
    }

    fun back(view: View) {
        onBackPressedDispatcher.onBackPressed()
    }
}