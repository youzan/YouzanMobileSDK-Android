package com.youzanyun.sdk.sample.cache.offline;

import android.content.Context;

import com.youzanyun.sdk.sample.cache.WebResource;
import com.youzanyun.sdk.sample.cache.loader.OkHttpResourceLoader;
import com.youzanyun.sdk.sample.cache.loader.ResourceLoader;
import com.youzanyun.sdk.sample.cache.loader.SourceRequest;

/**
 * Created by Ryan
 * at 2019/9/27
 */
public class DefaultRemoteResourceInterceptor implements ResourceInterceptor {

    private ResourceLoader mResourceLoader;

    DefaultRemoteResourceInterceptor(Context context) {
        mResourceLoader = new OkHttpResourceLoader(context);
    }

    @Override
    public WebResource load(Chain chain) {
        CacheRequest request = chain.getRequest();
        SourceRequest sourceRequest = new SourceRequest(request, true);
        WebResource resource = mResourceLoader.getResource(sourceRequest);
        if (resource != null) {
            return resource;
        }
        return chain.process(request);
    }
}
