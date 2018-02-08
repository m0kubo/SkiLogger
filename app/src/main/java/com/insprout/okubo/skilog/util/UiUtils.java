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

    public static void setText(Activity activity, int id, String text) {
        View view = activity.findViewById(id);
        if (view instanceof TextView) {
            ((TextView)view).setText(text);
        }
    }

}
