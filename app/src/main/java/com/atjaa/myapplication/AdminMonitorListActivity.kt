package com.atjaa.myapplication

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
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
import com.atjaa.myapplication.service.ImagePreviewDialog
import com.atjaa.myapplication.service.TextPreviewDialog
import com.atjaa.myapplication.utils.HttpUtils
import com.atjaa.myapplication.utils.SystemInforUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import kotlin.text.startsWith
import kotlin.text.substring

class AdminMonitorListActivity : AppCompatActivity() {
    lateinit var binding: ActivityAdminMonitorListBinding
    lateinit var ip: String
    var type: Int = 0
    val TAG = "AdminMonitorListActivity"
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

                R.id.menu_photo -> {
                    takePhoto()
                    true
                }

                R.id.menu_message -> {
                    takeMessage()
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

    fun takeMessage() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_input_text, null)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.et_content)

        // 2. 构建并展示弹窗
        MaterialAlertDialogBuilder(this)
            .setTitle("发送消息")
            .setView(dialogView)
            .setPositiveButton("确认") { _, _ ->
                val mes = editText.text.toString()
                if (mes.isNotEmpty()) {
                    Log.i(TAG, "发送消息到" + ip + ":" + mes)
                    val encodedMes = URLEncoder.encode(mes, "UTF-8")
                    var url =
                        "http://" + ip + ":" + ConstConfig.PORT + "/monitor/message?m=" + encodedMes
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            var result = HttpUtils.fetchUrlContent(url)
                            if (null != result && result.startsWith("ok#")) {
                                Log.i(TAG, "消息发送成功")
                                this@AdminMonitorListActivity.runOnUiThread {
                                    Toasty.info(this@AdminMonitorListActivity, "消息已发送").show()
                                }
                            } else {
                                Log.i(TAG, "消息发送失败")
                                this@AdminMonitorListActivity.runOnUiThread {
                                    Toasty.info(this@AdminMonitorListActivity, "消息发送失败")
                                        .show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "消息发送失败" + e.message)
                            this@AdminMonitorListActivity.runOnUiThread {
                                Toasty.info(this@AdminMonitorListActivity, "消息发送失败").show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun takePhoto() {

        var url = "http://" + ip + ":" + ConstConfig.PORT + "/monitor/photo"
        lifecycleScope.launch(Dispatchers.IO) {
            var result = HttpUtils.fetchUrlContent(url)
            if (null != result && result.startsWith("ok#")) {
                var dataStr = result.substring(3)
                if (dataStr.startsWith("手机")) {
                    runOnUiThread {
                        TextPreviewDialog(dataStr).show(supportFragmentManager, "preview")
                    }
                } else {
                    val decodedBytes =
                        android.util.Base64.decode(dataStr, android.util.Base64.DEFAULT)
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(
                        decodedBytes,
                        0,
                        decodedBytes.size
                    )

                    if (bitmap != null) {
                        runOnUiThread {
                            ImagePreviewDialog(bitmap).show(supportFragmentManager, "preview")
                        }
                    }
                }
            }
        }
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