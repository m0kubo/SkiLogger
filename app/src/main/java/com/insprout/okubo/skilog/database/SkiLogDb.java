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
import java.util.TimeZone;

/**
 * Created by okubo on 2018/01/30.
 * データベースアクセス用 クラス
 */

public class SkiLogDb {
    private static final String TAG = "database";

    private final static String SQLITE_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final static String SQLITE_TIMEZONE = "UTC";

    private static final String TABLE_1 = "ski_log";
    private static final String COL_1_0 = "_id";
    private static final String COL_1_1 = "altitude";
    private static final String COL_1_2 = "asc_total";
    private static final String COL_1_3 = "desc_total";
    private static final String COL_1_4 = "count";
    private static final String COL_1_5 = "created";
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

    private boolean deleteFromTable1(String selection, String[] selectionArgs) {
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
        return selectFromTable1(null, null, 0, 0, null );
    }

    public List<SkiLogData> selectFromTable1(long recordId) {
        String[] args = { Long.toString(recordId) };
        return selectFromTable1(COL_1_0 + " = ?", args, 0, 0, null );
    }

    public List<SkiLogData> selectFromTable1(String selection, String[] selectionArgs, int limit, int offset, String orderBy) {
        Cursor cursor = null;
        List<SkiLogData> result = new ArrayList<>();

        try{
            cursor = mDb.query( TABLE_1,
                    null,           // 全カラム返却
                    selection,
                    selectionArgs,
                    null,
                    null,
                    orderBy,
                    (limit > 0 ? String.format("%s,%s", offset, limit) : null));

            while( cursor.moveToNext() ) {
                // redmine #4251 start
                SkiLogData data = new SkiLogData();
                data.setId(cursor.getLong(cursor.getColumnIndex(COL_1_0)));
                data.setAltitude(cursor.getFloat(cursor.getColumnIndex(COL_1_1)));
                data.setAscTotal(cursor.getFloat(cursor.getColumnIndex(COL_1_2)));
                data.setDescTotal(cursor.getFloat(cursor.getColumnIndex(COL_1_3)));
                data.setCount(cursor.getInt(cursor.getColumnIndex(COL_1_4)));
                data.setCreated(getSQLiteDate(cursor, COL_1_5));
                // ここでは オブジェクトで返す
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

            if (cursor.moveToNext()) {
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


    private Date getSQLiteDate(Cursor cursor, String columnName) {
        if (cursor == null) return null;
        return toDate(cursor.getString(cursor.getColumnIndex(columnName)));
    }

    /**
     * Sqliteの TIMESTAMP型での CURRENT_TIMESTAMPは UTCで記録されるので、UTC TimeZoneで Date型に変換する
     * @param sqliteTimeStamp SQLiteの タイムスタンプ文字列
     * @return 変換されたDate値
     */
    private static Date toDate(String sqliteTimeStamp) {
        try {
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat dfUtc = new SimpleDateFormat(SQLITE_TIMESTAMP_FORMAT);
            dfUtc.setTimeZone(TimeZone.getTimeZone(SQLITE_TIMEZONE));
            return dfUtc.parse(sqliteTimeStamp);

        } catch (ParseException e) {
            return null;
        }
    }

}
