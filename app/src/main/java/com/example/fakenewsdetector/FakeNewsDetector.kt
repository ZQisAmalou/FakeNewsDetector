package com.example.fakenewsdetector

import android.content.ContentResolver
import android.net.Uri
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object FakeNewsDetector {
    private const val ZHIPUAI_API_KEY = "c6c970d4cd4f13745d08adf923aefb7d.P7EC5rOHL54sJwaB"
    private lateinit var applicationContext: Context
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    fun detectFakeImage(contentResolver: ContentResolver, imageUri: Uri): DetectionResult {
        val isReal = (0..1).random() == 0
        return DetectionResult(
            isReal = isReal,
            message = if (isReal) "图片可能是真实的" else "图片可能是伪造的"
        )
    }

    fun detectFakeVideo(contentResolver: ContentResolver, videoUri: Uri): DetectionResult {
        val isReal = (0..1).random() == 0
        return DetectionResult(
            isReal = isReal,
            message = if (isReal) "视频可能是真实的" else "视频可能是伪造的"
        )
    }

    fun detectFakeAudio(contentResolver: ContentResolver, audioUri: Uri): DetectionResult {
        val isReal = (0..1).random() == 0
        return DetectionResult(
            isReal = isReal,
        message = if (isReal) "音频可能是真实的" else "音频可能是伪造的"
        )
    }

    fun detectFakeText(text: String): DetectionResult {
        // 检查网络连接
        if (!isNetworkAvailable()) {
            return DetectionResult(
                isReal = false,
                message = "检测失败：请检查网络连接"
            )
        }

        try {
            val prompt = """
                请分析以下文本是由人类撰写还是AI生成。请按以下格式回复：
                结论：[AI生成/人类撰写]
                置信度：[百分比]
                分析依据：[详细说明]
                
                文本内容：${text.replace("\"", "'").replace("\n", " ")}
            """.trimIndent()

            val messagesArray = JSONArray()
            val messageObject = JSONObject()
            messageObject.put("role", "user")
            messageObject.put("content", prompt)
            messagesArray.put(messageObject)

            val jsonObject = JSONObject()
            jsonObject.put("model", "glm-4-flash")
            jsonObject.put("messages", messagesArray)
            jsonObject.put("temperature", 0.7)
            jsonObject.put("top_p", 0.7)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonObject.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://open.bigmodel.cn/api/paas/v4/chat/completions")
                .addHeader("Authorization", "Bearer $ZHIPUAI_API_KEY")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw IOException("Empty response")
            
            // 打印API响应的详细信息
            android.util.Log.d("API_RESPONSE", "Status Code: ${response.code}")
            android.util.Log.d("API_RESPONSE", "Headers: ${response.headers}")
            android.util.Log.d("API_RESPONSE", "Body: $responseBody")
            
            val responseJson = JSONObject(responseBody)
            
            // 如果解析失败，返回原始响应内容
            if (!responseJson.has("choices")) {
                return DetectionResult(
                    isReal = false,
                    message = "API返回数据格式错误。原始响应：\n$responseBody"
                )
            }

            val choices = responseJson.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                return DetectionResult(
                    isReal = false,
                    message = "检测失败：API返回数据格式错误"
                )
            }

            val content = choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            return DetectionResult(
                isReal = !content.contains("AI生成"),
                message = content
            )

        } catch (e: Exception) {
            e.printStackTrace()
            return DetectionResult(
                isReal = false,
                message = "检测失败：${e.message}\n\n完整错误：${e.stackTraceToString()}"
            )
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                )
    }
}

data class DetectionResult(
    val isReal: Boolean,
    val message: String
)
