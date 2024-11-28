package com.example.fakenewsdetector

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(private val historyItems: List<HistoryItem>) :
    RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val typeText: TextView = view.findViewById(R.id.tv_type)
        val contentText: TextView = view.findViewById(R.id.tv_content)
        val resultText: TextView = view.findViewById(R.id.tv_result)
        val timestampText: TextView = view.findViewById(R.id.tv_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyItems[position]

        holder.typeText.text = item.type
        holder.contentText.text = when (item.type) {
            "文本检测" -> item.content.take(100).let { if (item.content.length > 100) "$it..." else it }
            else -> "文件名：${item.content}"
        }

        holder.resultText.text = item.result
        holder.resultText.setTextColor(when {
            item.result.contains("AI生成") -> Color.parseColor("#F44336")  // 红色
            item.result.contains("人类撰写") -> Color.parseColor("#4CAF50")  // 绿色
            else -> Color.parseColor("#FF9800")  // 橙色
        })

        holder.timestampText.text = formatTimestamp(item.timestamp)
    }

    override fun getItemCount() = historyItems.size

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(timestamp)
    }
}