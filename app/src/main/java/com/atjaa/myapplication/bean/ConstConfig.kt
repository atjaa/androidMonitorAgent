package com.atjaa.myapplication.bean

/**
 * 系统静态变量存储类
 */
class ConstConfig {
    companion object {
        const val PORT: Int = 33789  // HTTP服务监控端口
        const val SKIP_KEY = "V100"  // 授权跳过判断码
        const val P_CODE = "atjaa-231asdcvb"
        const val remoteServerIp = "115.191.56.243"
//        const val remoteServerIp = "192.168.3.13"
        const val remoteServerPort ="8080"
    }
}