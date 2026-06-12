package com.youzanyun.sdk.sample.cache;

import android.content.Context;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;

import com.youzan.androidsdk.YouzanSDK;
import com.youzanyun.sdk.sample.cache.config.CacheConfig;
import com.youzanyun.sdk.sample.cache.config.FastCacheMode;
import com.youzanyun.sdk.sample.cache.offline.CacheRequest;
import com.youzanyun.sdk.sample.cache.offline.OfflineServer;
import com.youzanyun.sdk.sample.cache.offline.OfflineServerImpl;
import com.youzanyun.sdk.sample.cache.offline.ResourceInterceptor;
import com.youzan.androidsdk.utils.MimeTypeMapUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/**
 * Created by Ryan
 * 2018/2/7 下午5:07
 */
public class WebViewCacheImpl implements WebViewCache {

    private FastCacheMode mFastCacheMode = FastCacheMode.FORCE;
    private CacheConfig mCacheConfig;
    private OfflineServer mOfflineServer;
    private Context mContext;
    private volatile String mLastResourceSource = WebResource.SOURCE_WEBVIEW;
    private volatile Set<String> mHtmlCacheUrls = Collections.emptySet();

    public WebViewCacheImpl(Context context) {
        mContext = context;
    }

    @Override
    public WebResourceResponse getResource(WebResourceRequest webResourceRequest, int cacheMode, String userAgent) {
        if (mFastCacheMode == FastCacheMode.DEFAULT) {
            return null;
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            String url = webResourceRequest.getUrl().toString();
            String mimeType = resolveMimeType(url);
            if (shouldCacheHtml(url)) {
                mimeType = "text/html";
            }
            if ("text/html".equalsIgnoreCase(mimeType) && !shouldCacheHtml(url)) {
                OfflineCacheLogger.log("资源识别", "HTML未命中预缓存白名单，不走离线缓存，url=" + url);
                return null;
            }
            if (url.toLowerCase(Locale.ROOT).contains(".webp")) {
                OfflineCacheLogger.log("资源识别", "检测到webp资源，mime=" + mimeType + "，url=" + url);
            }
            if (!isSupportedMimeType(mimeType)) {
                OfflineCacheLogger.log("资源识别", "资源类型不支持离线缓存，mime=" + mimeType + "，url=" + url);
                return null;
            }
            CacheRequest cacheRequest = new CacheRequest();
            cacheRequest.setUrl(url);
            cacheRequest.setMime(mimeType);
            cacheRequest.setForceMode(mFastCacheMode == FastCacheMode.FORCE);
            cacheRequest.setUserAgent(userAgent);
            cacheRequest.setWebViewCacheMode(cacheMode);
            Map<String, String> headers = webResourceRequest.getRequestHeaders();
            cacheRequest.setHeaders(headers);
            WebResourceResponse response = getOfflineServer().get(cacheRequest);
            if (response != null) {
                mLastResourceSource = cacheRequest.getResolvedSource();
            } else {
                mLastResourceSource = WebResource.SOURCE_WEBVIEW;
            }
            return response;
        }
        throw new IllegalStateException("an error occurred.");
    }

    @Override
    public void setCacheMode(FastCacheMode mode, CacheConfig cacheConfig) {
        mFastCacheMode = mode;
        mCacheConfig = cacheConfig;
    }

    @Override
    public void addResourceInterceptor(ResourceInterceptor interceptor) {
        getOfflineServer().addResourceInterceptor(interceptor);
    }

    private synchronized OfflineServer getOfflineServer() {
        if (mOfflineServer == null) {
            mOfflineServer = new OfflineServerImpl(mContext, getCacheConfig());
        }
        return mOfflineServer;
    }

    private CacheConfig getCacheConfig() {
        return mCacheConfig != null ? mCacheConfig : generateDefaultCacheConfig();
    }

    private CacheConfig generateDefaultCacheConfig() {
        return new CacheConfig.Builder(mContext).build();
    }

    @Override
    public void destroy() {
        if (mOfflineServer != null) {
            mOfflineServer.destroy();
        }
        // help gc
        mCacheConfig = null;
        mOfflineServer = null;
        mContext = null;
    }

    public String getLastResourceSource() {
        return mLastResourceSource;
    }

    public void setHtmlCacheUrls(Set<String> htmlCacheUrls) {
        if (htmlCacheUrls == null || htmlCacheUrls.isEmpty()) {
            mHtmlCacheUrls = Collections.emptySet();
            return;
        }
        mHtmlCacheUrls = new HashSet<>(htmlCacheUrls);
    }

    public synchronized void setEnableJsCssCache(boolean enabled) {
        getCacheConfig().setEnableJsCssCache(enabled);
    }

    public synchronized boolean isEnableJsCssCache() {
        return getCacheConfig().isEnableJsCssCache();
    }

    public synchronized void setEnableImageCache(boolean enabled) {
        CacheConfig cacheConfig = getCacheConfig();
        if (cacheConfig.isEnableImageCache() == enabled) {
            return;
        }
        cacheConfig.setEnableImageCache(enabled);
        if (mOfflineServer != null) {
            mOfflineServer.destroy();
            mOfflineServer = null;
        }
    }

    public synchronized boolean isEnableImageCache() {
        return getCacheConfig().isEnableImageCache();
    }

    private String resolveMimeType(String url) {
        String extension = MimeTypeMapUtils.getFileExtensionFromUrl(url);
        String mimeType = MimeTypeMapUtils.getMimeTypeFromExtension(extension);
        if (mimeType != null) {
            return mimeType;
        }
        String lowerUrl = url == null ? "" : url.toLowerCase(Locale.ROOT);
        if (lowerUrl.contains(".webp")) {
            return "image/webp";
        }
        if (lowerUrl.contains(".png")) {
            return "image/png";
        }
        if (lowerUrl.contains(".jpg") || lowerUrl.contains(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerUrl.contains(".gif")) {
            return "image/gif";
        }
        if (lowerUrl.contains(".css")) {
            return "text/css";
        }
        if (lowerUrl.contains(".js")) {
            return "application/javascript";
        }
        return null;
    }

    private boolean isSupportedMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        if (mimeType.startsWith("image")) {
            boolean enableImageCache = getCacheConfig().isEnableImageCache();
            if (!enableImageCache) {
                OfflineCacheLogger.log("资源识别", "图片缓存未开启，不走离线缓存，mime=" + mimeType);
            }
            return enableImageCache;
        }
        if ("text/html".equalsIgnoreCase(mimeType)) {
            return true;
        }
        if ("text/css".equalsIgnoreCase(mimeType) || mimeType.contains("javascript")) {
            boolean enableJsCssCache = getCacheConfig().isEnableJsCssCache();
            if (!enableJsCssCache) {
                OfflineCacheLogger.log("资源识别", "CSS/JS缓存未开启，不走离线缓存，mime=" + mimeType);
            }
            return enableJsCssCache;
        }
        return false;
    }

    private boolean shouldCacheHtml(String url) {
        return url != null && mHtmlCacheUrls.contains(url);
    }
}
