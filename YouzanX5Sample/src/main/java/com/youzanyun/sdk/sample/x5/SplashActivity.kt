package com.youzanyun.sdk.sample.x5

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.EditText
import com.youzan.androidsdk.YouzanSDK

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        findViewById<View>(R.id.go_with_login).setOnClickListener { goWithLogin() }
        findViewById<View>(R.id.go_without_login).setOnClickListener { go() }
        findViewById<View>(R.id.go).setOnClickListener {
            val url: String = findViewById<EditText>(R.id.url).text.toString()
            if (url.startsWith("http")) {
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                intent.putExtra("url", url)
                startActivity(intent)
            }
        }


        findViewById<View>(R.id.logout).setOnClickListener {
          YouzanSDK.userLogout(this@SplashActivity)
        }

    }

    fun goWithLogin() {
        val clz = LoginActivity::class.java
        val intent = Intent(this@SplashActivity, clz)
        startActivity(intent)
    }

    fun go() {
        val clz = MainActivity::class.java
        val intent = Intent(this@SplashActivity, clz)
        startActivity(intent)
    }
}