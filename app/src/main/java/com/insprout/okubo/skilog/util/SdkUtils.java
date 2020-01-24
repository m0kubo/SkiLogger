package com.insprout.okubo.skilog.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.text.Html;
import android.text.Spanned;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by okubo on 2018/02/01.
 * androidのバージョンによる apiの違いを吸収する
 */

public class SdkUtils {

    //////////////////////////////////////////////////////////////////////////
    //
    // Runtime Permission関連
    //

    /**
     * 指定された パーミッションが付与されているか確認し、権限がない場合は指定されたrequestCodeで権限付与画面を呼び出す。
     * ただし、requestCodeが -1の場合は、権限付与画面は呼び出さない
     *
     * @param activity    この機能を利用するアクティビティ
     * @param permissions 確認するパーミッション(複数)
     * @param requestCode 権限付与画面をよびだす際の、リクエストコード。(onRequestPermissionsResult()で判別する)
     * @return true: すでに必要な権限は付与されている。false: 権限が不足している。
     */
    public static boolean requestRuntimePermissions(Activity activity, String[] permissions, int requestCode) {
        if (Build.VERSION.SDK_INT < 23) return true;

        // リクエストされたパーミッションの内、許可されてないものを調べる
        List<String> deniedPermissions = getDeniedPermissions(activity, permissions);
        if (deniedPermissions.size() == 0) return true;

        // リクエストコードが指定されている場合は、権限付与画面を呼び出す
        if (requestCode != -1) {
            // 許可のないパーミッションに対して 権限付与画面を呼び出す
            String[] requestPermissions = new String[deniedPermissions.size()];
            deniedPermissions.toArray(requestPermissions);
            activity.requestPermissions(requestPermissions, requestCode);
        }
        return false;       // 権限不足
    }

    /**
     * 指定された パーミッションが付与されているか確認し、権限がない場合は指定されたrequestCodeで権限付与画面を呼び出す。
     * ただし、requestCodeが -1の場合は、権限付与画面は呼び出さない
     *
     * @param context     コンテキスト
     * @param permissions 確認するパーミッション(複数)
     * @return true: すでに必要な権限は付与されている。false: 権限が不足している。
     */
    public static boolean hasRuntimePermissions(Context context, String[] permissions) {
        return (getDeniedPermissions(context, permissions).size() == 0);
    }

    /**
     * 指定された RUNTIMEパーミッションの内、許可されていないものを返す。
     *
     * @param context     コンテキスト
     * @param permissions 確認するパーミッション(複数)
     * @return 許可されていないバーミッションのリスト。すべて許可されている場合はサイズ0のリストを返す。(nullを返すことはない)
     */
    private static List<String> getDeniedPermissions(Context context, String[] permissions) {
        List<String> deniedPermissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT < 23) return deniedPermissions;
        if (permissions == null || permissions.length == 0) return deniedPermissions;

        for (String permission : permissions) {
            if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                // 許可のないpermissionを記録
                deniedPermissions.add(permission);
            }
        }
        return deniedPermissions;
    }

    public static boolean checkSelfPermission(Context context, String[] permissions) {
        if (Build.VERSION.SDK_INT < 23) return true;
        if (permissions == null || permissions.length == 0) return true;

        for (String permission : permissions) {
            if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                // 権限不足
                return false;
            }
        }
        return true;       // 必要なpermissionがすべてある
    }


    /**
     * onRequestPermissionsResult()で返された結果から、権限がすべて与えられたかチェックする
     *
     * @param grantResults チェックする権限
     * @return true: 必要な権限は全て付与されている。false: 権限が不足している。
     */
    public static boolean isGranted(int[] grantResults) {
        if (grantResults == null) return false;
        for (int result : grantResults) {
            // 必要な PERMISSIONは付与されなかった
            if (result != PackageManager.PERMISSION_GRANTED) return false;
        }
        // 必要なPERMISSIONがすべて付与されている
        return true;
    }

    public static boolean isPermissionRationale(Activity activity, String[] permissions) {
        if (Build.VERSION.SDK_INT >= 23) {
            for (String permission : permissions) {
                if (!activity.shouldShowRequestPermissionRationale(permission)) {
                    // 以前に 「今後は確認しない」にチェックが付けられた permission
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * DOZEの無効化画面を呼び出す。（既に無効化設定されている場合は何もしない）
     * この機能を利用するには、AndroidManifest.xmlに
     * "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" パーミッションの利用許可が必要
     *
     * @param context コンテキスト
     */
//    public static void requestDisableDozeModeIfNeeded(Context context) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            // DOZEの無効化設定リクエスト
//            String packageName = context.getPackageName();
//            PowerManager powerManager = context.getSystemService(PowerManager.class);
//            if (powerManager == null) return;
//            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
//                // request disabling doze
//                // ユーザに 指定のアプリを Doze無効にしてもらう
//
//                @SuppressLint("BatteryLife")
//                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//                intent.setData(Uri.parse("package:" + packageName));
//                context.startActivity(intent);
//            }
//        }
//    }


    //////////////////////////////////////////////////////////////////////////
    //
    // リソース系  (主にandroidのバージョンによる apiの違いを吸収するために用意)
    //

    /**
     * 指定されたリソースIDから Color値を返す
     * @param context    コンテキスト
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

    /**
     * Dimensionでsp単位でサイズ指定を行った場合、画面のdensityの影響をうけてしまうので
     * それを補正した値を返す
     * @param context コンテキスト
     * @param dimensionId dimensionリソースID
     * @return 取得された値
     */
    public static float getSpDimension(Context context, int dimensionId) {
        Resources res = context.getResources();
        return res.getDimension(dimensionId) / res.getDisplayMetrics().density * res.getConfiguration().fontScale;
    }

    /**
     * htmlの文字列から Spannedオブジェクトを返す
     * @param htmlText htmlの文字列
     * @return 生成されたSpannedオブジェクト
     */
    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String htmlText) {
        if (Build.VERSION.SDK_INT >= 24) {
            // API level 24以降ではこちらのメソッドを使用する
            return Html.fromHtml(htmlText, Html.FROM_HTML_MODE_LEGACY);
        } else {
            // API level 24以降では 非推奨
            return Html.fromHtml(htmlText);
        }
    }

    /**
     * 現在のテーマから、指定された属性IDの リソースIDを取り出す
     * @param context コンテキスト
     * @param attributeId 取得するColorの属性リソースID
     * @return 取得されたColor値
     */
    public static int getAttributeResourceId(Context context, int attributeId) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(attributeId, outValue, true);
        return outValue.resourceId;
    }

    /**
     * 現在のテーマから、指定された属性IDの Color値を取り出す
     * @param context コンテキスト
     * @param attributeId 取得するColorの属性リソースID
     * @return 取得されたColor値
     */
    public static int getAttributeColor(Context context, int attributeId) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(attributeId, outValue, true);
        return outValue.data;
    }


    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

}
