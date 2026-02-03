package com.atjaa.myapplication.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import com.atjaa.myapplication.bean.ConstConfig
import com.atjaa.myapplication.utils.CommonUtils
import com.atjaa.myapplication.utils.HttpUtils
import com.atjaa.myapplication.utils.MonitorUtils
import com.atjaa.myapplication.utils.SpCacheUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.LinkedList

class InstallReceiver : BroadcastReceiver() {
    val APP_ADD_MAX_SIZE = 100;
    val TAG = "InstallReceiver"
    override fun onReceive(context: Context, intent: Intent) {
        if (intent?.action == Intent.ACTION_PACKAGE_ADDED) {
            // 获取包名，例如 "com.example.game"
            val packageName = intent.data?.schemeSpecificPart ?: return
            Log.i(TAG, "监听到安装软件" + packageName)
            val pm = context.packageManager
            try {
                // 1. 获取应用信息
                val appInfo = pm.getApplicationInfo(packageName, 0)

                // 2. 获取 App 名称（例如 "某某游戏"）
                val appName = pm.getApplicationLabel(appInfo).toString()

                // 3. 获取 App 图标 (Drawable)
                val appIcon = pm.getApplicationIcon(appInfo)

                // 4. 执行业务逻辑：例如弹出警告弹窗或同步到家长端
                writeToCache(context, appName, appIcon!!.toBitmap(), packageName)
                Log.i(TAG, "软件信息写入磁盘")
                upToCloud(context, appName, appIcon!!.toBitmap(), packageName)
                Log.i(TAG, "软件信息上传云端")
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(TAG, "监听软件安装广播处理异常" + e.message)
            }
        }
    }

    fun upToCloud(context: Context, appName: String, appIcon: Bitmap, packageName: String) {
        val map: MutableMap<String, Any> = HashMap<String, Any>()
        map["uuid"] = CommonUtils.getPhoneUuid(context)
        map["appName"] = "应用名：" + appName
        map["packageName"] = "应用包：" +packageName
        var nowTime = Calendar.getInstance().getTimeInMillis()
        map["time"] = "安装时间：" +getTimeStrings(nowTime)
        map["appIcon"] = MonitorUtils.bitmapToBase64(appIcon)
        var url =
            "http://" + ConstConfig.remoteServerIp + ":" + ConstConfig.remoteServerPort + "/api/app/add/up/" + ConstConfig.P_CODE
        Log.e(TAG, "信息上报数据封装完成开始上传" + url)
        val pendingResult = goAsync()
        GlobalScope.launch(Dispatchers.IO) {
            try {
                var result = HttpUtils.sendPostRequest(url, map)
            } finally {
                pendingResult.finish() // 【必须调用】否则会触发 ANR
            }
        }

    }

    fun writeToCache(context: Context, appName: String, appIcon: Bitmap, packageName: String) {
        val map: MutableMap<String, Any> = HashMap<String, Any>()
        map.put("appName", appName)
        map.put("packageName", packageName)
        var nowTime = Calendar.getInstance().getTimeInMillis()
        map["time"] = "安装时间：" +getTimeStrings(nowTime)
        map.put("icon", MonitorUtils.bitmapToBase64(appIcon))
        SpCacheUtils.init(context)
        var appAddInfo = SpCacheUtils.get("appAddInfo")
        if ("".equals(appAddInfo)) {
            val dataList: MutableList<Map<String, Any>> = LinkedList<Map<String, Any>>()
            dataList.add(map)
            SpCacheUtils.put("appAddInfo", Gson().toJson(dataList))
        } else {
            val type = object : TypeToken<LinkedList<MutableMap<String, Any>>>() {}.type
            val data: LinkedList<MutableMap<String, Any>> = Gson().fromJson(appAddInfo, type)
            if (data.size >= APP_ADD_MAX_SIZE) {
                data.removeAt(0); // 移除索引为 0 的（最旧）
            }
            data.add(map)
            SpCacheUtils.put("appAddInfo", Gson().toJson(data))
        }
    }

    private fun getTimeStrings(t: Long): String {
        val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        return simpleDateFormat.format(t)
    }
}