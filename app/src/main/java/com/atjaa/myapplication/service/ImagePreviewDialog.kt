package com.atjaa.myapplication.service

import android.graphics.Bitmap
import android.os.Bundle
import android.widget.ImageView

/**
 * 图片弹框
 * 展示照片用
 */
class ImagePreviewDialog(private val bitmap: Bitmap) : androidx.fragment.app.DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val iv = ImageView(requireContext()).apply {
            setImageBitmap(bitmap)
        }
        return android.app.AlertDialog.Builder(requireContext())
            .setView(iv)
            .setTitle("TITLE")
            .setPositiveButton("关闭", null)
            .create()
    }
}