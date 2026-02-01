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
import com.atjaa.myapplication.databinding.ActivityMonitorBinding
import com.atjaa.myapplication.utils.MonitorUtils
import com.atjaa.myapplication.utils.SystemInforUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import es.dmoral.toasty.Toasty


/**
 * 被监控端自己展示页面
 */
class MonitorActivity : AppCompatActivity() {
    lateinit var binding: ActivityMonitorBinding
    var type: Int = 0
    fun back(view: View) {
        onBackPressedDispatcher.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMonitorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
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
        val systemInfor = SystemInforUtils(this, type)
        if (systemInfor.allPlayTime == 0L) {
            txtAllDuration.setText("总运行时长:0")
        } else {
            txtAllDuration.setText("总运行时长:" + DateUtils.formatElapsedTime(systemInfor.allPlayTime / 1000))
        }

        txtAllNumbers.setText("开机操作总次数:" + systemInfor.allUsedNumber)
        var datalist: List<Map<String, Any>> = ArrayList<Map<String, Any>>()
        try {
            datalist =
                MonitorUtils.getDataList(systemInfor.showDataList, 0)
        } catch (e: Exception) {
            Toasty.error(this, "运行异常").show()
        }

        val
        // 创建SimpleAdapter
                simpleAdapter = SimpleAdapter(
            this,  // 上下文
            datalist,  // 数据源
            R.layout.item_info_list,  // 每项的布局文件
            arrayOf<String>("name", "usedTime", "lastTime", "icon", "playNumber"),  // 数据源中Map的key
            intArrayOf(
                R.id.txt_name,
                R.id.txt_use_time,
                R.id.txt_duration,
                R.id.img_icon,
                R.id.txt_numbers
            ) // 对应布局中的控件ID
        )
        simpleAdapter.setViewBinder(object : SimpleAdapter.ViewBinder {
            override fun setViewValue(view: View?, o: Any?, s: String?): Boolean {
                if (view is ImageView && o is Drawable) {
                    val iv = view
                    iv.setImageDrawable(o)
                    return true
                } else {
                    return false
                }
            }
        })
        list.adapter = simpleAdapter

    }


}
