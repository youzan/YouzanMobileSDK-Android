package com.youzanyun.sdk.sample.cache.api;

import com.youzanyun.sdk.sample.cache.config.CacheConfig;
import com.youzanyun.sdk.sample.cache.config.FastCacheMode;
import com.youzanyun.sdk.sample.cache.offline.ResourceInterceptor;

/**
 * Created by Ryan
 * at 2019/11/1
 */
public interface FastOpenApi {

    void setCacheMode(FastCacheMode mode, CacheConfig cacheConfig);

    void addResourceInterceptor(ResourceInterceptor interceptor);
}
