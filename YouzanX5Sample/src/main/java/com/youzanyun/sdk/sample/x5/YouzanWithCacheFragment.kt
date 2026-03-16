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

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import com.youzanyun.sdk.sample.cache.WebResCacheManager
import com.youzanyun.sdk.sample.cache.WebViewPreloadManager


/**
 * 这里使用[WebViewFragment]对[WebView]生命周期有更好的管控.
 */
class YouzanWithCacheFragment : androidx.fragment.app.Fragment() {
    companion object {
        fun newInstance(): androidx.fragment.app.Fragment {
            return YouzanWithCacheFragment()
        }
    }

    val key = "youzan_webview_cache_${System.currentTimeMillis()}"


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.post {
            val webView = WebViewPreloadManager.getWebView(key, activity)
            webView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            view.findViewById<ViewGroup>(R.id.root_layout).addView(webView)
            webView.webViewClient = CacheWebClient()
        }
    }

}


class CacheWebClient: WebViewClient() {
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        return WebResourceResponseAdapter.adapter(WebResCacheManager.shouldInterceptRequest(request.url.toString()))
    }
}


