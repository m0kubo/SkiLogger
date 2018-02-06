package com.insprout.okubo.skilog.util;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by okubo on 2018/02/01.
 */

public class TimeUtils {

    // month は 0～11、0⇒1月  11⇒12月
    public static Date toDate(int year, int month, int day) {
        return toDate(year, month, day, 0, 0, 0);
    }

    // month は 0～11、0⇒1月  11⇒12月
    public static Date toDate(int year, int month, int day, int hour, int minute, int second) {
        Date date;
        Calendar cal = Calendar.getInstance();
        cal.setLenient(false);          // 日付のチェックを厳密に行う

        try {
            cal.set(year, month, day, hour, minute, second);
            cal.set(Calendar.MILLISECOND, 0);
            date = cal.getTime();

        } catch (IllegalArgumentException e){
            // 不正な日付の場合は nullを返す
            return null;
        }
        return date;
    }

    // Dateオブジェクトの getYear()メソッドが  非推奨になったため 推奨されるCalendarオブジェクトで作成
    public static int getYear(Date date) {
        Calendar cal = Calendar.getInstance();
        if (date != null) cal.setTime(date);
        return cal.get(Calendar.YEAR);
    }

    // Dateオブジェクトの getMonth()メソッドが deprecatedになったため 推奨されるCalendarオブジェクトで作成
    public static int getMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        if (date != null) cal.setTime(date);
        return cal.get(Calendar.MONTH);
    }

    // Dateオブジェクトの getDay()メソッドが deprecatedになったため 推奨されるCalendarオブジェクトで作成
    public static int getDay(Date date) {
        Calendar cal = Calendar.getInstance();
        if (date != null) cal.setTime(date);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    // Dateオブジェクトの getHour()メソッドが deprecatedになったため 推奨されるCalendarオブジェクトで作成
    public static int getHour(Date date) {
        Calendar cal = Calendar.getInstance();
        if (date != null) cal.setTime(date);
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    // Dateオブジェクトの getMinutes()メソッドが deprecatedになったため 推奨されるCalendarオブジェクトで作成
    public static int getMinutes(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.get(Calendar.MINUTE);
    }


    public static Date addYears(Date date, int years) {
        Calendar cal = Calendar.getInstance();
        if (date != null) cal.setTime(date);
        cal.add(Calendar.YEAR, years);
        return cal.getTime();
    }

    public static Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        if (date != null) cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    public static Date addTime(Date date, int second) {
        Calendar cal = Calendar.getInstance();
        if (date != null) cal.setTime(date);
        cal.add(Calendar.SECOND, second);
        return cal.getTime();
    }

    public static long subDate(Date date1, Date date2) {
        if (date1 == null || date2 == null) return 0;
        return date1.getTime() - date2.getTime();
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
     * unixtimeで指定される時間の 0時0分の時刻を Date型で返す
     * @param timeInMills unixtime
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

    // Calendarオブジェクトで作成 年、月、日、時、分、秒、ミリ秒 を一度に取得する
    // それぞれの値を配列で返す
    public static int[] getTime(Date date) {
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
}
