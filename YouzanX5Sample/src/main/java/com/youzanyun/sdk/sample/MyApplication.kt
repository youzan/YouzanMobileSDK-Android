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
import com.youzan.androidsdk.InitConfig
import com.youzan.androidsdk.YouzanSDK
import com.youzan.androidsdkx5.YouZanSDKX5Adapter
import com.youzanyun.sdk.sample.config.KaeConfig
import com.youzanyun.sdk.sample.helper.LoginHelper

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 初始化SDK
        //appkey:可以前往<a href="http://open.youzan.com/sdk/access">有赞开放平台</a>申请
        YouzanSDK.isDebug(true)
        //TODO clientId 写入
        val config = InitConfig.builder()
            .clientId(KaeConfig.S_CLIENT_ID)
            .appkey("")
            .adapter(YouZanSDKX5Adapter())
            .build()
        YouzanSDK.init(this, config)

        // 可选
        // 预取html，一般是预取店铺主页的url。
        // 注意：当发生重定向时，预取不生效。
        // YouzanPreloader.preloadHtml(this, "准备预加载的页面的URL");
        LoginHelper.init(this)
    }
}