package com.youzanyun.sdk.sample.x5

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.youzanyun.sdk.sample.helper.LoginHelper
import com.youzanyun.sdk.sample.x5.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        findViewById<View>(android.R.id.content).postDelayed(Runnable {
            val clz = if (LoginHelper.isLogin()) {
                MainActivity::class.java
            } else {
                LoginActivity::class.java
            }
            val intent = Intent(this@SplashActivity, clz)
            startActivity(intent)
            finish()
        }, 1000)
    }
}