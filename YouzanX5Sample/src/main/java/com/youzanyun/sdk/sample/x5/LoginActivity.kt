package com.youzanyun.sdk.sample.x5

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.youzan.androidsdk.YouzanSDK
import com.youzan.androidsdk.YouzanToken
import com.youzan.androidsdk.YzLoginCallback
import com.youzanyun.sdk.sample.helper.LoginHelper
import com.youzanyun.sdk.sample.helper.YouzanHelper
import com.youzanyun.sdk.sample.x5.R

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