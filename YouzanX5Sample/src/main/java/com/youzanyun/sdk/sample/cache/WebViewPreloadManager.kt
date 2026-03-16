package com.youzanyun.sdk.sample.cache

import android.app.Activity
import android.content.Context
import android.content.MutableContextWrapper
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import java.util.concurrent.ConcurrentHashMap

/**
 * WebView 预加载管理器
 * 用于管理 WebView 池，实现 WebView 的复用和预加载
 */
object WebViewPreloadManager {
    // 默认预加载数量
    private const val DEFAULT_PRELOAD_COUNT = 3
    // 预加载数量
    private var preloadCount = DEFAULT_PRELOAD_COUNT
    // WebView 池，使用 ConcurrentHashMap 保证线程安全
    private val webViewPool = ConcurrentHashMap<String, WebView>()

    // 上下文对象
    private lateinit var applicationContext: Context

    /**
     * 初始化方法，必须在使用前调用
     */
    fun init(context: Context) {
        if (!WebViewPreloadManager::applicationContext.isInitialized) {
            applicationContext = context.applicationContext
        }
    }

    /**
     * 设置预加载数量
     */
    fun setPreloadCount(count: Int) {
        if (count > 0) {
            preloadCount = count
        }
    }

    /**
     * 获取预加载数量
     */
    fun getPreloadCount(): Int {
        return preloadCount
    }

    /**
     * 获取 WebView
     * @param key WebView 的标识
     * @return WebView 实例
     */
    fun getWebView(key: String, activity: Activity?): WebView {
        checkInitialized()

        // 先判断 map 中是否存在对应的 webview
        var webView = webViewPool[key]

        // 如果不存在，创建一个新的 WebView 并放入 map 中
        if (webView == null) {
            webView = createWebView()
            webViewPool[key] = webView
        }

        // 替换
        if ((webView.context as? MutableContextWrapper)?.baseContext !is Activity && activity != null) {
            (webView.context as? MutableContextWrapper)?.baseContext = activity
        }

        return webView
    }

    /**
     * 预加载 WebView
     * @param key WebView 的标识
     */
    fun preloadWebView(key: String) {
        checkInitialized()

        if (!webViewPool.containsKey(key)) {
            val webView = createWebView()
            webViewPool[key] = webView
        }
    }

    /**
     * 批量预加载 WebView
     * @param keys WebView 的标识列表
     */
    fun preloadWebViews(keys: List<String>) {
        for (key in keys) {
            preloadWebView(key)
        }
    }

    /**
     * 创建 WebView
     */
    private fun createWebView(): WebView {
        val context= MutableContextWrapper(applicationContext)
        val webView = WebView(context)

        // 配置 WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // 其他配置...
        }
        return webView
    }

    /**
     * 回收 WebView
     * @param key WebView 的标识
     */
    fun recycleWebView(key: String) {
        val webView = webViewPool.remove(key)

        // 如果当前池子大小小于预加载数量，则将 WebView 放回池子中
        if (webViewPool.size < preloadCount && webView != null) {
            // 重置 WebView 状态
            webView.loadUrl("about:blank")
            webViewPool[key] = webView
        } else {
            // 否则销毁 WebView
            webView?.destroy()
        }
    }

    /**
     * 销毁所有 WebView
     */
    fun destroyAll() {
        for (webView in webViewPool.values) {
            webView.destroy()
        }
        webViewPool.clear()
    }

    /**
     * 获取当前池子大小
     */
    fun getPoolSize(): Int {
        return webViewPool.size
    }

    /**
     * 检查是否已初始化
     */
    private fun checkInitialized() {
        if (!WebViewPreloadManager::applicationContext.isInitialized) {
            throw IllegalStateException("WebViewPreloadManager 未初始化，请先调用 init 方法")
        }
    }
}


class CacheWebClient: WebViewClient() {
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        WebResCacheManager.shouldInterceptRequest(request.url.toString())
        return  null
    }
}