package com.atjaa.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.atjaa.myapplication.databinding.ActivityAppUpdateBinding
import com.atjaa.myapplication.service.AppUpdateManager
import es.dmoral.toasty.Toasty
import java.io.File

class AppUpdateActivity : AppCompatActivity(), AppUpdateManager.DownloadListener {
    private lateinit var appUpdateManager: AppUpdateManager
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var updateButton: Button
    val TAG: String = "MyAccessibilityService"

    // 授权成功码
    private val PERMISSION_REQUEST_CODE = 1001

    // TODO 示例APK下载地址，请替换为实际地址
    val downloadUrl = "http://192.168.3.13:8080/api/download/app-debug.apk"

    lateinit var binding: ActivityAppUpdateBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAppUpdateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        progressBar = binding.progressBar
        statusText = binding.statusText
        updateButton = binding.updateButton

        appUpdateManager = AppUpdateManager(this)
        appUpdateManager.setDownloadListener(this)

        updateButton.setOnClickListener {
            checkPermissions()
        }
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    100
                )
                return
            }
        }

        // 2. 检查安装未知来源权限 (API 26+ 强制要求)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                // 跳转到系统设置页让用户开启安装权限
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivityForResult(intent, 200)
                return
            }
        }
        appUpdateManager.downloadApk(downloadUrl)
    }

    /**
     * 对应 ActivityCompat.requestPermissions
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                appUpdateManager.downloadApk(downloadUrl)
            } else {
                Toasty.info(this, "需要存储权限才能下载更新", Toasty.LENGTH_SHORT).show()
            }
        }
    }

    override fun onProgress(progress: Int) {
        progressBar.progress = progress
        statusText.text = "下载进度: $progress%"
    }

    override fun onSuccess(file: File) {
        statusText.text = "下载完成，正在安装..."
        appUpdateManager.installApk(file)
    }

    override fun onError(error: String) {
        statusText.text = "下载失败: $error"
        Log.e(TAG, "下载失败" + error)
        Toasty.info(this, error, Toasty.LENGTH_LONG).show()
    }
    fun back(view: View) {
        onBackPressedDispatcher.onBackPressed()
    }
}