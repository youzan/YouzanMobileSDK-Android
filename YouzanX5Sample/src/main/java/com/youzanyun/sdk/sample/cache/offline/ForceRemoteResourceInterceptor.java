package com.youzanyun.sdk.sample.cache.offline;

import android.content.Context;
import android.text.TextUtils;

import com.youzan.androidsdk.YouzanLog;
import com.youzanyun.sdk.sample.cache.WebResource;
import com.youzanyun.sdk.sample.cache.config.CacheConfig;
import com.youzanyun.sdk.sample.cache.config.MimeTypeFilter;
import com.youzanyun.sdk.sample.cache.loader.OkHttpResourceLoader;
import com.youzanyun.sdk.sample.cache.loader.ResourceLoader;
import com.youzanyun.sdk.sample.cache.loader.SourceRequest;

/**
 * Created by Ryan
 * at 2019/9/27
 */
public class ForceRemoteResourceInterceptor implements Destroyable, ResourceInterceptor {

    private ResourceLoader mResourceLoader;
    private MimeTypeFilter mMimeTypeFilter;

    ForceRemoteResourceInterceptor(Context context, CacheConfig cacheConfig) {
        mResourceLoader = new OkHttpResourceLoader(context);
        mMimeTypeFilter = cacheConfig != null ? cacheConfig.getFilter() : null;
    }

    @Override
    public WebResource load(Chain chain) {
        CacheRequest request = chain.getRequest();
        String mime = request.getMime();
        boolean isFilter;
        if (TextUtils.isEmpty(mime)) {
            isFilter = isFilterHtml();
        } else {
            isFilter = mMimeTypeFilter.isFilter(mime);
        }


        SourceRequest sourceRequest = new SourceRequest(request, isFilter);
        WebResource resource = mResourceLoader.getResource(sourceRequest);
        if (resource != null) {
            YouzanLog.addLog(YouzanLog.S_EVENT_TYPE_OFFLINE, "命中 Okhttp, request url = " + request.getUrl()  + "mimeType = " + mime );
            return resource;
        }
        return chain.process(request);
    }

    @Override
    public void destroy() {
        if (mMimeTypeFilter != null) {
            mMimeTypeFilter.clear();
        }
    }

    private boolean isFilterHtml() {
        return mMimeTypeFilter.isFilter("text/html");
    }
}
