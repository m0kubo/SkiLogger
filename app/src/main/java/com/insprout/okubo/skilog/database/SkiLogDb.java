package com.insprout.okubo.skilog.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Created by okubo on 2018/01/30.
 * データベースアクセス用 クラス
 */

public class SkiLogDb {
    private static final String TAG = "database";

    private final static String SQLITE_DATE_FORMAT = "yyyy-MM-dd";
    private final static String SQLITE_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final static String SQLITE_TIMEZONE = "UTC";

    private static final String TABLE_1 = "ski_log";
    private static final String COL_1_0 = "_id";                                // TABLE_1の 第0カラム(PRIMARY KEY)
    private static final String COL_1_1 = "altitude";                           // TABLE_1の 第1カラム
    private static final String COL_1_2 = "asc_total";                          // TABLE_1の 第2カラム
    private static final String COL_1_3 = "desc_total";                         // TABLE_1の 第3カラム
    private static final String COL_1_4 = "count";                              // TABLE_1の 第4カラム
    private static final String COL_1_5 = "created";                            // TABLE_1の 第5カラム
    private static final String SQLITE_COUNT_1 = "COUNT(" + COL_1_0 + ")";      // sqliteは COUNT(*)だと遅い

    /**
     * DB および テーブル作成用 ヘルパークラス
     */
    private class DatabaseHelper extends SQLiteOpenHelper {

        private static final int DATABASE_VERSION = 3;
        private static final String DATABASE_NAME = "skilogger.db";


        // コンストラクタ
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // テーブルを作成する。DBファイルが存在しない場合に呼ばれる
            // DBファイルはあるけど、TABLEが存在しない場合には呼ばれないので注意
            db.execSQL(
                    "CREATE TABLE " + TABLE_1 + " ("
                            + COL_1_0 + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + COL_1_1 + " REAL NOT NULL DEFAULT 0, "
                            + COL_1_2 + " REAL NOT NULL DEFAULT 0, "
                            + COL_1_3 + " REAL NOT NULL DEFAULT 0, "
                            + COL_1_4 + " INTEGER NOT NULL DEFAULT 0, "
                            + COL_1_5 + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP )" );
        }

        @Override
        public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        }

    }


    private SQLiteDatabase mDb = null;

    public SkiLogDb(Context context) {
        // Helperを使用してデータベースを開く
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        mDb = dbHelper.getWritableDatabase();
    }

    public void close() {
        if (mDb != null) mDb.close();
    }

    public void beginTransaction() {
        if (mDb != null) mDb.beginTransaction();
    }

    public void setTransactionSuccessful() {
        if (mDb != null) mDb.setTransactionSuccessful();
    }

    public void endTransaction() {
        if (mDb != null) mDb.endTransaction();
    }


    /**
     * ログdataを データベースに insertする
     * @param data DBに登録する内容
     * @return insertされたレコードのID。エラーの場合は -1
     */
    public long insert(SkiLogData data){
        long insertedId = -1;

        if (data == null) return -1;
        try {
            // 挿入するデータはContentValuesに格納
            ContentValues record = new ContentValues();
            record.put(COL_1_1, data.getAltitude());
            record.put(COL_1_2, data.getAscTotal());
            record.put(COL_1_3,  data.getDescTotal());
            record.put(COL_1_4, data.getCount());
            insertedId = mDb.insert(TABLE_1, null, record);

        } catch (SQLException e) {
            // SQLite error
            Log.e(TAG, "DB error: " + e.toString());
        }
        return insertedId;
    }


    /**
     * 指定のログdataの recordを updateする
     * @param data 更新する内容
     * @return 変更した行数。成功の場合は 1が返る
     */
    public long update(SkiLogData data){
        long count = 0;

        if (data == null) return 0;
        String[] args = { data.getIdStr() };
        try {
            // 挿入するデータはContentValuesに格納
            ContentValues record = new ContentValues();
            record.put(COL_1_1, data.getAltitude());
            record.put(COL_1_2, data.getAscTotal());
            record.put(COL_1_3,  data.getDescTotal());
            record.put(COL_1_4, data.getCount());
            count = mDb.update(TABLE_1, record, COL_1_0 + " = ?", args);

        } catch (SQLException e) {
            // SQLite error
            Log.e(TAG, "DB error: " + e.toString());
        }
        return count;
    }


    /**
     * レコードを 1件削除する
     * @param data 削除するデータ。idのみ参照
     * @return 結果 boolean
     */
    public boolean delete(SkiLogData data) {
        if (data == null) return false;

        String[] args = { data.getIdStr() };
        return deleteFromTable1(COL_1_0 + " = ?", args );
    }

    public boolean deleteFromTable1(String selection, String[] selectionArgs) {
        try {
            int count = mDb.delete(TABLE_1, selection, selectionArgs);
            if (count == 0) return false;

        } catch (SQLException e) {
            // SQLite error
            Log.e(TAG, "DB error: " + e.toString());
            return false;
        }
        return true;
    }

    /** ファイルデータを検索 */

    public List<SkiLogData> selectFromTable1() {
        return selectFromTable1(null, null, 0, 0, null, null);
    }

    public List<SkiLogData> selectFromTable1(long recordId) {
        String[] args = { Long.toString(recordId) };
        return selectFromTable1(COL_1_0 + " = ?", args, 0, 0, null, null);
    }


//    public List<SkiLogData> selectFromTable1(String selection, String[] selectionArgs, int offset, int limit, String orderBy) {
//        return selectFromTable1(selection, selectionArgs, offset, limit, orderBy, null);
//    }

    public List<SkiLogData> selectFromTable1(String selection, String[] selectionArgs, int offset, int limit, String orderBy, String groupBy) {
        Cursor cursor = null;
        List<SkiLogData> result = new ArrayList<>();

        try{
            cursor = mDb.query( TABLE_1,
                    null,           // 全カラム返却
                    selection,
                    selectionArgs,
                    groupBy,
                    null,
                    orderBy,
                    (limit > 0 ? String.format("%s,%s", offset, limit) : null));

            while( cursor.moveToNext() ) {
                // 取得したデータを格納する
                SkiLogData data = new SkiLogData();
                data.setId(cursor.getLong(cursor.getColumnIndex(COL_1_0)));
                data.setAltitude(cursor.getFloat(cursor.getColumnIndex(COL_1_1)));
                data.setAscTotal(cursor.getFloat(cursor.getColumnIndex(COL_1_2)));
                data.setDescTotal(cursor.getFloat(cursor.getColumnIndex(COL_1_3)));
                data.setCount(cursor.getInt(cursor.getColumnIndex(COL_1_4)));
                data.setCreated(getSQLiteDate(cursor, COL_1_5));
                result.add(data);
            }

        } catch (SQLiteException e) {
            // SQLite error
            Log.e(TAG, "DB error: " + e.toString());

        } finally {
            // Cursorを忘れずにcloseする
            if (cursor != null) cursor.close();
        }
        return result;
    }

    public List<SkiLogData> selectLogSummaries(int offset, int limit) {
        return selectLogSummaries(null, null, offset, limit, null);
    }

    public List<SkiLogData> selectLogSummaries(Date fromDate, Date toDate) {
        return selectLogSummaries(fromDate, toDate, 0, 0, null);
    }

    public List<SkiLogData> selectLogSummaries(Date fromDate, Date toDate, int offset, int limit, String orderBy) {
        Cursor cursor = null;
        List<SkiLogData> result = new ArrayList<>();
        String groupBy = String.format("date(created,'%s')", utcModifier());
        String where = null;
        String whereArgs[] = null;

        if (fromDate != null && toDate != null) {
            // 開始日時、終了日時が指定されている場合
            where = "created >= ? AND created < ?";
            whereArgs = new String[] { formatUtcDateTime(fromDate), formatUtcDateTime(toDate) };

        } else if (fromDate != null) {
            // 開始日時のみが指定されている場合
            where = "created >= ?";
            whereArgs = new String[] { formatUtcDateTime(fromDate) };

        } else if (toDate != null) {
            // 終了日時のみが指定されている場合
            where = "created < ?";
            whereArgs = new String[] { formatUtcDateTime(toDate) };
        }

        try{
            cursor = mDb.query( TABLE_1,
                    new String[] { "MAX(_id)", "MAX(altitude)", "MAX(asc_total)", "MIN(desc_total)", "MAX(count)", "MAX(created)" },           // 全カラム返却
                    where,
                    whereArgs,
                    groupBy,
                    null,
                    orderBy,
                    (limit > 0 ? String.format("%s,%s", offset, limit) : null));

            while( cursor.moveToNext() ) {
                // 取得したデータを格納する
                SkiLogData data = new SkiLogData();
                data.setId(cursor.getLong(0));
                data.setAltitude(cursor.getFloat(1));
                data.setAscTotal(cursor.getFloat(2));
                data.setDescTotal(cursor.getFloat(3));
                data.setCount(cursor.getInt(4));
                data.setCreated(toUtcDate(cursor.getString(5)));
                result.add(data);
            }

        } catch (SQLiteException e) {
            // SQLite error
            Log.e(TAG, "DB error: " + e.toString());

        } finally {
            // Cursorを忘れずにcloseする
            if (cursor != null) cursor.close();
        }
        return result;
    }

    /**
     * SQLを rawQueryで実行して その結果を SkiLogDataクラスの Listを返す
     * ただし、SQLの結果列は、SkiLogDataクラスに格納できる形式であること
     * (ski_logテーブルの デフォルトカラム列と 同様であること。SELECT * FROM ski_log ～ であれば問題なし)
     * @param sql SQLコマンド
     * @param selectionArgs SQLコマンドに渡すパラメータ列
     * @return SQLの結果 (SkiLogDataクラスのList形式)
     */
    public List<SkiLogData> listByRawQuery(String sql, String[] selectionArgs) {
        Cursor cursor = null;
        List<SkiLogData> result = new ArrayList<>();

        try{
            cursor = mDb.rawQuery(sql, selectionArgs);

            while( cursor.moveToNext() ) {
                // 取得したデータを格納する
                SkiLogData data = new SkiLogData();
                data.setId(cursor.getLong(0));
                data.setAltitude(cursor.getFloat(1));
                data.setAscTotal(cursor.getFloat(2));
                data.setDescTotal(cursor.getFloat(3));
                data.setCount(cursor.getInt(4));
                data.setCreated(toUtcDate(cursor.getString(5)));
                result.add(data);
            }

        } catch (SQLiteException e) {
            // SQLite error
            Log.e(TAG, "DB error: " + e.toString());

        } finally {
            // Cursorを忘れずにcloseする
            if (cursor != null) cursor.close();
        }
        return result;
    }

    public long countFromTable1() {
        return countFromTable1(null, null );
    }

    public long countFromTable1(String selection, String[] selectionArgs) {
        Cursor cursor = null;
        long count = 0;

        try{
            cursor = mDb.query( TABLE_1,
                    new String[] { SQLITE_COUNT_1 },
                    selection,
                    selectionArgs,
                    null,
                    null,
                    null);

            if (cursor.moveToFirst()) {
                count =cursor.getInt(0);
            }

        } catch (SQLiteException e) {
            // SQLite error
            Log.e(TAG, "DB error: " + e.toString());

        } finally {
            // Cursorを忘れずにcloseする
            if (cursor != null) cursor.close();
        }
        return count;
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    // static
    //

    /**
     * 指定のカラムから SQLiteの TimeStampを Date型に変換して返す
     * @param cursor SQLiteの cursorオブジェクト
     * @param columnName TimeStampが格納されているカラム名
     * @return 変換されたDate値
     */
    private Date getSQLiteDate(Cursor cursor, String columnName) {
        if (cursor == null) return null;
        return toUtcDate(cursor.getString(cursor.getColumnIndex(columnName)));
    }

    /**
     * Sqliteの TIMESTAMP型での CURRENT_TIMESTAMPは UTCで記録されるので、UTC TimeZoneで Date型に変換する
     * @param sqliteTimeStamp SQLiteの タイムスタンプ文字列
     * @return 変換されたDate値
     */
    private static Date toUtcDate(String sqliteTimeStamp) {
        try {
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat dfUtc = new SimpleDateFormat(SQLITE_TIMESTAMP_FORMAT);
            dfUtc.setTimeZone(TimeZone.getTimeZone(SQLITE_TIMEZONE));
            return dfUtc.parse(sqliteTimeStamp);

        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * 与えられた date型変数から SQLite形式の日付文字列に変換する
     * 日付のみ(時刻は無し)。端末設定のタイムゾーン
     * @param date 変換する日時
     * @return SQLite形式の日付文字列
     */
    public static String formatDate(Date date) {
        if (date == null || date.getTime() == Long.MIN_VALUE) return null;
        SimpleDateFormat df = new SimpleDateFormat(SQLITE_DATE_FORMAT, Locale.getDefault());
        return df.format(date);
    }

    /**
     * 与えられた date型変数から SQLite形式の日時文字列に変換する
     * 端末設定のタイムゾーン
     * @param date 変換する日時
     * @return SQLite形式の日時文字列
     */
    public static String formatDateTime(Date date) {
        if (date == null || date.getTime() == Long.MIN_VALUE) return null;
        SimpleDateFormat df = new SimpleDateFormat(SQLITE_TIMESTAMP_FORMAT, Locale.getDefault());
        return df.format(date);
    }

    /**
     * 与えられた date型変数から SQLite形式のUTCでの日時文字列に変換する
     * @param date 変換する日時
     * @return SQLite形式の日時文字列
     */
    public static String formatUtcDateTime(Date date) {
        if (date == null || date.getTime() == Long.MIN_VALUE) return null;
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat dfUtc = new SimpleDateFormat(SQLITE_TIMESTAMP_FORMAT);
        dfUtc.setTimeZone(TimeZone.getTimeZone(SQLITE_TIMEZONE));
        return dfUtc.format(date);
    }

    /**
     * 現在の端末設定での TimeZoneとUTCの時差を SQLiteの 時差modifier文字列で返す
     * JSTの場合は、時差9時間なので "+32400 seconds"が 返る
     * @return SQLiteの modifier文字列
     */
    public static String utcModifier() {
        // 現在の端末設定の TimeZoneと UTCの時差(秒数)を取得
        int timeDiff = TimeZone.getDefault().getRawOffset() / 1000;
        return String.format(Locale.ENGLISH, "%+d seconds", timeDiff);
    }
}
