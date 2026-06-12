/*
 * Copyright (C) 2017 youzanyun.com, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.youzanyun.sdk.sample.basic;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.youzan.androidsdk.YouzanSDK;
import com.youzanyun.sdk.sample.cache.WebViewPreloadManager;


public class MainActivity extends Activity implements View.OnClickListener {
    private Button enableCacheButton;
    private Button disableCacheButton;
    private Button enableReuseWebViewButton;
    private Button disableReuseWebViewButton;
    private Button enableReuseResourceButton;
    private Button disableReuseResourceButton;
    private Button enableJsCssCacheButton;
    private Button disableJsCssCacheButton;
    private Button enableImageCacheButton;
    private Button disableImageCacheButton;

    public static boolean isCacheEnabled() {
        return WebViewPreloadManager.isCacheEnabled();
    }

    public static boolean isReuseWebViewEnabled() {
        return WebViewPreloadManager.isReuseWebViewEnabled();
    }

    public static boolean isReuseResourceEnabled() {
        return WebViewPreloadManager.isReuseResourceEnabled();
    }

    public static boolean isJsCssCacheEnabled() {
        return WebViewPreloadManager.isJsCssCacheEnabled();
    }

    public static boolean isImageCacheEnabled() {
        return WebViewPreloadManager.isImageCacheEnabled();
    }

    public static void setCacheEnabled(Activity activity, boolean enabled) {
        WebViewPreloadManager.setCacheEnabled(activity, enabled);
    }

    public static void setReuseWebViewEnabled(Activity activity, boolean enabled) {
        WebViewPreloadManager.setReuseWebViewEnabled(activity, enabled);
    }

    public static void setReuseResourceEnabled(boolean enabled) {
        WebViewPreloadManager.setReuseResourceEnabled(enabled);
    }

    public static void setJsCssCacheEnabled(boolean enabled) {
        WebViewPreloadManager.setJsCssCacheEnabled(enabled);
    }

    public static void setImageCacheEnabled(boolean enabled) {
        WebViewPreloadManager.setImageCacheEnabled(enabled);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.button_open).setOnClickListener(this);
        findViewById(R.id.button_clear).setOnClickListener(this);
        enableCacheButton = (Button) findViewById(R.id.btn_enable_cache);
        disableCacheButton = (Button) findViewById(R.id.btn_disable_cache);
        enableReuseWebViewButton = (Button) findViewById(R.id.btn_enable_reuse_webview);
        disableReuseWebViewButton = (Button) findViewById(R.id.btn_disable_reuse_webview);
        enableReuseResourceButton = (Button) findViewById(R.id.btn_enable_reuse_resource);
        disableReuseResourceButton = (Button) findViewById(R.id.btn_disable_reuse_resource);
        enableJsCssCacheButton = (Button) findViewById(R.id.btn_enable_js_css_cache);
        disableJsCssCacheButton = (Button) findViewById(R.id.btn_disable_js_css_cache);
        enableImageCacheButton = (Button) findViewById(R.id.btn_enable_image_cache);
        disableImageCacheButton = (Button) findViewById(R.id.btn_disable_image_cache);
        enableCacheButton.setOnClickListener(this);
        disableCacheButton.setOnClickListener(this);
        enableReuseWebViewButton.setOnClickListener(this);
        disableReuseWebViewButton.setOnClickListener(this);
        enableReuseResourceButton.setOnClickListener(this);
        disableReuseResourceButton.setOnClickListener(this);
        enableJsCssCacheButton.setOnClickListener(this);
        disableJsCssCacheButton.setOnClickListener(this);
        enableImageCacheButton.setOnClickListener(this);
        disableImageCacheButton.setOnClickListener(this);
        updateCacheButtons();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_open:
                //店铺链接, 可以从有赞后台`店铺=>店铺概况=>访问店铺`复制到相应的链接，这里是一个测试链接
//                gotoActivity("https://shop16911610.m.youzan.com/wscshop/showcase/homepage?kdt_id=16719442");
                gotoActivity(KaeConfig.URL_MAIN);
                break;
            case R.id.button_clear:
                YouzanSDK.userLogout(this);
                break;
            case R.id.btn_enable_cache:
                setCacheEnabled(this, true);
                updateCacheButtons();
                break;
            case R.id.btn_disable_cache:
                setCacheEnabled(this, false);
                updateCacheButtons();
                break;
            case R.id.btn_enable_reuse_webview:
                setReuseWebViewEnabled(this, true);
                updateCacheButtons();
                break;
            case R.id.btn_disable_reuse_webview:
                setReuseWebViewEnabled(this, false);
                updateCacheButtons();
                break;
            case R.id.btn_enable_reuse_resource:
                setReuseResourceEnabled(true);
                updateCacheButtons();
                break;
            case R.id.btn_disable_reuse_resource:
                setReuseResourceEnabled(false);
                updateCacheButtons();
                break;
            case R.id.btn_enable_js_css_cache:
                setJsCssCacheEnabled(true);
                updateCacheButtons();
                break;
            case R.id.btn_disable_js_css_cache:
                setJsCssCacheEnabled(false);
                updateCacheButtons();
                break;
            case R.id.btn_enable_image_cache:
                setImageCacheEnabled(true);
                updateCacheButtons();
                break;
            case R.id.btn_disable_image_cache:
                setImageCacheEnabled(false);
                updateCacheButtons();
                break;
            default:
                break;
        }
    }

    private void  gotoActivity(String url){
        Intent intent = new Intent(this, YouzanActivity.class);
        intent.putExtra(YouzanActivity.KEY_URL, url);
        startActivity(intent);
    }

    private void updateCacheButtons() {
        boolean enabled = isCacheEnabled();
        enableCacheButton.setEnabled(!enabled);
        disableCacheButton.setEnabled(enabled);
        enableReuseWebViewButton.setEnabled(enabled && !isReuseWebViewEnabled());
        disableReuseWebViewButton.setEnabled(enabled && isReuseWebViewEnabled());
        enableReuseResourceButton.setEnabled(enabled && !isReuseResourceEnabled());
        disableReuseResourceButton.setEnabled(enabled && isReuseResourceEnabled());
        boolean resourceEnabled = enabled && isReuseResourceEnabled();
        enableJsCssCacheButton.setEnabled(resourceEnabled && !isJsCssCacheEnabled());
        disableJsCssCacheButton.setEnabled(resourceEnabled && isJsCssCacheEnabled());
        enableImageCacheButton.setEnabled(resourceEnabled && !isImageCacheEnabled());
        disableImageCacheButton.setEnabled(resourceEnabled && isImageCacheEnabled());
    }
}
