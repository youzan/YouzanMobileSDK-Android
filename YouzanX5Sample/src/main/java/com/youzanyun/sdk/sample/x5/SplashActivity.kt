package com.youzanyun.sdk.sample.x5

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.youzan.androidsdk.YouzanSDK
import com.youzanyun.sdk.sample.config.KaeConfig

class SplashActivity : AppCompatActivity() {
    companion object {
        fun isCacheEnabled(): Boolean {
            return com.youzanyun.sdk.sample.cache.WebViewPreloadManager.isCacheEnabled()
        }

        fun isReuseWebViewEnabled(): Boolean {
            return com.youzanyun.sdk.sample.cache.WebViewPreloadManager.isReuseWebViewEnabled()
        }

        fun isReuseResourceEnabled(): Boolean {
            return com.youzanyun.sdk.sample.cache.WebViewPreloadManager.isReuseResourceEnabled()
        }

        fun isJsCssCacheEnabled(): Boolean {
            return com.youzanyun.sdk.sample.cache.WebViewPreloadManager.isJsCssCacheEnabled()
        }

        fun isImageCacheEnabled(): Boolean {
            return com.youzanyun.sdk.sample.cache.WebViewPreloadManager.isImageCacheEnabled()
        }

        fun setCacheEnabled(context: android.content.Context, enabled: Boolean) {
            com.youzanyun.sdk.sample.cache.WebViewPreloadManager.setCacheEnabled(context, enabled)
        }

        fun setReuseWebViewEnabled(context: android.content.Context, enabled: Boolean) {
            com.youzanyun.sdk.sample.cache.WebViewPreloadManager.setReuseWebViewEnabled(context, enabled)
        }

        fun setReuseResourceEnabled(enabled: Boolean) {
            com.youzanyun.sdk.sample.cache.WebViewPreloadManager.setReuseResourceEnabled(enabled)
        }

        fun setJsCssCacheEnabled(enabled: Boolean) {
            com.youzanyun.sdk.sample.cache.WebViewPreloadManager.setJsCssCacheEnabled(enabled)
        }

        fun setImageCacheEnabled(enabled: Boolean) {
            com.youzanyun.sdk.sample.cache.WebViewPreloadManager.setImageCacheEnabled(enabled)
        }
    }

    private lateinit var enableCacheButton: Button
    private lateinit var disableCacheButton: Button
    private lateinit var enableReuseWebViewButton: Button
    private lateinit var disableReuseWebViewButton: Button
    private lateinit var enableReuseResourceButton: Button
    private lateinit var disableReuseResourceButton: Button
    private lateinit var enableJsCssCacheButton: Button
    private lateinit var disableJsCssCacheButton: Button
    private lateinit var enableImageCacheButton: Button
    private lateinit var disableImageCacheButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        enableCacheButton = findViewById(R.id.btn_enable_cache)
        disableCacheButton = findViewById(R.id.btn_disable_cache)
        enableReuseWebViewButton = findViewById(R.id.btn_enable_reuse_webview)
        disableReuseWebViewButton = findViewById(R.id.btn_disable_reuse_webview)
        enableReuseResourceButton = findViewById(R.id.btn_enable_reuse_resource)
        disableReuseResourceButton = findViewById(R.id.btn_disable_reuse_resource)
        enableJsCssCacheButton = findViewById(R.id.btn_enable_js_css_cache)
        disableJsCssCacheButton = findViewById(R.id.btn_disable_js_css_cache)
        enableImageCacheButton = findViewById(R.id.btn_enable_image_cache)
        disableImageCacheButton = findViewById(R.id.btn_disable_image_cache)
        enableCacheButton.setOnClickListener {
            setCacheEnabled(this, true)
            updateCacheButtons()
        }
        disableCacheButton.setOnClickListener {
            setCacheEnabled(this, false)
            updateCacheButtons()
        }
        enableReuseWebViewButton.setOnClickListener {
            setReuseWebViewEnabled(this, true)
            updateCacheButtons()
        }
        disableReuseWebViewButton.setOnClickListener {
            setReuseWebViewEnabled(this, false)
            updateCacheButtons()
        }
        enableReuseResourceButton.setOnClickListener {
            setReuseResourceEnabled(true)
            updateCacheButtons()
        }
        disableReuseResourceButton.setOnClickListener {
            setReuseResourceEnabled(false)
            updateCacheButtons()
        }
        enableJsCssCacheButton.setOnClickListener {
            setJsCssCacheEnabled(true)
            updateCacheButtons()
        }
        disableJsCssCacheButton.setOnClickListener {
            setJsCssCacheEnabled(false)
            updateCacheButtons()
        }
        enableImageCacheButton.setOnClickListener {
            setImageCacheEnabled(true)
            updateCacheButtons()
        }
        disableImageCacheButton.setOnClickListener {
            setImageCacheEnabled(false)
            updateCacheButtons()
        }
        updateCacheButtons()
        findViewById<View>(R.id.go_with_login).setOnClickListener { goWithLogin() }
        findViewById<View>(R.id.go_without_login).setOnClickListener { go() }
        findViewById<View>(R.id.go).setOnClickListener {
            val url: String = findViewById<EditText>(R.id.url).text.toString()
            if (url.startsWith("http")) {
                val intent = Intent(this@SplashActivity, MainActivity::class.java)
                intent.putExtra("url", url)
                startActivity(intent)
            }
        }


        findViewById<View>(R.id.logout).setOnClickListener {
          YouzanSDK.userLogout(this@SplashActivity)
        }

    }

    fun goWithLogin() {
        val clz = LoginActivity::class.java
        val intent = Intent(this@SplashActivity, clz)
        startActivity(intent)
    }

    fun go() {
        val clz = MainActivity::class.java
        val intent = Intent(this@SplashActivity, clz)
        startActivity(intent)
    }

    private fun updateCacheButtons() {
        val enabled = isCacheEnabled()
        enableCacheButton.isEnabled = !enabled
        disableCacheButton.isEnabled = enabled
        enableReuseWebViewButton.isEnabled = enabled && !isReuseWebViewEnabled()
        disableReuseWebViewButton.isEnabled = enabled && isReuseWebViewEnabled()
        enableReuseResourceButton.isEnabled = enabled && !isReuseResourceEnabled()
        disableReuseResourceButton.isEnabled = enabled && isReuseResourceEnabled()
        val resourceEnabled = enabled && isReuseResourceEnabled()
        enableJsCssCacheButton.isEnabled = resourceEnabled && !isJsCssCacheEnabled()
        disableJsCssCacheButton.isEnabled = resourceEnabled && isJsCssCacheEnabled()
        enableImageCacheButton.isEnabled = resourceEnabled && !isImageCacheEnabled()
        disableImageCacheButton.isEnabled = resourceEnabled && isImageCacheEnabled()
    }
}
