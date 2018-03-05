package com.insprout.okubo.skilog.setting;

import android.content.Context;
import android.preference.PreferenceManager;

/**
 * Created by okubo on 2018/03/05.
 * Preferences関連をまとめたクラス
 */

public class Settings {

    private final static String KEY_APP_THEME = "app.THEME";


    public static int getThemeIndex(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(KEY_APP_THEME, Const.INDEX_THEME_LIGHT);
    }

    public static void putThemeIndex(Context context, int value) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putInt(KEY_APP_THEME, value).apply();
    }

    public static int getThemeStyle(Context context) {
        switch (getThemeIndex(context)) {
            case Const.INDEX_THEME_DARK:
                return Const.THEME_DARK;
            default:
                return Const.THEME_LIGHT;
        }
    }

}
