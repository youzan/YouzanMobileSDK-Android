package com.youzanyun.sdk.sample.cache.offline;

import android.content.Context;

import com.youzan.androidsdk.YouzanLog;
import com.youzanyun.sdk.sample.cache.WebResource;
import com.youzanyun.sdk.sample.cache.config.CacheConfig;
import com.youzanyun.sdk.sample.cache.loader.OkHttpResourceLoader;
import com.youzanyun.sdk.sample.cache.loader.ResourceLoader;
import com.youzanyun.sdk.sample.cache.loader.SourceRequest;

/**
 * Created by Ryan
 * at 2019/9/27
 */
public class ForceRemoteResourceInterceptor implements Destroyable, ResourceInterceptor {

    private ResourceLoader mResourceLoader;

    ForceRemoteResourceInterceptor(Context context, CacheConfig cacheConfig) {
        mResourceLoader = new OkHttpResourceLoader(context);
    }

    @Override
    public WebResource load(Chain chain) {
        CacheRequest request = chain.getRequest();
        String mime = request.getMime();
        boolean isFilter = false;


        SourceRequest sourceRequest = new SourceRequest(request, isFilter);
        WebResource resource = mResourceLoader.getResource(sourceRequest);
        if (resource != null) {
            request.setResolvedSource(sourceRequest.getResolvedSource());
            YouzanLog.addLog(YouzanLog.S_EVENT_TYPE_OFFLINE, "命中 Okhttp, request url = " + request.getUrl()  + "mimeType = " + mime );
            return resource;
        }
        return chain.process(request);
    }

    @Override
    public void destroy() {
    }

}
