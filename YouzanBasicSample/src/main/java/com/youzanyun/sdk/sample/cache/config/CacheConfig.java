package com.youzanyun.sdk.sample.cache.config;

import android.content.Context;


import com.youzan.androidsdk.utils.AppVersionUtil;
import com.youzan.androidsdk.utils.MemorySizeCalculator;

import java.io.File;

/**
 * Created by Ryan
 * 2018/2/7 下午5:41
 */
public class CacheConfig {

    private String mCacheDir;
    private int mVersion;
    private long mDiskCacheSize;
    private int mMemCacheSize;
    private MimeTypeFilter mFilter;
    private String mMemoryCacheDir;
    private long mMemoryDiskCacheSize;
    private String mImageCacheDir;
    private long mImageDiskCacheSize;
    private boolean mEnableImageCache;
    private boolean mEnableJsCssCache;

    private CacheConfig() {

    }

    public String getCacheDir() {
        return mCacheDir;
    }

    public int getVersion() {
        return mVersion;
    }

    public void setVersion(int version) {
        this.mVersion = version;
    }

    public MimeTypeFilter getFilter() {
        return mFilter;
    }

    public long getDiskCacheSize() {
        return mDiskCacheSize;
    }

    public int getMemCacheSize() {
        return mMemCacheSize;
    }

    public String getMemoryCacheDir() {
        return mMemoryCacheDir;
    }

    public long getMemoryDiskCacheSize() {
        return mMemoryDiskCacheSize;
    }

    public String getImageCacheDir() {
        return mImageCacheDir;
    }

    public long getImageDiskCacheSize() {
        return mImageDiskCacheSize;
    }

    public boolean isEnableImageCache() {
        return mEnableImageCache;
    }

    public void setEnableImageCache(boolean enableImageCache) {
        this.mEnableImageCache = enableImageCache;
    }

    public boolean isEnableJsCssCache() {
        return mEnableJsCssCache;
    }

    public void setEnableJsCssCache(boolean enableJsCssCache) {
        this.mEnableJsCssCache = enableJsCssCache;
    }

    public static class Builder {

        private static final String CACHE_DIR_NAME = "cached_webview_force";
        private static final String MEMORY_CACHE_DIR_NAME = "cached_webview_force_memory";
        private static final String IMAGE_CACHE_DIR_NAME = "cached_webview_force_image";
        private static final int DEFAULT_DISK_CACHE_SIZE = 200 * 1024 * 1024;
        private String cacheDir;
        private String memoryCacheDir;
        private String imageCacheDir;
        private int version;
        private long diskCacheSize = DEFAULT_DISK_CACHE_SIZE;
        private long memoryDiskCacheSize = DEFAULT_DISK_CACHE_SIZE / 2;
        private long imageDiskCacheSize = DEFAULT_DISK_CACHE_SIZE / 2;
        private int memoryCacheSize = MemorySizeCalculator.getSize();
        private MimeTypeFilter filter = new DefaultMimeTypeFilter();
        private boolean enableImageCache = false;
        private boolean enableJsCssCache = true;

        public Builder(Context context) {
            cacheDir = context.getCacheDir() + File.separator + CACHE_DIR_NAME;
            memoryCacheDir = context.getCacheDir() + File.separator + MEMORY_CACHE_DIR_NAME;
            imageCacheDir = context.getCacheDir() + File.separator + IMAGE_CACHE_DIR_NAME;
            version = AppVersionUtil.getVersionCode(context);
        }

        public Builder setCacheDir(String cacheDir) {
            this.cacheDir = cacheDir;
            return this;
        }

        public Builder setVersion(int version) {
            this.version = version;
            return this;
        }

        public Builder setDiskCacheSize(long diskCacheSize) {
            this.diskCacheSize = diskCacheSize;
            return this;
        }

        public Builder setMemoryCacheDir(String memoryCacheDir) {
            this.memoryCacheDir = memoryCacheDir;
            return this;
        }

        public Builder setMemoryDiskCacheSize(long memoryDiskCacheSize) {
            this.memoryDiskCacheSize = memoryDiskCacheSize;
            return this;
        }

        public Builder setImageCacheDir(String imageCacheDir) {
            this.imageCacheDir = imageCacheDir;
            return this;
        }

        public Builder setImageDiskCacheSize(long imageDiskCacheSize) {
            this.imageDiskCacheSize = imageDiskCacheSize;
            return this;
        }

        public Builder setEnableImageCache(boolean enableImageCache) {
            this.enableImageCache = enableImageCache;
            return this;
        }

        public Builder setEnableJsCssCache(boolean enableJsCssCache) {
            this.enableJsCssCache = enableJsCssCache;
            return this;
        }

        public Builder setExtensionFilter(MimeTypeFilter filter) {
            this.filter = filter;
            return this;
        }

        public Builder setMemoryCacheSize(int memoryCacheSize) {
            this.memoryCacheSize = memoryCacheSize;
            return this;
        }

        public CacheConfig build() {
            CacheConfig config = new CacheConfig();
            config.mCacheDir = cacheDir;
            config.mVersion = version;
            config.mDiskCacheSize = diskCacheSize;
            config.mFilter = filter;
            config.mMemCacheSize = memoryCacheSize;
            config.mMemoryCacheDir = memoryCacheDir != null ? memoryCacheDir : cacheDir;
            config.mMemoryDiskCacheSize = memoryDiskCacheSize > 0 ? memoryDiskCacheSize : diskCacheSize;
            config.mImageCacheDir = imageCacheDir != null ? imageCacheDir : cacheDir;
            config.mImageDiskCacheSize = imageDiskCacheSize > 0 ? imageDiskCacheSize : diskCacheSize;
            config.mEnableImageCache = enableImageCache;
            config.mEnableJsCssCache = enableJsCssCache;
            return config;
        }
    }
}
