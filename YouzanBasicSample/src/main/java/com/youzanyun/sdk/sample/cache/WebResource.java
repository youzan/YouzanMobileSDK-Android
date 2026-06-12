package com.youzanyun.sdk.sample.cache;

import static java.net.HttpURLConnection.HTTP_BAD_METHOD;
import static java.net.HttpURLConnection.HTTP_GONE;
import static java.net.HttpURLConnection.HTTP_MOVED_PERM;
import static java.net.HttpURLConnection.HTTP_MULT_CHOICE;
import static java.net.HttpURLConnection.HTTP_NOT_AUTHORITATIVE;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NOT_IMPLEMENTED;
import static java.net.HttpURLConnection.HTTP_NO_CONTENT;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_REQ_TOO_LONG;

import java.util.Map;

import okhttp3.internal.http.StatusLine;

/**
 * Created by Ryan
 * 2018/3/1 下午5:40
 */
public class WebResource {

    public static final int MIN_CACHE_BYTES = 10 * 1024;

    public static final String SOURCE_NETWORK = "network";
    public static final String SOURCE_MEMORY = "memory";
    public static final String SOURCE_DISK = "disk";
    public static final String SOURCE_WEBVIEW = "webview";

    private int responseCode;

    private String reasonPhrase;

    private Map<String, String> responseHeaders;

    private boolean isModified = true;

    private boolean isCacheByOurselves = false;

    private byte[] originBytes;

    private String source = SOURCE_WEBVIEW;
    private String sourceDetail = SOURCE_WEBVIEW;

    public void setModified(boolean modified) {
        isModified = modified;
    }

    public boolean isModified() {
        return isModified;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public void setCacheByOurselves(boolean isCacheByOurselves) {
        this.isCacheByOurselves = isCacheByOurselves;
    }

    public boolean isCacheByOurselves() {
        return isCacheByOurselves;
    }

    public void setOriginBytes(byte[] bytes) {
        this.originBytes = bytes;
    }

    public byte[] getOriginBytes() {
        return originBytes;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getSource() {
        return source;
    }

    public void setSourceDetail(String sourceDetail) {
        this.sourceDetail = sourceDetail;
    }

    public String getSourceDetail() {
        return sourceDetail;
    }

    public void setReasonPhrase(String reasonPhrase) {
        this.reasonPhrase = reasonPhrase;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    public void setResponseCode(int responseCode) {
        this.responseCode = responseCode;
    }

    public int getResponseCode() {
        return responseCode;
    }

    /**
     * reference {@link okhttp3.internal.cache.CacheStrategy}
     */
    public boolean isCacheable() {
        return responseCode == HTTP_OK
                || responseCode == HTTP_NOT_AUTHORITATIVE
                || responseCode == HTTP_NO_CONTENT
                || responseCode == HTTP_MULT_CHOICE
                || responseCode == HTTP_MOVED_PERM
                || responseCode == HTTP_NOT_FOUND
                || responseCode == HTTP_BAD_METHOD
                || responseCode == HTTP_GONE
                || responseCode == HTTP_REQ_TOO_LONG
                || responseCode == HTTP_NOT_IMPLEMENTED
                || responseCode == StatusLine.HTTP_PERM_REDIRECT;
    }

    public boolean isLargeEnoughToCache() {
        return originBytes != null && originBytes.length > MIN_CACHE_BYTES;
    }
}
