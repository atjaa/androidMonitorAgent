package com.atjaa.myapplication

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SimpleAdapter

import androidx.lifecycle.lifecycleScope
import com.atjaa.myapplication.bean.ConstConfig
import com.atjaa.myapplication.databinding.FragmentCurrentBinding
import com.atjaa.myapplication.utils.HttpUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.get
import kotlin.text.startsWith
import kotlin.text.substring

class MonitorCurrentFragment : Fragment() {
    lateinit var binding: FragmentCurrentBinding
    lateinit var currentIp: String
    val TAG = "MonitorFragment"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCurrentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            refreshData()
        }
    }

    private fun refreshData() {
        Log.d(TAG, "CurrentFragment页面可见，开始刷新...")
        val context = requireContext()
        var list = binding.list
        var url = "http://" + currentIp + ":" + ConstConfig.PORT + "/monitor/current"
        lifecycleScope.launch(Dispatchers.IO) {
            var result = HttpUtils.fetchUrlContent(url)
            if (null != result && result.startsWith("ok#")) {
                var dataStr = result.substring(3)
                val type =
                    object : TypeToken<MutableMap<String, MutableList<Map<String, Any>>>>() {}.type
                val data: MutableMap<String, MutableList<Map<String, Any>>> =
                    Gson().fromJson(dataStr, type)

                // 处理 正在运行的应用
                if (null == data.get("currentApp")) {
                    activity?.runOnUiThread {
                        binding?.let { b ->
                            b.currentName.text = "最近1分钟没有运行APP"
                        }
                    }
                } else {
                    val currentAppList: MutableList<Map<String, Any>>? = data.get("currentApp")
                    val currentApp = currentAppList?.get(0)
                    var currentName: String? = currentApp?.get("name") as? String
                    var currentUseTime: String? = currentApp?.get("lastTime") as? String
                    var currentNumbers: String? = currentApp?.get("playNumber") as? String
                    var icon: String? = currentApp?.get("icon") as? String
                    // 只有主线程才能操作页面
                    activity?.runOnUiThread {
                        binding?.let { b ->
                            b.currentName.text = currentName
                            b.currentUseTime.text = currentUseTime
                            b.currentNumbers.text = currentNumbers
                            if (null != icon) {
                                val decodedBytes =
                                    android.util.Base64.decode(icon, android.util.Base64.DEFAULT)
                                val bitmap = android.graphics.BitmapFactory.decodeByteArray(
                                    decodedBytes,
                                    0,
                                    decodedBytes.size
                                )

                                if (bitmap != null) {
                                    b.currentImgIcon.setImageBitmap(bitmap)
                                }
                            }
                        }
                    }
                }


                // 处理安装记录
                var datalist: MutableList<Map<String, Any>>? = data.get("appAddInfo")
                if (null != datalist) {
                    val
                    // 创建SimpleAdapter
                            simpleAdapter = SimpleAdapter(
                        context,  // 上下文
                        datalist,  // 数据源
                        R.layout.item_ip_list,  // 暂时使用这个模板
                        arrayOf<String>(
                            "appName",
                            "packageName",
                            "icon"
                        ),  // 数据源中Map的key
                        intArrayOf(
                            R.id.txt_ip,
                            R.id.txt_name,
                            R.id.img_icon
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
                                        android.util.Base64.decode(
                                            data,
                                            android.util.Base64.DEFAULT
                                        )
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
                    activity?.runOnUiThread {
                        list.adapter = simpleAdapter
                    }
                }
            }
        }
    }

    fun updateContent(currentIp: String) {
        this.currentIp = currentIp
    }


    companion object {

        fun newInstance(param1: String, param2: String) =
            MonitorCurrentFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}