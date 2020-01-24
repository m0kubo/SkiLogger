package com.insprout.okubo.skilog.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

/**
 * Created by okubo on 2018/02/05.
 * View関連の 一般的な操作を まとめる
 */

public class UiUtils {

    public static void setEnabled(Activity activity, int id, boolean enabled) {
        View view = activity.findViewById(id);
        if (view != null) {
            view.setEnabled(enabled);
        }
    }

    public static void setVisibility(Activity activity, int id, int visibility) {
        View view = activity.findViewById(id);
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    public static void setSelected(Activity activity, int id, boolean selected) {
        View view = activity.findViewById(id);
        if (view != null) {
            view.setSelected(selected);
        }
    }

    public static void setText(Activity activity, int id, int textId) {
        View view = activity.findViewById(id);
        if (view instanceof TextView) {
            ((TextView)view).setText(textId);
        }
    }

    public static void setText(Activity activity, int id, CharSequence text) {
        View view = activity.findViewById(id);
        if (view instanceof TextView) {
            ((TextView)view).setText(text);
        }
    }

    public static void setDrawables(Activity activity, int id, int foregroundResId, int backgroundResId) {
        View view = activity.findViewById(id);
        if (view instanceof ImageButton) {
            ((ImageButton)view).setImageResource(foregroundResId);
            ((ImageButton)view).setBackgroundResource(backgroundResId);
        }
    }

    public static boolean intentActionView(Context context, Uri uri) {
        // 別アプリで 指定のUrlを開く
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            // 対応するアプリがインストールされていない場合
            return false;
        }
        return true;
    }


    //
    // Menu系
    //

    public static void setEnabled(Menu menu, int id, boolean enabled) {
        if (menu != null) {
            MenuItem item = menu.findItem(id);
            if (item != null) {
                item.setEnabled(enabled);
            }
        }
    }

    public static void setTitle(Menu menu, int id, CharSequence text) {
        if (menu != null) {
            MenuItem item = menu.findItem(id);
            if (item != null) {
                item.setTitle(text);
            }
        }
    }

    public static void setTitle(Menu menu, int id, int resourceId) {
        if (menu != null) {
            MenuItem item = menu.findItem(id);
            if (item != null) {
                item.setTitle(resourceId);
            }
        }
    }


    /**
     * ViewのBitmapを返す。Bitmapは不要になったら呼び出し側で recycle()を実行し
     * メモリーリークしないようにすること。
     * @param view Bitmapを取得するView
     * @return viewのBitmap
     */
    public static Bitmap getBitmap(View view) {
        if (view == null) return null;

        Bitmap image = null;
        view.setDrawingCacheEnabled(true);
        // Viewのキャッシュを取得
        Bitmap cache = view.getDrawingCache();
        if (cache != null) {
            // View#getDrawingCacheはシステムの持っている Bitmap の参照を返す。
            // システム側から recycle されるので Bitmap#createBitmapで作り直す必要がある。
            image = Bitmap.createBitmap(cache);
        }
        view.setDrawingCacheEnabled(false);
        return image;
    }

}
