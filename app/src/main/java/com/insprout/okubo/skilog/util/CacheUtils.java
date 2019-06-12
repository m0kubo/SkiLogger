package com.insprout.okubo.skilog.util;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;


public class CacheUtils {
    private static LruCache<String, Bitmap> mImageCache = null;

    private CacheUtils() {
        //  通常コンストラクタは 使用させない
        int cacheSize = (int)Runtime.getRuntime().maxMemory() / 8;
        mImageCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(@NonNull String key, @NonNull Bitmap value) {
                return value.getRowBytes() * value.getHeight();
            }
        };
    }

    public static LruCache<String, Bitmap> getInstance() {
        if (mImageCache == null) {
            new CacheUtils();
        }
        return mImageCache;
    }

    public static void clean() {
        getInstance();
        mImageCache.evictAll();
    }

    // メモリキャッシュから 削除する
    // キャッシュファイルが存在した場合、ファイルも削除する
    public static void remove(String key) {
        getInstance();
        mImageCache.remove(key);
    }

    public static void putBitmap(String key, Bitmap bitmap) {
        getInstance();
        mImageCache.put(key, bitmap);
    }

    public static Bitmap getBitmap(String key) {
        getInstance();
        return mImageCache.get(key);
    }

    public static void putBitmap(Long key, Bitmap bitmap) {
        getInstance();
        mImageCache.put(Long.toHexString(key), bitmap);
    }

    public static Bitmap getBitmap(Long key) {
        getInstance();
        return mImageCache.get(Long.toHexString(key));
    }

}

