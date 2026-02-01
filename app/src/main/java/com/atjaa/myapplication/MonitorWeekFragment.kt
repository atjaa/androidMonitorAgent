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
import com.atjaa.myapplication.databinding.FragmentMonitorWeekBinding
import com.atjaa.myapplication.utils.HttpUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.get
import kotlin.text.startsWith
import kotlin.text.substring


class MonitorWeekFragment : Fragment() {
    lateinit var binding: FragmentMonitorWeekBinding
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
        binding = FragmentMonitorWeekBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            refreshData()
        }
    }

    fun updateContent(currentIp: String) {
        this.currentIp = currentIp
    }

    /**
     * 获取监控数据
     */
    fun refreshData() {
        Log.d(TAG, "MonitorWeekFragment页面可见，开始刷新...")
        var list = binding.list
        var txtAllDuration = binding.txtAllDuration
        var txtAllNumbers = binding.txtAllNumbers
        val context = requireContext()
        var url = "http://" + currentIp + ":" + ConstConfig.PORT + "/monitor/week"
        lifecycleScope.launch(Dispatchers.IO) {
            var result = HttpUtils.fetchUrlContent(url)
            if (null != result && result.startsWith("ok#")) {
                var dataStr = result.substring(3)
                var dataList = Gson().fromJson(dataStr, Map::class.java)
                var allDuration: String = dataList.get("txtAllDuration") as String
                var allNumbers: String = dataList.get("txtAllNumbers") as String
                // 只有主线程才能操作页面
                activity?.runOnUiThread {
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
                activity?.runOnUiThread {
                    list.adapter = simpleAdapter
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MonitorWeekFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}