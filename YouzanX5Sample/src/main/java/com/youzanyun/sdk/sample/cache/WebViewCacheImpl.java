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

import java.util.Map;

/**
 * Created by Ryan
 * 2018/2/7 下午5:07
 */
public class WebViewCacheImpl implements WebViewCache {

    private FastCacheMode mFastCacheMode = FastCacheMode.FORCE;
    private CacheConfig mCacheConfig;
    private OfflineServer mOfflineServer;
    private Context mContext;

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
            String extension = MimeTypeMapUtils.getFileExtensionFromUrl(url);
            String mimeType = MimeTypeMapUtils.getMimeTypeFromExtension(extension);
            if (mimeType != null && mimeType.startsWith("video")) {
                return null; // todo 暂不兼容video， 后续支持
            }
            CacheRequest cacheRequest = new CacheRequest();
            cacheRequest.setUrl(url);
            cacheRequest.setMime(mimeType);
            cacheRequest.setForceMode(mFastCacheMode == FastCacheMode.FORCE);
            cacheRequest.setUserAgent(userAgent);
            cacheRequest.setWebViewCacheMode(cacheMode);
            Map<String, String> headers = webResourceRequest.getRequestHeaders();
            cacheRequest.setHeaders(headers);
            return getOfflineServer().get(cacheRequest);
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
}