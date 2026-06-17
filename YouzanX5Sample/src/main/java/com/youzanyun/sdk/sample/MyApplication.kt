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
import android.widget.ImageView
import com.youzan.androidsdk.InitConfig
import com.youzan.androidsdk.YouzanSDK
import com.youzan.androidsdk.adapter.IImageAdapter
import com.youzan.androidsdkx5.YouZanSDKX5Adapter
import com.youzan.spiderman.cache.CachePreference
import com.youzan.spiderman.remote.config.ConfigManager
import com.youzan.spiderman.remote.config.ConfigPref
import com.youzanyun.sdk.sample.cache.WebResCacheManager
import com.youzanyun.sdk.sample.cache.WebViewPreloadManager
import com.youzanyun.sdk.sample.config.KaeConfig
import com.youzanyun.sdk.sample.helper.LoginHelper

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


            // 初始化SDK
            //appkey:可以前往<a href="http://open.youzan.com/sdk/access">有赞开放平台</a>申请
            WebResCacheManager.init(this)
            WebViewPreloadManager.init(this)


//        WebViewPreloadManager.getWebView(KaeConfig.S_URL_MAIN, null).loadUrl(KaeConfig.S_URL_MAIN)
            YouzanSDK.isDebug(true)
            val config = InitConfig.builder()
                .clientId(KaeConfig.S_CLIENT_ID)
                .appkey(KaeConfig.S_APP_KEY)
                .adapter(YouZanSDKX5Adapter())
                .setImageAdapter(object : IImageAdapter {
                    override fun setImage(view: ImageView, url: String) {
                        // 图片框架加载
                    }

                    override fun setImage(view: ImageView, res: Int): Boolean {
                        return false
                    }
                })
                .initCallBack { ready, message ->
                    Log.d("lsd", "${ready}, ${message}")
                }
                .build()
            YouzanSDK.init(this, config)
//        YouzanPreloader.preloadHtml(this, KaeConfig.S_URL_MAIN)
            LoginHelper.init(this)

            // 初始化SDK
            //appkey:可以前往<a href="http://open.youzan.com/sdk/access">有赞开放平台</a>申请
            val configPref = ConfigPref()
            configPref.configEntity.config.resourceConfig.enableCache = false
            CachePreference.flush(configPref, "config_pref")
            ConfigManager.getInstance().setEnableCache(false)
        }
    }
}