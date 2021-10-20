/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.youzan.androidsdkx5.YouzanBrowser;


/**
 * A fragment that displays a WebView.
 * The WebView is automically paused or resumed when the Fragment is paused or resumed.
 */
public abstract class WebViewFragment extends Fragment {
    private YouzanBrowser mWebView;
    private boolean mIsWebViewAvailable;

    public WebViewFragment() {
    }

    /**
     * Called to instantiate the view. Creates and returns the WebView.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (mWebView != null) {
            mWebView.destroy();
        }
        View contentView = inflater.inflate(getLayoutId(), container, false);
        mWebView = (YouzanBrowser) contentView.findViewById(getWebViewId());
        mIsWebViewAvailable = true;
        return contentView;
    }

    /**
     * @return The id of WebView in layout
     */
    @IdRes
    protected abstract int getWebViewId();

    /**
     * @return the layout id for Fragment
     */
    @LayoutRes
    protected abstract int getLayoutId();

    /**
     * Called when the fragment is visible to the user and actively running. Resumes the WebView.
     */
    @Override
    public void onPause() {
        super.onPause();
        mWebView.onPause();
    }

    /**
     * Called when the fragment is no longer resumed. Pauses the WebView.
     */
    @Override
    public void onResume() {
        mWebView.onResume();
        super.onResume();
    }

    /**
     * Called when the WebView has been detached from the fragment.
     * The WebView is no longer available after this time.
     */
    @Override
    public void onDestroyView() {
        mIsWebViewAvailable = false;
        super.onDestroyView();
    }

    /**
     * Called when the fragment is no longer in use. Destroys the internal state of the WebView.
     */
    @Override
    public void onDestroy() {
        if (mWebView != null) {
            mWebView.destroy();
            mWebView = null;
        }
        super.onDestroy();
    }

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     *
     * @return True if the host application wants to handle back press by itself, otherwise return false.
     */
    public boolean onBackPressed() {
        return false;
    }

    /**
     * Gets the WebView.
     */
    public YouzanBrowser getWebView() {
        return mIsWebViewAvailable ? mWebView : null;
    }
}
