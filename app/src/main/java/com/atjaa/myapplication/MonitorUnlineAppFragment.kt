package com.atjaa.myapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SimpleAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.atjaa.myapplication.bean.ConstConfig
import com.atjaa.myapplication.databinding.FragmentMonitorUnlineAppBinding
import com.atjaa.myapplication.utils.HttpUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.reflect.Type
import java.util.LinkedList


class MonitorUnlineAppFragment : Fragment() {

    lateinit var binding: FragmentMonitorUnlineAppBinding
    lateinit var uuid: String
    val TAG = "MonitorUnlineAppFragment"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMonitorUnlineAppBinding.inflate(inflater, container, false)
        // 第一个Fragment 默认打开就刷数据
        refreshData()
        return binding.root
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            refreshData()
        }
    }

    fun refreshData() {
        Log.d(TAG, "MonitorUnlineAppFragment页面可见，开始刷新...")
        var list = binding.list
        val context = requireContext()
        var url =
            "http://" + ConstConfig.remoteServerIp + ":" + ConstConfig.remoteServerPort + "/api/app/" + uuid
        lifecycleScope.launch(Dispatchers.IO) {
            var result = HttpUtils.fetchUrlContent(url)
            if (null != result && result.startsWith("ok#")) {
                var dataStr = result.substring(3)
                val listType: Type? = object : TypeToken<LinkedList<HashMap<String,String>>>() {}.getType()
                val appList: LinkedList<HashMap<String,String>> = Gson().fromJson(dataStr, listType)

                val
                // 创建SimpleAdapter
                        simpleAdapter = SimpleAdapter(
                    context,  // 上下文
                    appList,  // 数据源
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

    fun updateContent(uuid: String) {
        this.uuid = uuid
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            MonitorUnlineAppFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}