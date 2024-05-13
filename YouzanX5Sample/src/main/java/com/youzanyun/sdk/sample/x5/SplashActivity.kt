package com.youzanyun.sdk.sample.x5

import android.content.Intent
import android.os.Bundle
import android.support.v4.os.HandlerCompat.postDelayed
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.youzanyun.sdk.sample.helper.LoginHelper
import com.youzanyun.sdk.sample.x5.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        findViewById<View>(R.id.go_with_login).setOnClickListener { goWithLogin() }
        findViewById<View>(R.id.go_without_login).setOnClickListener { go() }


    }

    fun goWithLogin() {
        val clz = if (LoginHelper.isLogin()) {
            MainActivity::class.java
        } else {
            LoginActivity::class.java
        }
        val intent = Intent(this@SplashActivity, clz)
        startActivity(intent)
    }

    fun go() {
        val clz = MainActivity::class.java
        val intent = Intent(this@SplashActivity, clz)
        startActivity(intent)
    }
}