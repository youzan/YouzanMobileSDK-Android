package com.youzanyun.sdk.sample.cache.loader;


import com.youzanyun.sdk.sample.cache.WebResource;

/**
 * Created by Ryan
 * 2018/2/7 下午7:53
 */
public interface ResourceLoader {

    WebResource getResource(SourceRequest request);
}



