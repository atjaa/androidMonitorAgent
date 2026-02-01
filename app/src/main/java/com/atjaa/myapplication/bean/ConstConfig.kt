package com.atjaa.myapplication.bean

/**
 * 系统静态变量存储类
 */
class ConstConfig {
    companion object {
        const val PORT: Int = 33789  // HTTP服务监控端口
        const val SKIP_KEY = "V100"  // 授权跳过判断码
    }
}