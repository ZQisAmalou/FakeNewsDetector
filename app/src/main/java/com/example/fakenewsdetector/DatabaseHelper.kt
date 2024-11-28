package com.example.fakenewsdetector

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "FakeNewsDetector.db"
        const val DATABASE_VERSION = 2

        // 用户表
        const val TABLE_USER = "User"
        const val COLUMN_USER_ID = "id"
        const val COLUMN_USERNAME = "username"
        const val COLUMN_PASSWORD = "password"

        // 历史记录表
        const val TABLE_HISTORY = "History"
        const val COLUMN_HISTORY_ID = "id"
        const val COLUMN_USER_ID_FK = "user_id"
        const val COLUMN_TYPE = "type"
        const val COLUMN_FILENAME = "filename"
        const val COLUMN_RESULT = "result"
        const val COLUMN_TIMESTAMP = "timestamp"

        // 文本检测历史表
        const val TABLE_TEXT_DETECTION = "text_detection_history"
        const val COLUMN_TEXT = "text"
        const val COLUMN_IS_AI = "is_ai"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // 创建用户表
        val createUserTable = """
            CREATE TABLE $TABLE_USER (
                $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT NOT NULL UNIQUE,
                $COLUMN_PASSWORD TEXT NOT NULL
            )
        """.trimIndent()

        // 创建文本检测历史表
        val createTextDetectionHistoryTable = """
            CREATE TABLE text_detection_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER,
                text TEXT,
                is_ai INTEGER,
                timestamp INTEGER,
                FOREIGN KEY(user_id) REFERENCES $TABLE_USER($COLUMN_USER_ID)
            )
        """.trimIndent()

        // 创建历史记录表
        val createHistoryTable = """
            CREATE TABLE $TABLE_HISTORY (
                $COLUMN_HISTORY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USER_ID_FK INTEGER,
                $COLUMN_TYPE TEXT,
                content TEXT,
                $COLUMN_RESULT TEXT,
                $COLUMN_TIMESTAMP INTEGER,
                FOREIGN KEY($COLUMN_USER_ID_FK) REFERENCES $TABLE_USER($COLUMN_USER_ID)
            )
        """.trimIndent()

        db?.execSQL(createUserTable)
        db?.execSQL(createTextDetectionHistoryTable)
        db?.execSQL(createHistoryTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS text_detection_history")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
        onCreate(db)
    }

    // 插入新用户
    fun insertUser(username: String, password: String): Long {
        val db = writableDatabase
        val values = ContentValues()
        values.put(COLUMN_USERNAME, username)
        values.put(COLUMN_PASSWORD, password)
        return db.insert(TABLE_USER, null, values)
    }

    // 验证用户登录
    fun validateUser(username: String, password: String): Boolean {
        val db = readableDatabase
        val query = "SELECT * FROM $TABLE_USER WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(username, password))
        val isValid = cursor.count > 0
        cursor.close()
        return isValid
    }

    // 插入历史记录
    fun insertHistory(userId: Int, type: String, content: String, result: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USER_ID_FK, userId)
            put(COLUMN_TYPE, type)
            put("content", content)
            put(COLUMN_RESULT, result)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        }
        db.insert(TABLE_HISTORY, null, values)
    }

    // 获取用户的历史记录
    fun getAllHistory(userId: Int): List<HistoryItem> {
        val history = mutableListOf<HistoryItem>()
        val db = readableDatabase

        // 获取文本检测历史
        val textQuery = """
            SELECT text, is_ai, timestamp 
            FROM text_detection_history 
            WHERE user_id = ?
        """
        db.rawQuery(textQuery, arrayOf(userId.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                val text = cursor.getString(0)
                val isAI = cursor.getInt(1) == 1
                val timestamp = cursor.getLong(2)
                history.add(HistoryItem(
                    "文本检测",
                    text,
                    if (isAI) "AI生成" else "人类撰写",
                    timestamp
                ))
            }
        }

        // 获取图片和视频检测历史
        val mediaQuery = """
            SELECT type, content, result, timestamp 
            FROM $TABLE_HISTORY 
            WHERE $COLUMN_USER_ID_FK = ?
        """
        db.rawQuery(mediaQuery, arrayOf(userId.toString())).use { cursor ->
            while (cursor.moveToNext()) {
                val type = cursor.getString(0)
                val content = cursor.getString(1)
                val result = cursor.getString(2)
                val timestamp = cursor.getLong(3)
                history.add(HistoryItem(type, content, result, timestamp))
            }
        }

        return history.sortedByDescending { it.timestamp }
    }

    fun getUserId(username: String): Int {
        val db = readableDatabase
        val query = "SELECT $COLUMN_USER_ID FROM $TABLE_USER WHERE $COLUMN_USERNAME = ?"
        val cursor = db.rawQuery(query, arrayOf(username))
        var userId = -1
        if (cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_USER_ID))
        }
        cursor.close()
        return userId
    }

    fun insertTextDetectionRecord(userId: Int, text: String, isAI: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("user_id", userId)
            put("text", text)
            put("is_ai", if (isAI) 1 else 0)
            put("timestamp", System.currentTimeMillis())
        }
        db.insert("text_detection_history", null, values)
    }

}

data class HistoryItem(
    val type: String,
    val content: String,
    val result: String,
    val timestamp: Long
)
