package com.atjaa.myapplication.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.format.DateUtils
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.atjaa.myapplication.bean.AppInforBean
import com.atjaa.myapplication.worker.ServiceCheckWorker
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class MonitorUtils {
    companion object {

        fun getDataList(
            showDataList: ArrayList<AppInforBean>,
            outType: Int
        ): List<Map<String, Any>> {
            var dataList: MutableList<Map<String, Any>> = ArrayList<Map<String, Any>>()
            for (bean in showDataList) {
                val map: MutableMap<String, Any> = HashMap<String, Any>()
                if (outType == 0) {
                    map.put("icon", bean.icon)
                } else {
                    if (bean.icon != null) {
                        // TODO 需要在压缩
                        // 将 Drawable 转换为 Bitmap
                        val bitmap = bean.icon!!.toBitmap()
                        map.put("icon", bitmapToBase64(bitmap))
                    }

                }
                map.put("name", bean.appName)
                if (bean.usedTimes != 0L) {
                    map.put(
                        "usedTime",
                        "运行时长: " + DateUtils.formatElapsedTime(bean.usedTimes / 1000)
                    )
                } else {
                    map.put(
                        "usedTime",
                        "运行时长: 0"
                    )
                }
                map.put("lastTime", "最近使用时间点: " + getTimeStrings(bean.beginPlayTime))
                map.put("playNumber", "本次开机操作次数: " + bean.usedNumbers)
                dataList.add(map)
            }
            return dataList
        }
        fun bitmapToBase64(bitmap: Bitmap): String {
            val scaledBitmap = if (bitmap.width > 128 || bitmap.height > 128) {
                Bitmap.createScaledBitmap(bitmap, 128, 128, true)
            } else {
                bitmap
            }
            val outputStream = ByteArrayOutputStream()
            // 质量压缩：70 表示压缩 30%，保留 70% 质量
            // 在 2026 年，建议使用 Bitmap.CompressFormat.WEBP_LOSSY 以获得最佳体积比
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                // API 30 及以上使用新常量
                scaledBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 50, outputStream)
            } else {
                // API 30 以下使用旧常量（已弃用但仍可用，且支持 API 24）
                @Suppress("DEPRECATION")
                scaledBitmap.compress(Bitmap.CompressFormat.WEBP, 50, outputStream)
            }
            val byteArray = outputStream.toByteArray()            // 转为 Base64 字符串方便嵌入 JSON 传输
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        }

        private fun getTimeStrings(t: Long): String {
            val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            return simpleDateFormat.format(t)
        }

        private fun MutableMap<String, Any>.put(key: String, value: Drawable?) {
            if (value != null) {
                // 调用真正的 Map 存储方法
                this[key] = value
            }
        }
    }



}