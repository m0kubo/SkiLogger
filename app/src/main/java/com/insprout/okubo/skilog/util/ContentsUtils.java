package com.insprout.okubo.skilog.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ContentsUtils {

    public static List<Uri> getImageList(Context context, Date dateFrom, Date dateTo) {
        List<Uri> files = new ArrayList<>();

        ContentResolver resolver = context.getContentResolver();
        if (resolver == null) return files;

        long secFrom = dateFrom != null ? dateFrom.getTime() / 1000 : 0;
        long secTo = dateTo != null ? dateTo.getTime() / 1000 : Integer.MAX_VALUE;
        Uri mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI; //SDカード から画像ファイルを検索
        Cursor cursor = resolver.query(
                mediaUri,
                new String[] { MediaStore.Images.Media._ID },
                String.format(Locale.ENGLISH,"%1$s >= ? AND %1$s < ? ", MediaStore.Images.Media.DATE_ADDED),
                new String[] { String.valueOf(secFrom), String.valueOf(secTo) },
                MediaStore.Images.Media.DATE_ADDED + " ASC"
        );

        if (cursor == null) return files;
        while (cursor.moveToNext()) {
            files.add(ContentUris.withAppendedId(mediaUri, cursor.getLong(0)));
        }
        cursor.close();

        return files;
    }

    public static Bitmap getBitmap(Context context, Uri uri) {
        if (uri == null) return null;
        try {
            return MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
        } catch (IOException e) {
            return null;
        }
    }

    public static Date getDate(Context context, Uri contextUri) {
        ContentResolver resolver = context.getContentResolver();
        if (resolver == null) return null;

        Date date = null;
        String[] projection = {MediaStore.Images.Media.DATE_ADDED};

        // try-with-resources構文で closeを自動的に呼び出す
        try (Cursor cursor = resolver.query(contextUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                date = new Date(cursor.getLong(0) * 1000);
            }
        }
        return date;
    }
}
