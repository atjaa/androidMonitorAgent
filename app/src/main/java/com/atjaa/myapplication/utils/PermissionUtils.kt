package com.atjaa.myapplication.utils

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.accessibility.AccessibilityManager
import androidx.core.content.ContextCompat
import com.atjaa.myapplication.receiver.BootReceiver
import com.atjaa.myapplication.worker.AtjaaKeepAliveService
import com.google.gson.Gson
import java.util.Locale
import kotlin.collections.set

/**
 * 授权判断工具类
 */
object PermissionUtils {
    /**
     * 检查特定的无障碍服务是否已开启
     */
    fun isAccessibilitySettingsOn(
        context: Context,
        service: Class<out AccessibilityService>
    ): Boolean {
        val serviceName = "${context.packageName}/${service.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""

        // 1. 字符串匹配检查
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            if (colonSplitter.next().equals(serviceName, ignoreCase = true)) {
                return true
            }
        }

        // 2. 活跃服务列表二次检查
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledAppList =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (info in enabledAppList) {
            if (info.resolveInfo.serviceInfo.packageName == context.packageName &&
                info.resolveInfo.serviceInfo.name == service.name
            ) {
                return true
            }
        }

        return false
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        // unsafeCheckOpNoThrow 是 API 29 后的推荐做法
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // API 29 及以上使用 unsafeCheckOpNoThrow
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            // API 29 以下使用老的 checkOpNoThrow
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }

        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        // 检查当前应用包名是否在白名单中
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun isUsageCompat(context: Context): Boolean {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    fun isNotificationPermission(context: Context): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            return true
        }
        return false
    }

    fun isXiaomiFloatingEnabled(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        try {
            val method = ops.javaClass.getMethod(
                "checkOp",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            // 10021 是小米私有的悬浮通知 Op 码
            val result =
                method.invoke(ops, 10021, android.os.Process.myUid(), context.packageName) as Int
            return result == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            return false // 非小米或获取失败
        }
    }

    fun isMiui(): Boolean {
        return try {
            val clz = Class.forName("android.os.SystemProperties")
            val getMethod = clz.getMethod("get", String::class.java)
            val result = getMethod.invoke(null, "ro.miui.ui.version.name") as String
            result.isNotBlank()
        } catch (e: Exception) {
            // 如果反射失败，检查厂商名称
            android.os.Build.MANUFACTURER.lowercase(Locale.ROOT).contains("xiaomi")
        }
    }

    /**
     * 小米手机判断是否开启应用自启动
     */
    fun isAutoStartEnabledXiaoMi(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        try {
            val method = ops.javaClass.getMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java
            )
            // 10008 是 MIUI 隐藏的自启动权限代码
            val result =
                method.invoke(ops, 10008, Binder.getCallingUid(), context.packageName) as Int
            return result == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    fun isBootReceiverEnabled(context: Context): Boolean {
        val component = ComponentName(context, BootReceiver::class.java)
        val status = context.packageManager.getComponentEnabledSetting(component)
        return status != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    fun getPermissionInfoJson(context: Context):String{
        return Gson().toJson(getPermissionInfo(context))
    }
    fun getPermissionInfo(context: Context): List<HashMap<String, String>> {
        var res = ArrayList<HashMap<String, String>>()
        val item1 = hashMapOf("tvCheck" to "检查无障碍授权", "tvCheckResult" to "未开启")
        val isAcc = isAccessibilitySettingsOn(context, AtjaaKeepAliveService::class.java)
        if (isAcc) {
            item1["tvCheckResult"] = "通过"
        }
        res.add(item1)

        if (isMiui()) {
            val item2 =
                hashMapOf("tvCheck" to "检查开机自启动(小米)授权", "tvCheckResult" to "未开启")
            val isAutoStart = isAutoStartEnabledXiaoMi(context)
            if(isAutoStart){
                item2["tvCheckResult"] = "通过"
            }
            res.add(item2)
        }

        val item21 =
            hashMapOf("tvCheck" to "检查开机自启动(RootReceiver)授权", "tvCheckResult" to "未开启")
        val isAutoStartComm = isBootReceiverEnabled(context)
        if(isAutoStartComm){
            item21["tvCheckResult"] = "通过"
        }
        res.add(item21)

        val item3 = hashMapOf("tvCheck" to "检查有权查看使用情况授权", "tvCheckResult" to "未开启")
        val isUsage = hasUsageStatsPermission(context)
        if(isUsage){
            item3["tvCheckResult"] = "通过"
        }
        res.add(item3)

        val item4 = hashMapOf("tvCheck" to "检查电源白名单授权", "tvCheckResult" to "未开启")
        val isIgnoringBattery = isIgnoringBatteryOptimizations(context)
        if(isIgnoringBattery){
            item4["tvCheckResult"] = "通过"
        }
        res.add(item4)


        val item5 = hashMapOf("tvCheck" to "检查摄像头拍照授权", "tvCheckResult" to "未开启")
        val isUsageCompat = isUsageCompat(context)
        if(isUsageCompat){
            item5["tvCheckResult"] = "通过"
        }
        res.add(item5)

        val item6 = hashMapOf("tvCheck" to "检查消息渠道授权", "tvCheckResult" to "未开启")
        val isNotificationPermission = isNotificationPermission(context)
        if(isNotificationPermission){
            item6["tvCheckResult"] = "通过"
        }
        res.add(item6)

        val item7 = hashMapOf("tvCheck" to "检查小米私有悬浮通知授权", "tvCheckResult" to "未开启")
        val isXiaomiFloatingEnabled = isXiaomiFloatingEnabled(context)
        if(isXiaomiFloatingEnabled){
            item7["tvCheckResult"] = "通过"
        }
        res.add(item7)
        return res
    }
}