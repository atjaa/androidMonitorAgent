package com.atjaa.myapplication.utils

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.SystemClock
import com.atjaa.myapplication.bean.AppInforBean
import java.util.Calendar
import kotlin.properties.Delegates


class SystemInforUtils {
    companion object {
        const val DAY: Int = 0
        const val WEEK: Int = 1
        const val MONTH: Int = 2
        const val YEAR: Int = 3
    }

    var context: Context? = null
    var type by Delegates.notNull<Int>() // 统计周期
    lateinit var appInforList: ArrayList<AppInforBean>
    lateinit var showDataList: ArrayList<AppInforBean>//最终展示的
    var allPlayTime: Long = 0 //运行时间
    var allUsedNumber: Int = 0//次开机操作次数

    constructor(context: Context, type: Int) {
        try {
            this.context = context
            this.type = type
            statUsageList()
            setShowList()
        } catch (e: Exception) {
            println("$e")
        }
    }


    /**
     * 统计指定周期内应用使用时间
     */
    fun statUsageList() {
        var manager = context?.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager?
        appInforList = ArrayList<AppInforBean>()
        if (manager != null) {
            var nowTime = Calendar.getInstance().getTimeInMillis()
            var begintime = getBeginTime()
            var result: List<UsageStats>
            if (type == DAY) {
                result =
                    manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, begintime, nowTime);
                appInforList =
                    getAccurateDailyStatsList(context, result, manager, begintime, nowTime);
            } else {
                if (type === WEEK) result =
                    manager.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY, begintime, nowTime)
                else if (type === MONTH) result =
                    manager.queryUsageStats(UsageStatsManager.INTERVAL_MONTHLY, begintime, nowTime)
                else if (type === YEAR) result =
                    manager.queryUsageStats(UsageStatsManager.INTERVAL_YEARLY, begintime, nowTime)
                else {
                    result =
                        manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, begintime, nowTime)
                }

                val mergeresult: ArrayList<UsageStats> = MergeList(result)
                for (usageStats in mergeresult) {
                    var appInforBean = AppInforBean(usageStats, context)
                    if (appInforBean.isSuccess) {
                        appInforList.add(appInforBean)
                    }
                }
                calculateLaunchTimesAfterBootOn(context, appInforList)
            }
        }
    }

    /**
     * 根据UsageEvents 精确计算APP开机的启动(activity打开的)次数
     */
    fun calculateLaunchTimesAfterBootOn(context: Context?, appInforList: ArrayList<AppInforBean>) {
        val manager = context?.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager?
        if (manager == null || appInforList == null || appInforList.size < 1) {
            return
        }

        //针对每个packageName建立一个  使用信息
        val mapData = HashMap<String?, AppInforBean?>()

        val events = manager.queryEvents(bootTime(), System.currentTimeMillis())
        for (appInforBean in appInforList) {
            mapData.put(appInforBean.packageName, appInforBean)
        }

        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            val packageName = e.getPackageName()
            val appInforBean = mapData.get(packageName)
            if (appInforBean == null) {
                continue
            }

            if (e.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                appInforBean.timesPlusPlus()
            }
        }
    }

    /**
     * 将次数和时间为0的应用信息过滤掉
     */
    fun setShowList() {
        showDataList = ArrayList<AppInforBean>()
        allPlayTime = 0

        for (i in 0..appInforList.size - 1) {
            if (appInforList.get(i).usedTimes > 0) { //&& !isSystemApp(context, AppInfoList.get(i).getPackageName())) {
                showDataList.add(appInforList.get(i))
                allPlayTime += appInforList.get(i).usedTimes
                allUsedNumber += appInforList.get(i).usedNumbers
            }
        }


        //将显示列表中的应用按显示顺序排序
        for (i in 0..<showDataList.size - 1) {
            for (j in 0..<showDataList.size - i - 1) {
                if (showDataList.get(j).usedTimes < showDataList.get(j + 1).usedTimes) {
                    val bean: AppInforBean = showDataList.get(j)
                    showDataList.set(j, showDataList.get(j + 1))
                    showDataList.set(j + 1, bean)
                }
            }
        }
    }

    /**
     * 根据UsageEvents来对当天的操作次数和开机后运行时间来进行精确计算
     */
    fun getAccurateDailyStatsList(
        context: Context?,
        result: List<UsageStats>,
        manager: UsageStatsManager,
        beginTime: Long,
        nowTime: Long
    ): ArrayList<AppInforBean> {
        //针对每个packageName建立一个  使用信息
        var mapData = HashMap<String, AppInforBean>()
        for (stats in result) {
            if (stats.getLastTimeUsed() > beginTime && stats.getTotalTimeInForeground() > 0) {
                if (mapData.get(stats.getPackageName()) == null) {

                    val information = AppInforBean(stats, context)
                    if (information.isSuccess) {
                        //重置总运行时间  开机操作次数
//                    information.usedNumbers = 0
//                    information.usedTimes = 0
                        mapData.put(stats.getPackageName(), information)
                    }


                }
            }
        }
        //这个是相对比较精确的
        var bootTime = bootTime()
        var events = manager.queryEvents(bootTime, nowTime)
        var e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            var packageName = e.getPackageName();
            var appInforBean = mapData.get(packageName);
            if (appInforBean == null) {
                continue
            }
            //这里在同时计算开机后的操作次数和运行时间，所以如果获取到的时间戳是昨天的话就得过滤掉 continue
            if (e.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                appInforBean.timesPlusPlus()
                if (e.getTimeStamp() < beginTime) {
                    continue
                }
                appInforBean.timeStampMoveToForeground = e.getTimeStamp()
            } else if (e.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND) {
                if (e.getTimeStamp() < beginTime) {
                    continue
                }
                appInforBean.timeStampMoveToBackGround = e.getTimeStamp()
                //当前应用是在昨天进入的前台，0点后转入了后台，所以会先得到MOVE_TO_BACKGROUND 的timeStamp
                if (appInforBean.timeStampMoveToForeground < 0) {
                    //从今天开始计算即可
                    appInforBean.timeStampMoveToForeground = beginTime
                }
            }
            appInforBean.calculateRunningTime();
        }


        //再计算一次当前应用的运行时间，因为当前应用，最后得不到MOVE_TO_BACKGROUND 的timeStamp
        val appInforBean: AppInforBean = mapData.get(context?.getPackageName())!!
        appInforBean.timeStampMoveToBackGround = nowTime
        appInforBean.calculateRunningTime()

        return ArrayList(mapData.values)
    }

    /**
     * 根据时间做过滤
     */
    fun MergeList(result: List<UsageStats>): ArrayList<UsageStats> {
        val mergeresult: ArrayList<UsageStats> = ArrayList<UsageStats>()
        val begintime = getBeginTime()
        for (i in result) {
            if (i.getLastTimeUsed() > begintime) {
                val number: Int = FoundUsageStats(mergeresult, i)
                if (number >= 0) {
                    val stats = mergeresult.get(number)
                    stats.add(i)
                    mergeresult.set(number, stats)
                } else mergeresult.add(i)
            }
        }

        return mergeresult
    }

    fun FoundUsageStats(mergeresult: ArrayList<UsageStats>, usageStats: UsageStats): Int {
        for (i in 0..<mergeresult.size) {
            if (mergeresult.get(i).getPackageName().equals(usageStats.getPackageName())) {
                return i
            }
        }
        return -1
    }

    fun bootTime(): Long {
        return System.currentTimeMillis() - SystemClock.elapsedRealtime()
    }

    fun getBeginTime(): Long {
        val calendar = Calendar.getInstance()
        val begintime: Long
        if (type === WEEK) {
            calendar.add(Calendar.DATE, -7)
            begintime = calendar.getTimeInMillis()
        } else if (type === MONTH) {
            calendar.add(Calendar.DATE, -30)
            begintime = calendar.getTimeInMillis()
        } else if (type === YEAR) {
            calendar.add(Calendar.YEAR, -1)
            begintime = calendar.getTimeInMillis()
        } else {
            //剩下的输入均显示当天的数据
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val second = calendar.get(Calendar.SECOND)

            calendar.add(Calendar.SECOND, -1 * second)
            calendar.add(Calendar.MINUTE, -1 * minute)
            calendar.add(Calendar.HOUR, -1 * hour)
            begintime = calendar.getTimeInMillis()
        }
        return begintime
    }
}