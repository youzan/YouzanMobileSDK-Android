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

package com.youzanyun.sdk.sample.basic;

import android.app.Application;
import android.os.Build;
import android.os.Looper;
import android.os.MessageQueue;

import com.youzan.androidsdk.InitCallBack;
import com.youzan.androidsdk.InitConfig;
import com.youzan.androidsdk.YouzanSDK;
import com.youzan.androidsdk.basic.YouzanBasicSDKAdapter;
import com.youzan.androidsdk.basic.YouzanPreloader;


public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化SDK
        //appkey:可以前往<a href="http://open.youzan.com/sdk/access">有赞开放平台</a>申请
        YouzanSDK.isDebug(true);
        YouzanSDK.init(this, new InitConfig.Builder()
                .clientId("0073bccbaf5369028a")
                .isSupportOffline(false)
                        .initCallBack(new InitCallBack() {
                            @Override
                            public void readyCallBack(boolean ready, String message) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    Looper.getMainLooper().getQueue().addIdleHandler(new MessageQueue.IdleHandler() {
                                        @Override
                                        public boolean queueIdle() {
                                            // 可选， 需要在主线程执行
                                            YouzanPreloader.preloadOfflineRes(MyApplication.this, KaeConfig.URL_MAIN );
                                            return false;
                                        }
                                    });
                                }
                            }
                        })
                .adapter(new YouzanBasicSDKAdapter())
                .build()
        );




    }
}
