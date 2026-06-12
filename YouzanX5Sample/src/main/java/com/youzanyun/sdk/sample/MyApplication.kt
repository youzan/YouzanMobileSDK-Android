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

//import com.youzanyun.sdk.sample.config.KaeConfig.S_URL_MINE
import android.app.AppOpsManager
import android.app.Application
import android.app.AsyncNotedAppOp
import android.app.SyncNotedAppOp
import android.os.Build
import android.util.Log
import com.youzan.androidsdk.InitConfig
import com.youzan.androidsdk.LogCallback
import com.youzan.androidsdk.YouzanSDK
import com.youzan.androidsdkx5.YouZanSDKX5Adapter
import com.youzanyun.sdk.sample.cache.OfflineCacheLogger
import com.youzanyun.sdk.sample.config.KaeConfig
import com.youzanyun.sdk.sample.helper.LoginHelper

class MyApplication : Application() {
    companion object {
        @JvmField
        val HTML_CACHE_URLS = listOf(
            "https://shop92396879.m.youzan.com/v2/showcase/homepage?alias=xR6aSOPPhM"
        )
    }

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
        val config = InitConfig.builder()
            .clientId(KaeConfig.S_CLIENT_ID)
            .appkey("")
            .adapter(YouZanSDKX5Adapter())
            .initCallBack { ready, message ->

            }
            .logCallback(object : LogCallback {
                override fun onLog(eventType: String, message: String) {

                }
            })
            .build()
        YouzanSDK.init(this, config)
        
        // 确保 X5 内核初始化完成（回调或者延迟）后再执行预加载
        com.tencent.smtt.sdk.QbSdk.setTbsListener(object : com.tencent.smtt.sdk.TbsListener {
            override fun onDownloadFinish(i: Int) {}
            override fun onInstallFinish(i: Int) {}
            override fun onDownloadProgress(i: Int) {}
        })
        
        com.tencent.smtt.sdk.QbSdk.initX5Environment(this, object : com.tencent.smtt.sdk.QbSdk.PreInitCallback {
            override fun onCoreInitFinished() {
                // X5 内核初始化完成
                OfflineCacheLogger.log("X5初始化", "内核初始化完成")
            }
            override fun onViewInitFinished(isX5Core: Boolean) {
                OfflineCacheLogger.log("X5初始化", "内核初始化完成，isX5Core=$isX5Core，开始预加载WebView")
            }
        })
        com.youzanyun.sdk.sample.cache.WebViewPreloadManager.preload(
            this@MyApplication,
            KaeConfig.S_URL_MAIN,
            HTML_CACHE_URLS
        )
        LoginHelper.init(this)
    }
}
