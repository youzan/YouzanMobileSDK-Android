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
import android.util.Log;

import com.youzan.androidsdk.InitConfig;
import com.youzan.androidsdk.LogCallback;
import com.youzan.androidsdk.YouzanLog;
import com.youzan.androidsdk.YouzanSDK;
import com.youzan.androidsdk.basic.YouzanBasicSDKAdapter;

import org.jetbrains.annotations.NotNull;


public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // 初始化SDK
        //appkey:可以前往<a href="http://open.youzan.com/sdk/access">有赞开放平台</a>申请
        YouzanSDK.isDebug(true);
        //TODO clientId 写入
        YouzanSDK.init(this, new InitConfig.Builder()
                        .advanceHideX5Loading(false)
                        .logCallback(new LogCallback() {
                            @Override
                            public boolean onLog(@NotNull String eventType, @NotNull String message) {
                                return true;
                            }
                        })
                        .clientId("0073bccbaf5369028a")
                        .appkey("")
                        .adapter(new YouzanBasicSDKAdapter())

                .build());
        // 可选
        // 预取html，一般是预取店铺主页的url。
        // 注意：当发生重定向时，预取不生效。
//         YouzanPreloader.preloadHtml(this, "准备预加载的页面的URL");
    }
}
