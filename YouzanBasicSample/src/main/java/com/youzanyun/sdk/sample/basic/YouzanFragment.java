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

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JsPromptResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.youzanyun.sdk.sample.cache.OfflineCacheLogger;
import com.youzanyun.sdk.sample.cache.WebResource;
import com.youzanyun.sdk.sample.cache.WebViewCacheImpl;
import com.youzanyun.sdk.sample.cache.WebViewPreloadManager;

import org.json.JSONObject;

import java.util.Locale;


/**
 * 这里使用{@link WebViewFragment}对{@link android.webkit.WebView}生命周期有更好的管控.
 */
public class YouzanFragment extends WebViewFragment implements SwipeRefreshLayout.OnRefreshListener {
    private static final int CODE_REQUEST_LOGIN = 0x1000;
    private static final String TIMING_PROMPT_PREFIX = "yz_timing://report?data=";

    private YouzanBrowser mView;
    private Toolbar mToolbar;
    private TextView metricsView;
    private String webViewReuseState = "unknown";
    private long pageStartAt;
    private long firstProgressAt;
    private long pageFinishAt;
    private String pageUrl;
    private String jsTimingText;
    private Context appContext;
    private volatile int cachedWebViewCacheMode = WebSettings.LOAD_DEFAULT;
    private volatile String cachedUserAgent;

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        setupViews(view);
        setupYouzan();

        WebSettings settings = mView.getSettings();
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        appContext = view.getContext().getApplicationContext();
        cachedWebViewCacheMode = settings.getCacheMode();
        cachedUserAgent = settings.getUserAgentString();
        if (WebViewPreloadManager.getCacheImpl() == null) {
            WebViewPreloadManager.preload(appContext, KaeConfig.URL_MAIN, MyApplication.HTML_CACHE_URLS);
        }

        String url = getArguments() == null ? null : getArguments().getString(YouzanActivity.KEY_URL);
        if (TextUtils.isEmpty(url)) {
            url = KaeConfig.URL_MAIN;
        }
        resetMetricsForNewLoad(url);
        mView.loadUrl(url);
        //加载H5时，开启默认loading
        //设置自定义loading图片
//        mView.setLoadingImage(R.mipmap.ic_launcher);
    }

    private void setupViews(View contentView) {
        mView = getWebView();
        if ("reused".equals(webViewReuseState)) {
            resetInternalClientWrappersIfSupported();
        }
        metricsView = (TextView) contentView.findViewById(R.id.tv_page_metrics);
        mToolbar = (Toolbar) contentView.findViewById(R.id.toolbar);

        mToolbar.setTitle(R.string.loading_page);
        mToolbar.inflateMenu(R.menu.menu_youzan_share);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_share:
                        mView.sharePage();
                        return true;
                    case R.id.action_refresh:
                        mView.reload();
                        return true;
                    case R.id.action_print_timing:
                        printPageTiming();
                        return true;
                    default:
                        return false;
                }
            }
        });

        // 复用 WebView 进入页面时，重新绑定当前 Fragment 的 client，避免沿用预加载 client。
        mView.setWebChromeClient(createFragmentChromeClient());
        mView.setWebViewClient(createFragmentWebViewClient());
    }

    private CompatWebChromeClient createFragmentChromeClient() {
        return new CompatWebChromeClient(
                new WebChromeClientConfig(
                        true, new VideoCallback() {
                    @Override
                    public void onVideoCallback(boolean b) {
                        Toast.makeText(getActivity(), "" + b, Toast.LENGTH_SHORT).show();
                    }
                })
        ) {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (mToolbar != null) {
                    mToolbar.setTitle(title);
                }
            }

            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress > 0 && firstProgressAt == 0L) {
                    firstProgressAt = SystemClock.elapsedRealtime();
                    updateMetrics("开始渲染");
                }
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
                if (!TextUtils.isEmpty(message) && handleTimingPrompt(message)) {
                    result.confirm("");
                    return true;
                }
                return super.onJsPrompt(view, url, message, defaultValue, result);
            }
        };
    }

    private WebViewClient createFragmentWebViewClient() {
        return new WebViewClient() {
            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                // 接入是需手动处理此部分证书逻辑
                handler.proceed();
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                pageUrl = url;
                pageStartAt = SystemClock.elapsedRealtime();
                firstProgressAt = 0L;
                pageFinishAt = 0L;
                jsTimingText = null;
                OfflineCacheLogger.log("页面耗时", "onPageStarted，url=" + safe(url) + "，webView=" + getWebViewLabel());
                updateMetrics("开始加载");
            }

            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                pageFinishAt = SystemClock.elapsedRealtime();
                OfflineCacheLogger.log(
                        "页面耗时",
                        "pageStart=" + pageStartAt
                                + "，pageFinished=" + pageFinishAt
                                + "，总耗时=" + formatDuration(getPageTotalDuration())
                                + "，url=" + safe(url)
                                + "，webView=" + getWebViewLabel()
                );
                updateMetrics("页面完成");
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return null;
//                if (request == null || request.getUrl() == null) {
//                    return super.shouldInterceptRequest(view, request);
//                }
//                if (!MainActivity.isCacheEnabled() || !MainActivity.isReuseResourceEnabled()) {
//                    OfflineCacheLogger.log("资源分发", "总开关=" + MainActivity.isCacheEnabled() + "，资源复用=" + MainActivity.isReuseResourceEnabled() + "，直接交给 WebView 处理，url=" + request.getUrl());
//                    return super.shouldInterceptRequest(view, request);
//                }
//                WebViewCacheImpl cacheImpl = WebViewPreloadManager.getCacheImpl();
//                if (cacheImpl == null) {
//                    return super.shouldInterceptRequest(view, request);
//                }
//                String scheme = request.getUrl().getScheme();
//                String method = request.getMethod();
//                if ((TextUtils.equals("http", scheme) || TextUtils.equals("https", scheme))
//                        && "GET".equalsIgnoreCase(method)) {
//                    WebResourceResponse resourceResponse = cacheImpl.getResource(request, cachedWebViewCacheMode, cachedUserAgent);
//                    if (resourceResponse != null) {
//                        String source = cacheImpl.getLastResourceSource();
//                        if (WebResource.SOURCE_MEMORY.equals(source)
//                                || WebResource.SOURCE_NETWORK.equals(source)
//                                || WebResource.SOURCE_DISK.equals(source)) {
//                            OfflineCacheLogger.log(
//                                    "资源分发",
//                                    "Fragment返回缓存结果，来源=" + source + "，url=" + request.getUrl() + "，webView=" + getWebViewLabel()
//                            );
//                            return resourceResponse;
//                        }
//                        OfflineCacheLogger.log(
//                                "资源分发",
//                                "本次来源=" + source + "，交给WebView自行请求，url=" + request.getUrl() + "，webView=" + getWebViewLabel()
//                        );
//                    } else {
//                        OfflineCacheLogger.log(
//                                "资源分发",
//                                "Fragment未命中离线缓存，回退系统网络栈，url=" + request.getUrl() + "，webView=" + getWebViewLabel()
//                        );
//                    }
//                }
//                return super.shouldInterceptRequest(view, request);
            }
        };
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
                    public void onSuccess(final YouzanToken youzanToken) {
                        mView.post(new Runnable() {
                            @Override
                            public void run() {
                                mView.sync(youzanToken);
                            }
                        });
                    }

                    @Override
                    public void onFail(String message, int code) {

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
    protected YouzanBrowser createWebView(View contentView) {
        FrameLayout container = (FrameLayout) contentView.findViewById(R.id.webview_container);
        String targetUrl = getArguments() == null ? null : getArguments().getString(YouzanActivity.KEY_URL);
        if (TextUtils.isEmpty(targetUrl)) {
            targetUrl = KaeConfig.URL_MAIN;
        }
        WebViewPreloadManager.WebViewAcquireResult acquireResult = WebViewPreloadManager.getWebView(getActivity(), targetUrl);
        YouzanBrowser browser = acquireResult.webView;
        webViewReuseState = acquireResult.reused ? "reused" : "new";
        OfflineCacheLogger.log("进入页面", "当前WebView实例=" + getWebViewLabel());
        if (acquireResult.reused) {
            OfflineCacheLogger.log("进入页面", "复用实例仅完成WebView创建预加载，进入页面后加载目标URL，url=" + targetUrl);
        }
        Toast.makeText(getActivity(), "WebView: " + getWebViewLabel(), Toast.LENGTH_SHORT).show();
        if (browser.getParent() instanceof FrameLayout) {
            ((FrameLayout) browser.getParent()).removeView(browser);
        } else if (browser.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) browser.getParent()).removeView(browser);
        }
        container.addView(browser, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));
        return browser;
    }

    @Override
    protected int getWebViewId() {
        return -1;
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

    private void updateMetrics(String stage) {
        if (metricsView != null) {
            metricsView.setText(buildMetricsText(stage));
        }
    }

    private void resetMetricsForNewLoad(String url) {
        pageUrl = url;
        pageStartAt = 0L;
        firstProgressAt = 0L;
        pageFinishAt = 0L;
        jsTimingText = null;
        updateMetrics("等待加载");
    }

    private String buildMetricsText(String stage) {
        long total = getPageTotalDuration();
        long firstProgress = pageStartAt > 0L && firstProgressAt > 0L ? firstProgressAt - pageStartAt : 0L;
        String nativeText = "页面耗时: 阶段=" + stage
                + " | 总耗时(pageStart-pageFinished)=" + formatDuration(total)
                + " | 首次渲染=" + formatDuration(firstProgress)
                + " | URL=" + safe(pageUrl);
        if (TextUtils.isEmpty(jsTimingText)) {
            return nativeText;
        }
        return nativeText + "\nJS耗时: " + jsTimingText;
    }

    private long getPageTotalDuration() {
        return pageStartAt > 0L && pageFinishAt > 0L ? pageFinishAt - pageStartAt : 0L;
    }

    private String formatDuration(long duration) {
        return duration <= 0L ? "-" : duration + "ms";
    }

    private void printPageTiming() {
        long total = getPageTotalDuration();
        String message = total > 0L
                ? "pageStart 到 pageFinished 总耗时=" + formatDuration(total) + "，url=" + safe(pageUrl != null ? pageUrl : mView.getUrl()) + "，webView=" + getWebViewLabel()
                : "暂无完整 pageStart/pageFinished 耗时，pageStart=" + pageStartAt + "，pageFinished=" + pageFinishAt + "，url=" + safe(pageUrl != null ? pageUrl : mView.getUrl());
        updateMetrics("手动打印耗时");
        OfflineCacheLogger.log("页面耗时", message);
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
        printNavigationTimingByJs();
    }

    private void printNavigationTimingByJs() {
        String script = "(function(){"
                + "var nav=(performance.getEntriesByType&&performance.getEntriesByType('navigation')[0])||null;"
                + "if(!nav){prompt('" + TIMING_PROMPT_PREFIX + "'+encodeURIComponent(JSON.stringify({supported:false,reason:'当前浏览器不支持 PerformanceNavigationTiming API'})));return;}"
                + "var d=function(s,e){var v=e-s;return v>=0?Number(v.toFixed(2)):0;};"
                + "var m={redirect:d(nav.redirectStart,nav.redirectEnd),dns:d(nav.domainLookupStart,nav.domainLookupEnd),tcp:d(nav.connectStart,nav.connectEnd),ttfb:d(nav.requestStart,nav.responseStart),download:d(nav.responseStart,nav.responseEnd),domParse:d(nav.responseEnd,nav.domInteractive),domContentLoaded:d(nav.domContentLoadedEventStart,nav.domContentLoadedEventEnd),domComplete:d(nav.domInteractive,nav.domComplete),loadEvent:d(nav.loadEventStart,nav.loadEventEnd),total:d(nav.startTime,nav.loadEventEnd),whiteScreen:d(nav.startTime,nav.responseStart),interactive:d(nav.startTime,nav.domInteractive)};"
                + "console.log('页面性能各阶段耗时统计 (单位: ms)');console.table(m);"
                + "prompt('" + TIMING_PROMPT_PREFIX + "'+encodeURIComponent(JSON.stringify({supported:true,metrics:m,loadEventEnd:nav.loadEventEnd})));"
                + "})();";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mView.evaluateJavascript(script, null);
        } else {
            mView.loadUrl("javascript:" + script);
        }
    }

    private boolean handleTimingPrompt(String message) {
        if (!message.startsWith(TIMING_PROMPT_PREFIX)) {
            return false;
        }
        String encodedPayload = message.substring(TIMING_PROMPT_PREFIX.length());
        String payload;
        try {
            payload = Uri.decode(encodedPayload);
        } catch (Exception e) {
            payload = null;
        }
        if (payload == null) {
            jsTimingText = "解析失败";
            updateMetrics("JS耗时解析失败");
            OfflineCacheLogger.log("页面耗时", "JS耗时结果解析失败，message=" + message);
            return true;
        }
        applyTimingPayload(payload);
        return true;
    }

    private void applyTimingPayload(String payload) {
        try {
            JSONObject jsonObject = new JSONObject(payload);
            if (!jsonObject.optBoolean("supported", false)) {
                String reason = jsonObject.optString("reason", "当前页面暂不支持");
                jsTimingText = "不支持，原因=" + reason;
                updateMetrics("JS耗时不可用");
                OfflineCacheLogger.log("页面耗时", "JS耗时不可用，原因=" + reason);
                return;
            }
            JSONObject metrics = jsonObject.optJSONObject("metrics");
            if (metrics == null) {
                jsTimingText = "未获取到统计结果";
                updateMetrics("JS耗时缺失");
                OfflineCacheLogger.log("页面耗时", "JS耗时结果为空");
                return;
            }
            String summary = "总加载=" + formatJsDuration(metrics.optDouble("total"))
                    + " | 白屏=" + formatJsDuration(metrics.optDouble("whiteScreen"))
                    + " | TTFB=" + formatJsDuration(metrics.optDouble("ttfb"))
                    + " | DOM可交互=" + formatJsDuration(metrics.optDouble("interactive"))
                    + " | DOM完成=" + formatJsDuration(metrics.optDouble("domComplete"));
            jsTimingText = summary;
            updateMetrics("已打印JS耗时");
            OfflineCacheLogger.log("页面耗时", "JS耗时统计：" + summary);
        } catch (Exception e) {
            jsTimingText = "解析异常";
            updateMetrics("JS耗时解析异常");
            OfflineCacheLogger.log("页面耗时", "JS耗时解析异常，error=" + e.getMessage());
        }
    }

    private String formatJsDuration(double duration) {
        return duration <= 0.0 ? "-" : String.format(Locale.US, "%.2fms", duration);
    }

    private String getWebViewLabel() {
        return "reused".equals(webViewReuseState) ? "复用" : "新建";
    }

    private String safe(String value) {
        return TextUtils.isEmpty(value) ? "-" : value;
    }

    private void resetInternalClientWrappersIfSupported() {
        try {
            mView.getClass().getMethod("resetClientWrappers", Context.class).invoke(mView, getActivity());
            OfflineCacheLogger.log("进入页面", "复用WebView已重置内部ChromeClient和WebViewClient");
        } catch (Exception ignore) {
            OfflineCacheLogger.log("进入页面", "当前Basic SDK不支持重置内部ClientWrapper，已重新绑定Fragment的Client");
        }
    }
}
