## 安卓手机助手
####  监控手机App使用情况
- 主页面 
>SplashActivity.kt 3秒倒计时，之后进入登录页面
- 登录页面 
>LoginActivity.kt
1) 输入1234 进入App监控列表页面 MonitorActivity.kt  
分 天、星期、月份查看App使用情况
2) 输入7758521 进入App监控管理页面 AdminMonitorActivity.kt  
这是一个可以局域网查看其他手机（安装了本app）的功能，一样可以分 天、星期、月份查看远程手机App使用情况
- 服务
> MonitorService.kt  
1) 启动http访问服务，默认监听本机（0.0.0.0）33789端口
2) 提供天、星期、月份查看本机App使用情况的接口
3) 通过BootReceiver.kt监控开机广播启动本服务
4) 通过ServiceCheckWorker.kt进行保活
5) 通过PhotoService.kt提供前置摄像头拍照
- 保活优化  
增加小米手机支持的无障碍保活机制  
AtjaaKeepAliveService.kt
- 安卓权限  
```xml
    <uses-permission
        android:name="android.permission.PACKAGE_USAGE_STATS"
        tools:ignore="ProtectedPermissions" /><!-- 获取其他APP信息 -->
    <uses-permission
        android:name="android.permission.QUERY_ALL_PACKAGES"
        tools:ignore="QueryAllPackagesPermission" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" /> <!-- 自启动，有bug -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE"
        tools:ignore="ForegroundServicesPolicy" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_REMOTE_MESSAGING" />
    <uses-permission android:name="android.permission.INTERNET" /> <!-- 网络权限 -->
    <uses-permission android:name="android.permission.BLUETOOTH" /><!-- 获取蓝牙名称通常是获取用户自定义手机名最通用的办法 -->
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.WAKE_LOCK" /><!-- 确保 Socket 长连接在 CPU 进入休眠时不中断，申请和使用 WAKE_LOCK -->
    <uses-feature android:name="android.hardware.camera.any" /> <!-- 摄像头权限 -->
    <uses-permission android:name="android.permission.CAMERA" /> <!-- 摄像头权限 -->
```
### 后续内容  
* 保活还不够  
熄屏时无法提供http服务（相当于失联）
* 授权优化  
拍照和电源白名单实现自动授权（遮罩层不生效？）  
自启动和查看其他App信息暂时无法自动，优化了引导  
无障碍只能手动
* 静默截屏涉及安全暂时无法实现 
* 前置摄像头抓取优化
* 做一个快速连接到系统敏感设置的页面
* 局域网内自动升级
* 其他能力还未想到

本APP仅无聊学习安卓开发练手，请勿他用，如有任何责任均与本人无关
代码参考列表  
>forked from ChiHuo-Super/AppMonitor   
>其他大部分为Google AI提供  
