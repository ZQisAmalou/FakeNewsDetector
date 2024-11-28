package com.example.fakenewsdetector

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        dbHelper = DatabaseHelper(this)

        val usernameEditText: EditText = findViewById(R.id.et_username)
        val passwordEditText: EditText = findViewById(R.id.et_password)
        val loginButton: Button = findViewById(R.id.btn_login)
        val registerButton: Button = findViewById(R.id.btn_register)

        loginButton.setOnClickListener {
            try {
                val username = usernameEditText.text.toString()
                val password = passwordEditText.text.toString()

                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "用户名和密码不能为空", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (dbHelper.validateUser(username, password)) {
                    Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("username", username)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "用户名或密码错误", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "登录失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (dbHelper.insertUser(username, password) > 0) {
                Toast.makeText(this, "Registration Successful", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Registration Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
