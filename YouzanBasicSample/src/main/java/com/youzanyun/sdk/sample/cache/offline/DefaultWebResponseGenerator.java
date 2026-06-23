package com.youzanyun.sdk.sample.cache.offline;

import android.os.Build;
import android.text.TextUtils;
import android.webkit.WebResourceResponse;


import com.youzanyun.sdk.sample.cache.WebResource;
import com.youzan.androidsdk.utils.LogUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Ryan
 * at 2019/10/8
 */
public class DefaultWebResponseGenerator implements WebResourceResponseGenerator {

    public static final String KEY_CONTENT_TYPE = "Content-Type";
    private static final String KEY_FROM = "from";

    @Override
    public WebResourceResponse generate(WebResource resource, String urlMime) {
        if (resource == null) {
            return null;
        }
        Map<String, String> headers = resource.getResponseHeaders();
        String contentType = null;
        String charset = null;
        if (headers != null) {
            String contentTypeValue = getContentType(headers, KEY_CONTENT_TYPE);
            if (!TextUtils.isEmpty(contentTypeValue)) {
                String[] contentTypeArray = contentTypeValue.split(";");
                if (contentTypeArray.length >= 1) {
                    contentType = contentTypeArray[0];
                }
                if (contentTypeArray.length >= 2) {
                    charset = contentTypeArray[1];
                    String[] charsetArray = charset.split("=");
                    if (charsetArray.length >= 2) {
                        charset = charsetArray[1];
                    }
                }
            }
        }
        if (!TextUtils.isEmpty(contentType)) {
            urlMime = contentType;
        }
        if (TextUtils.isEmpty(urlMime)) {
            urlMime = "application/octet-stream";
        }
        byte[] resourceBytes = resource.getOriginBytes();
        if (resourceBytes == null || resourceBytes.length < 0) {
            return null;
        }
        if (resourceBytes.length == 0 && resource.getResponseCode() == 304) {
            LogUtils.d("the response bytes can not be empty if we get 304.");
            return null;
        }
        InputStream bis = new ByteArrayInputStream(resourceBytes);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int status = resource.getResponseCode();
            String reasonPhrase = resource.getReasonPhrase();
            if (TextUtils.isEmpty(reasonPhrase)) {
                reasonPhrase = PhraseList.getPhrase(status);
            }
            return new WebResourceResponse(urlMime, charset, status, reasonPhrase, buildResponseHeaders(resource), bis);
        }
        return new WebResourceResponse(urlMime, charset, bis);
    }

    private Map<String, String> buildResponseHeaders(WebResource resource) {
        Map<String, String> originHeaders = resource.getResponseHeaders();
        Map<String, String> headers = originHeaders == null ? new HashMap<String, String>() : new HashMap<>(originHeaders);
        String source = resource.getSource();
        if (WebResource.SOURCE_MEMORY.equals(source)) {
            headers.put(KEY_FROM, WebResource.SOURCE_MEMORY);
        } else if (WebResource.SOURCE_DISK.equals(source)) {
            headers.put(KEY_FROM, WebResource.SOURCE_DISK);
        }
        return headers;
    }

    private String getContentType(Map<String, String> headers, String key) {
        if (headers != null) {
            String value = headers.get(key);
            return value != null ? value : headers.get(key.toLowerCase());
        }
        return null;
    }
}
