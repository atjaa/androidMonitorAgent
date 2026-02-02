package com.atjaa.myapplication.app

import android.app.Application
import com.atjaa.myapplication.utils.CommonUtils

class AtjaaApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        CommonUtils.scheduleServiceCheck(this)
        CommonUtils.scheduleReportWork(this)
    }
}