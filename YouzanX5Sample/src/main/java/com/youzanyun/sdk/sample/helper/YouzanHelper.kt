package com.youzanyun.sdk.sample.helper

import android.content.Context
import com.youzan.androidsdk.YouzanSDK
import com.youzan.androidsdk.YouzanToken
import com.youzan.androidsdk.YzLoginCallback

/**
 * auther: liusaideng
 * created on :  2023/7/18 10:12 AM
 * desc:
 */
object YouzanHelper {

    fun loginYouzan(context: Context, callback: (youzanToken: YouzanToken) -> Unit) {
        YouzanSDK.yzlogin(
            "31467761",
            "https://cdn.daddylab.com/Upload/android/20210113/021119/au9j4d6aed5xfweg.jpeg?w=1080&h=1080",
            "",
            "一百亿养乐多",
            "0",
            object : YzLoginCallback {
                override fun onSuccess(youzanToken: YouzanToken) {
                    YouzanSDK.sync(context, youzanToken)
                    callback.invoke(youzanToken)
                }

                override fun onFail(s: String) {}
            })
    }
}