package com.insprout.okubo.skilog.database;

import android.content.Context;

import com.insprout.okubo.skilog.model.SkiLogDb;
import com.insprout.okubo.skilog.model.TagDb;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return database.countAll(new SkiLogDb());
        }
    }

    /**
     * Log用 Tableに新規行を挿入する
     * @param context コンテキスト
     * @param data insertするLogデータ
     * @return 挿入された行の id。エラーの場合は0
     */
    public static long insertLog(Context context, SkiLogDb data) {
        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return database.insert(data);
        }
    }

    /**
     * Log用 Tableの行を更新する
     * @param context コンテキスト
     * @param data updateするLogデータ
     * @return updateされた行の行数。成功の場合は1。エラーの場合は0
     */
    public static long updateLog(Context context, SkiLogDb data) {
        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return database.update(data);
        }
    }


    /**
     * 指定された日付の ログデータを削除する。
     * @param context コンテキスト
     * @param date 削除するデータの日付。(時刻は無視される。日付のみが有効)
     * @return 結果
     */
    public static boolean deleteLogs(Context context, Date date) {
        if (date == null) return false;

        String selection = String.format("date(created,'%s') = ?", DbSQLite.utcModifier());
        String[] selectionArgs = { DbSQLite.formatDate(date) };

        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return database.deleteFromTable(SkiLogDb.TABLE_NAME, selection, selectionArgs);
        }
    }

    /**
     * 指定された期間の ログデータを削除する。
     * @param context コンテキスト
     * @param fromDate 指定日時以降のデータを取得する (指定時刻ジャストのデータを含む)
     * @param toDate 指定日時未満のデータを取得する (指定時刻ジャストのデータは含まない)
     * @return 結果
     */
    public static boolean deleteLogs(Context context, Date fromDate, Date toDate) {
        if (fromDate == null || toDate == null || !fromDate.before(toDate)) return false;

        String selection = "created >= ? AND created < ?";
        String[] selectionArgs = new String[] { DbSQLite.formatUtcDateTime(fromDate), DbSQLite.formatUtcDateTime(toDate) };

        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return database.deleteFromTable(SkiLogDb.TABLE_NAME, selection, selectionArgs);
        }
    }

    public static List<SkiLogDb> selectLogs(Context context, Date date) {
        return selectLogs(context, date, 0, 0, null);
    }

    @SuppressWarnings("unchecked")
    public static List<SkiLogDb> selectLogs(Context context, Date date, int offset, int limit, String orderBy) {
        if (date == null) return new ArrayList<>();

        String selection = String.format("date(created,'%s') = ?", DbSQLite.utcModifier());
        //String selection = String.format(Locale.ENGLISH, "date(created,'%s') = ? AND altitude > -500", SkiLogDb.utcModifier());
        String[] selectionArgs = { DbSQLite.formatDate(date) };

        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return (List<SkiLogDb>)database.select(new SkiLogDb(), selection, selectionArgs, offset, limit, orderBy, null);
        }
    }

    // 指定された日付の 記録開始日時と記録終了日時を取得する
    public static Date[] selectTimePeriods(Context context, Date date) {
        // 非効率だが、開始時刻と 終了時刻は 2回に分けて取得する
        List<SkiLogDb> startTime = selectLogs(context, date, 0, 1, "created ASC");
        if (startTime == null || startTime.size() < 1) return null;

        List<SkiLogDb> endTime = selectLogs(context, date, 0, 1, "created DESC");
        if (endTime == null || endTime.size() < 1) return null;

        Date[] times = new Date[2];
        times[0] = startTime.get(0).getCreated();
        times[1] = endTime.get(0).getCreated();
        return times;
    }

    /**
     * 日別記録のサマリー情報を取得する。1日1サマリー、複数日分の記録を返す。
     * orderは 日付の古い順
     * @param context コンテキスト
     * @param offset 取得するデータのオフセット
     * @param limit 取得するデータの件数上限。0以下の値が指定された場合は全件を取得する
     * @return データ(複数件)
     */
    @SuppressWarnings("unchecked")
    public static List<SkiLogDb> selectLogSummaries(Context context, int offset, int limit) {
        String sqlCmd = String.format(
                "SELECT * FROM ski_log WHERE _id IN (SELECT MAX(_id) FROM ski_log GROUP BY date(created,'%1$s')) %2$s",
                DbSQLite.utcModifier(),
                sqlLimit(offset, limit)
        );

        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return (List<SkiLogDb>)database.rawQuery(new SkiLogDb(), sqlCmd, null);
        }
    }

    /**
     * 日別記録のサマリー情報を取得する。1日1サマリー、複数日分の記録を返す。
     * orderは 日付の古い順
     * @param context コンテキスト
     * @param fromDate 指定日時以降のデータを取得する (指定時刻ジャストのデータを含む)
     * @param toDate 指定日時未満のデータを取得する (指定時刻ジャストのデータは含まない)
     * @return データ(複数件)
     */
    @SuppressWarnings("unchecked")
    public static List<SkiLogDb> selectLogSummaries(Context context, Date fromDate, Date toDate) {
        String sqlCmd = String.format(
                "SELECT * FROM ski_log WHERE _id IN (SELECT MAX(_id) FROM ski_log WHERE created >= ? AND created < ? GROUP BY date(created,'%1$s'))",
                DbSQLite.utcModifier()
        );
        String[] sqlArgs = { DbSQLite.formatUtcDateTime(fromDate), DbSQLite.formatUtcDateTime(toDate) };

        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return (List<SkiLogDb>)database.rawQuery(new SkiLogDb(), sqlCmd, sqlArgs);
        }
    }

    /**
     * 日別記録のサマリー情報を取得する。1日1サマリー、複数日分の記録を返す。
     * orderは 日付の古い順
     * @param context コンテキスト
     * @param tag データを絞りこむタグ
     * @return データ(複数件)
     */
    @SuppressWarnings("unchecked")
    public static List<SkiLogDb> selectLogSummaries(Context context, String tag) {
        // タグが指定されてない場合は全日分取得
        if (tag == null) return selectLogSummaries(context, 0, 0);

        String sqlCmd = String.format(
                "SELECT * FROM ski_log WHERE _id IN (SELECT MAX(_id) FROM ski_log WHERE date(created,'%1$s') IN (SELECT date(date,'%1$s') FROM ski_tag WHERE tag = ? ) GROUP BY date(created,'%1$s'))",
                DbSQLite.utcModifier()
        );
        String[] sqlArgs = { tag };

        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return (List<SkiLogDb>)database.rawQuery(new SkiLogDb(), sqlCmd, sqlArgs);
        }
    }

    private static String sqlLimit(int offset, int limit) {
        String limitCmd = "";
        if (limit >= 1) {
            limitCmd = "LIMIT " + limit;
            if (offset >= 1) {
                limitCmd += " OFFSET " + offset;
            }
        }
        return limitCmd;
    }


    public static int exportLogs(Context context, File exportFile) {
        if (exportFile == null) return -1;

        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return database.exportToFile(new SkiLogDb(), exportFile);
        }
    }

    public static int importLogs(Context context, File importFile) {
        if (importFile == null || !importFile.exists() || !importFile.isFile()) return -1;

        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            DbSQLite.IModelSQLite model = new SkiLogDb();
            database.deleteAllFromTable(model.getTable());      // 全てのレコードを削除
            database.resetAutoIncrement(model.getTable());      // auto-incrementの情報をリセット
            return database.importFromFile(model, importFile);  // データimport
        }
    }

    public static int exportTags(Context context, File exportFile) {
        if (exportFile == null) return -1;

        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return database.exportToFile(new TagDb(), exportFile);
        }
    }

    public static int importTags(Context context, File importFile) {
        if (importFile == null || !importFile.exists() || !importFile.isFile()) return -1;

        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            DbSQLite.IModelSQLite model = new TagDb();
            database.deleteAllFromTable(model.getTable());      // 全てのレコードを削除
            database.resetAutoIncrement(model.getTable());      // auto-incrementの情報をリセット
            return database.importFromFile(model, importFile);  // データimport
        }
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
        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return database.countAll(new TagDb());
        }
    }

    /**
     * tag用 Tableから指定行を削除する
     * @param context コンテキスト
     * @param data 削除するtagデータ
     * @return 結果
     */
    public static boolean deleteTag(Context context, TagDb data) {
        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return database.delete(data);
        }
    }

    /**
     * 指定された日付の タグデータを削除する。
     * @param context コンテキスト
     * @param targetDate 削除するタグの日付
     * @return 結果
     */
    public static boolean deleteTags(Context context, Date targetDate) {
        if (targetDate == null) return false;

        String selection = "date(date, ?) = ?";
        String[] selectionArgs = new String[] { DbSQLite.utcModifier(), DbSQLite.formatDate(targetDate) };

        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return database.deleteFromTable(TagDb.TABLE_NAME, selection, selectionArgs);
        }
    }

    /**
     * 指定された期間の タグデータを削除する。
     * @param context コンテキスト
     * @param fromDate 指定日時以降のデータを取得する (指定時刻ジャストのデータを含む)
     * @param toDate 指定日時未満のデータを取得する (指定時刻ジャストのデータは含まない)
     * @return 結果
     */
    public static boolean deleteTags(Context context, Date fromDate, Date toDate) {
        if (fromDate == null || toDate == null || !fromDate.before(toDate)) return false;

        String selection = "created >= ? AND created < ?";
        String[] selectionArgs = new String[] { DbSQLite.formatUtcDateTime(fromDate), DbSQLite.formatUtcDateTime(toDate) };

        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return database.deleteFromTable(TagDb.TABLE_NAME, selection, selectionArgs);
        }
    }

    /**
     * tag用 Tableに新規行を挿入する
     * @param context コンテキスト
     * @param data insertするLogデータ
     * @return 挿入された行の id。エラーの場合は0
     */
    public static long insertTag(Context context, TagDb data) {
        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return database.insert(data);
        }
    }


    /**
     * 指定された日付の タグデータを取得する。
     * @param context コンテキスト
     * @param targetDate 取得するタグの日付
     * @return データ(複数件)
     */
    @SuppressWarnings("unchecked")
    public static List<TagDb> selectTags(Context context, Date targetDate) {
        String selection = null;
        String[] selectionArgs = null;

        if (targetDate != null) {
            selection = "date(date, ?) = ?";
            selectionArgs = new String[] { DbSQLite.utcModifier(), DbSQLite.formatDate(targetDate) };
        }

        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return (List<TagDb>)database.select(new TagDb(), selection, selectionArgs, 0, 0, "updated DESC", null);
        }
    }

    /**
     * 重複しないタグのデータを取得する。
     * 結果は、付与された日時の新しい順で返される
     * @param context コンテキスト
     * @return データ(複数件)
     */
    @SuppressWarnings("unchecked")
    public static List<TagDb> selectDistinctTags(Context context) {
        // 新しい順でソートさせたいので、DISTINCTを使用しない。
        // _idは自動採番なので、値の大きいものが 新しいと見做す
        String sqlCmd = "SELECT * FROM ski_tag WHERE _id IN (SELECT MAX(_id) FROM ski_tag GROUP BY tag) ORDER BY _id DESC";

        // try-with-resources構文で closeを自動的に呼び出す
        try (DbSQLite database = new DbSQLite(new DbConfiguration(context))) {
            return (List<TagDb>)database.rawQuery(new TagDb(), sqlCmd, null);
        }

    }

}
