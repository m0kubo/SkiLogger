package com.insprout.okubo.skilog.database;

import android.content.Context;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by okubo on 2018/01/30.
 * データベース操作用 ユーティリティクラス
 */

public class DbUtils {

    public static List<SkiLogData> selectAll(Context context) {
        List<SkiLogData> res = new ArrayList<>();

        SkiLogDb fbkDatabase = null;
        try {
            fbkDatabase = new SkiLogDb(context);
            res = fbkDatabase.selectFromTable1();

        } catch(Exception ex) {
            return res;

        } finally {
            if (fbkDatabase != null) fbkDatabase.close();
        }
        return res;
    }

    public static List<SkiLogData> select(Context context, Date date) {
        return select(context, date, 0, 0, null);
    }

    public static List<SkiLogData> select(Context context, Date date, int offset, int limit, String orderBy) {
        List<SkiLogData> res = new ArrayList<>();
        if (date == null) return res;

        String selection = String.format(Locale.ENGLISH, "date(created,'%s') = ?", SkiLogDb.utcModifier());
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
     * 日別記録の内 1日で記録時間が最も遅い記録を抽出する
     * 一日一件、複数日分の記録を返す。最大100日分
     * @param context コンテキスト
     * @return データ(複数件)
     */
    public static List<SkiLogData> selectDailyLogs(Context context) {
        return selectDailyLogs(context, 0, 100);
    }

    /**
     * 日別記録の内 1日で記録時間が最も遅い記録を抽出する
     * 一日一件、複数日分の記録を返す。
     * @param context コンテキスト
     * @param offset 取得するデータのオフセット
     * @param limit 取得するデータの件数上限
     * @return データ(複数件)
     */
    public static List<SkiLogData> selectDailyLogs(Context context, int offset, int limit) {
        List<SkiLogData> res = new ArrayList<>();

        SkiLogDb fbkDatabase = null;
        try {
            fbkDatabase = new SkiLogDb(context);
//            res = fbkDatabase.selectFromTable1();
            // SQLiteの CURRENT_DATEは UTCで記録されているので、日付で集計する場合は時差を補正する事
            // 日本時間以外に対応する場合に、考慮すべき
            res = fbkDatabase.selectDailySummaries(offset, limit, "_id DESC");

        } catch(Exception ex) {
            return res;

        } finally {
            if (fbkDatabase != null) fbkDatabase.close();
        }
        return res;
    }

    public static List<SkiLogData> listByRawQuery(Context context, String sql, String[] selectionArgs) {
        List<SkiLogData> res = new ArrayList<>();

        SkiLogDb fbkDatabase = null;
        try {
            fbkDatabase = new SkiLogDb(context);
//            res = fbkDatabase.selectFromTable1();
            // SQLiteの CURRENT_DATEは UTCで記録されているので、日付で集計する場合は時差を補正する事
            // 日本時間以外に対応する場合に、考慮すべき
            res = fbkDatabase.listByRawQuery(sql, selectionArgs);

        } catch(Exception ex) {
            return res;

        } finally {
            if (fbkDatabase != null) fbkDatabase.close();
        }
        return res;
    }


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

}
