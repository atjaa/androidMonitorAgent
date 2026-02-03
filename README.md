## 安卓手机助手

### 监控手机App使用情况

#### 核心页面

> SplashActivity.kt 3秒倒计时，之后进入登录页面  
> LoginActivity.kt 登录页面，可以跳转不同后台    
> MonitorActivity.kt 本地监控页  
> AdminMonitorActivity.kt 管理监控页  

#### 服务

> MonitorService.kt 核心服务，提供监控大部分能力
1) 启动http访问服务，默认监听本机（0.0.0.0）33789端口
2) 提供天、星期、月份查看本机App使用情况的接口
3) 通过BootReceiver.kt监控开机广播启动本服务
4) 通过ServiceCheckWorker.kt进行保活
5) 通过PhotoService.kt提供前置摄像头拍照  

> PhotoService  拍照服务
  调用前置摄像头进行拍照，并发送给远程调用方  
> AppUpdateManager  应用自动升级服务

#### 广播接受器  

> BootReceiver 监听加电和自己应用更新广播  
> InstallReceiver 监听手机应用安装广播  

#### Worker组件  

> ReportWorker 信息上报  
> ServiceCheckWorker 保活  

#### 保活优化  
1、 无障碍保活  AtjaaKeepAliveService  
2、 worker定时保活  ServiceCheckWorker  
以上两个都受自启动限制  
3、 账号同步保活  
> res/xml/authenticator.xml 声明账号类型  
> res/xml/sync_adapter.xml (声明同步逻辑)  
> class AuthenticatorService : Service  认证服务：让系统识别你的账号
> class SyncService : Service   同步服务：这是系统“复活”你的入口
> class StubProvider : ContentProvider  占位 Provider
> AndroidManifest.xml 中注册  

#### 局域网自动升级  
  AppUpdateActivity 实现局域网内指定ip和端口的自动升级，省去每次更新都USB安装的问题
#### 广域网信息上报  
提供跨网监控手段
> 被监控方每20分钟上报当前正在打开的应用信息  
> 被监控方安装新APP时自动上报安装的软件信息  
> 监控方分两个页面展示以上信息

#### 授权状态检查  
  PermissionCheckActivity 对本应用所需的权限进行整理报告

#### 安卓权限
```xml

<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS"
    tools:ignore="ProtectedPermissions" /><!-- 获取其他APP信息 -->
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES"
tools:ignore="QueryAllPackagesPermission" /><uses-permission
android:name="android.permission.RECEIVE_BOOT_COMPLETED" /> <!-- 自启动，有bug -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"
tools:ignore="ForegroundServicesPolicy" /><uses-permission
android:name="android.permission.FOREGROUND_SERVICE_REMOTE_MESSAGING" /><uses-permission
android:name="android.permission.INTERNET" /> <!-- 网络权限 -->
<uses-permission android:name="android.permission.BLUETOOTH" /><!-- 获取蓝牙名称通常是获取用户自定义手机名最通用的办法 -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" /><uses-permission
android:name="android.permission.WAKE_LOCK" /><!-- 确保 Socket 长连接在 CPU 进入休眠时不中断，申请和使用 WAKE_LOCK -->
<uses-feature android:name="android.hardware.camera.any" /> <!-- 摄像头权限 -->
<uses-permission android:name="android.permission.CAMERA" /> <!-- 摄像头权限 -->
<!-- 剩下略   -->
```

#### 后续内容

* 保活还不够
  无障碍保活需要亮屏，熄屏ServiceCheckWorker.ket ？
* 授权优化  
  拍照和电源白名单实现自动授权（遮罩层不生效？）  
  自启动和查看其他App信息暂时无法自动，优化了引导  
  无障碍只能手动
* 静默截屏涉及安全暂时无法实现
* 个别手机重启无法提供监控服务要等15分钟后，需要排查

本APP仅无聊学习安卓开发练手，请勿他用，如有任何责任均与本人无关
代码参考列表
> forked from ChiHuo-Super/AppMonitor   
> 其他大部分为Google AI提供

### 注意项
* MonitorService本来是在Intent.ACTION_BOOT_COMPLETED（电源开启即手机重启时拉起的），开发了自动更新后，这个服务需要在更新后重新拉起，因为里面有http监听服务，否则重新安装会导致服务挂起，当然等15分钟的无障碍保活应该也能拉起
