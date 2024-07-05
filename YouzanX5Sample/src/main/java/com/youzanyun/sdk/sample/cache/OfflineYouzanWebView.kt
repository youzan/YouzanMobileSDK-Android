package com.youzanyun.sdk.sample.cache

import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import android.text.TextUtils
import android.util.AttributeSet
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.youzan.androidsdk.tool.WebUtil
import com.youzan.androidsdkx5.YouzanBrowser
import com.youzan.androidsdkx5.cache.WebResourceRequestAdapter
import com.youzan.androidsdkx5.cache.WebResourceResponseAdapter

/**
 * auther: liusaideng
 * created on :  2024/7/5 10:27
 * desc:
 */
class OfflineYouzanWebView : YouzanBrowser {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
//    private fun loadFromWebViewCache(request: WebResourceRequest): WebResourceResponse? {
//        val scheme = request.url.scheme!!.trim { it <= ' ' }
//        val method = request.method.trim { it <= ' ' }
//        if ((TextUtils.equals(WebUtil.SCHEME_HTTP, scheme)
//                    || TextUtils.equals(WebUtil.SCHEME_HTTPS, scheme))
//            && method.equals(WebUtil.METHOD_GET, ignoreCase = true)
//        ) {
//            val resourceResponse: android.webkit.WebResourceResponse =
//                mWebViewCache.getResource(WebResourceRequestAdapter.adapter(request), mWebSetting.getCacheMode(), mWebSetting.getUserAgentString())
//            if (resourceResponse != null) {
//                return WebResourceResponseAdapter.adapter(resourceResponse)
//            }
//        }
//        return null
//    }
}