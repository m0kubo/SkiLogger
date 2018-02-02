package com.insprout.okubo.skilog.util;

import android.content.Context;
import android.os.Build;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by okubo on 2018/02/01.
 * androidのバージョンによる apiの違いを吸収する
 */

public class SdkUtils {

    /**
     * 指定されたリソースIDから Color値を返す
     * @param context コンテキスト
     * @param resourceId 取得するColorのリソースID
     * @return 取得されたColor値
     */
    @SuppressWarnings("deprecation")
    public static int getColor(Context context, int resourceId) {
        if (Build.VERSION.SDK_INT >= 23) {
            //API level 23以降は Contextから カラー値を参照する
            return context.getColor(resourceId);

        } else {
            // Resources経由の カラー値取得は、API level 23以降は 非推奨
            return context.getResources().getColor(resourceId);
        }
    }

}
