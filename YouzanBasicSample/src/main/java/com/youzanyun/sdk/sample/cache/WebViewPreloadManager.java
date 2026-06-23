package com.youzanyun.sdk.sample.cache;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.MutableContextWrapper;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.TextUtils;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.youzan.androidsdk.basic.YouzanBrowser;
import com.youzanyun.sdk.sample.cache.config.CacheConfig;
import com.youzanyun.sdk.sample.cache.config.DefaultMimeTypeFilter;
import com.youzanyun.sdk.sample.cache.config.FastCacheMode;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Basic WebView 预加载及离线缓存管理器。
 * 缓存池 size = 1。
 */
public final class WebViewPreloadManager {

    private static final String MEMORY_CACHE_DIR_NAME = "offline_cache_js_css";
    private static final String IMAGE_CACHE_DIR_NAME = "offline_cache_images";
    private static final int MAX_POOL_SIZE = 2;

    private static final LinkedList<YouzanBrowser> WEB_VIEW_POOL = new LinkedList<>();
    private static final Map<YouzanBrowser, String> PRELOADED_URL_MAP = new HashMap<>();
    private static final Map<YouzanBrowser, Long> PRELOAD_PAGE_START_AT_MAP = new HashMap<>();
    private static final Map<YouzanBrowser, Long> PRELOAD_PAGE_FINISH_AT_MAP = new HashMap<>();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private static volatile WebViewCacheImpl webViewCache;
    private static volatile String preloadedHomeUrl;
    private static volatile Set<String> htmlCacheUrls = new LinkedHashSet<>();
    private static volatile boolean htmlPreloading;
    private static volatile boolean cacheEnabled = true;
    private static volatile boolean reuseWebViewEnabled = false;
    private static volatile boolean reuseResourceEnabled = true;
    private static volatile boolean jsCssCacheEnabled = false;
    private static volatile boolean imageCacheEnabled = false;

    private WebViewPreloadManager() {
    }

    public static void preload(Context context, String homeUrl, List<String> htmlUrls) {
        if (context == null) {
            return;
        }
        Context appContext = context.getApplicationContext();
        preloadedHomeUrl = homeUrl;
        htmlCacheUrls = new LinkedHashSet<>(htmlUrls == null ? new ArrayList<String>() : htmlUrls);
        if (!TextUtils.isEmpty(homeUrl)) {
            htmlCacheUrls.add(homeUrl);
        }
        
        ensureCacheInitialized(appContext);
        if (webViewCache != null) {
            webViewCache.setHtmlCacheUrls(htmlCacheUrls);
        }
        if (!cacheEnabled) {
            OfflineCacheLogger.log("缓存开关", "总开关=false，跳过WebView实例预加载和离线资源预获取");
            return;
        }
        if (reuseWebViewEnabled) {
            ensureWebViewInstance(appContext, homeUrl, "启动预加载");
        } else {
            OfflineCacheLogger.log("缓存开关", "WebView复用=false，跳过WebView实例预加载，仅执行离线资源预获取");
        }
        preloadHtmlCacheOnly(appContext, homeUrl);
    }

    private static void ensureCacheInitialized(Context appContext) {
        if (webViewCache != null) {
            return;
        }
        webViewCache = new WebViewCacheImpl(appContext);
        CacheConfig cacheConfig = new CacheConfig.Builder(appContext)
                .setExtensionFilter(new DefaultMimeTypeFilter())
                .setMemoryCacheDir(new File(appContext.getCacheDir(), MEMORY_CACHE_DIR_NAME).getAbsolutePath())
                .setMemoryDiskCacheSize(1024L * 1024L * 50L)
                .setImageCacheDir(new File(appContext.getCacheDir(), IMAGE_CACHE_DIR_NAME).getAbsolutePath())
                .setImageDiskCacheSize(1024L * 1024L * 80L)
                .setEnableJsCssCache(jsCssCacheEnabled)
                .setEnableImageCache(imageCacheEnabled)
                .setMemoryCacheSize(1024 * 1024 * 50)
                .build();
        webViewCache.setCacheMode(FastCacheMode.FORCE, cacheConfig);
        webViewCache.setHtmlCacheUrls(htmlCacheUrls);
        OfflineCacheLogger.log(
                "初始化",
                "JS/CSS缓存目录=" + cacheConfig.getMemoryCacheDir()
                        + "，图片缓存目录=" + cacheConfig.getImageCacheDir()
                        + "，JS/CSS缓存=" + (cacheConfig.isEnableJsCssCache() ? "开启" : "关闭")
                        + "，图片缓存=" + (cacheConfig.isEnableImageCache() ? "开启(内存+磁盘)" : "关闭")
        );
    }

    private static void ensureWebViewInstance(final Context appContext, final String homeUrl, final String reason) {
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                if (!cacheEnabled || !reuseWebViewEnabled) {
                    OfflineCacheLogger.log("预加载WebView", "总开关=" + cacheEnabled + "，WebView复用=" + reuseWebViewEnabled + "，跳过创建预加载实例，原因=" + reason);
                    return;
                }
                if (WEB_VIEW_POOL.size() >= MAX_POOL_SIZE) {
                    OfflineCacheLogger.log("预加载WebView", "跳过创建，原因=" + reason + "，当前缓存池数量=" + WEB_VIEW_POOL.size());
                    return;
                }

                OfflineCacheLogger.log("预加载WebView", "开始创建实例，原因=" + reason);
                MutableContextWrapper wrapper = new MutableContextWrapper(appContext);
                YouzanBrowser webView = new YouzanBrowser(wrapper);
                WEB_VIEW_POOL.add(webView);
                OfflineCacheLogger.log("预加载WebView", "实例已创建但不加载URL，并放入缓存池，当前缓存池数量=" + WEB_VIEW_POOL.size());
            }
        });
    }

    private static void preloadHtmlCacheOnly(final Context appContext, final String homeUrl) {
        final List<String> urls = new ArrayList<>();
        for (String url : htmlCacheUrls) {
            if (!TextUtils.isEmpty(url) && !urls.contains(url)) {
                urls.add(url);
            }
        }
        if (urls.isEmpty() || htmlPreloading) {
            return;
        }
        htmlPreloading = true;
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                startHtmlPreloadQueue(appContext, urls, homeUrl);
            }
        });
    }

    private static void startHtmlPreloadQueue(Context appContext, List<String> urls, String homeUrl) {
        if (urls.isEmpty()) {
            htmlPreloading = false;
            return;
        }
        preloadNextHtml(appContext, new LinkedList<>(urls), homeUrl);
    }

    private static void preloadNextHtml(Context appContext, LinkedList<String> queue, String homeUrl) {
        if (!cacheEnabled) {
            htmlPreloading = false;
            OfflineCacheLogger.log("预热HTML", "总开关=false，停止HTML预加载队列");
            return;
        }
        if (queue.isEmpty()) {
            htmlPreloading = false;
            OfflineCacheLogger.log("预热HTML", "HTML 预加载队列已完成");
            return;
        }
        String preloadUrl = queue.removeFirst();
        OfflineCacheLogger.log("预热HTML", "创建临时WebView顺序预获取H5离线资源，url=" + preloadUrl);
        MutableContextWrapper wrapper = new MutableContextWrapper(appContext);
        YouzanBrowser webView = new YouzanBrowser(wrapper);
        setupCacheInterceptor(webView);
        webView.setTag(new PreloadTask(preloadUrl, queue, homeUrl, appContext));
        webView.loadUrl(preloadUrl);
    }

    private static void releasePreloadWebView(final YouzanBrowser webView, final String url) {
        webView.post(new Runnable() {
            @Override
            public void run() {
                try {
                    webView.stopLoading();
                } catch (Exception ignore) {
                }
                try {
                    PRELOADED_URL_MAP.remove(webView);
                    PRELOAD_PAGE_START_AT_MAP.remove(webView);
                    PRELOAD_PAGE_FINISH_AT_MAP.remove(webView);
                    webView.destroy();
                    OfflineCacheLogger.log("预热HTML", "临时WebView预热完成并释放，url=" + url);
                } catch (Exception ignore) {
                }
            }
        });
    }

    public static WebViewAcquireResult getWebView(Context context, String targetUrl) {
        if (!cacheEnabled || !reuseWebViewEnabled) {
            OfflineCacheLogger.log("获取WebView", "总开关=" + cacheEnabled + "，WebView复用=" + reuseWebViewEnabled + "，强制新建实例，不复用缓存池，url=" + safe(targetUrl));
            return new WebViewAcquireResult(new YouzanBrowser(context), false, null, 0L, 0L);
        }
        if (!WEB_VIEW_POOL.isEmpty()) {
            OfflineCacheLogger.log("获取WebView", "命中复用实例，来源=缓存池");
            YouzanBrowser webView = WEB_VIEW_POOL.removeFirst();
            if (webView.getContext() instanceof MutableContextWrapper) {
                ((MutableContextWrapper) webView.getContext()).setBaseContext(context);
            }
            if (preloadedHomeUrl != null) {
                ensureWebViewInstance(context.getApplicationContext(), preloadedHomeUrl, "复用实例被取走后自动补位");
            }
            return new WebViewAcquireResult(
                    webView,
                    true,
                    PRELOADED_URL_MAP.get(webView),
                    valueOrZero(PRELOAD_PAGE_START_AT_MAP.get(webView)),
                    valueOrZero(PRELOAD_PAGE_FINISH_AT_MAP.get(webView))
            );
        }

        OfflineCacheLogger.log("获取WebView", "缓存池为空，创建新实例，等待页面挂载后加载URL，url=" + safe(targetUrl));
        return new WebViewAcquireResult(new YouzanBrowser(context), false, null, 0L, 0L);
    }

    public static void setupCacheInterceptor(final YouzanBrowser webView) {
        final int cacheMode = webView.getSettings().getCacheMode();
        final String userAgent = webView.getSettings().getUserAgentString();
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                PRELOAD_PAGE_START_AT_MAP.put(webView, SystemClock.elapsedRealtime());
                PRELOAD_PAGE_FINISH_AT_MAP.remove(webView);
                OfflineCacheLogger.log("预加载页面耗时", "onPageStarted，url=" + safe(url));
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                PRELOAD_PAGE_FINISH_AT_MAP.put(webView, SystemClock.elapsedRealtime());
                long startAt = valueOrZero(PRELOAD_PAGE_START_AT_MAP.get(webView));
                long finishAt = valueOrZero(PRELOAD_PAGE_FINISH_AT_MAP.get(webView));
                long total = startAt > 0L && finishAt > 0L ? finishAt - startAt : 0L;
                OfflineCacheLogger.log("预加载页面耗时", "onPageFinished，总耗时=" + (total > 0L ? total + "ms" : "-") + "，url=" + safe(url));
                Object tag = webView.getTag();
                if (!(tag instanceof PreloadTask)) {
                    return;
                }
                PreloadTask task = (PreloadTask) tag;
                if (TextUtils.equals(task.url, url)) {
                    OfflineCacheLogger.log("预热HTML", "页面加载完成，继续下一个 HTML，url=" + url);
                    webView.setTag(null);
                    PRELOADED_URL_MAP.remove(webView);
                    PRELOAD_PAGE_START_AT_MAP.remove(webView);
                    PRELOAD_PAGE_FINISH_AT_MAP.remove(webView);
                    releasePreloadWebView(webView, url);
                    preloadNextHtml(task.appContext, task.queue, task.homeUrl);
                }
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (!isCacheEnabled() || !isReuseResourceEnabled()) {
                    return super.shouldInterceptRequest(view, request);
                }
                if (request == null || request.getUrl() == null || request.getUrl().toString().contains("data.json")) {
                    return super.shouldInterceptRequest(view, request);
                }
                WebViewCacheImpl cacheImpl = getCacheImpl();
                if (cacheImpl == null) {
                    return super.shouldInterceptRequest(view, request);
                }
                String scheme = request.getUrl().getScheme();
                String method = request.getMethod();
                if ((TextUtils.equals("http", scheme) || TextUtils.equals("https", scheme))
                        && "GET".equalsIgnoreCase(method)) {
                    WebResourceResponse resourceResponse = cacheImpl.getResource(request, cacheMode, userAgent);
                    if (resourceResponse != null) {
                        String source = cacheImpl.getLastResourceSource();
                        if (WebResource.SOURCE_MEMORY.equals(source)
                                || WebResource.SOURCE_NETWORK.equals(source)
                                || WebResource.SOURCE_DISK.equals(source)) {
                            OfflineCacheLogger.log("预加载资源", "预加载命中资源，来源=" + source + "，url=" + request.getUrl());
                            return resourceResponse;
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });
    }

    public static WebViewCacheImpl getCacheImpl() {
        if (!cacheEnabled || !reuseResourceEnabled) {
            return null;
        }
        return webViewCache;
    }

    public static void setCacheEnabled(Context context, boolean enabled) {
        cacheEnabled = enabled;
        OfflineCacheLogger.log("缓存开关", "缓存已" + (enabled ? "开启" : "关闭"));
        if (enabled) {
            if (reuseWebViewEnabled && context != null && preloadedHomeUrl != null) {
                preload(context.getApplicationContext(), preloadedHomeUrl, new ArrayList<>(htmlCacheUrls));
            }
        } else {
            htmlPreloading = false;
            clearWebViewPool("缓存关闭");
        }
    }

    public static boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public static void setReuseWebViewEnabled(Context context, boolean enabled) {
        reuseWebViewEnabled = enabled;
        OfflineCacheLogger.log("缓存开关", "WebView复用已" + (enabled ? "开启" : "关闭"));
        if (!enabled) {
            htmlPreloading = false;
            clearWebViewPool("WebView复用关闭");
            return;
        }
        if (cacheEnabled && context != null && preloadedHomeUrl != null) {
            preload(context.getApplicationContext(), preloadedHomeUrl, new ArrayList<>(htmlCacheUrls));
        }
    }

    public static boolean isReuseWebViewEnabled() {
        return reuseWebViewEnabled;
    }

    public static void setReuseResourceEnabled(boolean enabled) {
        reuseResourceEnabled = enabled;
        OfflineCacheLogger.log("缓存开关", "离线资源复用已" + (enabled ? "开启" : "关闭"));
    }

    public static boolean isReuseResourceEnabled() {
        return reuseResourceEnabled;
    }

    public static void setJsCssCacheEnabled(boolean enabled) {
        jsCssCacheEnabled = enabled;
        WebViewCacheImpl cacheImpl = webViewCache;
        if (cacheImpl != null) {
            cacheImpl.setEnableJsCssCache(enabled);
        }
        OfflineCacheLogger.log("缓存开关", "CSS/JS缓存已" + (enabled ? "开启" : "关闭"));
    }

    public static boolean isJsCssCacheEnabled() {
        WebViewCacheImpl cacheImpl = webViewCache;
        return cacheImpl != null ? cacheImpl.isEnableJsCssCache() : jsCssCacheEnabled;
    }

    public static void setImageCacheEnabled(boolean enabled) {
        imageCacheEnabled = enabled;
        WebViewCacheImpl cacheImpl = webViewCache;
        if (cacheImpl != null) {
            cacheImpl.setEnableImageCache(enabled);
        }
        OfflineCacheLogger.log("缓存开关", "图片缓存已" + (enabled ? "开启，支持内存+磁盘" : "关闭"));
    }

    public static boolean isImageCacheEnabled() {
        WebViewCacheImpl cacheImpl = webViewCache;
        return cacheImpl != null ? cacheImpl.isEnableImageCache() : imageCacheEnabled;
    }

    private static void clearWebViewPool(final String reason) {
        MAIN_HANDLER.post(new Runnable() {
            @Override
            public void run() {
                while (!WEB_VIEW_POOL.isEmpty()) {
                    YouzanBrowser oldWebView = WEB_VIEW_POOL.removeFirst();
                    PRELOADED_URL_MAP.remove(oldWebView);
                    PRELOAD_PAGE_START_AT_MAP.remove(oldWebView);
                    PRELOAD_PAGE_FINISH_AT_MAP.remove(oldWebView);
                    try {
                        oldWebView.stopLoading();
                    } catch (Exception ignore) {
                    }
                    try {
                        oldWebView.destroy();
                    } catch (Exception ignore) {
                    }
                }
                OfflineCacheLogger.log("获取WebView", reason + "，已清空预加载缓存池");
            }
        });
    }

    private static long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private static String safe(String value) {
        return TextUtils.isEmpty(value) ? "-" : value;
    }

    public static final class WebViewAcquireResult {
        public final YouzanBrowser webView;
        public final boolean reused;
        public final String preloadedUrl;
        public final long preloadedPageStartAt;
        public final long preloadedPageFinishAt;

        WebViewAcquireResult(YouzanBrowser webView, boolean reused, String preloadedUrl, long preloadedPageStartAt, long preloadedPageFinishAt) {
            this.webView = webView;
            this.reused = reused;
            this.preloadedUrl = preloadedUrl;
            this.preloadedPageStartAt = preloadedPageStartAt;
            this.preloadedPageFinishAt = preloadedPageFinishAt;
        }
    }

    private static final class PreloadTask {
        final String url;
        final LinkedList<String> queue;
        final String homeUrl;
        final Context appContext;

        PreloadTask(String url, LinkedList<String> queue, String homeUrl, Context appContext) {
            this.url = url;
            this.queue = queue;
            this.homeUrl = homeUrl;
            this.appContext = appContext;
        }
    }
}
