package com.youzanyun.sdk.sample.x5

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.youzanyun.sdk.sample.helper.LoginHelper
import com.youzanyun.sdk.sample.helper.YouzanHelper

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findViewById<View>(R.id.login_tv).setOnClickListener {

            YouzanHelper.loginYouzan(this@LoginActivity) {
                LoginHelper.setLogin(true)
                Intent(this@LoginActivity, MainActivity::class.java).apply {
                    this.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(this)
                    this@LoginActivity.finish()
                }
            }
        }
    }
}