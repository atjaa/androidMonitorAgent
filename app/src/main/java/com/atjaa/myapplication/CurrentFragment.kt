package com.atjaa.myapplication

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.lifecycle.lifecycleScope
import com.atjaa.myapplication.bean.ConstConfig
import com.atjaa.myapplication.databinding.FragmentCurrentBinding
import com.atjaa.myapplication.utils.HttpUtils
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.collections.get
import kotlin.text.startsWith
import kotlin.text.substring

class CurrentFragment : Fragment() {
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
        var url = "http://" + currentIp + ":" + ConstConfig.PORT + "/monitor/current"
        lifecycleScope.launch(Dispatchers.IO) {
            var result = HttpUtils.fetchUrlContent(url)
            if (null != result && result.startsWith("ok#")) {
                var dataStr = result.substring(3)
                if ("null".equals(dataStr)) {
                    activity?.runOnUiThread {
                        binding?.let { b ->
                            b.currentName.text = "最近1分钟没有运行APP"
                        }
                    }
                } else {
                    var data = Gson().fromJson(dataStr, Map::class.java)

                    var currentName: String = data.get("name") as String
                    var currentUseTime: String = data.get("lastTime") as String
                    var currentNumbers: String = data.get("playNumber") as String
                    var icon: String = data.get("icon") as String
                    // 只有主线程才能操作页面
                    activity?.runOnUiThread {
                        binding?.let { b ->
                            b.currentName.text = currentName
                            b.currentUseTime.text = currentUseTime
                            b.currentNumbers.text = currentNumbers
                            if(null != icon){
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
            }
        }
    }

    fun updateContent(currentIp: String) {
        this.currentIp = currentIp
    }


    companion object {

        fun newInstance(param1: String, param2: String) =
            CurrentFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}