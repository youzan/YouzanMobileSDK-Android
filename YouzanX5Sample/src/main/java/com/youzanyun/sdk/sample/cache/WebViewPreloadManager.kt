package com.youzanyun.sdk.sample.cache

import android.annotation.TargetApi
import android.content.Context
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.text.TextUtils
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import com.youzan.androidsdkx5.YouzanBrowser
import com.youzanyun.sdk.sample.cache.config.CacheConfig
import com.youzanyun.sdk.sample.cache.config.DefaultMimeTypeFilter
import com.youzanyun.sdk.sample.cache.config.FastCacheMode
import com.youzanyun.sdk.sample.x5.WebResourceRequestAdapter
import com.youzanyun.sdk.sample.x5.WebResourceResponseAdapter
import java.io.File
import java.util.LinkedHashSet
import java.util.LinkedList

/**
 * X5 WebView 预加载及离线缓存管理器。
 * WebView 实例只预创建不 loadUrl，H5 离线资源由临时 WebView 串行预取。
 */
object WebViewPreloadManager {
    private const val MEMORY_CACHE_DIR_NAME = "offline_cache_js_css"
    private const val IMAGE_CACHE_DIR_NAME = "offline_cache_images"
    private const val MAX_POOL_SIZE = 2

    private val webViewPool = LinkedList<YouzanBrowser>()
    private val preloadedUrlMap = HashMap<YouzanBrowser, String>()
    private val preloadPageStartAtMap = HashMap<YouzanBrowser, Long>()
    private val preloadPageFinishAtMap = HashMap<YouzanBrowser, Long>()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile private var webViewCache: WebViewCacheImpl? = null
    @Volatile private var preloadedHomeUrl: String? = null
    @Volatile private var htmlCacheUrls: Set<String> = LinkedHashSet()
    @Volatile private var htmlPreloading = false
    @Volatile private var cacheEnabled = true
    @Volatile private var reuseWebViewEnabled = true
    @Volatile private var reuseResourceEnabled = true
    @Volatile private var jsCssCacheEnabled = true
    @Volatile private var imageCacheEnabled = false

    fun preload(context: Context?, homeUrl: String?, htmlUrls: List<String>? = null) {
        if (context == null) {
            return
        }
        val appContext = context.applicationContext
        preloadedHomeUrl = homeUrl
        val urls = LinkedHashSet<String>()
        htmlUrls?.filterTo(urls) { !it.isNullOrBlank() }
        if (!homeUrl.isNullOrBlank()) {
            urls.add(homeUrl)
        }
        htmlCacheUrls = urls

        ensureCacheInitialized(appContext)
        webViewCache?.setHtmlCacheUrls(htmlCacheUrls)

        if (!cacheEnabled) {
            OfflineCacheLogger.log("缓存开关", "总开关=false，跳过WebView实例预加载和离线资源预获取")
            return
        }
        if (reuseWebViewEnabled) {
            ensureWebViewInstance(appContext, "启动预加载")
        } else {
            OfflineCacheLogger.log("缓存开关", "WebView复用=false，跳过WebView实例预加载，仅执行离线资源预获取")
        }
        preloadHtmlCacheOnly(appContext)
    }

    @Synchronized
    private fun ensureCacheInitialized(appContext: Context) {
        if (webViewCache != null) {
            return
        }
        val cacheImpl = WebViewCacheImpl(appContext)
        val cacheConfig = CacheConfig.Builder(appContext)
            .setExtensionFilter(DefaultMimeTypeFilter())
            .setMemoryCacheDir(File(appContext.cacheDir, MEMORY_CACHE_DIR_NAME).absolutePath)
            .setMemoryDiskCacheSize(1024L * 1024L * 50L)
            .setImageCacheDir(File(appContext.cacheDir, IMAGE_CACHE_DIR_NAME).absolutePath)
            .setImageDiskCacheSize(1024L * 1024L * 80L)
            .setEnableJsCssCache(jsCssCacheEnabled)
            .setEnableImageCache(imageCacheEnabled)
            .setMemoryCacheSize(1024 * 1024 * 50)
            .build()
        cacheImpl.setCacheMode(FastCacheMode.FORCE, cacheConfig)
        cacheImpl.setHtmlCacheUrls(htmlCacheUrls)
        webViewCache = cacheImpl
        OfflineCacheLogger.log(
            "初始化",
            "JS/CSS缓存目录=${cacheConfig.memoryCacheDir}，图片缓存目录=${cacheConfig.imageCacheDir}，JS/CSS缓存=${if (cacheConfig.isEnableJsCssCache()) "开启" else "关闭"}，图片缓存=${if (cacheConfig.isEnableImageCache()) "开启(内存+磁盘)" else "关闭"}"
        )
    }

    private fun ensureWebViewInstance(appContext: Context, reason: String) {
        mainHandler.post {
            if (!cacheEnabled || !reuseWebViewEnabled) {
                OfflineCacheLogger.log("预加载WebView", "总开关=$cacheEnabled，WebView复用=$reuseWebViewEnabled，跳过创建预加载实例，原因=$reason")
                return@post
            }
            if (webViewPool.size >= MAX_POOL_SIZE) {
                OfflineCacheLogger.log("预加载WebView", "跳过创建，原因=$reason，当前缓存池数量=${webViewPool.size}")
                return@post
            }

            OfflineCacheLogger.log("预加载WebView", "开始创建实例，原因=$reason")
            val wrapper = MutableContextWrapper(appContext)
            val webView = YouzanBrowser(wrapper)
            webViewPool.add(webView)
            OfflineCacheLogger.log("预加载WebView", "实例已创建但不加载URL，并放入缓存池，当前缓存池数量=${webViewPool.size}")
        }
    }

    private fun preloadHtmlCacheOnly(appContext: Context) {
        val urls = htmlCacheUrls.filter { it.isNotBlank() }.distinct()
        if (urls.isEmpty() || htmlPreloading) {
            return
        }
        htmlPreloading = true
        mainHandler.post {
            preloadNextHtml(appContext, LinkedList(urls))
        }
    }

    private fun preloadNextHtml(appContext: Context, queue: LinkedList<String>) {
        if (!cacheEnabled) {
            htmlPreloading = false
            OfflineCacheLogger.log("预热HTML", "总开关=false，停止HTML预加载队列")
            return
        }
        if (queue.isEmpty()) {
            htmlPreloading = false
            OfflineCacheLogger.log("预热HTML", "HTML 预加载队列已完成")
            return
        }

        val preloadUrl = queue.removeFirst()
        OfflineCacheLogger.log("预热HTML", "创建临时WebView顺序预获取H5离线资源，url=$preloadUrl")
        val wrapper = MutableContextWrapper(appContext)
        val webView = YouzanBrowser(wrapper)
        setupCacheInterceptor(webView)
        webView.tag = PreloadTask(preloadUrl, queue, appContext)
        webView.loadUrl(preloadUrl)
    }

    private fun releasePreloadWebView(webView: YouzanBrowser, url: String) {
        webView.post {
            try {
                webView.stopLoading()
            } catch (_: Exception) {
            }
            try {
                preloadedUrlMap.remove(webView)
                preloadPageStartAtMap.remove(webView)
                preloadPageFinishAtMap.remove(webView)
                webView.destroy()
                OfflineCacheLogger.log("预热HTML", "临时WebView预热完成并释放，url=$url")
            } catch (_: Exception) {
            }
        }
    }

    fun getWebView(context: Context, targetUrl: String?): WebViewAcquireResult {
        if (!cacheEnabled || !reuseWebViewEnabled) {
            OfflineCacheLogger.log("获取WebView", "总开关=$cacheEnabled，WebView复用=$reuseWebViewEnabled，强制新建实例，不复用缓存池，url=${safe(targetUrl)}")
            return WebViewAcquireResult(YouzanBrowser(context), reused = false, preloadedUrl = null, preloadedPageStartAt = 0L, preloadedPageFinishAt = 0L)
        }
        if (webViewPool.isNotEmpty()) {
            OfflineCacheLogger.log("获取WebView", "命中复用实例，来源=缓存池")
            val webView = webViewPool.removeFirst()
            (webView.context as? MutableContextWrapper)?.setBaseContext(context)
            preloadedHomeUrl?.let {
                ensureWebViewInstance(context.applicationContext, "复用实例被取走后自动补位")
            }
            return WebViewAcquireResult(
                webView = webView,
                reused = true,
                preloadedUrl = preloadedUrlMap[webView],
                preloadedPageStartAt = preloadPageStartAtMap[webView] ?: 0L,
                preloadedPageFinishAt = preloadPageFinishAtMap[webView] ?: 0L
            )
        }

        OfflineCacheLogger.log("获取WebView", "缓存池为空，创建新实例，等待页面挂载后加载URL，url=${safe(targetUrl)}")
        return WebViewAcquireResult(YouzanBrowser(context), reused = false, preloadedUrl = null, preloadedPageStartAt = 0L, preloadedPageFinishAt = 0L)
    }

    fun setupCacheInterceptor(webView: YouzanBrowser) {
        val cacheMode = webView.settings.cacheMode
        val userAgent = webView.settings.userAgentString
        webView.setWebViewClient(object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                preloadPageStartAtMap[webView] = SystemClock.elapsedRealtime()
                preloadPageFinishAtMap.remove(webView)
                OfflineCacheLogger.log("预加载页面耗时", "onPageStarted，url=${safe(url)}")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                preloadPageFinishAtMap[webView] = SystemClock.elapsedRealtime()
                val startAt = preloadPageStartAtMap[webView] ?: 0L
                val finishAt = preloadPageFinishAtMap[webView] ?: 0L
                val total = if (startAt > 0L && finishAt > 0L) finishAt - startAt else 0L
                OfflineCacheLogger.log("预加载页面耗时", "onPageFinished，总耗时=${if (total > 0L) "${total}ms" else "-"}，url=${safe(url)}")

                val task = webView.tag as? PreloadTask ?: return
                if (TextUtils.equals(task.url, url)) {
                    OfflineCacheLogger.log("预热HTML", "页面加载完成，继续下一个 HTML，url=$url")
                    webView.tag = null
                    preloadedUrlMap.remove(webView)
                    preloadPageStartAtMap.remove(webView)
                    preloadPageFinishAtMap.remove(webView)
                    releasePreloadWebView(webView, url ?: task.url)
                    preloadNextHtml(task.appContext, task.queue)
                }
            }

            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                if (!cacheEnabled || !reuseResourceEnabled) {
                    return super.shouldInterceptRequest(view, request)
                }
                if (request == null || request.url == null || request.url.toString().contains("data.json")) {
                    return super.shouldInterceptRequest(view, request)
                }
                val cacheImpl = getCacheImpl() ?: return super.shouldInterceptRequest(view, request)
                val scheme = request.url.scheme
                val method = request.method
                if ((TextUtils.equals("http", scheme) || TextUtils.equals("https", scheme))
                    && method.equals("GET", ignoreCase = true)
                ) {
                    val adapterRequest = WebResourceRequestAdapter.adapter(request)
                    val resourceResponse = adapterRequest?.let {
                        cacheImpl.getResource(it, cacheMode, userAgent)
                    }
                    if (resourceResponse != null) {
                        val source = cacheImpl.lastResourceSource ?: WebResource.SOURCE_WEBVIEW
                        if (source == WebResource.SOURCE_MEMORY || source == WebResource.SOURCE_NETWORK || source == WebResource.SOURCE_DISK) {
                            OfflineCacheLogger.log("预加载资源", "预加载命中资源，来源=$source，url=${request.url}")
                            return WebResourceResponseAdapter.adapter(resourceResponse)
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
        })
    }

    fun getCacheImpl(): WebViewCacheImpl? {
        if (!cacheEnabled || !reuseResourceEnabled) {
            return null
        }
        return webViewCache
    }

    fun setCacheEnabled(context: Context?, enabled: Boolean) {
        cacheEnabled = enabled
        OfflineCacheLogger.log("缓存开关", "缓存已${if (enabled) "开启" else "关闭"}")
        if (enabled) {
            if (context != null && preloadedHomeUrl != null) {
                preload(context.applicationContext, preloadedHomeUrl, htmlCacheUrls.toList())
            }
        } else {
            htmlPreloading = false
            clearWebViewPool("缓存关闭")
        }
    }

    fun isCacheEnabled(): Boolean = cacheEnabled

    fun setReuseWebViewEnabled(context: Context?, enabled: Boolean) {
        reuseWebViewEnabled = enabled
        OfflineCacheLogger.log("缓存开关", "WebView复用已${if (enabled) "开启" else "关闭"}")
        if (!enabled) {
            htmlPreloading = false
            clearWebViewPool("WebView复用关闭")
            return
        }
        if (cacheEnabled && context != null && preloadedHomeUrl != null) {
            preload(context.applicationContext, preloadedHomeUrl, htmlCacheUrls.toList())
        }
    }

    fun isReuseWebViewEnabled(): Boolean = reuseWebViewEnabled

    fun setReuseResourceEnabled(enabled: Boolean) {
        reuseResourceEnabled = enabled
        OfflineCacheLogger.log("缓存开关", "离线资源复用已${if (enabled) "开启" else "关闭"}")
    }

    fun isReuseResourceEnabled(): Boolean = reuseResourceEnabled

    fun setJsCssCacheEnabled(enabled: Boolean) {
        jsCssCacheEnabled = enabled
        webViewCache?.setEnableJsCssCache(enabled)
        OfflineCacheLogger.log("缓存开关", "CSS/JS缓存已${if (enabled) "开启" else "关闭"}")
    }

    fun isJsCssCacheEnabled(): Boolean {
        return webViewCache?.isEnableJsCssCache() ?: jsCssCacheEnabled
    }

    fun setImageCacheEnabled(enabled: Boolean) {
        imageCacheEnabled = enabled
        webViewCache?.setEnableImageCache(enabled)
        OfflineCacheLogger.log("缓存开关", "图片缓存已${if (enabled) "开启，支持内存+磁盘" else "关闭"}")
    }

    fun isImageCacheEnabled(): Boolean {
        return webViewCache?.isEnableImageCache() ?: imageCacheEnabled
    }

    private fun clearWebViewPool(reason: String) {
        mainHandler.post {
            while (webViewPool.isNotEmpty()) {
                val oldWebView = webViewPool.removeFirst()
                preloadedUrlMap.remove(oldWebView)
                preloadPageStartAtMap.remove(oldWebView)
                preloadPageFinishAtMap.remove(oldWebView)
                try {
                    oldWebView.stopLoading()
                } catch (_: Exception) {
                }
                try {
                    oldWebView.destroy()
                } catch (_: Exception) {
                }
            }
            OfflineCacheLogger.log("获取WebView", "$reason，已清空预加载缓存池")
        }
    }

    private fun safe(value: String?): String {
        return if (value.isNullOrBlank()) "-" else value
    }

    data class WebViewAcquireResult(
        val webView: YouzanBrowser,
        val reused: Boolean,
        val preloadedUrl: String?,
        val preloadedPageStartAt: Long,
        val preloadedPageFinishAt: Long
    )

    private data class PreloadTask(
        val url: String,
        val queue: LinkedList<String>,
        val appContext: Context
    )
}
