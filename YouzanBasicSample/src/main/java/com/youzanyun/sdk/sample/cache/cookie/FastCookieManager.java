package com.youzanyun.sdk.sample.cache.cookie;

import android.content.Context;

import com.youzanyun.sdk.sample.cache.offline.Destroyable;

import java.util.ArrayList;
import java.util.List;

import okhttp3.CookieJar;

/**
 * Created by Ryan
 * on 2019/10/29
 */
public class FastCookieManager implements Destroyable {

    private List<CookieInterceptor> mRequestCookieInterceptors;
    private List<CookieInterceptor> mResponseCookieInterceptors;
    private CookieJar mUserCookieJar;

    private FastCookieManager() {
        mRequestCookieInterceptors = new ArrayList<>();
        mResponseCookieInterceptors = new ArrayList<>();
    }

    @Override
    public void destroy() {
        mRequestCookieInterceptors.clear();
        mResponseCookieInterceptors.clear();
        mUserCookieJar = null;
    }

    private static class SingletonHolder {
        private static final FastCookieManager INSTANCE = new FastCookieManager();
    }

    public static FastCookieManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    List<CookieInterceptor> getRequestCookieInterceptors() {
        return mRequestCookieInterceptors;
    }

    public void addRequestCookieInterceptor(CookieInterceptor requestCookieInterceptor) {
        if (!mRequestCookieInterceptors.contains(requestCookieInterceptor)) {
            mRequestCookieInterceptors.add(requestCookieInterceptor);
        }
    }

    List<CookieInterceptor> getResponseCookieInterceptors() {
        return mResponseCookieInterceptors;
    }

    public void addResponseCookieInterceptor(CookieInterceptor responseCookieInterceptor) {
        if (!mResponseCookieInterceptors.contains(responseCookieInterceptor)) {
            mResponseCookieInterceptors.add(responseCookieInterceptor);
        }
    }

    public CookieJar getCookieJar(Context context) {
        return mUserCookieJar != null ? mUserCookieJar : new CookieJarImpl();
    }

    public void setCookieJar(CookieJar cookieJar) {
        this.mUserCookieJar = cookieJar;
    }

}
