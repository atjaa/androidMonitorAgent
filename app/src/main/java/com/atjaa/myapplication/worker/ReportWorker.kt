package com.atjaa.myapplication.worker

import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.atjaa.myapplication.bean.AppInforBean
import com.atjaa.myapplication.bean.ConstConfig
import com.atjaa.myapplication.utils.CommonUtils
import com.atjaa.myapplication.utils.HttpUtils
import com.atjaa.myapplication.utils.MonitorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.collections.isNotEmpty
import kotlin.collections.sortedByDescending

class ReportWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    val TAG = "ReportWorker"


    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.e(TAG, "信息上报任务开始执行")

            /**
             * 上报结构
             * {
             *  uuid    设备io
             *  name    设备名称
             *  ip      设备ip
             *  time    上报时间
             *  appInfo {最近运行APP
             *          name    App名称
             *          icon    App图片
             *          usedTime    使用时间
             *          playNumber  使用次数
             *          lastTime    最近打开时间 改为 上报时间
             *          }
             *  }
             */
            val map: MutableMap<String, Any> = HashMap<String, Any>()
            val foregroundData = getForegroundApp()
            if (null != foregroundData) {
                map["appInfo"] = foregroundData
            }
            map["uuid"] = CommonUtils.getPhoneUuid(applicationContext)
            map["name"] = CommonUtils.getCustomDeviceNameSimple(applicationContext)
            var localIp = CommonUtils.getLocalIp()
            if (null != localIp) {
                map["ip"] = localIp
            } else {
                map["ip"] = "无法识别"
            }
            var nowTime = Calendar.getInstance().getTimeInMillis()
            map["time"] = getTimeStrings(nowTime)
            var url =
                "http://" + ConstConfig.remoteServerIp + ":" + ConstConfig.remoteServerPort + "/api/phone/up/" + ConstConfig.P_CODE
            Log.e(TAG, "信息上报数据封装完成开始上传" + url)
            var result = HttpUtils.sendPostRequest(url, map)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "信息上报数据上传异常" + e.message)
            Result.retry()
        }

    }

    /**
     * 获取当前运行APP
     *  Map.icon
     *  Map.name
     *  Map.usedTime
     *  Map.lastTime
     *  Map.playNumber
     */
    fun getForegroundApp(): Map<String, Any>? {
        val context = applicationContext
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 1000 * 60 // 查询最近 1 分钟
        // 获取统计列表
        val stats =
            usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        if (stats != null && stats.isNotEmpty()) {
            // 按最后使用时间排序
            val sortedStats = stats.sortedByDescending { it.lastTimeUsed }
            var appInforBean = AppInforBean(sortedStats[0], context)
            if (appInforBean.isSuccess) {
                var beanMap = MonitorUtils.getData(appInforBean, 1)
                // 覆盖未当前上报时间点
                var nowTime = Calendar.getInstance().getTimeInMillis()
                beanMap.put("lastTime", "上报时间点: " +getTimeStrings(nowTime))
                return beanMap
            }
        }
        return null
    }

    private fun getTimeStrings(t: Long): String {
        val simpleDateFormat: SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
        return simpleDateFormat.format(t)
    }

}