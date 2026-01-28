package com.atjaa.myapplication.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class HttpUtils {
    companion object {
        suspend fun fetchUrlContent(url: String): String? = withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(url)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext "错误码: ${response.code}"
                    return@withContext response.body?.string() // 返回网页或 JSON 文本内容
                }
            } catch (e: Exception) {
                println("Http请求异常：" + e)
                return@withContext "请求失败: ${e.message}"
            }
        }
    }
}