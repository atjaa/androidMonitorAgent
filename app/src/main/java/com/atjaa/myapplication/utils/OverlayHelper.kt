package com.atjaa.myapplication.utils

import android.annotation.SuppressLint
import com.atjaa.myapplication.R
import android.content.Context
import android.graphics.PixelFormat
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView

/**
 * 在AccessibilityService中使用没有效果？
 */
@SuppressLint("StaticFieldLeak")
object OverlayHelper {
    private var overlayView: View? = null
    private var isShowing = false

    fun show(context: Context, message: String) {
        overlayView = LayoutInflater.from(context).inflate(R.layout.layout_overlay, null)
        // 1. 如果已经在显示了，只更新文字，不重新添加 View (防止闪烁)
        if (isShowing) {
            overlayView?.findViewById<TextView>(R.id.tv_msg)?.text = message
            return
        }

        // 2. 真正的添加逻辑
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            format = PixelFormat.TRANSLUCENT
        }

        overlayView = LayoutInflater.from(context).inflate(R.layout.layout_overlay, null)
        overlayView?.findViewById<TextView>(R.id.tv_msg)?.text = message

        try {
            wm.addView(overlayView, params)
            isShowing = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun hide(context: Context) {
        if (!isShowing) return
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try {
            wm.removeView(overlayView)
            overlayView = null
            isShowing = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}