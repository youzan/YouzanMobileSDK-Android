package com.youzanyun.sdk.sample.cache.offline;


import com.youzan.androidsdk.YouzanLog;
import com.youzanyun.sdk.sample.cache.OfflineCacheLogger;
import com.youzanyun.sdk.sample.cache.WebResource;
import com.youzanyun.sdk.sample.cache.config.CacheConfig;
import android.support.v4.util.LruCache;

/**
 * Created by Ryan
 * on 2020/4/14
 */
public class MemResourceInterceptor implements ResourceInterceptor, Destroyable {

    private LruCache<String, WebResource> mLruCache;
    private CacheConfig mCacheConfig;

    public MemResourceInterceptor(CacheConfig cacheConfig) {
        mCacheConfig = cacheConfig;
        int memorySize = cacheConfig.getMemCacheSize();
        if (memorySize > 0) {
            mLruCache = new ResourceMemCache(memorySize);
        }
    }

    @Override
    public WebResource load(Chain chain) {
        CacheRequest request = chain.getRequest();
        if (mLruCache != null) {
            WebResource resource = mLruCache.get(request.getKey());
            if (checkResourceValid(resource) && shouldUseMemoryCache(request, resource)) {
                YouzanLog.addLog(YouzanLog.S_EVENT_TYPE_OFFLINE, "命中 mem cache, request url = " + request.getUrl() );
                OfflineCacheLogger.log("资源来源", "本次走内存缓存，类型=" + getMemoryType(request, resource) + "，url=" + request.getUrl());
                resource.setSource(WebResource.SOURCE_MEMORY);
                resource.setSourceDetail(getMemoryType(request, resource) + "_memory");
                request.setResolvedSource(WebResource.SOURCE_MEMORY);
                return resource;
            }
        }
        WebResource resource = chain.process(request);
        if (mLruCache != null && checkResourceValid(resource) && resource.isCacheable()) {
            String mime = request.getMime();
            if (!resource.isLargeEnoughToCache()) {
                OfflineCacheLogger.log("资源写入", "资源体积小于等于10KB，跳过内存缓存，size=" + resource.getOriginBytes().length + "，url=" + request.getUrl());
            } else if (shouldSaveToMemory(mime, getContentType(resource))) {
                OfflineCacheLogger.log("资源写入", "写入内存缓存，类型=" + getMemoryType(request, resource) + "，url=" + request.getUrl());
                mLruCache.put(request.getKey(), resource);
            }
        }
        return resource;
    }

    private boolean checkResourceValid(WebResource resource) {
        return resource != null
                && resource.getOriginBytes() != null
                && resource.getOriginBytes().length >= 0
                && resource.getResponseHeaders() != null
                && !resource.getResponseHeaders().isEmpty();
    }

    private String getMemoryType(CacheRequest request, WebResource resource) {
        if (isHtml(request.getMime()) || isHtml(getContentType(resource))) {
            return "html";
        }
        if (isImage(request.getMime()) || isImage(getContentType(resource))) {
            return "image";
        }
        return "js_css";
    }

    private String getContentType(WebResource resource) {
        if (resource == null || resource.getResponseHeaders() == null) {
            return null;
        }
        String value = resource.getResponseHeaders().get("Content-Type");
        if (value == null) {
            value = resource.getResponseHeaders().get("content-type");
        }
        if (value == null) {
            return null;
        }
        String[] values = value.split(";");
        return values.length > 0 ? values[0].trim() : value.trim();
    }

    private boolean isJavaScriptOrCss(String mime) {
        return mime != null && (mime.contains("javascript") || mime.contains("css"));
    }

    private boolean isHtml(String mime) {
        return mime != null && "text/html".equalsIgnoreCase(mime);
    }

    private boolean isImage(String mime) {
        return mime != null && mime.startsWith("image");
    }

    private boolean shouldSaveToMemory(String requestMime, String realMime) {
        if (isHtml(requestMime) || isHtml(realMime)) {
            return true;
        }
        if (isJavaScriptOrCss(requestMime) || isJavaScriptOrCss(realMime)) {
            return mCacheConfig == null || mCacheConfig.isEnableJsCssCache();
        }
        return (isImage(requestMime) || isImage(realMime))
                && mCacheConfig != null
                && mCacheConfig.isEnableImageCache();
    }

    private boolean shouldUseMemoryCache(CacheRequest request, WebResource resource) {
        return shouldSaveToMemory(request.getMime(), getContentType(resource));
    }

    @Override
    public void destroy() {
        if (mLruCache != null) {
            mLruCache.evictAll();
            mLruCache = null;
        }
    }

    private static class ResourceMemCache extends LruCache<String, WebResource> {

        /**
         * @param maxSize for caches that do not override {@link #sizeOf}, this is
         *                the maximum number of entries in the cache. For all other caches,
         *                this is the maximum sum of the sizes of the entries in this cache.
         */
        ResourceMemCache(int maxSize) {
            super(maxSize);
        }

        @Override
        protected int sizeOf(String key, WebResource value) {
            int size = 0;
            if (value != null && value.getOriginBytes() != null) {
                size = value.getOriginBytes().length;
            }
            return size;
        }
    }
}
