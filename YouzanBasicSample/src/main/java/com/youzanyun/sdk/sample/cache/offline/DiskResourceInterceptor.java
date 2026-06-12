package com.youzanyun.sdk.sample.cache.offline;

import android.text.TextUtils;

import com.youzan.androidsdk.YouzanLog;
import com.youzanyun.sdk.sample.cache.OfflineCacheLogger;
import com.youzanyun.sdk.sample.cache.WebResource;
import com.youzanyun.sdk.sample.cache.config.CacheConfig;
import com.youzan.androidsdk.utils.HeaderUtils;
import com.youzan.androidsdk.utils.LogUtils;
import com.youzan.androidsdk.utils.StreamUtils;
import com.youzan.androidsdk.utils.lru.DiskLruCache;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import okhttp3.Headers;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

/**
 * Created by Ryan
 * at 2019/9/27
 */
public class DiskResourceInterceptor implements Destroyable, ResourceInterceptor {

    private static final int ENTRY_META = 0;
    private static final int ENTRY_BODY = 1;
    private static final int ENTRY_COUNT = 2;
    public static final int CACHE_TYPE_JS_CSS = 1;
    public static final int CACHE_TYPE_IMAGE = 2;
    private DiskLruCache mDiskLruCache;
    private CacheConfig mCacheConfig;
    private final int mCacheType;

    DiskResourceInterceptor(CacheConfig cacheConfig, int cacheType) {
        mCacheConfig = cacheConfig;
        mCacheType = cacheType;
    }

    private synchronized void ensureDiskLruCacheCreate() {
        if (mDiskLruCache != null && !mDiskLruCache.isClosed()) {
            return;
        }
        String dir = getDiskCacheDir();
        int version = mCacheConfig.getVersion();
        long cacheSize = getDiskCacheSize();
        try {
            mDiskLruCache = DiskLruCache.open(new File(dir), version, ENTRY_COUNT, cacheSize);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private WebResource getFromDiskCache(String key) {
        try {
            if (mDiskLruCache.isClosed()) {
                return null;
            }
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
            if (snapshot != null) {
                BufferedSource entrySource = Okio.buffer(Okio.source(snapshot.getInputStream(ENTRY_META)));
                // 1. read status
                String responseCode = entrySource.readUtf8LineStrict();
                String reasonPhrase = entrySource.readUtf8LineStrict();
                // 2. read headers
                long headerSize = entrySource.readDecimalLong();
                Map<String, String> headers;
                Headers.Builder responseHeadersBuilder = new Headers.Builder();
                // read first placeholder line
                String placeHolder = entrySource.readUtf8LineStrict();
                if (!TextUtils.isEmpty(placeHolder.trim())) {
                    responseHeadersBuilder.add(placeHolder);
                    headerSize--;
                }
                for (int i = 0; i < headerSize; i++) {
                    String line = entrySource.readUtf8LineStrict();
                    if (!TextUtils.isEmpty(line)) {
                        responseHeadersBuilder.add(line);
                    }
                }
                headers = HeaderUtils.generateHeadersMap(responseHeadersBuilder.build());
                // 3. read body
                InputStream inputStream = snapshot.getInputStream(ENTRY_BODY);
                if (inputStream != null) {
                    WebResource webResource = new WebResource();
                    webResource.setReasonPhrase(reasonPhrase);
                    webResource.setResponseCode(Integer.valueOf(responseCode));
                    webResource.setOriginBytes(StreamUtils.streamToBytes(inputStream));
                    webResource.setResponseHeaders(headers);
                    webResource.setModified(false);
                    return webResource;
                }
                snapshot.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    @Override
    public WebResource load(Chain chain) {
        CacheRequest request = chain.getRequest();
        if (!isCacheTypeEnabled()) {
            return chain.process(request);
        }
        ensureDiskLruCacheCreate();
        WebResource webResource = getFromDiskCache(request.getKey());
        if (webResource != null && isCacheableForThisInterceptor(request, webResource)) {
            LogUtils.d(String.format("disk cache hit: %s", request.getUrl()));
            webResource.setSource(WebResource.SOURCE_DISK);
            webResource.setSourceDetail(getSourceDetailLabel());
            request.setResolvedSource(WebResource.SOURCE_DISK);
            OfflineCacheLogger.log("资源来源", "本次走磁盘缓存，类型=" + getTypeCode() + "，url=" + request.getUrl());
            YouzanLog.addLog(YouzanLog.S_EVENT_TYPE_OFFLINE, "命中 disk cache, request url = " + request.getUrl() );
            return webResource;
        }
        webResource = chain.process(request);
        if (webResource != null && shouldSaveToDisk(request, webResource)) {
            if (!webResource.isLargeEnoughToCache()) {
                OfflineCacheLogger.log("资源写入", "资源体积小于等于10KB，跳过磁盘缓存，类型=" + getTypeCode() + "，size=" + webResource.getOriginBytes().length + "，url=" + request.getUrl());
            } else {
                OfflineCacheLogger.log("资源写入", "写入磁盘缓存，类型=" + getTypeCode() + "，url=" + request.getUrl());
                cacheToDisk(request.getKey(), webResource);
            }
        }
        return webResource;
    }

    @Override
    public void destroy() {
        if (mDiskLruCache != null && !mDiskLruCache.isClosed()) {
            try {
                mDiskLruCache.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void cacheToDisk(String key, WebResource webResource) {
        if (webResource == null || !webResource.isCacheable()) {
            return;
        }
        if (mDiskLruCache.isClosed()) {
            return;
        }
        try {
            DiskLruCache.Editor editor = mDiskLruCache.edit(key);
            if (editor == null) {
                LogUtils.d("Another edit is in progress!");
                return;
            }
            OutputStream metaOutput = editor.newOutputStream(ENTRY_META);
            BufferedSink sink = Okio.buffer(Okio.sink(metaOutput));
            // 1. write status
            sink.writeUtf8(String.valueOf(webResource.getResponseCode())).writeByte('\n');
            sink.writeUtf8(webResource.getReasonPhrase()).writeByte('\n');
            // 2. write response header
            Map<String, String> headers = webResource.getResponseHeaders();
            sink.writeDecimalLong(headers.size()).writeByte('\n');
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String headerKey = entry.getKey();
                String headerValue = entry.getValue();
                sink.writeUtf8(headerKey)
                        .writeUtf8(": ")
                        .writeUtf8(headerValue)
                        .writeByte('\n');
            }
            sink.flush();
            sink.close();
            // 3. write response body
            OutputStream bodyOutput = editor.newOutputStream(ENTRY_BODY);
            sink = Okio.buffer(Okio.sink(bodyOutput));
            byte[] originBytes = webResource.getOriginBytes();
            if (originBytes != null && originBytes.length > 0) {
                sink.write(originBytes);
                sink.flush();
                editor.commit();
            }
            sink.close();
        } catch (IOException e) {
            LogUtils.e("cache to disk failed. cause by: " + e.getMessage());
            try {
                // clean the redundant data
                mDiskLruCache.remove(key);
            } catch (IOException ignore) {
            }
        } catch (Exception e) {
            LogUtils.e(e.getMessage());
        }
    }

    private boolean isRealMimeTypeCacheable(WebResource resource) {
        if (resource == null) {
            return false;
        }
        Map<String, String> headers = resource.getResponseHeaders();
        String contentType = null;
        if (headers != null) {
            String uppercaseKey = "Content-Type";
            String lowercaseKey = uppercaseKey.toLowerCase();
            String contentTypeValue = headers.containsKey(uppercaseKey) ? headers.get(uppercaseKey) : headers.get(lowercaseKey);
            if (!TextUtils.isEmpty(contentTypeValue)) {
                String[] contentTypeArray = contentTypeValue.split(";");
                if (contentTypeArray.length >= 1) {
                    contentType = contentTypeArray[0];
                }
            }
        }
        return contentType != null && !mCacheConfig.getFilter().isFilter(contentType);
    }

    private boolean shouldSaveToDisk(CacheRequest request, WebResource resource) {
        if (!(resource.isCacheByOurselves() || isRealMimeTypeCacheable(resource))) {
            return false;
        }
        if (mCacheType == CACHE_TYPE_JS_CSS) {
            return isJavaScriptOrCss(request.getMime()) || isJavaScriptOrCss(getContentType(resource));
        }
        return isImageMime(request.getMime()) || isImageMime(getContentType(resource));
    }

    private boolean isCacheableForThisInterceptor(CacheRequest request, WebResource resource) {
        if (mCacheType == CACHE_TYPE_JS_CSS) {
            return isRealMimeTypeCacheable(resource) && (isJavaScriptOrCss(request.getMime()) || isJavaScriptOrCss(getContentType(resource)));
        }
        return isRealMimeTypeCacheable(resource) && (isImageMime(request.getMime()) || isImageMime(getContentType(resource)));
    }

    private String getDiskCacheDir() {
        if (mCacheType == CACHE_TYPE_IMAGE) {
            return mCacheConfig.getImageCacheDir();
        }
        return mCacheConfig.getMemoryCacheDir();
    }

    private long getDiskCacheSize() {
        if (mCacheType == CACHE_TYPE_IMAGE) {
            return mCacheConfig.getImageDiskCacheSize();
        }
        return mCacheConfig.getMemoryDiskCacheSize();
    }

    private String getTypeCode() {
        return mCacheType == CACHE_TYPE_IMAGE ? "image" : "js_css";
    }

    private String getSourceDetailLabel() {
        return mCacheType == CACHE_TYPE_IMAGE ? "image_disk" : "js_css_disk";
    }

    private String getContentType(WebResource resource) {
        if (resource == null || resource.getResponseHeaders() == null) {
            return null;
        }
        Map<String, String> headers = resource.getResponseHeaders();
        String value = headers.get("Content-Type");
        if (TextUtils.isEmpty(value)) {
            value = headers.get("content-type");
        }
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        String[] values = value.split(";");
        return values.length > 0 ? values[0].trim() : value.trim();
    }

    private boolean isImageMime(String mime) {
        return !TextUtils.isEmpty(mime) && mime.startsWith("image");
    }

    private boolean isJavaScriptOrCss(String mime) {
        return !TextUtils.isEmpty(mime) && (mime.contains("javascript") || mime.contains("css"));
    }

    private boolean isCacheTypeEnabled() {
        if (mCacheType == CACHE_TYPE_IMAGE) {
            return mCacheConfig.isEnableImageCache();
        }
        return mCacheConfig.isEnableJsCssCache();
    }
}

