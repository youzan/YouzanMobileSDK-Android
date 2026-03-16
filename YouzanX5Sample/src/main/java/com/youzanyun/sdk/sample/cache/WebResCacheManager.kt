package com.youzanyun.sdk.sample.cache

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.bumptech.glide.Glide
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import com.bumptech.glide.load.engine.DiskCacheStrategy
import okhttp3.internal.cache.DiskLruCache
import okhttp3.internal.io.FileSystem
import okio.Okio
import java.io.FileInputStream

// 将类转换为 object
object WebResCacheManager {
    private const val MEMORY_CACHE_SIZE = 10 * 1024 * 1024 // 10MB
    private const val DISK_CACHE_SIZE = 50 * 1024 * 1024 // 50MB
    private const val DISK_CACHE_VERSION = 1
    private const val CACHE_ENTRY_VALUE_COUNT = 1

    private const val TAG = "WebCache"
    private val CSS_MIME_TYPE = "text/css"
    private val JS_MIME_TYPE = "application/javascript"
    private val IMAGE_MIME_TYPES = mapOf(
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "png" to "image/png",
        "gif" to "image/gif",
        "webp" to "image/webp",
        "svg" to "image/svg+xml"
    )
    
    private val memoryCache = ConcurrentHashMap<String, CacheEntry>()
    private lateinit var diskCache: DiskLruCache
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var appContext: Context

    // 初始化方法，替代原来的 getInstance
    fun init(context: Context) {
        if (::appContext.isInitialized) return
        
        appContext = context.applicationContext
        
        val cacheDir = File(appContext.cacheDir, "webview_cache")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        
        diskCache = DiskLruCache.create(
            FileSystem.SYSTEM,
            cacheDir,
            DISK_CACHE_VERSION,
            CACHE_ENTRY_VALUE_COUNT,
            DISK_CACHE_SIZE.toLong()
        )
        
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }
    
    fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
        if (!::appContext.isInitialized) {
            throw IllegalStateException("WebCacheManager 必须先调用 init 方法进行初始化")
        }

       return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
           shouldInterceptRequest(request.url?.toString())
       } else {
          null
       }
    }


    fun shouldInterceptRequest(url: String?): WebResourceResponse? {
        if (!::appContext.isInitialized) {
            throw IllegalStateException("WebCacheManager 必须先调用 init 方法进行初始化")
        }

        url ?: return null
        val fileExtension = getFileExtension(url)
        // 只处理 CSS、JS 和图片资源
        val response = when {

                fileExtension == "css" -> handleCssJsRequest(url, CSS_MIME_TYPE)
                fileExtension == "js" -> handleCssJsRequest(url, JS_MIME_TYPE)
                IMAGE_MIME_TYPES.containsKey(fileExtension) ->
                    handleImageRequest(url, IMAGE_MIME_TYPES[fileExtension] ?: "image/jpeg")
                else -> null // 不处理其他类型的请求
            }

        val responseHeaders = HashMap<String, String>()
        responseHeaders["Access-Control-Allow-Origin"] = "*"
        responseHeaders["Access-Control-Allow-Methods"] = "GET, POST, OPTIONS"
        responseHeaders["Access-Control-Allow-Headers"] = "Origin, X-Requested-With, Content-Type, Accept"
        responseHeaders["Access-Control-Allow-Credentials"] = "true"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            response?.setResponseHeaders(responseHeaders)
        }

       return response
    }




    private fun handleCssJsRequest(url: String, mimeType: String): WebResourceResponse? {
        // 先从内存缓存中获取
        val memCacheEntry = memoryCache[url]
        if (memCacheEntry != null) {
            Log.d(TAG, "命中内存缓存 ${url}")
            return WebResourceResponse(
                mimeType,
                "UTF-8",
                ByteArrayInputStream(memCacheEntry.data)
            )
        }
        
        // 再从磁盘缓存中获取
        try {
            val key = url.hashCode().toString()
            val snapshot = diskCache.get(key)
            if (snapshot != null) {
                Log.d(TAG, "命中磁盘缓存 ${url}")
                snapshot.use {

                    it.getSource(0).use {
                        Okio.buffer(it).use {
                            val bytes = it.readByteArray()
                            // 放入内存缓存
                            memoryCache[url] = CacheEntry(bytes)
                            Log.d(TAG, "添加到内存缓存 ${url}")
                            return WebResourceResponse(
                                mimeType,
                                "UTF-8",
                                ByteArrayInputStream(bytes))
                        }
                    }

                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        
        // 缓存中没有，通过网络获取
        return downloadAndCacheResource(url, mimeType)
    }
    
    private fun handleImageRequest(url: String, mimeType: String): WebResourceResponse? {
//        try {
//            // 使用 Glide 加载图片
//            val future = Glide.with(appContext)
//                .asFile()
//                .load(url)
//                .diskCacheStrategy(DiskCacheStrategy.ALL)
//                .submit()
//            Log.d(TAG, "命中 glide缓存 ${url}")
//            return WebResourceResponse(mimeType, "UTF-8", FileInputStream(future.get()))
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }

        // Glide 加载失败，使用 OkHttp 下载
        return downloadAndCacheResource(url, mimeType)
    }
    
    private fun downloadAndCacheResource(url: String, mimeType: String): WebResourceResponse? {
        try {
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return null
            }

            Log.d(TAG, "${url}. content-type: ${response.header("content-type")}")
            val responseBody = response.body()
            if (responseBody != null) {
                val bytes = responseBody.bytes()
                
                // 缓存到内存
                Log.d(TAG, "添加到内存缓存 ${url}")
                memoryCache[url] = CacheEntry(bytes)
                
                // 缓存到磁盘
                val key = url.hashCode().toString()
                val editor = diskCache.edit(key)
                if (editor != null) {
                    editor.newSink(0).use {
                        Okio.buffer(it).use {
                            Log.d(TAG, "添加到磁盘缓存 ${url}")
                            it.write(bytes)
                        }
                    }
                    editor.commit()
                }
                
                return WebResourceResponse(
                    mimeType,
                    "UTF-8",
                    ByteArrayInputStream(bytes)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return null
    }
    
    private fun getFileExtension(url: String): String {
        val lastDotIndex = url.lastIndexOf(".")
        val lastSlashIndex = url.lastIndexOf("/")
        
        if (lastDotIndex > lastSlashIndex && lastDotIndex < url.length - 1) {
            return url.substring(lastDotIndex + 1).toLowerCase()
        }
        
        return ""
    }
    
    // 清除缓存
    fun clearCache() {
        if (!::diskCache.isInitialized) return
        
        memoryCache.clear()
        try {
            diskCache.delete()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    
    // 内存缓存条目
    private data class CacheEntry(val data: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as CacheEntry
            
            return data.contentEquals(other.data)
        }
        
        override fun hashCode(): Int {
            return data.contentHashCode()
        }
    }
}