package com.insprout.okubo.skilog.util;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

/**
 * Created by okubo on 2018/02/05.
 * View関連の 一般的な操作を まとめる
 */

public class UiUtils {

    public static void enableView(Activity activity, int id, boolean enabled) {
        View view = activity.findViewById(id);
        if (view != null) {
            view.setEnabled(enabled);
        }
    }

    public static void setText(Activity activity, int id, int textId) {
        View view = activity.findViewById(id);
        if (view instanceof TextView) {
            ((TextView)view).setText(textId);
        }
    }

    public static void setText(Activity activity, int id, String text) {
        View view = activity.findViewById(id);
        if (view instanceof TextView) {
            ((TextView)view).setText(text);
        }
    }

}
