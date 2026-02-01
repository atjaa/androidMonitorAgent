package com.atjaa.myapplication.service


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.os.PowerManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import com.atjaa.myapplication.utils.MonitorUtils


/**
 * 提供前置摄像头拍照服务
 */
class PhotoService : LifecycleService() {
    // 1. 定义一个 Binder 类，返回 Service 实例
    inner class LocalBinder : Binder() {
        fun getService(): PhotoService = this@PhotoService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        // 立即创建通知并启动前台服务 (适配 Android 8.0+)
        startForeground(1, createNotification())
    }

    fun takePhoto(): String {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = pm.isInteractive // 屏幕是否亮着
        if(!isScreenOn){
            return "手机处于锁屏状态"
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        var dataStr: String? = null
        cameraProviderFuture.addListener({
            Log.e(
                "CameraX",
                "是否有拍照全权限: ${
                    ContextCompat.checkSelfPermission(
                        this,
                        android.Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                }"
            )
            val cameraProvider = cameraProviderFuture.get()
            val imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    this as LifecycleOwner,
                    cameraSelector,
                    imageCapture
                )
                camera.cameraInfo.cameraState.observe(this) { state ->
                    if (state.type == CameraState.Type.OPEN) {
                        // 此时相机已打开，可以安全调用 takePicture
                        // 直接在内存中捕获图像
                        imageCapture.takePicture(
                            ContextCompat.getMainExecutor(this),
                            object : ImageCapture.OnImageCapturedCallback() {
                                override fun onCaptureSuccess(image: ImageProxy) {
                                    try {
                                        // 将 ImageProxy 转换为 Bitmap
                                        val bitmap = image.toBitmap()

                                        // 镜像处理：前置摄像头拍出来的通常是反的，需要翻转
                                        val rotatedBitmap =
                                            rotateAndMirrorBitmap(
                                                bitmap,
                                                image.imageInfo.rotationDegrees
                                            )

                                        // 在这里使用你的 bitmap
                                        dataStr =
                                            MonitorUtils.bitmapToBase64(rotatedBitmap, 800, 800)
                                        // processYourBitmap(rotatedBitmap)
                                    } finally {
                                        // 必须关闭 image 否则会阻塞后续拍摄
                                        image.close()
                                    }
                                    Log.i("CameraX", "拍照完成")
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("CameraX", "拍照失败: ${exception.message}")
                                }
                            }
                        )
                    }
                }


            } catch (e: Exception) {
                Log.e("CameraX", "绑定失败", e)
            }
        }, ContextCompat.getMainExecutor(this))
        var i: Int = 0
        while (i < 4) {
            val currentStr = dataStr // 读取你的变量
            if (currentStr != null) {
                return currentStr // 成功获取，退出循环
            }
            i++
            Thread.sleep(500)
        }
        return ""
    }

    /**
     * 获取正面摄像头
     */
    private fun rotateAndMirrorBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        val matrix = Matrix()
        // 旋转到正确角度
        matrix.postRotate(rotationDegrees.toFloat())
        // 水平镜像翻转 (前置摄像头特有)
        matrix.postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
    private fun createNotification(): Notification {
        val channelId = "photo_service"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, "Photo Server", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Photo服务运行中")
            .setContentText("Photo服务运行中")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()
    }
}