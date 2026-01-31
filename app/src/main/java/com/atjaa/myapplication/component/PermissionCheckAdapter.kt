package com.atjaa.myapplication.component


import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.atjaa.myapplication.R


class PermissionCheckAdapter(private val mData: MutableList<HashMap<String, String>>) :
    RecyclerView.Adapter<PermissionCheckAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        // 确保这里是 R.layout 而不是 R.drawable
        val v: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_check_permission, parent, false)
        return MyViewHolder(v)
    }
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = mData[position]
        val resultText = item["tvCheckResult"] ?: ""

        holder.tvCheck.text = item["tvCheck"]
        holder.tvCheckResult.text = resultText

        // 根据内容动态设置颜色
        val color = when (resultText) {
            "通过" -> Color.parseColor("#4CAF50") // 绿色
            "未开启" -> Color.parseColor("#F44336") // 红色
            "......" -> Color.parseColor("#9E9E9E")   // 灰色（等待中）
            else -> Color.parseColor("#333333")    // 默认深灰色
        }

        holder.tvCheckResult.setTextColor(color)
    }

    override fun getItemCount(): Int = mData.size

    // 2. 这里的 ViewHolder 建议不要加 inner，保持独立
    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // 使用 val 声明，并在 findViewById 时显式指定非空 TextView
        val tvCheck: TextView = itemView.findViewById(R.id.tv_check)
        val tvCheckResult: TextView = itemView.findViewById(R.id.tv_check_result)
    }
}