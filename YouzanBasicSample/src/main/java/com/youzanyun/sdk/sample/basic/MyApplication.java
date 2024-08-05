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

import com.youzan.androidsdk.InitCallBack;
import com.youzan.androidsdk.InitConfig;
import com.youzan.androidsdk.YouzanSDK;
import com.youzan.androidsdk.basic.YouzanBasicSDKAdapter;


public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化SDK
        //appkey:可以前往<a href="http://open.youzan.com/sdk/access">有赞开放平台</a>申请
        YouzanSDK.isDebug(true);
        YouzanSDK.init(this, new InitConfig.Builder()
                .clientId("0073bccbaf5369028a")
                .appkey("")
                        .initCallBack(new InitCallBack() {
                            @Override
                            public void readyCallBack(boolean ready, String message) {
                            }
                        })
                .adapter(new YouzanBasicSDKAdapter())
                .build()
        );




    }
}
