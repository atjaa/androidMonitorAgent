package com.atjaa.myapplication

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SimpleAdapter
import androidx.lifecycle.lifecycleScope
import com.atjaa.myapplication.bean.ConstConfig
import com.atjaa.myapplication.databinding.FragmentMonitorUnlinePermissionBinding
import com.atjaa.myapplication.utils.HttpUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.LinkedList
import kotlin.text.startsWith
import kotlin.text.substring


class MonitorUnlinePermissionFragment : Fragment() {
    lateinit var binding: FragmentMonitorUnlinePermissionBinding
    lateinit var uuid: String
    val TAG = "MonitorUnlinePermissionFragment"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMonitorUnlinePermissionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            refreshData()
        }
    }

    fun refreshData() {
        Log.d(TAG, "MonitorUnlinePermissionFragment页面可见，开始刷新...")
        var list = binding.list
        val context = requireContext()
        var url =
            "http://" + ConstConfig.remoteServerIp + ":" + ConstConfig.remoteServerPort + "/api/permission/" + uuid
        lifecycleScope.launch(Dispatchers.IO) {
            var result = HttpUtils.fetchUrlContent(url)
            if (null != result && result.startsWith("ok#")) {
                var dataStr = result.substring(3)
                val listType =
                    object : TypeToken<LinkedList<HashMap<String, String>>>() {}.getType()
                val permissionList: LinkedList<HashMap<String, String>?> =
                    Gson().fromJson(dataStr, listType)


                val
                // 创建SimpleAdapter
                        simpleAdapter = SimpleAdapter(
                    context,  // 上下文
                    permissionList,  // 数据源
                    R.layout.item_check_permission,  // 每项的布局文件
                    arrayOf<String>(
                        "tvCheck",
                        "tvCheckResult"
                    ),  // 数据源中Map的key
                    intArrayOf(
                        R.id.tv_check,
                        R.id.tv_check_result
                    ) // 对应布局中的控件ID
                )

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
            MonitorUnlinePermissionFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}