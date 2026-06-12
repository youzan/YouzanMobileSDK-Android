package com.youzanyun.sdk.sample.cache.offline;

import android.webkit.WebResourceResponse;

import com.youzanyun.sdk.sample.cache.WebResource;


/**
 * Created by Ryan
 * at 2019/10/8
 */
public interface WebResourceResponseGenerator {

    WebResourceResponse generate(WebResource resource, String urlMime);

}
