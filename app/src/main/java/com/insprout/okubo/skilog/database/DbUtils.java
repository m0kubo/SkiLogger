package com.insprout.okubo.skilog.database;

import android.content.Context;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by okubo on 2018/01/30.
 * データベース操作用 ユーティリティクラス
 */

public class DbUtils {

    /**
     * Logデータの件数を取得する
     * @param context コンテキスト
     * @return 件数
     */
    public static long count(Context context) {
        long res = 0;

        SkiLogDb fbkDatabase = null;
        try {
            fbkDatabase = new SkiLogDb(context);
            res = fbkDatabase.countFromTable1();

        } catch(Exception ex) {
            return res;

        } finally {
            if (fbkDatabase != null) fbkDatabase.close();
        }
        return res;
    }

    /**
     * Log用 Tableに新規行を挿入する
     * @param context コンテキスト
     * @param data insertするLogデータ
     * @return 挿入された行の id。エラーの場合は0
     */
    public static long insert(Context context, SkiLogData data) {
        long id = 0;

        SkiLogDb database = null;
        try {
            database = new SkiLogDb(context);
            id = database.insert(data);

        } finally {
            if (database != null) database.close();
        }
        return id;
    }


    public static List<SkiLogData> select(Context context, Date date) {
        return select(context, date, 0, 0, null);
    }

    public static List<SkiLogData> select(Context context, Date date, int offset, int limit, String orderBy) {
        List<SkiLogData> res = new ArrayList<>();
        if (date == null) return res;

        String selection = String.format(Locale.ENGLISH, "date(created,'%s') = ?", SkiLogDb.utcModifier());
        //String selection = String.format(Locale.ENGLISH, "date(created,'%s') = ? AND altitude > -500", SkiLogDb.utcModifier());
        String[] selectionArgs = { SkiLogDb.formatDate(date) };

        SkiLogDb fbkDatabase = null;
        try {
            fbkDatabase = new SkiLogDb(context);
            res = fbkDatabase.selectFromTable1(selection, selectionArgs, offset, limit, orderBy, null);

        } catch(Exception ex) {
            return res;

        } finally {
            if (fbkDatabase != null) fbkDatabase.close();
        }
        return res;
    }


    /**
     * 日別記録のサマリー情報を取得する。1日1サマリー、複数日分の記録を返す。
     * orderは 日付の古い順
     * @param context コンテキスト
     * @param offset 取得するデータのオフセット
     * @param limit 取得するデータの件数上限
     * @return データ(複数件)
     */
    public static List<SkiLogData> selectLogSummaries(Context context, int offset, int limit) {
        List<SkiLogData> res = new ArrayList<>();

        SkiLogDb fbkDatabase = null;
        try {
            fbkDatabase = new SkiLogDb(context);
            res = fbkDatabase.selectLogSummaries(offset, limit);

        } catch(Exception ex) {
            return res;

        } finally {
            if (fbkDatabase != null) fbkDatabase.close();
        }
        return res;
    }

    /**
     * 日別記録のサマリー情報を取得する。1日1サマリー、複数日分の記録を返す。
     * orderは 日付の古い順
     * @param context コンテキスト
     * @param fromDate 指定日時以降のデータを取得する (指定時刻ジャストのデータを含む)
     * @param toDate 指定日時未満のデータを取得する (指定時刻ジャストのデータは含まない)
     * @return データ(複数件)
     */
    public static List<SkiLogData> selectLogSummaries(Context context, Date fromDate, Date toDate) {
        List<SkiLogData> res = new ArrayList<>();

        SkiLogDb fbkDatabase = null;
        try {
            fbkDatabase = new SkiLogDb(context);
//            res = DbUtils.listByRawQuery(
//                    context,
//                    "SELECT * FROM ski_log WHERE _id IN (SELECT MAX(_id) FROM ski_log WHERE created >= ? AND created < ? GROUP BY date(created, ?))",
//                    new String[]{SkiLogDb.formatUtcDateTime(fromDate), SkiLogDb.formatUtcDateTime(toDate), SkiLogDb.utcModifier()});
            res = fbkDatabase.selectLogSummaries(fromDate, toDate);

        } catch(Exception ex) {
            return res;

        } finally {
            if (fbkDatabase != null) fbkDatabase.close();
        }
        return res;
    }

    public static List<SkiLogData> listByRawQuery(Context context, String sql, String[] sqlArgs) {
        List<SkiLogData> res = new ArrayList<>();

        SkiLogDb fbkDatabase = null;
        try {
            fbkDatabase = new SkiLogDb(context);
            res = fbkDatabase.listByRawQuery(sql, sqlArgs);

        } catch(Exception ex) {
            return res;

        } finally {
            if (fbkDatabase != null) fbkDatabase.close();
        }
        return res;
    }


}
