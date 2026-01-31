package com.atjaa.myapplication

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.atjaa.myapplication.component.PermissionCheckAdapter
import com.atjaa.myapplication.databinding.ActivityPermissionCheckBinding
import androidx.lifecycle.lifecycleScope
import com.atjaa.myapplication.utils.PermissionUtils
import com.atjaa.myapplication.worker.AtjaaKeepAliveService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PermissionCheckActivity : AppCompatActivity() {
    lateinit var binding: ActivityPermissionCheckBinding
    lateinit var recyclerView: RecyclerView
    lateinit var adapter: PermissionCheckAdapter
    private val checkList: MutableList<HashMap<String, String>> =
        ArrayList<HashMap<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPermissionCheckBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        recyclerView = binding.rvCheckList
        adapter = PermissionCheckAdapter(checkList)
        // 设置为垂直列表样式
        recyclerView.setLayoutManager(LinearLayoutManager(this))
        recyclerView.setAdapter(adapter)
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch(Dispatchers.Default) {
            if (checkList.isEmpty()) {
                // --- 1. 检查无障碍 ---
                val item1 = hashMapOf("tvCheck" to "检查无障碍授权", "tvCheckResult" to "......")
                safeAddAndNotify(item1) // 封装的安全方法

                delay(500)
                val isAcc = PermissionUtils.isAccessibilitySettingsOn(this@PermissionCheckActivity, AtjaaKeepAliveService::class.java)
                safeUpdateAndNotify(item1, if (isAcc) "通过" else "未开启")

                delay(500)

                // --- 2. 检查开机自启动 ---
                val item2 = hashMapOf("tvCheck" to "检查开机自启动授权", "tvCheckResult" to "无法检查")
                safeAddAndNotify(item2)

                delay(500)

                // --- 3. 检查使用情况 ---
                val item3 = hashMapOf("tvCheck" to "检查有权查看使用情况授权", "tvCheckResult" to "......")
                safeAddAndNotify(item3)

                delay(500)
                val isUsage = PermissionUtils.hasUsageStatsPermission(this@PermissionCheckActivity)
                safeUpdateAndNotify(item3, if (isUsage) "通过" else "未开启")
                delay(500)

                // --- 4. 检查电源白名单 ---
                val item4 = hashMapOf("tvCheck" to "检查电源白名单授权", "tvCheckResult" to "......")
                safeAddAndNotify(item4)
                delay(500)
                val isIgnoringBattery = PermissionUtils.isIgnoringBatteryOptimizations(this@PermissionCheckActivity)
                safeUpdateAndNotify(item4, if (isIgnoringBattery) "通过" else "未开启")
                delay(500)

                // --- 5. 检查摄像头拍照授权 ---
                val item5 = hashMapOf("tvCheck" to "检查摄像头拍照授权", "tvCheckResult" to "......")
                safeAddAndNotify(item5)
                delay(500)
                val isUsageCompat = PermissionUtils.isUsageCompat(this@PermissionCheckActivity)
                safeUpdateAndNotify(item5, if (isUsageCompat) "通过" else "未开启")
                delay(500)

            }
        }

    }

    // 提取两个安全操作方法，确保数据修改和 UI 通知在同一线程
    private suspend fun safeAddAndNotify(item: HashMap<String, String>) {
        withContext(Dispatchers.Main) {
            checkList.add(item)
            adapter.notifyItemInserted(checkList.size - 1)
            binding.rvCheckList.scrollToPosition(checkList.size - 1)
        }
    }
    private suspend fun safeUpdateAndNotify(item: HashMap<String, String>, result: String) {
        withContext(Dispatchers.Main) {
            val index = checkList.indexOf(item)
            if (index != -1) {
                item["tvCheckResult"] = result
                adapter.notifyItemChanged(index)
            }
        }
    }

    fun back(view: View) {
        onBackPressedDispatcher.onBackPressed()
    }
}