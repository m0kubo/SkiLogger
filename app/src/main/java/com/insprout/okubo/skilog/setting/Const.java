package com.insprout.okubo.skilog.setting;

import android.Manifest;
import android.annotation.SuppressLint;

import com.insprout.okubo.skilog.R;

/**
 * Created by okubo on 2018/03/05.
 * 定数をまとめたクラス
 */

public class Const {
    public final static int INDEX_THEME_LIGHT = 0;
    public final static int INDEX_THEME_DARK = 1;

    public final static int THEME_LIGHT = R.style.AppTheme;
    public final static int THEME_DARK = R.style.AppThemeDark;

    public final static String EXTRA_PARAM1 = "extra.PARAM_1";
    public final static String EXTRA_PARAM2 = "extra.PARAM_2";


    @SuppressLint("InlinedApi")
    public final static String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
    // コンテンツ管理DBにアクセスするには、STORAGEの読み取り権限が必要
    public final static String[] PERMISSIONS_CONTENTS = {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    public final static int MAX_TAG_CANDIDATE_BY_LOCATION = 8;

}
