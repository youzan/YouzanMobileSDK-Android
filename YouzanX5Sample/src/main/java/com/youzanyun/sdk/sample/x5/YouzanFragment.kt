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
package com.youzanyun.sdk.sample.x5

import android.annotation.TargetApi
import android.app.Activity
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback
import com.tencent.smtt.export.external.interfaces.JsPromptResult
import com.tencent.smtt.export.external.interfaces.WebResourceError
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import com.youzan.androidsdk.event.*
import com.youzan.androidsdk.model.goods.GoodsShareModel
import com.youzan.androidsdk.model.refresh.RefreshChangeModel
import com.youzan.androidsdk.model.trade.TradePayFinishedModel
import com.youzan.androidsdkx5.YouzanBrowser
import com.youzan.androidsdkx5.compat.CompatWebChromeClient
import com.youzan.androidsdkx5.compat.VideoCallback
import com.youzan.androidsdkx5.compat.WebChromeClientConfig
import com.youzan.spiderman.cache.SpiderMan
import com.youzan.spiderman.html.HtmlHeader
import com.youzan.spiderman.html.HtmlStatistic
import com.youzanyun.sdk.sample.cache.OfflineCacheLogger
import com.youzanyun.sdk.sample.helper.YouzanHelper
import kotlinx.android.synthetic.main.activity_splash.*
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*


/**
 * 这里使用[WebViewFragment]对[WebView]生命周期有更好的管控.
 */
class YouzanFragment : WebViewFragment(), OnRefreshListener {
    private val timingPromptPrefix = "yz_timing://report?data="
    private lateinit var mView: YouzanBrowser
    private val mRefreshLayout: SwipeRefreshLayout? = null
    private var mToolbar: Toolbar? = null
    private var geolocationCallback: GeolocationPermissionsCallback? = null
    private var geolocationOrigin: String? = null
    private var webViewReuseState: String = "unknown"
    private var pendingTargetUrl: String? = null
    private var metricsView: TextView? = null
    @Volatile private var cachedWebViewCacheMode: Int = WebSettings.LOAD_DEFAULT
    @Volatile private var cachedUserAgent: String? = null
    private var pageStartAt = 0L
    private var firstProgressAt = 0L
    private var pageFinishAt = 0L
    private var pageUrl: String? = null
    private var jsTimingText: String? = null

    companion object {
        private const val CODE_REQUEST_LOGIN = 0x1000
        private const val CODE_REQUEST_GEOLOCATION = 0x1001

        fun newInstance(url: String): Fragment {
            val fg = YouzanFragment()
            fg.arguments = Bundle().apply {
                putString(YouzanActivity.KEY_URL, url)
            }
            return fg
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
        view.findViewById<View>(R.id.back).setOnClickListener {
            onBackPressed()
        }
        setupViews(view)
        setupYouzan()
        val settings = webView.settings
        settings.cacheMode = WebSettings.LOAD_NO_CACHE
        settings.setGeolocationEnabled(true)
        cachedWebViewCacheMode = settings.cacheMode
        cachedUserAgent = settings.userAgentString
        metricsView?.text = buildMetricsText("等待加载")

        var url : String? = arguments!!.getString(YouzanActivity.KEY_URL)
        if (android.text.TextUtils.isEmpty(url)) {
            url = com.youzanyun.sdk.sample.config.KaeConfig.S_URL_MAIN
        }
        pendingTargetUrl = url
        if (url != null) {
            resetMetricsForNewLoad(url)
            mView.loadUrl(url)
        }


        //加载H5时，开启默认loading
        //设置自定义loading图片
//        mView.setLoadingImage(R.mipmap.ic_launcher);
    }

    private fun setupViews(contentView: View) {
        //WebView
        mView = webView
        if (webViewReuseState == "reused") {
            mView.resetClientWrappers(activity)
            OfflineCacheLogger.log("进入页面", "复用WebView已重置内部ChromeClient和WebViewClient")
        }
        if (mView.getX5WebViewExtension() != null) {
            val data = Bundle()
            data.putBoolean("standardFullScreen", true) // true表示标准全屏，false表示X5全屏；不设置默认false，
            data.putBoolean("supportLiteWnd", true) // false：关闭小窗；true：开启小窗；不设置默认true，
            data.putInt("DefaultVideoScreen", 2) // 1：以页面内开始播放，2：以全屏开始播放；不设置默认：1
            mView.getX5WebViewExtension().invokeMiscMethod("setVideoParams", data)
        }
        metricsView = contentView.findViewById(R.id.tv_page_metrics)
        mToolbar = contentView.findViewById<View>(R.id.toolbar) as Toolbar
        //        mRefreshLayout = (SwipeRefreshLayout) contentView.findViewById(R.id.swipe);

//        mView.setSaveImageListener(object : SaveImageListener {
//            override fun onSaveImage(result: WebView.HitTestResult?): Boolean {
//                // 长按保存图片流程
//                return  true
//            }
//        })

        //分享按钮
        mToolbar!!.setTitle(R.string.loading_page)
        mToolbar!!.inflateMenu(R.menu.menu_youzan_share)
        mToolbar!!.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_share -> {
                    mView.sharePage()
                    true
                }
                R.id.action_refresh -> {
                    mView.reload()
                    true
                }
                R.id.action_print_timing -> {
                    printPageTiming()
                    true
                }
                else -> false
            }
        }

        //刷新
//        mRefreshLayout.setOnRefreshListener(this);
//        mRefreshLayout.setColorSchemeColors(Color.BLUE, Color.RED);
//        mRefreshLayout.setEnabled(false);
//        mView.setWebChromeClient(object : WebChromeClient() {
//            override fun onShowCustomView(view: View, customViewCallback: IX5WebChromeClient.CustomViewCallback) {
//                super.onShowCustomView(view, customViewCallback)
//                customViewCallback.onCustomViewHidden() // 避免视频未播放时，点击全屏白屏的问题
//            }
//
//
//        })

        mView.setWebChromeClient(createFragmentChromeClient())

        mView.setWebViewClient(createFragmentWebViewClient())
    }

    private fun createFragmentChromeClient(): CompatWebChromeClient {
        return object: CompatWebChromeClient(
            WebChromeClientConfig(
                true, object : VideoCallback {
                    override fun onVideoCallback(b: Boolean) {
                        Toast.makeText(activity, "" + b, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        ) {
            override fun onReceivedTitle(p0: WebView?, p1: String?) {
                super.onReceivedTitle(p0, p1)
                mToolbar?.title = p1
            }

            override fun onJsPrompt(
                view: WebView?,
                url: String?,
                message: String?,
                defaultValue: String?,
                result: JsPromptResult?
            ): Boolean {
                if (!message.isNullOrBlank() && handleTimingPrompt(message)) {
                    result?.confirm("")
                    return true
                }
                return super.onJsPrompt(view, url, message, defaultValue, result)
            }

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress > 0 && firstProgressAt == 0L) {
                    firstProgressAt = SystemClock.elapsedRealtime()
                    updateMetrics("开始渲染")
                }
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: GeolocationPermissionsCallback?
            ) {
                if (origin == null || callback == null) {
                    return
                }
                if (hasLocationPermission()) {
                    callback.invoke(origin, true, false)
                    return
                }
                geolocationOrigin = origin
                geolocationCallback = callback
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), CODE_REQUEST_GEOLOCATION)
            }
        }
    }

    private fun createFragmentWebViewClient(): WebViewClient {
        return object : WebViewClient() {
            override fun onReceivedError(p0: WebView?, p1: WebResourceRequest?, p2: WebResourceError?) {
                super.onReceivedError(p0, p1, p2)
            }

            override fun shouldOverrideUrlLoading(p0: WebView?, p1: WebResourceRequest?): Boolean {
                return super.shouldOverrideUrlLoading(p0, p1)
            }
            override fun onPageFinished(p0: WebView?, p1: String?) {
                super.onPageFinished(p0, p1)
                Log.d("lsd", "onPageFinished")

            }

            override fun onPageStarted(p0: WebView?, p1: String?, p2: Bitmap?) {
                super.onPageStarted(p0, p1, p2)
                Log.d("lsd", "onPageStarted")
                Toast.makeText(activity, "onPageStarted", Toast.LENGTH_SHORT).show()
                pageUrl = p1
                pageStartAt = SystemClock.elapsedRealtime()
                firstProgressAt = 0L
                pageFinishAt = 0L
                jsTimingText = null
                updateMetrics("开始加载")
            }


            @TargetApi(21)
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                if (!SplashActivity.isCacheEnabled() || !SplashActivity.isReuseResourceEnabled()) {
                    OfflineCacheLogger.log("资源分发", "总开关=${SplashActivity.isCacheEnabled()}，资源复用=${SplashActivity.isReuseResourceEnabled()}，直接交给 WebView 处理，url=${request.url}")
                    return super.shouldInterceptRequest(view, request)
                }
                var cacheImpl = com.youzanyun.sdk.sample.cache.WebViewPreloadManager.getCacheImpl()
                if (cacheImpl == null) {
                    val appContext = activity?.applicationContext
                    if (appContext == null) {
                        OfflineCacheLogger.log("资源分发", "页面Context为空，交给WebView自行请求，url=${request.url}")
                        return super.shouldInterceptRequest(view, request)
                    }
                    com.youzanyun.sdk.sample.cache.WebViewPreloadManager.preload(
                        appContext,
                        com.youzanyun.sdk.sample.config.KaeConfig.S_URL_MAIN,
                        com.youzanyun.sdk.sample.MyApplication.HTML_CACHE_URLS
                    )
                    cacheImpl = com.youzanyun.sdk.sample.cache.WebViewPreloadManager.getCacheImpl()
                }
                if (cacheImpl != null) {
                    val scheme = request.url.scheme?.trim()
                    val method = request.method?.trim()
                    if ((android.text.TextUtils.equals("http", scheme) || android.text.TextUtils.equals("https", scheme))
                        && method.equals("GET", ignoreCase = true)
                    ) {
                        val adapterRequest = WebResourceRequestAdapter.adapter(request)
                        val resourceResponse = adapterRequest?.let {
                            cacheImpl.getResource(it, cachedWebViewCacheMode, cachedUserAgent)
                        }
                        if (resourceResponse != null) {
                            val source = cacheImpl.lastResourceSource ?: "offline_unknown"
                            if (source == com.youzanyun.sdk.sample.cache.WebResource.SOURCE_MEMORY
                                || source == com.youzanyun.sdk.sample.cache.WebResource.SOURCE_NETWORK
                                || source == com.youzanyun.sdk.sample.cache.WebResource.SOURCE_DISK
                            ) {
                                OfflineCacheLogger.log(
                                    "资源分发",
                                    "Fragment返回缓存结果，来源=$source，url=${request.url}，webView=${if (webViewReuseState == "reused") "复用" else "新建"}"
                                )
                                return WebResourceResponseAdapter.adapter(resourceResponse)
                            }
                            OfflineCacheLogger.log(
                                "资源分发",
                                "本次来源=$source，交给WebView自行请求，url=${request.url}，webView=${if (webViewReuseState == "reused") "复用" else "新建"}"
                            )
                        } else {
                            OfflineCacheLogger.log(
                                "资源分发",
                                "Fragment未命中离线缓存，回退系统网络栈，url=${request.url}，webView=${if (webViewReuseState == "reused") "复用" else "新建"}"
                            )
                        }
                    }
                }

                val res = super.shouldInterceptRequest(view, request)
                return res;
            }
        }
    }

    private fun setupYouzan() {
        mView!!.subscribe(object : AbsCheckAuthMobileEvent() {})
        //认证事件, 回调表示: 需要需要新的认证信息传入
        mView!!.subscribe(object : AbsAuthEvent() {
            override fun call(context: Context, needLogin: Boolean) {
                /**
                 * 建议实现逻辑:
                 *
                 * 判断App内的用户是否登录?
                 * => 已登录: 请求带用户角色的认证信息(login接口);
                 * => 未登录: needLogin为true, 唤起App内登录界面, 请求带用户角色的认证信息(login接口);
                 * => 未登录: needLogin为false, 请求不带用户角色的认证信息(initToken接口).
                 *
                 * 服务端接入文档: https://www.youzanyun.com/docs/guide/appsdk/683
                 */
                //TODO 自行编码实现. 具体可参考开发文档中的伪代码实现
                //TODO 手机号自己填入
                YouzanHelper.loginYouzan(activity!!, {
                    mView.postDelayed({
                        mView.reload()
                    }, 500)

                })

            }
        })
        mView!!.subscribe(object : AbsCheckAuthMobileEvent() {})
        //文件选择事件, 回调表示: 发起文件选择. (如果app内使用的是系统默认的文件选择器, 该事件可以直接删除)
        mView!!.subscribe(object : AbsChooserEvent() {
            @kotlin.jvm.Throws(ActivityNotFoundException::class)
            override fun call(context: Context, intent: Intent, requestCode: Int) {
                startActivityForResult(intent, requestCode)
            }
        })

        mView!!.subscribe(object : AbsChangePullRefreshEvent() {
            override fun call(refreshChangeModel: RefreshChangeModel?) {
                if (refreshChangeModel != null && refreshChangeModel.enable != null) {
                    //新建收货地址页下滑与页面下拉刷新冲突时，禁止该页面下拉刷新
//                    mRefreshLayout.setEnabled(refreshChangeModel.getEnable());
                }
            }
        })

        //页面状态事件, 回调表示: 页面加载完成
        mView!!.subscribe(object : AbsStateEvent() {
            override fun call(context: Context) {
                mToolbar!!.title = mView!!.title
                //停止刷新
//                mRefreshLayout.setRefreshing(false);
//                mRefreshLayout.setEnabled(true);

                pageFinishAt = SystemClock.elapsedRealtime()
                OfflineCacheLogger.log(
                    "页面耗时",
                    "pageStart=${pageStartAt}，pageFinished=${pageFinishAt}，总耗时=${formatDuration(getPageTotalDuration())}，url=${pageUrl ?: "-"}，webView=${if (webViewReuseState == "reused") "复用" else "新建"}"
                )
                updateMetrics("页面完成")
            }
        })
        mView!!.subscribe(object : AbsCustomEvent() {
            override fun callAction(context: Context, action: String, data: String) {
                when (action) {
                    "openHome" ->                         //此处仅举例，具体实现根据对应需求做调整
                        try {
                            val jsonObject = JSONObject(data)
                            val paramObj = jsonObject.optJSONObject("params")
                            val result = paramObj.optString("test")
                            Toast.makeText(activity, "test:$result", Toast.LENGTH_LONG).show()
                        } catch (e: JSONException) {
                            throw RuntimeException(e)
                        }
                }
            }
        })
        //分享事件, 回调表示: 获取到当前页面的分享信息数据
        mView!!.subscribe(object : AbsShareEvent() {
            override fun call(context: Context, data: GoodsShareModel) {
                /**
                 * 在获取数据后, 可以使用其他分享SDK来提高分享体验.
                 * 这里调用系统分享来简单演示分享的过程.
                 */
                val content = data.desc + data.link
                val sendIntent = Intent()
                sendIntent.action = Intent.ACTION_SEND
                sendIntent.putExtra(Intent.EXTRA_TEXT, content)
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, data.title)
                sendIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                sendIntent.type = "text/plain"
                startActivity(sendIntent)
            }
        })
        mView!!.subscribe(object : AbsPaymentFinishedEvent() {
            override fun call(context: Context, tradePayFinishedModel: TradePayFinishedModel) {}
        })
    }

    override fun onResume() {
        super.onResume()
    }

    override fun createWebView(contentView: View): YouzanBrowser {
        val container = contentView.findViewById<android.widget.FrameLayout>(R.id.webview_container)
        val targetUrl = arguments?.getString(YouzanActivity.KEY_URL)
            ?: com.youzanyun.sdk.sample.config.KaeConfig.S_URL_MAIN
        val acquireResult = com.youzanyun.sdk.sample.cache.WebViewPreloadManager.getWebView(activity!!, targetUrl) // 使用 Activity Context 避免 X5 创建异常
        val browser = acquireResult.webView
        webViewReuseState = if (acquireResult.reused) "reused" else "new"
        OfflineCacheLogger.log("进入页面", "当前WebView实例=${if (webViewReuseState == "reused") "复用" else "新建"}，进入页面后加载目标URL=${targetUrl}")
        if (acquireResult.reused) {
            OfflineCacheLogger.log("进入页面", "复用实例来自缓存池，实例预创建阶段不加载URL，本次由Fragment重新加载目标URL")
        }
        Toast.makeText(activity, "WebView: $webViewReuseState", Toast.LENGTH_SHORT).show()
        container.addView(browser, android.widget.FrameLayout.LayoutParams(
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            android.widget.FrameLayout.LayoutParams.MATCH_PARENT
        ))
        return browser
    }

    override fun getWebViewId(): Int {
        return -1 // 不再通过 ID 获取
    }

    override fun getLayoutId(): Int {
        //布局文件
        return R.layout.fragment_youzan
    }

    override fun onBackPressed(): Boolean {
        //页面回退
        return mView.pageGoBack();
    }

    override fun onRefresh() {
        //重新加载页面
        mView!!.reload()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (CODE_REQUEST_LOGIN == requestCode) { // 如果是登录事件返回
            if (resultCode == Activity.RESULT_OK) {
                // 登录成功设置token
            } else {
                // 登录失败
                mView!!.syncNot()
            }
        } else if (mView!!.receiveFile(requestCode, data)){
            // return true 标识处理的上传了文件
        } else {

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != CODE_REQUEST_GEOLOCATION) {
            return
        }
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        geolocationCallback?.invoke(geolocationOrigin, granted, false)
        geolocationCallback = null
        geolocationOrigin = null
    }

    private fun hasLocationPermission(): Boolean {
        val context = context ?: return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateMetrics(stage: String) {
        metricsView?.text = buildMetricsText(stage)
    }

    private fun resetMetricsForNewLoad(url: String?) {
        pageUrl = url
        pageStartAt = 0L
        firstProgressAt = 0L
        pageFinishAt = 0L
        jsTimingText = null
        updateMetrics("等待加载")
    }

    private fun buildMetricsText(stage: String): String {
        val total = getPageTotalDuration()
        val firstProgress = if (pageStartAt > 0L && firstProgressAt > 0L) firstProgressAt - pageStartAt else 0L
        val nativeText = "页面耗时: 阶段=${stage} | 总耗时(pageStart-pageFinished)=${formatDuration(total)} | 首次渲染=${formatDuration(firstProgress)} | URL=${pageUrl ?: "-"}"
        val timingText = jsTimingText
        return if (timingText.isNullOrBlank()) nativeText else "$nativeText\nJS耗时: $timingText"
    }

    private fun getPageTotalDuration(): Long {
        return if (pageStartAt > 0L && pageFinishAt > 0L) pageFinishAt - pageStartAt else 0L
    }

    private fun formatDuration(duration: Long): String {
        return if (duration <= 0L) "-" else "${duration}ms"
    }

    private fun printPageTiming() {
        val total = getPageTotalDuration()
        val message = if (total > 0L) {
            "pageStart 到 pageFinished 总耗时=${formatDuration(total)}，url=${pageUrl ?: mView.url ?: "-"}，webView=${if (webViewReuseState == "reused") "复用" else "新建"}"
        } else {
            "暂无完整 pageStart/pageFinished 耗时，pageStart=${pageStartAt}，pageFinished=${pageFinishAt}，url=${pageUrl ?: mView.url ?: "-"}"
        }
        updateMetrics("手动打印耗时")
        OfflineCacheLogger.log("页面耗时", message)
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }

    private fun handleTimingPrompt(message: String): Boolean {
        if (!message.startsWith(timingPromptPrefix)) {
            return false
        }
        val encodedPayload = message.removePrefix(timingPromptPrefix)
        val payload = try {
            Uri.decode(encodedPayload)
        } catch (e: Exception) {
            null
        }
        if (payload == null) {
            jsTimingText = "解析失败"
            updateMetrics("JS耗时解析失败")
            OfflineCacheLogger.log("页面耗时", "JS耗时结果解析失败，message=$message")
            return true
        }
        applyTimingPayload(payload)
        return true
    }

    private fun applyTimingPayload(payload: String) {
        try {
            val jsonObject = JSONObject(payload)
            if (!jsonObject.optBoolean("supported", false)) {
                val reason = jsonObject.optString("reason", "当前页面暂不支持")
                jsTimingText = "不支持，原因=$reason"
                updateMetrics("JS耗时不可用")
                OfflineCacheLogger.log("页面耗时", "JS耗时不可用，原因=$reason")
                return
            }
            val metrics = jsonObject.optJSONObject("metrics")
            if (metrics == null) {
                jsTimingText = "未获取到统计结果"
                updateMetrics("JS耗时缺失")
                OfflineCacheLogger.log("页面耗时", "JS耗时结果为空")
                return
            }
            val summary = "总加载=${formatJsDuration(metrics.optDouble("total"))} | 白屏=${formatJsDuration(metrics.optDouble("whiteScreen"))} | TTFB=${formatJsDuration(metrics.optDouble("ttfb"))} | DOM可交互=${formatJsDuration(metrics.optDouble("interactive"))} | DOM完成=${formatJsDuration(metrics.optDouble("domComplete"))}"
            jsTimingText = summary
            updateMetrics("已打印JS耗时")
            OfflineCacheLogger.log("页面耗时", "JS耗时统计：$summary")
        } catch (e: Exception) {
            jsTimingText = "解析异常"
            updateMetrics("JS耗时解析异常")
            OfflineCacheLogger.log("页面耗时", "JS耗时解析异常，error=${e.message}")
        }
    }

    private fun formatJsDuration(duration: Double): String {
        return if (duration <= 0.0) "-" else "${String.format(Locale.US, "%.2f", duration)}ms"
    }
}

class WebResourceResponseAdapter private constructor(private val mWebResourceResponse: android.webkit.WebResourceResponse) : WebResourceResponse() {
    companion object {
        private const val DEFAULT_ENCODING = "utf-8"

        fun adapter(webResourceResponse: android.webkit.WebResourceResponse?): WebResourceResponseAdapter? {
            return webResourceResponse?.let { WebResourceResponseAdapter(it) }
        }
    }

    override fun getMimeType(): String {
        return mWebResourceResponse.mimeType ?: "text/plain"
    }

    override fun getData(): InputStream {
        return mWebResourceResponse.data ?: ByteArrayInputStream(ByteArray(0))
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun getStatusCode(): Int {
        return mWebResourceResponse.statusCode
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun getResponseHeaders(): Map<String, String> {
        return mWebResourceResponse.responseHeaders ?: emptyMap()
    }

    override fun getEncoding(): String {
        // System WebResourceResponse allows a null encoding, but X5 treats it as non-null.
        return mWebResourceResponse.encoding ?: DEFAULT_ENCODING
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun getReasonPhrase(): String {
        return mWebResourceResponse.reasonPhrase ?: "OK"
    }
}


@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class WebResourceRequestAdapter private constructor(private val mWebResourceRequest: WebResourceRequest) : android.webkit.WebResourceRequest {
    override fun getUrl(): Uri {
        return mWebResourceRequest.url
    }

    override fun isForMainFrame(): Boolean {
        return mWebResourceRequest.isForMainFrame
    }

    override fun isRedirect(): Boolean {
        return mWebResourceRequest.isRedirect
    }

    override fun hasGesture(): Boolean {
        return mWebResourceRequest.hasGesture()
    }

    override fun getMethod(): String {
        return mWebResourceRequest.method
    }

    override fun getRequestHeaders(): Map<String, String> {
        return mWebResourceRequest.requestHeaders
    }

    companion object {
        fun adapter(x5Request: WebResourceRequest?): WebResourceRequestAdapter? {
            return x5Request?.let { WebResourceRequestAdapter(it) }
        }
    }
}
