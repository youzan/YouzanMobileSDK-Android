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


import static android.app.Activity.RESULT_OK;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.gson.Gson;
import com.youzan.androidsdk.YouzanLog;
import com.youzan.androidsdk.YouzanSDK;
import com.youzan.androidsdk.YouzanToken;
import com.youzan.androidsdk.YzLoginCallback;
import com.youzan.androidsdk.basic.YouzanBrowser;
import com.youzan.androidsdk.basic.compat.CompatWebChromeClient;
import com.youzan.androidsdk.basic.compat.VideoCallback;
import com.youzan.androidsdk.basic.compat.WebChromeClientConfig;
import com.youzan.androidsdk.event.AbsAuthEvent;
import com.youzan.androidsdk.event.AbsCheckAuthMobileEvent;
import com.youzan.androidsdk.event.AbsChooserEvent;
import com.youzan.androidsdk.event.AbsPaymentFinishedEvent;
import com.youzan.androidsdk.event.AbsShareEvent;
import com.youzan.androidsdk.event.AbsStateEvent;
import com.youzan.androidsdk.model.goods.GoodsShareModel;
import com.youzan.androidsdk.model.trade.TradePayFinishedModel;


/**
 * 这里使用{@link WebViewFragment}对{@link android.webkit.WebView}生命周期有更好的管控.
 */
public class YouzanFragment extends WebViewFragment implements SwipeRefreshLayout.OnRefreshListener {
    private YouzanBrowser mView;
    private SwipeRefreshLayout mRefreshLayout;
    private Toolbar mToolbar;
    private static final int CODE_REQUEST_LOGIN = 0x1000;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupViews(view);
        setupYouzan();

        final String url = getArguments().getString(YouzanActivity.KEY_URL);
        mView.loadUrl(url);
        //加载H5时，开启默认loading
        //设置自定义loading图片
//        mView.setLoadingImage(R.mipmap.ic_launcher);
    }

    private void setupViews(View contentView) {
        //WebView
        mView = getWebView();

        mToolbar = (Toolbar) contentView.findViewById(R.id.toolbar);
//        mRefreshLayout = (SwipeRefreshLayout) contentView.findViewById(R.id.swipe);

        //分享按钮
        mToolbar.setTitle(R.string.loading_page);
        mToolbar.inflateMenu(R.menu.menu_youzan_share);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_share:
                        mView.sharePage();
                        return true;
                    default:
                        return false;
                }
            }
        });

        //刷新
//        mRefreshLayout.setOnRefreshListener(this);
//        mRefreshLayout.setColorSchemeColors(Color.BLUE, Color.RED);
//        mRefreshLayout.setEnabled(false);

        mView.setWebViewClient(new WebViewClient() {


            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // 接入是需手动处理此部分证书逻辑
                handler.proceed();
            }

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // 页面加载完成后执行 JavaScript 获取 performance.timing
                view.evaluateJavascript("javascript:(function() { return JSON.stringify(window.performance.timing); })();",
                        new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String timingJson) {
                                // timingJson 包含 performance.timing 的 JSON 字符串
                                YouzanLog.addLog(YouzanLog.S_EVENT_TYPE_PERF, timingJson);
//                                printPerf(timingJson);
                            }
                        });
            }
        });

        mView.setWebChromeClient(new CompatWebChromeClient(
                new WebChromeClientConfig(
                        true, new VideoCallback() {
                    @Override
                    public void onVideoCallback(boolean b) {
                        Toast.makeText(getActivity(), "" + b, Toast.LENGTH_SHORT).show();
                    }


                }
                )
        ) {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
            }
        });

    }

    private void setupYouzan() {
        mView.subscribe(new AbsCheckAuthMobileEvent(){});
        //认证事件, 回调表示: 需要需要新的认证信息传入
        mView.subscribe(new AbsAuthEvent() {

            @Override
            public void call(Context context, boolean needLogin) {
                /**
                 * 建议实现逻辑:
                 *
                 *     判断App内的用户是否登录?
                 *       => 已登录: 请求带用户角色的认证信息(login接口);
                 *       => 未登录: needLogin为true, 唤起App内登录界面, 请求带用户角色的认证信息(login接口);
                 *       => 未登录: needLogin为false, 请求不带用户角色的认证信息(initToken接口).
                 *
                 *      服务端接入文档: https://www.youzanyun.com/docs/guide/appsdk/683
                 */
                //TODO 自行编码实现. 具体可参考开发文档中的伪代码实现
                //TODO 手机号自己填入
                YouzanSDK.yzlogin("31467761", "https://cdn.daddylab.com/Upload/android/20210113/021119/au9j4d6aed5xfweg.jpeg?w=1080&h=1080", "", "一百亿养乐多", "0", new YzLoginCallback() {
                    @Override
                    public void onSuccess(YouzanToken youzanToken) {
                        mView.post(new Runnable() {
                            @Override
                            public void run() {
                                mView.sync(youzanToken);
                            }
                        });
                    }

                    @Override
                    public void onFail(String s) {

                    }
                });
            }
        });
        mView.subscribe(new AbsCheckAuthMobileEvent() {});
        //文件选择事件, 回调表示: 发起文件选择. (如果app内使用的是系统默认的文件选择器, 该事件可以直接删除)
        mView.subscribe(new AbsChooserEvent() {
            @Override
            public void call(Context context, Intent intent, int requestCode) throws ActivityNotFoundException {
                startActivityForResult(intent, requestCode);
            }
        });

        //页面状态事件, 回调表示: 页面加载完成
        mView.subscribe(new AbsStateEvent() {
            @Override
            public void call(Context context) {
                mToolbar.setTitle(mView.getTitle());

                //停止刷新
//                mRefreshLayout.setRefreshing(false);
//                mRefreshLayout.setEnabled(true);
            }
        });
        //分享事件, 回调表示: 获取到当前页面的分享信息数据
        mView.subscribe(new AbsShareEvent() {
            @Override
            public void call(Context context, GoodsShareModel data) {
                /**
                 * 在获取数据后, 可以使用其他分享SDK来提高分享体验.
                 * 这里调用系统分享来简单演示分享的过程.
                 */
                String content = data.getDesc() + data.getLink();
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, content);
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, data.getTitle());
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
            }
        });

        mView.subscribe(new AbsPaymentFinishedEvent() {
            @Override
            public void call(Context context, TradePayFinishedModel tradePayFinishedModel) {

            }
        });
    }


    @Override
    protected int getWebViewId() {
        //YouzanBrowser在布局文件中的id
        return R.id.view;
    }


    @Override
    protected int getLayoutId() {
        //布局文件
        return R.layout.fragment_youzan;
    }

    @Override
    public boolean onBackPressed() {
        //页面回退
        return getWebView().pageGoBack();
    }

    @Override
    public void onRefresh() {
        //重新加载页面
        mView.reload();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (CODE_REQUEST_LOGIN == requestCode) {// 如果是登录事件返回
            if (resultCode == RESULT_OK) {
                // 登录成功设置token

            } else {
                // 登录失败
                mView.syncNot();
            }
        } else {
            // 文件选择事件处理。
            mView.receiveFile(requestCode, data);
        }
    }

    public void printPerf(String timingJson) {
        try {
            String value1 = timingJson.replaceAll("\\\\\"", "\"");
            TimingData timingData = new Gson().fromJson(value1.substring(1, value1.length() - 1), TimingData.class);
//            // 计算关键时间点的耗时
            long navigationTime = timingData.fetchStart - timingData.navigationStart;
            long domainLookupTime = timingData.domainLookupEnd - timingData.domainLookupStart;
            long connectTime = timingData.connectEnd - timingData.connectStart;
            long requestTime = timingData.responseStart - timingData.requestStart;
            long responseTime = timingData.responseEnd - timingData.responseStart;
            long domParsingTime = timingData.domComplete - timingData.domLoading;
            long domInteractiveTime = timingData.domInteractive - timingData.fetchStart;
            long domContentLoadedTime = timingData.domContentLoadedEventEnd - timingData.domContentLoadedEventStart;
            long loadEventTime = timingData.loadEventEnd - timingData.loadEventStart;
            long totalTime = timingData.loadEventEnd - timingData.navigationStart;
//
            // 打印耗时信息或者做其他处理
            YouzanLog.addLog(YouzanLog.S_EVENT_TYPE_PERF, "PerformanceTiming, Timing JSON: " + timingJson);
            YouzanLog.addLog(YouzanLog.S_EVENT_TYPE_PERF, "Navigation Time: " + navigationTime + " ms");
            YouzanLog.addLog(YouzanLog.S_EVENT_TYPE_PERF, "Domain Lookup Time: " + domainLookupTime + " ms");
            YouzanLog.addLog(YouzanLog.S_EVENT_TYPE_PERF, "Connect Time: " + connectTime + " ms");
            YouzanLog.addLog(YouzanLog.S_EVENT_TYPE_PERF, "Request Time: " + requestTime + " ms");
            YouzanLog.addLog(YouzanLog.S_EVENT_TYPE_PERF, "Response Time: " + responseTime + " ms");
            YouzanLog.addLog(YouzanLog.S_EVENT_TYPE_PERF, "DOM Parsing Time: " + domParsingTime + " ms");
            YouzanLog.addLog(YouzanLog.S_EVENT_TYPE_PERF, "DOM Interactive Time: " + domInteractiveTime + " ms");
            YouzanLog.addLog(YouzanLog.S_EVENT_TYPE_PERF, "DOM Content Loaded Time: " + domContentLoadedTime + " ms");
            YouzanLog.addLog(YouzanLog.S_EVENT_TYPE_PERF, "Load Event Time: " + loadEventTime + " ms");
            YouzanLog.addLog(YouzanLog.S_EVENT_TYPE_PERF, "Total Time: " + totalTime + " ms");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class TimingData {
        private long connectStart;
        private long navigationStart;
        private long loadEventEnd;
        private long domLoading;
        private long secureConnectionStart;
        private long fetchStart;
        private long domContentLoadedEventStart;
        private long responseStart;
        private long responseEnd;
        private long domInteractive;
        private long domainLookupEnd;
        private long redirectStart;
        private long requestStart;
        private long unloadEventEnd;
        private long unloadEventStart;
        private long domComplete;
        private long domainLookupStart;
        private long loadEventStart;
        private long domContentLoadedEventEnd;
        private long redirectEnd;
        private long connectEnd;
    }
}

