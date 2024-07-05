package com.youzanyun.sdk.sample.cache.offline;

import com.youzanyun.sdk.sample.cache.WebResource;

/**
 * Created by Ryan
 * at 2019/9/27
 */
public interface ResourceInterceptor {

    WebResource load(Chain chain);

}
