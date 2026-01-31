package com.atjaa.myapplication.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * APP升级处理类
 */
class AppUpdateManager(private val context: Context) {
    private var downloadListener: DownloadListener? = null
    val TAG: String = "AppUpdateManager"

    interface DownloadListener {
        fun onProgress(progress: Int)
        fun onSuccess(file: File)
        fun onError(error: String)
    }

    fun setDownloadListener(listener: DownloadListener) {
        this.downloadListener = listener
    }

    fun downloadApk(url: String) {
        Thread {
            try {
                val apkUrl = URL(url)
                val connection = apkUrl.openConnection() as HttpURLConnection
                connection.connect()

                val fileLength = connection.contentLength
                val inputStream = connection.inputStream
                val apkFile = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                    "app_update.apk"
                )
                val outputStream = apkFile.outputStream()

                var total = 0
                val buffer = ByteArray(1024)
                var count: Int

                while (inputStream.read(buffer).also { count = it } != -1) {
                    total += count
                    outputStream.write(buffer, 0, count)
                    val progress = (total * 100 / fileLength)
                    (context as Activity).runOnUiThread {
                        downloadListener?.onProgress(progress)
                    }
                }

                outputStream.close()
                inputStream.close()

                (context as Activity).runOnUiThread {
                    downloadListener?.onSuccess(apkFile)
                }
            } catch (e: Exception) {
                (context as Activity).runOnUiThread {
                    downloadListener?.onError(e.message ?: "下载失败")
                }
            }
        }.start()
    }

    fun installApk(apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                intent.setDataAndType(uri, "application/vnd.android.package-archive")
            } else {
                intent.setDataAndType(
                    Uri.fromFile(apkFile),
                    "application/vnd.android.package-archive"
                )
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "自动升级安装apk异常" + e)
        }
    }

    fun cleanupApkFile(apkFile: File) {
        if (apkFile.exists()) {
            apkFile.delete()
        }
    }

    /**
     * 清理下载目录中的所有APK文件
     */
    fun cleanupAllApkFiles() {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (downloadDir != null && downloadDir.exists()) {
            val apkFiles = downloadDir.listFiles { file ->
                file.name.endsWith(".apk")
            }
            apkFiles?.forEach { file ->
                file.delete()
            }
        }
    }

    /**
     * 清理指定名称的APK文件
     */
    fun cleanupSpecificApk(fileName: String) {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (downloadDir != null) {
            val apkFile = File(downloadDir, fileName)
            cleanupApkFile(apkFile)
        }
    }

    /**
     * 在安装完成后自动清理APK文件
     */
    fun cleanupAfterInstall(apkFile: File) {
        // 安装完成后立即清理
        cleanupApkFile(apkFile)
    }
}