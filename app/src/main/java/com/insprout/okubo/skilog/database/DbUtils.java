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


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    // Logデータ関連 メソッド
    //

    /**
     * Logデータの件数を取得する
     * @param context コンテキスト
     * @return 件数
     */
    public static long countLogs(Context context) {
        long res = 0;

        SkiLogDb fbkDatabase = null;
        try {
            fbkDatabase = new SkiLogDb(context);
            res = fbkDatabase.countFromTable1(null, null);

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
    public static long insertLog(Context context, SkiLogData data) {
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

    /**
     * Log用 Tableの行を更新する
     * @param context コンテキスト
     * @param data updateするLogデータ
     * @return updateされた行の行数。成功の場合は1。エラーの場合は0
     */
    public static long updateLog(Context context, SkiLogData data) {
        long id = 0;

        SkiLogDb database = null;
        try {
            database = new SkiLogDb(context);
            id = database.update(data);

        } finally {
            if (database != null) database.close();
        }
        return id;
    }


    /**
     * 指定された日付の ログデータを削除する。
     * @param context コンテキスト
     * @param date 削除するデータの日付。(時刻は無視される。日付のみが有効)
     * @return 結果
     */
    public static boolean deleteLogs(Context context, Date date) {
        boolean res;
        if (date == null) return false;

        String selection = String.format("date(created,'%s') = ?", SkiLogDb.utcModifier());
        String[] selectionArgs = { SkiLogDb.formatDate(date) };

        SkiLogDb fbkDatabase = null;
        try {
            fbkDatabase = new SkiLogDb(context);
            res = fbkDatabase.deleteFromTable1(selection, selectionArgs);

        } catch(Exception ex) {
            return false;

        } finally {
            if (fbkDatabase != null) fbkDatabase.close();
        }
        return res;
    }

    /**
     * 指定された日付の ログデータを削除する。
     * @param context コンテキスト
     * @param fromDate 指定日時以降のデータを取得する (指定時刻ジャストのデータを含む)
     * @param toDate 指定日時未満のデータを取得する (指定時刻ジャストのデータは含まない)
     * @return 結果
     */
    public static boolean deleteLogs(Context context, Date fromDate, Date toDate) {
        if (fromDate == null || toDate == null || !fromDate.before(toDate)) return false;

        boolean res;
        String selection = "created >= ? AND created < ?";
        String[] selectionArgs = new String[] { SkiLogDb.formatUtcDateTime(fromDate), SkiLogDb.formatUtcDateTime(toDate) };

        SkiLogDb fbkDatabase = null;
        try {
            fbkDatabase = new SkiLogDb(context);
            res = fbkDatabase.deleteFromTable1(selection, selectionArgs);

        } catch(Exception ex) {
            return false;

        } finally {
            if (fbkDatabase != null) fbkDatabase.close();
        }
        return res;
    }

    public static List<SkiLogData> selectLogs(Context context, Date date) {
        return selectLogs(context, date, 0, 0, null);
    }

    public static List<SkiLogData> selectLogs(Context context, Date date, int offset, int limit, String orderBy) {
        List<SkiLogData> res = new ArrayList<>();
        if (date == null) return res;

        String selection = String.format("date(created,'%s') = ?", SkiLogDb.utcModifier());
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
     * @param limit 取得するデータの件数上限。0以下の値が指定された場合は全件を取得する
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
            //res = fbkDatabase.selectLogSummaries(fromDate, toDate);
            String sqlCmd = String.format("SELECT * FROM ski_log WHERE _id IN (SELECT MAX(_id) FROM ski_log WHERE created >= ? AND created < ? GROUP BY date(created,'%1$s'))", SkiLogDb.utcModifier());
            res = DbUtils.listByRawQuery(
                    context,
                    sqlCmd,
                    new String[]{ SkiLogDb.formatUtcDateTime(fromDate), SkiLogDb.formatUtcDateTime(toDate) } );

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
     * @param tag データを絞りこむタグ
     * @return データ(複数件)
     */
    public static List<SkiLogData> selectLogSummaries(Context context, String tag) {
        List<SkiLogData> res = new ArrayList<>();

        SkiLogDb fbkDatabase = null;
        try {
            fbkDatabase = new SkiLogDb(context);
            String sqlCmd = String.format("SELECT * FROM ski_log WHERE _id IN (SELECT MAX(_id) FROM ski_log WHERE date(created,'%1$s') IN (SELECT date(date,'%1$s') FROM ski_tag WHERE tag = ? ) GROUP BY date(created,'%1$s'))", SkiLogDb.utcModifier());
            res = DbUtils.listByRawQuery(
                    context,
                    sqlCmd,
                    new String[]{ tag });

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

    public static List<TagData> listByRawQueryOnTable2(Context context, String sql, String[] sqlArgs) {
        List<TagData> res = new ArrayList<>();

        SkiLogDb fbkDatabase = null;
        try {
            fbkDatabase = new SkiLogDb(context);
            res = fbkDatabase.listByRawQueryOnTable2(sql, sqlArgs);

        } catch(Exception ex) {
            return res;

        } finally {
            if (fbkDatabase != null) fbkDatabase.close();
        }
        return res;
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    // Logデータ関連 メソッド
    //

    /**
     * tagデータの件数を取得する
     * @param context コンテキスト
     * @return 件数
     */
    public static long countTags(Context context) {
        long res = 0;

        SkiLogDb fbkDatabase = null;
        try {
            fbkDatabase = new SkiLogDb(context);
            res = fbkDatabase.countFromTable2(null, null);

        } catch(Exception ex) {
            return res;

        } finally {
            if (fbkDatabase != null) fbkDatabase.close();
        }
        return res;
    }

    /**
     * tag用 Tableに新規行を挿入する
     * @param context コンテキスト
     * @param data insertするLogデータ
     * @return 挿入された行の id。エラーの場合は0
     */
    public static long insertTag(Context context, TagData data) {
        long id = 0;

        SkiLogDb database = null;
        try {
            database = new SkiLogDb(context);
            id = database.insertIntoTable2(data);

        } finally {
            if (database != null) database.close();
        }
        return id;
    }


    /**
     * 日別記録のサマリー情報を取得する。1日1サマリー、複数日分の記録を返す。
     * orderは 更新時刻の新しい順
     * @param context コンテキスト
     * @return データ(複数件)
     */
    public static List<TagData> selectTags(Context context) {
        List<TagData> res = new ArrayList<>();

        SkiLogDb fbkDatabase = null;
        try {
            fbkDatabase = new SkiLogDb(context);
            res = fbkDatabase.selectFromTable2(null, null, 0, 0, "updated DESC", null);

        } catch(Exception ex) {
            return res;

        } finally {
            if (fbkDatabase != null) fbkDatabase.close();
        }
        return res;
    }


}
