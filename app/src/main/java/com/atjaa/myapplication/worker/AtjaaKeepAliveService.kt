package com.atjaa.myapplication.worker

import android.accessibilityservice.AccessibilityService

import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.atjaa.myapplication.utils.CommonUtils
import com.atjaa.myapplication.utils.OverlayHelper


/**
 * 无障碍服务
 * 1、提供保活
 * 2、进行自动授权
 */
class AtjaaKeepAliveService : AccessibilityService() {

    val TAG: String = "MyAccessibilityService"
    private var lastCheckTime = 0L
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // 只在窗口状态变化（如新弹窗出现）时检查自动授权，避免滑动时频繁扫描
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkgName = event.packageName?.toString() ?: ""
            // 仅兼容小米
            if (pkgName.contains("com.lbe.security.miui")
                || pkgName.contains("com.miui.securitycenter")
                || pkgName.contains("com.android.settings")
            ) {
                Log.d(TAG, "检测到权限相关页面: $pkgName")
                handleAutoClick(event)
            }
        }

        val currentTime = System.currentTimeMillis()
        // 间隔少于 2 分钟 则直接跳过
        if (currentTime - lastCheckTime < 1000 * 60 * 2) {
            return
        }

        lastCheckTime = currentTime
        Log.d(TAG, "触发一次无障碍检查")
        CommonUtils.scheduleServiceCheck(this)
    }

    override fun onInterrupt() {
        Log.d(TAG, "AtjaaKeepAliveService无障碍服务被中断")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AtjaaKeepAliveService无障碍服务已连接")
    }

    fun handleAutoClick(event: AccessibilityEvent) {
        val rootNode: AccessibilityNodeInfo? = rootInActiveWindow
        // TODO 性能需要优化
        // 小米手机拍照授权 “仅在使用中允许” 点击一下即可
        val hasNodesPhoto =
            rootNode.hasText("拍摄照片或录制视频") && rootNode.hasText("仅在使用中允许")
        if (hasNodesPhoto == true) {
            OverlayHelper.show(this, "权限申请中...")
            try {
                var word: String = "仅在使用中允许"
                var nodes = rootNode?.findAccessibilityNodeInfosByText(word)
                if (nodes != null) {
                    for (node in nodes) {
                        // 2. 执行点击
                        val actionSuccess = performSafeClick(node)
                        if (actionSuccess) {
                            Log.d(TAG, "成功点击了：$word")
                            node.recycle()
                            return // 点击成功后退出循环
                        }
                        node.recycle()
                    }
                }
            } finally {
                OverlayHelper.hide(this)
            }
        }
        // 小米手机耗电详情 “无限制” 点击后还要点返回
        val hasNodesPower =
            rootNode.hasText("耗电详情") && rootNode.hasText("无限制")
        if (hasNodesPower == true) {
            OverlayHelper.show(this, "权限申请中...")
            try {
                var word = "无限制"
                var nodes = rootNode?.findAccessibilityNodeInfosByText(word)
                if (nodes != null) {
                    for (node in nodes) {
                        // 2. 执行点击
                        val actionSuccess = performSafeClick(node)
                        if (actionSuccess) {
                            Log.d(TAG, "成功点击了：$word")
                            Thread.sleep(500)
                            // 执行返回动作
                            val backSuccess =
                                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                            Log.d(TAG, "执行全局返回动作: $backSuccess")
                            node.recycle()
                            return // 点击成功后退出循环
                        }
                        node.recycle()
                    }
                }
            } finally {
                OverlayHelper.hide(this)
            }
        }

        // 小米消息通知授权 “始终允许” 点击一下即可
        val hasNotification =
            rootNode.hasText("发送通知") && rootNode.hasText("始终允许")
        if (hasNotification == true) {
            OverlayHelper.show(this, "权限申请中...")
            Log.i(TAG, "自动处理权限:" + "发送通知权限")
            try {
                var word: String = "始终允许"
                var nodes = rootNode?.findAccessibilityNodeInfosByText(word)
                if (nodes != null) {
                    for (node in nodes) {
                        // 2. 执行点击
                        val actionSuccess = performSafeClick(node)
                        if (actionSuccess) {
                            Log.d(TAG, "成功点击了：$word")
                            node.recycle()
                            return // 点击成功后退出循环
                        }
                        node.recycle()
                    }
                }
            } finally {
                OverlayHelper.hide(this)
            }
        }

        // 小米消息通知授权 “始终允许” 点击一下即可
        val hasNotification2 =
            rootNode.hasText("Phone Assistant") && rootNode.hasText("允许通知") && rootNode.hasText("焦点通知")
        if (hasNotification2 == true) {
            OverlayHelper.show(this, "权限申请中...")
            Log.i(TAG, "自动处理权限:" + "发送通知权限 高级通知（小米）")
            try {
                var word: String = "允许通知"
                var nodes = rootNode?.findAccessibilityNodeInfosByText(word)
                if (nodes != null) {
                    for (node in nodes) {
                        // 2. 执行点击
                        val actionSuccess = performSafeClick(node)
                        if (actionSuccess) {
                            Log.d(TAG, "成功点击了：$word")
                            Thread.sleep(500)
                            // 执行返回动作
                            val backSuccess =
                                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                            Log.d(TAG, "执行全局返回动作: $backSuccess")
                            node.recycle()
                            return // 点击成功后退出循环
                        }
                        node.recycle()
                    }
                }
            } finally {
                OverlayHelper.hide(this)
            }
        }
        // 使用情况访问权限 TODO 不生效
//        val isNodesOtherApp =
//            rootNode.hasText("使用情况访问权限") && rootNode.hasText("Phone Assistant")
//        if (isNodesOtherApp == true) {
//            var word = "Phone Assistant"
//            var nodes = rootNode?.findAccessibilityNodeInfosByText(word)
//            if (nodes != null) {
//                for (node in nodes) {
//                    // 2. 执行点击
//                    val actionSuccess = performSafeClick(node)
//                    if (actionSuccess) {
//                        Log.d(TAG, "成功点击了：$word")
//                        node.recycle()
//                        return // 点击成功后退出循环
//                    }
//                    node.recycle()
//                }
//            }
//        }
        // 使用情况访问权限
//        val isNodesOtherAppInfo = rootNode.hasText("授予使用情况访问权限")
//        if (isNodesOtherAppInfo == true) {
//            var word = "授予使用情况访问权限"
//            var nodes = rootNode?.findAccessibilityNodeInfosByText(word)
//            if (nodes != null) {
//                for (node in nodes) {
//                    // 2. 执行点击
//                    val actionSuccess = performSafeClick(node)
//                    if (actionSuccess) {
//                        Log.d(TAG, "成功点击了：$word")
//                        Thread.sleep(500)
//                        // 执行返回动作
//                        val backSuccess =
//                            performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
//                        // 第二次返回
//                        Handler(Looper.getMainLooper()).postDelayed({
//                            val success =
//                                performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
//                            Log.d(TAG, "第二次返回执行状态: $success")
//                        }, 600)
//                        node.recycle()
//                        return // 点击成功后退出循环
//                    }
//                    node.recycle()
//                }
//            }
//        }
    }

    fun AccessibilityNodeInfo?.hasText(text: String): Boolean {
        val nodes = this?.findAccessibilityNodeInfosByText(text)
        val exist = !nodes.isNullOrEmpty()
        nodes?.forEach { it.recycle() }
        return exist
    }


    private fun performSafeClick(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            // 递归查找父布局直到找到可点击的
            return performSafeClick(node.parent)
        }
    }
}