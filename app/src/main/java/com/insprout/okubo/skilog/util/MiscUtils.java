package com.insprout.okubo.skilog.util;

import android.graphics.Bitmap;

import com.insprout.okubo.skilog.database.TagData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by okubo on 2018/02/01.
 * 雑多な ユーティリティ メソッド
 */

public class MiscUtils {

    /**
     * 与えられた値の中で、最小のものを返す。
     * Math.min()などとの違いは、任意の個数の値を指定できる所
     * @param values 比較する値(複数指定可)
     * @return 最小の値
     */
    public static float minValue(float... values) {
        if (values.length == 0) return Float.NEGATIVE_INFINITY;

        float min = Float.POSITIVE_INFINITY;
        for (float value : values) {
            if (value < min) min = value;
        }
        return min;
    }

    /**
     * 与えられた値の中で、最大のものを返す。
     * Math.max()などとの違いは、任意の個数の値を指定できる所
     * @param values 比較する値(複数指定可)
     * @return 最大の値
     */
    public static float maxValue(float... values) {
        if (values.length == 0) return Float.POSITIVE_INFINITY;

        float max = Float.NEGATIVE_INFINITY;
        for (float value : values) {
            if (value > max) max = value;
        }
        return max;
    }


    /**
     * 年、月、日を指定して Date型の値を返す
     * @param year 年 (2桁で省略しない。2015等指定する)
     * @param month 月、(0～11、0⇒1月  11⇒12月)
     * @param day 日 (1～ )
     * @return Date型の値
     */
    public static Date toDate(int year, int month, int day) {
        return toDate(year, month, day, 0, 0, 0);
    }

    /**
     * 年、月、日、時、分、秒を指定して Date型の値を返す
     * 秒数で、少数点以下の値は切り捨てて 0にする
     * @param year 年 (2桁で省略しない。2015等指定する)
     * @param month 月 (0～11、0⇒1月  11⇒12月)
     * @param day 日 (1～ )
     * @param hour 時 (0～23)
     * @param minute 分 (0～59)
     * @param second 秒 (0～59)
     * @return Date型の値
     */
    public static Date toDate(int year, int month, int day, int hour, int minute, int second) {
        Calendar cal = Calendar.getInstance();
        cal.setLenient(false);          // 日付のチェックを厳密に行う

        try {
            cal.set(year, month, day, hour, minute, second);
            cal.set(Calendar.MILLISECOND, 0);                   // 小数点以下の秒数は 明示的に設定しないと不定になるので注意
            return new Date(cal.getTimeInMillis());

        } catch (IllegalArgumentException e){
            // 不正な日付の場合は nullを返す
            return null;
        }
    }

    /**
     * 指定された Date型の値から (端末設定のTimeZoneでの)年数を取得する
     * Dateオブジェクトの getYear()メソッドが  非推奨になったため 推奨されるCalendarオブジェクトで作成
     * @param date 入力する日付
     * @return (端末設定のTimeZoneでの)年数
     */
    public static int getYear(Date date) {
        Calendar cal = Calendar.getInstance();
        if (date != null) cal.setTime(date);
        return cal.get(Calendar.YEAR);
    }

    /**
     * 指定された Date型の値から (端末設定のTimeZoneでの)月を取得する
     * Dateオブジェクトの getYear()メソッドが  非推奨になったため 推奨されるCalendarオブジェクトで作成
     * @param date 入力する日付
     * @return 月 (0～11、0⇒1月  11⇒12月)
     */
    public static int getMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        if (date != null) cal.setTime(date);
        return cal.get(Calendar.MONTH);
    }

    /**
     * 指定された Date型の値から (端末設定のTimeZoneでの)日を取得する
     * Dateオブジェクトの getDay()メソッドが  非推奨になったため 推奨されるCalendarオブジェクトで作成
     * @param date 入力する日付
     * @return (端末設定のTimeZoneでの)日
     */
    public static int getDay(Date date) {
        Calendar cal = Calendar.getInstance();
        if (date != null) cal.setTime(date);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 指定された Date型の値から (端末設定のTimeZoneでの)時刻を取得する
     * Dateオブジェクトの getHour()メソッドが  非推奨になったため 推奨されるCalendarオブジェクトで作成
     * @param date 入力する日付
     * @return (端末設定のTimeZoneでの)時刻
     */
    public static int getHour(Date date) {
        Calendar cal = Calendar.getInstance();
        if (date != null) cal.setTime(date);
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 指定された Date型の値から (端末設定のTimeZoneでの)分を取得する
     * Dateオブジェクトの getMinutes()メソッドが  非推奨になったため 推奨されるCalendarオブジェクトで作成
     * @param date 入力する日付
     * @return (端末設定のTimeZoneでの)分
     */
    public static int getMinutes(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.MINUTE);
    }

    /**
     * 指定されたDate型の  (端末設定のTimeZoneでの) 年、月、日、時、分、秒、ミリ秒を 配列で返す
     * @param date 入力する日付
     * @return 7個のint型配列。内容は先頭から 年、月、日、時、分、秒、ミリ秒
     */
    public static int[] getDateValues(Date date) {
        int time[] = new int [7];

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        time[0] = cal.get(Calendar.YEAR);
        time[1] = cal.get(Calendar.MONTH);
        time[2] = cal.get(Calendar.DAY_OF_MONTH);
        time[3] = cal.get(Calendar.HOUR_OF_DAY);
        time[4] = cal.get(Calendar.MINUTE);
        time[5] = cal.get(Calendar.SECOND);
        time[6] = cal.get(Calendar.MILLISECOND);
        return time;
    }


    /**
     * 日付に 指定年数を加える。年数には負の値も指定可。
     * @param date 日付
     * @param years 加える年数。(負の値も指定可)
     * @return 結果の日付
     */
    public static Date addYears(Date date, int years) {
        Calendar cal = Calendar.getInstance();
        if (date != null) cal.setTime(date);
        cal.add(Calendar.YEAR, years);
        return cal.getTime();
    }

    /**
     * 日付に 指定日数を加える。日数には負の値も指定可。
     * @param date 日付
     * @param days 加える日数。(負の値も指定可)
     * @return 結果の日付
     */
    public static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        if (date != null) cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    /**
     * 日付に 指定秒数を加える。秒数には負の値も指定可。
     * @param date 日付
     * @param second 加える秒数。(負の値も指定可)
     * @return 結果の日付
     */
    public static Date addTime(Date date, int second) {
        Calendar cal = Calendar.getInstance();
        if (date != null) cal.setTime(date);
        cal.add(Calendar.SECOND, second);
        return cal.getTime();
    }

    /**
     * 2つのDate型の値での差分を返す。
     * 返される値は、ミリ秒単位
     * @param date1 引かれる値
     * @param date2 引く値
     * @return 差(ミリ秒単位)
     */
    public static long subDate(Date date1, Date date2) {
        if (date1 == null || date2 == null) return 0;
        return date1.getTime() - date2.getTime();
    }

    /**
     * ２つの Dateが (端末設定のTimeZoneで)同じ日付か判断する
     * (時分秒は考慮しない。年月日のみ)
     * @param date1 比較されるDate
     * @param date2 比較するDate
     * @return 結果
     */
    public static boolean isSameDate(Date date1, Date date2) {
        if (date1 == null || date2 == null) return false;
        int[] time1 = getDateValues(date1);
        int[] time2 = getDateValues(date2);

        return (time1[0]==time2[0] && time1[1]==time2[1] && time1[2]==time2[2]);
    }

    /**
     * 指定日付の 0時0分の時刻を Date型で返す
     * @param date 指定日時
     * @return 指定日の am0:00を示すDate型
     */
    public static Date getDate(Date date) {
        Calendar cal = Calendar.getInstance();
        if (date != null) cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    /**
     * unix timeで指定される時間の 0時0分の時刻を Date型で返す
     * @param timeInMills unix time(ミリ秒単位)
     * @return 指定日の am0:00を示すDate型
     */
    public static Date getDate(long timeInMills) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timeInMills);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

//    /**
//     * 与えられたListオブジェクトから String型の配列を返す。
//     * List<String>以外の型にも対応している。要素の toString()メソッドを呼び出すので、
//     * 要素が独自クラスの場合は適切に toString()メソッドをOverrideしておくこと。
//     * @param list String型の配列に変換する Listオブジェクト
//     * @return String型の配列
//     */
//    public static String[] toStringArray(List<?> list) {
//        return toStringArray(list, -1);
//    }
//
//    /**
//     * 与えられたListオブジェクトから String型の配列を返す。
//     * List<String>以外の型にも対応している。要素の toString()メソッドを呼び出すので、
//     * 要素が独自クラスの場合は適切に toString()メソッドをOverrideしておくこと。
//     * @param list String型の配列に変換する Listオブジェクト
//     * @param limit 生成するString配列数の上限 (1以上を指定する。0以下の場合は無視される)
//     * @return String型の配列
//     */
//    public static String[] toStringArray(List<?> list, int limit) {
//        if (list == null) return null;
//
//        int arraySize = list.size();
//        if (limit >= 1 && limit < arraySize ) arraySize = limit;
//        String[] array = new String[ arraySize ];
//        for (int i=0; i<arraySize; i++) {
//            array[i] = list.get(i).toString();
//        }
//        return array;
//    }

    public static boolean saveBitmap(File file, Bitmap bitmap) {
        if (bitmap == null) return false;

        boolean success = false;
        if (file != null) {
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file, false);
                // 画像のフォーマットと画質と出力先を指定して保存
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                fos.flush();

                success = true;

            } catch (Exception e) {
                //e.printStackTrace();

            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException ie) {
                        //fos = null;
                    }
                }
            }
        }
        // 明示的にBitmapのメモリを開放しておく
        bitmap.recycle();

        return success;
    }

}
