package com.atjaa.myapplication

import MenuBtn
import MenuButtonAdapter
import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.atjaa.myapplication.bean.ConstConfig
import com.atjaa.myapplication.databinding.ActivityAdminMonitorListBinding
import com.atjaa.myapplication.service.ImagePreviewDialog
import com.atjaa.myapplication.service.TextPreviewDialog
import com.atjaa.myapplication.utils.HttpUtils
import com.atjaa.myapplication.utils.SystemInforUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder
import kotlin.text.startsWith
import kotlin.text.substring

/**
 * 远程机器监控展示页面
 */
class AdminMonitorListActivity : AppCompatActivity() {
    lateinit var binding: ActivityAdminMonitorListBinding
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    lateinit var ip: String
    var type: Int = 0
    val TAG = "AdminMonitorListActivity"
    // 定义 Fragment 实例
    private var currentFragment = CurrentFragment()
    private var dayFragment = MonitorDayFragment()
    private var weekFragment = MonitorWeekFragment()
    private var activeFragment: Fragment = dayFragment

    @SuppressLint("ClickableViewAccessibility")
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

        setupKingKongArea()

        dayFragment.updateContent(ip)
        currentFragment.updateContent(ip)
        weekFragment.updateContent(ip)

        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, currentFragment, "3").hide(currentFragment)
            add(R.id.fragment_container, weekFragment, "2").hide(weekFragment)
            add(R.id.fragment_container, dayFragment, "1")
        }.commit()

        var bottomNavView: BottomNavigationView = binding.bottomNavView
        bottomNavView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menu_day -> {
                    type = SystemInforUtils.DAY
                    switchFragment(dayFragment)
                    true
                }

                R.id.menu_week -> {
                    type = SystemInforUtils.WEEK
                    switchFragment(weekFragment)
                    true
                }

                R.id.menu_info -> {
                    switchFragment(currentFragment)
                    true
                }

                else -> false
            }
        }
        val voiceItemView = bottomNavView.findViewById<View>(R.id.menu_voice)

        voiceItemView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 1. 手指按下：开始录音
                    Log.d("Voice", "开始录音")
                    v.setBackgroundColor(Color.LTGRAY)
                    startRecording()
                    true // 返回 true 拦截点击，防止触发 Fragment 切换
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // 2. 手指松开：结束并发送
                    Log.d("Voice", "停止录音并发送")
                    v.setBackgroundColor(Color.TRANSPARENT)
                    stopAndSend()
                    true
                }

                else -> false
            }
        }
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    override fun onResume() {
        super.onResume()
    }
    private fun switchFragment(targetFragment: Fragment) {
        if (activeFragment != targetFragment) {
            supportFragmentManager.beginTransaction()
                .hide(activeFragment)
                .show(targetFragment)
                .commit()
            activeFragment = targetFragment
        }
    }

    private fun setupKingKongArea() {
        val menuItems = listOf(
            MenuBtn("sendMes", R.drawable.chat_bubble_left_right),
            MenuBtn("takePhoto", android.R.drawable.presence_video_online),
        )
        // 设置为 4 列的网格布局
        binding.rvKingkong.layoutManager = GridLayoutManager(this, 2)
        binding.rvKingkong.adapter = MenuButtonAdapter(menuItems) { pos ->
            if("sendMes".equals(menuItems[pos].title)){
                takeMessage()
            }
            if("takePhoto".equals(menuItems[pos].title)){
                takePhoto()
            }
        }
    }

    // 开始录音逻辑
    private fun startRecording() {
        if (isRecording) return // 如果正在录音，直接跳过
        isRecording = true
        val fileName = "voice_${System.currentTimeMillis()}.m4a"
        audioFile = File(externalCacheDir, fileName)
        mediaRecorder =
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(audioFile?.absolutePath)
                prepare()
                start()
            }
    }

    // 停止并执行发送
    private fun stopAndSend() {
        if (!isRecording) return
        isRecording = false
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null

            // 执行发送
            audioFile?.let { file ->
                if (file.exists() && file.length() > 0) {
                    Log.d(TAG, "音频文件地址" + file.absolutePath)
                    Log.d(TAG, "音频文件大小" + file.length())
                    var url =
                        "http://" + ip + ":" + ConstConfig.PORT + "/monitor/voice"
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            var result = HttpUtils.uploadVoiceFile(file, url)
                            if (null != result && result.startsWith("ok#")) {
                                Log.i(TAG, "音频发送成功")
                                audioFile?.delete()
                                this@AdminMonitorListActivity.runOnUiThread {
                                    Toasty.info(this@AdminMonitorListActivity, "音频已发送").show()
                                }
                            } else {
                                Log.i(TAG, "音频发送失败")
                                audioFile?.delete()
                                this@AdminMonitorListActivity.runOnUiThread {
                                    Toasty.info(this@AdminMonitorListActivity, "音频发送失败")
                                        .show()
                                }
                            }
                        } catch (e: Exception) {
                            audioFile?.delete()
                            Log.e(TAG, "音频发送异常" + e.message)
                            this@AdminMonitorListActivity.runOnUiThread {
                                Toasty.info(this@AdminMonitorListActivity, "音频发送异常").show()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("Voice", "录音时间太短或失败")
            Toasty.info(this@AdminMonitorListActivity, "录音时间太短或失败").show()
        }
    }

    // 定义权限请求器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Toasty.success(this, "权限已开启，请长按录音").show()
        } else {
            Toasty.error(this, "录音权限被拒绝，无法发送语音").show()
        }
    }

    /**
     * 发送消息
     */
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

    /**
     * 获取对方摄像头图片
     */
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

    fun back(view: View) {
        onBackPressedDispatcher.onBackPressed()
    }
}