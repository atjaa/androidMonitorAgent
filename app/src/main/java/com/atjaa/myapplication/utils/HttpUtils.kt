package com.atjaa.myapplication.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

/**
 * 提供http访问能力
 */
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
                    return@withContext response.body?.string()
                }
            } catch (e: Exception) {
                println("Http请求异常：" + e)
                return@withContext "请求失败: ${e.message}"
            }
        }
        suspend fun uploadVoiceFile(file: File,url: String) : String? = withContext(Dispatchers.IO){
            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "voice",
                    file.name,
                    file.asRequestBody("audio/wav".toMediaTypeOrNull()) // 根据文件类型调整 MediaType
                )
                .build()
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext "错误码: ${response.code}"
                    return@withContext response.body?.string()
                }
            } catch (e: Exception) {
                println("Http请求异常：" + e)
                return@withContext "请求失败: ${e.message}"
            }
        }
    }
}