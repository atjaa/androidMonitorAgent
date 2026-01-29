package com.atjaa.myapplication.utils

import android.content.Context
import android.content.SharedPreferences

object SpCacheUtils {
    private const val PREF_NAME = "my_cache"
    private var sharedPreferences: SharedPreferences? = null

    // 在 Application 或 Activity 中初始化一次即可
    fun init(context: Context) {
        if (sharedPreferences == null) {
            sharedPreferences = context.applicationContext
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * 保存数据
     */
    fun put(key: String, value: String) {
        sharedPreferences?.edit()?.putString(key, value)?.apply()
    }

    /**
     * 读取数据
     */
    fun get(key: String, defaultValue: String = ""): String {
        return sharedPreferences?.getString(key, defaultValue) ?: defaultValue
    }

    /**
     * 清除特定数据或全部
     */
    fun remove(key: String) = sharedPreferences?.edit()?.remove(key)?.apply()
    fun clear() = sharedPreferences?.edit()?.clear()?.apply()
}