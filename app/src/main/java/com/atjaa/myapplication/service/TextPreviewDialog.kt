package com.atjaa.myapplication.service

import android.os.Bundle

/**
 * 提供弹框确认能力
 */
class TextPreviewDialog(private val content: String) : androidx.fragment.app.DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val scrollView = android.widget.ScrollView(requireContext())
        val textView = android.widget.TextView(requireContext()).apply {
            text = content
            setPadding(40, 40, 40, 40)
            setTextIsSelectable(true) // 允许长按选择文字
        }
        scrollView.addView(textView)

        return android.app.AlertDialog.Builder(requireContext())
            .setTitle("TITLE")
            .setView(scrollView)
            .setPositiveButton("确定", null)
            .create()
    }
}