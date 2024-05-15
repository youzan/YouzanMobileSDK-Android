/*
 * Copyright (C) 2017 youzanyun.com, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.youzanyun.sdk.sample

import android.app.Application
import android.content.Context
import com.youzan.androidsdk.InitConfig
import com.youzan.androidsdk.LogCallback
import com.youzan.androidsdk.YouzanSDK
import com.youzan.androidsdk.YouzanSettings
import com.youzan.androidsdkx5.YouZanSDKX5Adapter
import com.youzan.androidsdkx5.YouzanPreloader
import com.youzanyun.sdk.sample.config.KaeConfig
import com.youzanyun.sdk.sample.helper.LoginHelper
import ren.yale.android.cachewebviewlib.WebViewCacheInterceptor
import ren.yale.android.cachewebviewlib.WebViewCacheInterceptorInst

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 初始化SDK
        //appkey:可以前往<a href="http://open.youzan.com/sdk/access">有赞开放平台</a>申请
        YouzanSDK.isDebug(true)
        //TODO clientId 写入
        val config = InitConfig.builder()
            .settings(mutableMapOf(
                YouzanSettings.SETTINGS_SUPPORT_SAVE_IMAGE_WITH_LONG_PRESS to true).toMap()
            )
            .clientId(KaeConfig.S_CLIENT_ID)
            .appkey("")
            .adapter(object : YouZanSDKX5Adapter() {
                override fun clearCookieByHost(p0: Context?, p1: String?) {
                    super.clearCookieByHost(p0, p1)
                }

                override fun clearLocalStorage() {
                    super.clearLocalStorage()
                }
            })
            .logCallback(object : LogCallback {
                override fun onLog(eventType: String, message: String): Boolean {
                    return false
                }
            })

            .build()
        YouzanSDK.init(this, config)

        // 可选
        // 预取html，一般是预取店铺主页的url。
        // 注意：当发生重定向时，预取不生效。
        YouzanPreloader.preloadHtml(this, KaeConfig.S_URL_MAIN);
        WebViewCacheInterceptorInst.getInstance().init(WebViewCacheInterceptor.Builder(this))
        LoginHelper.init(this)
    }
}