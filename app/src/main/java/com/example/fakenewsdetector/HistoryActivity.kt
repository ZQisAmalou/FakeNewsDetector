package com.example.fakenewsdetector

import android.os.Bundle
import android.widget.ListView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

class HistoryActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        dbHelper = DatabaseHelper(this)
        val recyclerView: RecyclerView = findViewById(R.id.rv_history)
        recyclerView.layoutManager = LinearLayoutManager(this)

        try {
            val username = intent.getStringExtra("username") ?: ""
            val userId = dbHelper.getUserId(username)
            val historyItems = dbHelper.getAllHistory(userId)

            val adapter = HistoryAdapter(historyItems)
            recyclerView.adapter = adapter

            if (historyItems.isEmpty()) {
                Toast.makeText(this, "暂无历史记录", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "获取历史记录失败: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        return format.format(date)
    }
}
