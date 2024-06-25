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
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.Toast
import com.tencent.smtt.export.external.interfaces.WebResourceError
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.WebSettings
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import com.youzan.androidsdk.YouzanLog
import com.youzan.androidsdk.event.*
import com.youzan.androidsdk.model.goods.GoodsShareModel
import com.youzan.androidsdk.model.refresh.RefreshChangeModel
import com.youzan.androidsdk.model.trade.TradePayFinishedModel
import com.youzan.androidsdkx5.YouzanBrowser
import com.youzan.androidsdkx5.YouzanPreloader
import com.youzan.androidsdkx5.compat.CompatWebChromeClient
import com.youzan.androidsdkx5.compat.VideoCallback
import com.youzan.androidsdkx5.compat.WebChromeClientConfig
import com.youzan.spiderman.cache.SpiderMan
import com.youzan.spiderman.html.HtmlHeader
import com.youzan.spiderman.html.HtmlStatistic
import com.youzanyun.sdk.sample.config.KaeConfig
import com.youzanyun.sdk.sample.helper.YouzanHelper
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.InputStream
import java.util.*


/**
 * 这里使用[WebViewFragment]对[WebView]生命周期有更好的管控.
 */
class YouzanFragment : WebViewFragment(), OnRefreshListener {
    private val client: OkHttpClient = OkHttpClient()
    private lateinit var mView: YouzanBrowser
    private val mRefreshLayout: SwipeRefreshLayout? = null
    private var mToolbar: Toolbar? = null

    companion object {
        private const val CODE_REQUEST_LOGIN = 0x1000

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
        view.findViewById<View>(R.id.back).setOnClickListener {
            onBackPressed()
        }
        setupViews(view)
        setupYouzan()
        val settings = webView.settings
        settings.cacheMode = WebSettings.LOAD_NO_CACHE

        val url : String? = arguments!!.getString(YouzanActivity.KEY_URL)
        if (url != null) {
            mView.loadUrl(url)
        }


        //加载H5时，开启默认loading
        //设置自定义loading图片
//        mView.setLoadingImage(R.mipmap.ic_launcher);
    }

    private fun setupViews(contentView: View) {
        //WebView
        mView = webView
        if (mView.getX5WebViewExtension() != null) {
            val data = Bundle()
            data.putBoolean("standardFullScreen", true) // true表示标准全屏，false表示X5全屏；不设置默认false，
            data.putBoolean("supportLiteWnd", true) // false：关闭小窗；true：开启小窗；不设置默认true，
            data.putInt("DefaultVideoScreen", 2) // 1：以页面内开始播放，2：以全屏开始播放；不设置默认：1
            mView.getX5WebViewExtension().invokeMiscMethod("setVideoParams", data)
        }
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

        mView.setWebChromeClient(object: CompatWebChromeClient(
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
        })

        mView.setWebViewClient(object : WebViewClient() {

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
            }

            private fun interceptHtmlRequest(context: Context, url: String): WebResourceResponse? {
                val statistic = HtmlStatistic(url)
                val htmlResponse = SpiderMan.getInstance().interceptHtml(context, url, statistic)
                if (htmlResponse != null) {
                    val webResourceResponse = WebResourceResponse(
                        "text/html", htmlResponse.encoding, htmlResponse.contentStream
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        webResourceResponse.responseHeaders = HtmlHeader.transferHeaderMapList(htmlResponse.header) // add response header
                    }
                    return webResourceResponse
                }
                return null
            }
//
//            override fun shouldInterceptRequest(webView: WebView?, s: String?): WebResourceResponse? {
//                return WebResourceResponseAdapter.adapter(WebViewCacheInterceptorInst.getInstance().interceptRequest(s))
//            }
//            https://shop139935761.youzan.com/wscuser/membercenter?alias=Qn7FnnQwAB&reft=1715852464115&spm=f.131511492
//             "https://shop139935761.youzan.com/wscuser/membercenter?alias=Qn7FnnQwAB&reft=1715852464115&spm=f.131511492"
//             "https://shop139935761.youzan.com/wscuser/membercenter?alias=Qn7FnnQwAB&reft=1715852656259&spm=f.131511492"
            @TargetApi(21)
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString()
                if(url == KaeConfig.S_URL_MAIN ) {
                    YouzanLog.addSDKLog( "1. 命中的intercept html:$url")
                    val  res: WebResourceResponse? =  interceptHtmlRequest(view.context, url)
                    if (res != null) {
                        YouzanLog.addSDKLog( "2. intercept html:$url")
                        return res
                    } else {
                        YouzanPreloader.preloadHtml(view.context, url);
                    }
                }

//                return WebResourceResponseAdapter.adapter(WebViewCacheInterceptorInst.getInstance().
//                interceptRequest(WebResourceRequestAdapter.adapter(request)));
//
//
//                //                Logger.e(TAG, "intercept request, url:" + url);
//                if (url == KaeConfig.S_URL_MAIN || url.endsWith(".js") || url.endsWith(".css")) { // html
//                    YouzanLog.addSDKLog( "1. 命中的intercept html:$url")
//                    // may call multi time with one html url, so the stream is cannot be used in two clients
//                    val  res: WebResourceResponse? =  interceptHtmlRequest(view.context, url)
//                    if (res != null) {
//                        YouzanLog.addSDKLog( "2. intercept html:$url")
//                        return res
//                    } else {
//                        YouzanPreloader.preloadHtml(view.context, url);
//                    }
//                    return super.shouldInterceptRequest(view, url)
//                }

                return super.shouldInterceptRequest(view, request)
            }
        })
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

    override fun getWebViewId(): Int {
        //YouzanBrowser在布局文件中的id
//        return 0
        return R.id.view
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
}

class WebResourceResponseAdapter private constructor(private val mWebResourceResponse: android.webkit.WebResourceResponse) : WebResourceResponse() {
    override fun getMimeType(): String {
        return mWebResourceResponse.mimeType
    }

    override fun getData(): InputStream {
        return mWebResourceResponse.data
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    override fun getStatusCode(): Int {
        return mWebResourceResponse.statusCode
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun getResponseHeaders(): Map<String, String> {
        return mWebResourceResponse.responseHeaders
    }

    override fun getEncoding(): String {
        return mWebResourceResponse.encoding
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun getReasonPhrase(): String {
        return mWebResourceResponse.reasonPhrase
    }

    companion object {
        fun adapter(webResourceResponse: android.webkit.WebResourceResponse?): WebResourceResponseAdapter? {
            return webResourceResponse?.let { WebResourceResponseAdapter(it) }
        }
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
        fun adapter(x5Request: WebResourceRequest): WebResourceRequestAdapter {
            return WebResourceRequestAdapter(x5Request)
        }
    }
}


