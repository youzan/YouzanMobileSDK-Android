package com.youzanyun.sdk.sample.x5

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.startActivity
import com.youzanyun.sdk.sample.helper.LoginHelper
import com.youzanyun.sdk.sample.x5.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        findViewById<View>(android.R.id.content).postDelayed(Runnable {
            ActivityCompat.requestPermissions(this@SplashActivity, arrayOf(Manifest.permission.READ_PHONE_STATE), 1)
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