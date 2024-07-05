package com.youzanyun.sdk.sample.cache;

import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import com.youzanyun.sdk.sample.cache.api.FastOpenApi;
import com.youzanyun.sdk.sample.cache.offline.Destroyable;

/**
 * Created by Ryan
 * 2018/2/7 下午5:06
 */
public interface WebViewCache extends FastOpenApi, Destroyable {

    WebResourceResponse getResource(WebResourceRequest request, int webViewCacheMode, String userAgent);

}
