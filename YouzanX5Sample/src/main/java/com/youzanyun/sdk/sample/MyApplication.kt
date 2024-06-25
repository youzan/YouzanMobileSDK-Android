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

import android.app.AppOpsManager
import android.app.Application
import android.app.AsyncNotedAppOp
import android.app.SyncNotedAppOp
import android.content.Context
import android.os.Build
import android.util.Log
import com.youzan.androidsdk.InitConfig
import com.youzan.androidsdk.LogCallback
import com.youzan.androidsdk.YouzanSDK
import com.youzan.androidsdk.basic.YouzanSettings
import com.youzan.androidsdkx5.YouZanSDKX5Adapter
import com.youzan.androidsdkx5.YouzanPreloader
import com.youzanyun.sdk.sample.config.KaeConfig
//import com.youzanyun.sdk.sample.config.KaeConfig.S_URL_MINE
import com.youzanyun.sdk.sample.helper.LoginHelper
import ren.yale.android.cachewebviewlib.WebViewCacheInterceptor
import ren.yale.android.cachewebviewlib.WebViewCacheInterceptorInst

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appOpsCallback = object : AppOpsManager.OnOpNotedCallback() {
                private fun logPrivateDataAccess(opCode: String, trace: String) {
                    Log.i(
                        "lsd", "Private data accessed. " +
                                "Operation: $opCode\nStack Trace:\n$trace"
                    )
                }

                override fun onNoted(syncNotedAppOp: SyncNotedAppOp) {
                    logPrivateDataAccess(
                        syncNotedAppOp.op, Throwable().stackTrace.toString()
                    )
                }

                override fun onSelfNoted(syncNotedAppOp: SyncNotedAppOp) {
                    logPrivateDataAccess(
                        syncNotedAppOp.op, Throwable().stackTrace.toString()
                    )
                }

                override fun onAsyncNoted(asyncNotedAppOp: AsyncNotedAppOp) {
                    logPrivateDataAccess(asyncNotedAppOp.op, asyncNotedAppOp.message)
                }
            }
            val appOpsManager = getSystemService(AppOpsManager::class.java) as AppOpsManager
            appOpsManager.setOnOpNotedCallback(mainExecutor, appOpsCallback)
        }


        // 初始化SDK
        //appkey:可以前往<a href="http://open.youzan.com/sdk/access">有赞开放平台</a>申请
        YouzanSDK.isDebug(true)
        //TODO clientId 写入
        val config = InitConfig.builder()
            .settings(
                mutableMapOf(
                    com.youzan.androidsdk.basic.YouzanSettings.SETTINGS_SUPPORT_ENABLE_IMEI to false
                ).toMap()
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

                override fun getX5TbsSettings(): MutableMap<String, Any> {
                    return super.getX5TbsSettings()
                }

                override fun onStartX5TbsInit() {
                    super.onStartX5TbsInit()
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