package com.example.fakenewsdetector

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FakeNewsDetector.initialize(this)

        dbHelper = DatabaseHelper(this)

        val uploadImageButton: Button = findViewById(R.id.btn_upload_image)
        val uploadVideoButton: Button = findViewById(R.id.btn_upload_video)
        val uploadAudioButton: Button = findViewById(R.id.btn_upload_audio)
        val resultTextView: TextView = findViewById(R.id.tv_result_placeholder)
        val username = intent.getStringExtra("username")
        val btnHistory: Button = findViewById(R.id.btn_history)

        btnHistory.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            intent.putExtra("username", username) // 传递用户名到历史记录界面
            startActivity(intent)
        }

        // Handle image upload
        val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                resultTextView.text = "Analyzing Image..."
                val result = FakeNewsDetector.detectFakeImage(contentResolver, it)
                resultTextView.text = result.message
                username?.let { name ->
                    val userId = dbHelper.getUserId(name)
                    if (userId != -1) {
                        dbHelper.insertHistory(userId, "图片检测", it.toString(), result.message)
                    }
                }
            }
        }

        uploadImageButton.setOnClickListener {
            imagePicker.launch("image/*")
        }

        // Handle video upload
        val videoPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val videoUri = result.data?.data
                videoUri?.let {
                    resultTextView.text = "Analyzing Video..."
                    val result = FakeNewsDetector.detectFakeVideo(contentResolver, it)
                    resultTextView.text = result.message
                    username?.let { name ->
                        val userId = dbHelper.getUserId(name)
                        if (userId != -1) {
                            dbHelper.insertHistory(userId, "视频检测", it.toString(), result.message)
                        }
                    }
                }
            }
        }

        uploadVideoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            videoPicker.launch(intent)
        }
        
        val audioPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                resultTextView.text = "正在分析音频..."
                val result = FakeNewsDetector.detectFakeAudio(contentResolver, it)
                resultTextView.text = result.message
                username?.let { name ->
                    val userId = dbHelper.getUserId(name)
                    if (userId != -1) {
                        dbHelper.insertHistory(userId, "音频检测", it.toString(), result.message)
                    }
                }
            }
        }

        uploadAudioButton.setOnClickListener {
            audioPicker.launch("audio/*")
        }

        val etInputText: EditText = findViewById(R.id.et_input_text)
        val btnDetectText: Button = findViewById(R.id.btn_detect_text)

        btnDetectText.setOnClickListener {
            val inputText = etInputText.text.toString().trim()
            if (inputText.isEmpty()) {
                resultTextView.text = "请输入文本后再检测"
                return@setOnClickListener
            }

            resultTextView.visibility = View.GONE
            val resultLayout = findViewById<View>(R.id.detection_result)
            resultLayout.visibility = View.VISIBLE

            val conclusion = resultLayout.findViewById<TextView>(R.id.tv_conclusion)
            val confidence = resultLayout.findViewById<TextView>(R.id.tv_confidence)
            val analysis = resultLayout.findViewById<TextView>(R.id.tv_analysis)
            val btnViewRaw = resultLayout.findViewById<Button>(R.id.btn_view_raw)

            resultTextView.text = "正在分析文本..."
            coroutineScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        FakeNewsDetector.detectFakeText(inputText)
                    }

                    val resultLines = result.message.split("\n")
                    val isAI = resultLines.find { it.contains("结论：") }?.contains("AI生成") ?: false

                    withContext(Dispatchers.Main) {
                        conclusion.text = resultLines.find { it.contains("结论：") }?.substringAfter("结论：")?.trim() ?: "未知"
                        conclusion.setTextColor(if (isAI) Color.parseColor("#F44336") else Color.parseColor("#4CAF50"))
                        conclusion.setBackgroundColor(if (isAI) Color.parseColor("#FFEBEE") else Color.parseColor("#E8F5E9"))

                        confidence.text = resultLines.find { it.contains("置信度：") }?.substringAfter("置信度：")?.trim() ?: "未知"
                        
                        // 提取并处理分析依据
                        val analysisText = result.message
                            .substringAfter("分析依据：", "")
                            .substringBefore("需要注意的是")
                            .trim()
                        
                        analysis.text = if (analysisText.isNotEmpty()) analysisText else "未知"

                        resultLayout.visibility = View.VISIBLE
                        
                        // 添加查看原始信息的按钮点击事件
                        btnViewRaw.setOnClickListener {
                            val dialog = android.app.AlertDialog.Builder(this@MainActivity)
                                .setTitle("API原始返回")
                                .setMessage(result.message)
                                .setPositiveButton("复制") { _, _ ->
                                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("API返回内容", result.message)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(this@MainActivity, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                                }
                                .setNegativeButton("关闭", null)
                                .create()
                            
                            // 使对话框中的文本可选择
                            dialog.show()
                            dialog.findViewById<TextView>(android.R.id.message)?.setTextIsSelectable(true)
                        }
                    }

                    username?.let { name ->
                        val userId = dbHelper.getUserId(name)
                        if (userId != -1) {
                            dbHelper.insertTextDetectionRecord(userId, inputText, isAI)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        resultTextView.text = "检测失败，请重试"
                        Toast.makeText(this@MainActivity, e.localizedMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    }
}
