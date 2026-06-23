package com.youzanyun.sdk.sample.cache.offline;

import android.content.Context;
import android.webkit.WebResourceResponse;


import com.youzanyun.sdk.sample.cache.WebResource;
import com.youzanyun.sdk.sample.cache.OfflineCacheLogger;
import com.youzanyun.sdk.sample.cache.config.CacheConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Ryan
 * at 2019/9/27
 */
public class OfflineServerImpl implements OfflineServer {

    private Context mContext;
    private CacheConfig mCacheConfig;
    private List<ResourceInterceptor> mBaseInterceptorList;
    private List<ResourceInterceptor> mForceModeChainList;
    private List<ResourceInterceptor> mDefaultModeChainList;
    private WebResourceResponseGenerator mResourceResponseGenerator;
    private final Object mRequestLocksGuard = new Object();
    private final Map<String, RequestLock> mRequestLocks = new HashMap<>();

    public OfflineServerImpl(Context context, CacheConfig cacheConfig) {
        mContext = context.getApplicationContext();
        mCacheConfig = cacheConfig;
        mResourceResponseGenerator = new DefaultWebResponseGenerator();
    }

    private List<ResourceInterceptor> buildForceModeChain(Context context, CacheConfig cacheConfig) {
        if (mForceModeChainList == null) {
            int interceptorsCount = 1 + getBaseInterceptorsCount();
            List<ResourceInterceptor> interceptors = new ArrayList<>(interceptorsCount);
            if (mBaseInterceptorList != null && !mBaseInterceptorList.isEmpty()) {
                interceptors.addAll(mBaseInterceptorList);
            }
            OfflineCacheLogger.log("初始化", "资源链路=本地OkHttp请求，不读取/写入内存缓存和磁盘缓存");
            interceptors.add(new ForceRemoteResourceInterceptor(context, cacheConfig));
            mForceModeChainList = interceptors;
        }
        return mForceModeChainList;
    }

    private List<ResourceInterceptor> buildDefaultModeChain(Context context) {
        if (mDefaultModeChainList == null) {
            int interceptorsCount = 1 + getBaseInterceptorsCount();
            List<ResourceInterceptor> interceptors = new ArrayList<>(interceptorsCount);
            if (mBaseInterceptorList != null && !mBaseInterceptorList.isEmpty()) {
                interceptors.addAll(mBaseInterceptorList);
            }
            interceptors.add(new DefaultRemoteResourceInterceptor(context));
            mDefaultModeChainList = interceptors;
        }
        return mDefaultModeChainList;
    }

    @Override
    public WebResourceResponse get(CacheRequest request) {
        boolean isForceMode = true;
        Context context = mContext;
        CacheConfig config = mCacheConfig;
        List<ResourceInterceptor> interceptors = buildForceModeChain(context, config);
        WebResource resource = callChainWithKeyLock(interceptors, request);
        return mResourceResponseGenerator.generate(resource, request.getMime());
    }

    @Override
    public synchronized void addResourceInterceptor(ResourceInterceptor interceptor) {
        if (mBaseInterceptorList == null) {
            mBaseInterceptorList = new ArrayList<>();
        }
        mBaseInterceptorList.add(interceptor);
    }

    @Override
    public synchronized void destroy() {
        destroyAll(mDefaultModeChainList);
        destroyAll(mForceModeChainList);
    }

    private WebResource callChainWithKeyLock(List<ResourceInterceptor> interceptors, CacheRequest request) {
        String key = request.getKey();
        if (key == null) {
            return callChain(interceptors, request);
        }
        RequestLock lock;
        synchronized (mRequestLocksGuard) {
            lock = mRequestLocks.get(key);
            if (lock == null) {
                lock = new RequestLock();
                mRequestLocks.put(key, lock);
            } else {
                OfflineCacheLogger.log("并发控制", "相同资源等待前序请求完成，url=" + request.getUrl());
            }
            lock.referenceCount++;
        }
        synchronized (lock) {
            try {
                return callChain(interceptors, request);
            } finally {
                synchronized (mRequestLocksGuard) {
                    lock.referenceCount--;
                    if (lock.referenceCount <= 0) {
                        mRequestLocks.remove(key);
                    }
                }
            }
        }
    }

    private WebResource callChain(List<ResourceInterceptor> interceptors, CacheRequest request) {

        Chain chain = new Chain(interceptors);
        return chain.process(request);
    }

    private void destroyAll(List<ResourceInterceptor> interceptors) {
        if (interceptors == null || interceptors.isEmpty()) {
            return;
        }
        for (ResourceInterceptor interceptor : interceptors) {
            if (interceptor instanceof Destroyable) {
                ((Destroyable) interceptor).destroy();
            }
        }
    }

    private int getBaseInterceptorsCount() {
        return mBaseInterceptorList == null ? 0 : mBaseInterceptorList.size();
    }

    private static class RequestLock {
        int referenceCount;
    }
}
