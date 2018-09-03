package com.insprout.okubo.skilog.database;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.Closeable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


public class DbSQLite implements Closeable {

    public interface IModelSQLite {
        String getTable();

        String getPrimaryKeyName();

        String getPrimaryKeyValue();

        ContentValues toRecord();

        IModelSQLite fromDatabase(Cursor cursor);
    }


    private static final String TAG = "database";

    private final static String SQLITE_DATE_FORMAT = "yyyy-MM-dd";
    private final static String SQLITE_TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private final static String SQLITE_TIMEZONE = "UTC";

    private SQLiteDatabase mDb = null;


    public DbSQLite(SQLiteDatabase database) {
        // Helperを使用してデータベースを開く
        mDb = database;
    }

    public DbSQLite(SQLiteOpenHelper helper) {
        // Helperを使用してデータベースを開く
        mDb = helper.getWritableDatabase();
    }

    @Override
    public void close() {
        try {
            if (mDb != null) mDb.close();
        } catch (SQLException ignored) {
        }
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


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    // 共通操作の メソッド
    //

    /**
     * 指定のテーブルから指定されたレコードを削除する
     * @param tableName 操作するテーブル
     * @param selection where句
     * @param selectionArgs where句にあたえる引数
     * @return 結果 boolean
     */
    public boolean deleteFromTable(String tableName, String selection, String[] selectionArgs) {
        try {
            int count = mDb.delete(tableName, selection, selectionArgs);
            if (count == 0) return false;

        } catch (SQLException e) {
            // SQLite error
            Log.e(TAG, "DB error: " + e.toString());
            return false;
        }
        return true;
    }

    /**
     * 指定のテーブルから全てのレコードを削除する
     * (空のテーブルとなる。DROP TABLEではない)
     * @param tableName 操作するテーブル
     * @return 結果 boolean
     */
    public boolean deleteAllFromTable(String tableName) {
        if (tableName == null || tableName.isEmpty()) return false;

        try {
            // 全削除の際 DROP TABLEは使用しない。 次回使用時に Helperコンストラクタの、onCreate()が呼ばれない為エラーになる
            mDb.delete(tableName, "1", null);
            mDb.execSQL( "vacuum" );

        } catch (SQLException e) {
            // SQLite error
            Log.e(TAG, "DB error: " + e.toString());
            return false;
        }
        return true;
    }


    /**
     * 指定のテーブルの 該当するレコードの数を返す
     * @param tableName 操作するテーブル
     * @param countColumn COUNTに使用するカラム名 (SQLiteは COUNT(*)だと遅いので Primary keyなどを指定する)
     * @param selection where句
     * @param selectionArgs where句にあたえる引数
     * @return 結果件数
     */
    public long countFromTable(String tableName, String countColumn, String selection, String[] selectionArgs) {
        String countCommand = String.format("COUNT(%s)", countColumn);          // sqliteは COUNT(*)だと遅いので、primaryキーをカウントする

        try (Cursor cursor = mDb.query(tableName,
                new String[]{ countCommand },
                selection,
                selectionArgs,
                null,
                null,
                null)) {

            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }

        } catch (SQLiteException e) {
            // SQLite error
            Log.e(TAG, "DB error: " + e.toString());
        }
        return 0;
    }


    public long update(String tableName, ContentValues record, String selection, String[] selectionArgs) {
        long count = 0;

        if (tableName == null || tableName.isEmpty() || record == null) return 0;
        try {
            // 挿入するデータはContentValuesに格納
            count = mDb.update(tableName, record, selection, selectionArgs);

        } catch (SQLException e) {
            // SQLite error
            Log.e(TAG, "DB error: " + e.toString());
        }
        return count;
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    // IModelSQLiteインターフェースをパラメータに指定するメソッド
    //

    /**
     * レコードを 1件削除する
     * @param model 削除するデータ。idのみ参照
     * @return 結果 boolean
     */
    public boolean delete(IModelSQLite model) {
        if (model == null) return false;

        return deleteFromTable(model.getTable(), primarySelection(model), primaryArgs(model));
    }

    /**
     * 指定のログdataの recordを updateする
     * @param model 更新する内容
     * @return 変更した行数。成功の場合は 1が返る
     */
    public long update(IModelSQLite model) {
        if (model == null) return 0;
        return update(model.getTable(), model.toRecord(), primarySelection(model), primaryArgs(model));
    }


    /**
     * ログdataを データベースに insertする
     * @param model DBに登録する内容
     * @return insertされたレコードのID。エラーの場合は -1
     */
    public long insert(IModelSQLite model) {
        long insertedId = -1;

        if (model == null) return -1;
        try {
            // 挿入するデータはContentValuesに格納
            insertedId = mDb.insert(model.getTable(), null, model.toRecord());

        } catch (SQLException e) {
            // SQLite error
            Log.e(TAG, "DB error: " + e.toString());
        }
        return insertedId;
    }

    /**
     * 指定のログdataの recordを replace (INSERT OR REPLACE)する
     * @param model 更新する内容
     * @return 挿入/変更した行の rowId。失敗の場合は -1が返る
     */
    public long replace(IModelSQLite model){
        long rowId = -1;

        if (model == null) return -1;
        try {
            // 挿入するデータはContentValuesに格納
            rowId = mDb.replace(model.getTable(), null, model.toRecord());

        } catch (SQLException e) {
            // SQLite error
            Log.e(TAG, "DB error: " + e.toString());
        }
        return rowId;
    }

    /**
     * テーブルの全件数を返す
     * @param model カウントするテーブルの model。テーブル名/Primary Key名を参照
     * @return 結果 件数
     */
    public long countAll(IModelSQLite model) {
        if (model == null) return 0;

        return count(model, null, null );
    }

    /**
     * テーブルの全件数を返す
     * @param model カウントするテーブルの model。テーブル名/Primary Key名を参照
     * @return 結果 件数
     */
    public long count(IModelSQLite model, String selection, String[] selectionArgs) {
        if (model == null) return 0;

        return countFromTable(model.getTable(), model.getPrimaryKeyName(), selection, selectionArgs);
    }


    /**
     * selectクエリーを実行しその結果を modelのListで返す
     * @param model 情報を取得するテーブルの model
     * @param selection where句
     * @param selectionArgs where句にあたえる引数
     * @param offset オフセット
     * @param limit 取得件数
     * @param orderBy 順序
     * @param groupBy グループ化条件
     * @return 結果
     */
    public List<? extends IModelSQLite> select(IModelSQLite model, String selection, String[] selectionArgs, int offset, int limit, String orderBy, String groupBy) {
        if (model == null) return null;

        List<IModelSQLite> result = new ArrayList<>();

        try (Cursor cursor = mDb.query(
                model.getTable(),
                null,           // 全カラム返却
                selection,
                selectionArgs,
                groupBy,
                null,
                orderBy,
                limitArg(offset, limit))) {

            while (cursor.moveToNext()) {
                // 取得したデータを格納する
                IModelSQLite data = model.fromDatabase(cursor);
                result.add(data);
            }

        } catch (SQLiteException e) {
            // SQLite error
            Log.e(TAG, "DB error: " + e.toString());
        }

        return result;
    }


    /**
     * primaryキーで指定される 1レコードのデータを取得する
     * @param model sqlの結果を格納するデータの model
     * @return 取得した1レコードの結果
     */
    public IModelSQLite fetch(IModelSQLite model) {
        if (model == null) return null;

        List<? extends IModelSQLite>  results = select(model, primarySelection(model), primaryArgs(model), 0, 0, null, null);
        return (results != null && !results.isEmpty() ? results.get(0) : null);
    }


    /**
     * SQLを rawQueryで実行して その結果を SkiLogDataクラスの Listを返す
     * ただし、SQLの結果列は、SkiLogDataクラスに格納できる形式であること
     * (ski_logテーブルの デフォルトカラム列と 同様であること。SELECT * FROM ski_log ～ であれば問題なし)
     * @param model sqlの結果を格納するデータの model
     * @param sql SQLコマンド
     * @param selectionArgs SQLコマンドに渡すパラメータ列
     * @return SQLの結果
     */
    public List<? extends IModelSQLite> rawQuery(IModelSQLite model, String sql, String[] selectionArgs) {
        List<IModelSQLite> result = new ArrayList<>();

        try (Cursor cursor = mDb.rawQuery(sql, selectionArgs)) {
            while( cursor.moveToNext() ) {
                // 取得したデータを格納する
                IModelSQLite data = model.fromDatabase(cursor);
                result.add(data);
            }

        } catch (SQLiteException e) {
            // SQLite error
            Log.e(TAG, "DB error: " + e.toString());
        }
        return result;
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    // private メソッド
    //

    // primaryキー名に基づく selection文字列を返す
    private String primarySelection(IModelSQLite model) {
        return model.getPrimaryKeyName() + " = ?";
    }

    // primaryキー値のみの selectionArgs配列を返す
    private String[] primaryArgs(IModelSQLite model) {
        return new String[] { model.getPrimaryKeyValue() };
    }

    // offset、limitの値から SQLite関数にあたえる limit文字列を生成する
    private String limitArg(int offset, int limit) {
        return (limit > 0 && offset >= 0 ? Integer.toString(offset) + "," + Integer.toString(limit) : null);
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    // static メソッド
    //

    /**
     * 指定のカラムから SQLiteの TimeStampを Date型に変換して返す
     * @param cursor SQLiteの cursorオブジェクト
     * @param columnName TimeStampが格納されているカラム名
     * @return 変換されたDate値
     */
    public static Date getSQLiteDate(Cursor cursor, String columnName) {
        if (cursor == null) return null;
        return toUtcDate(cursor.getString(cursor.getColumnIndex(columnName)));
    }

    /**
     * Sqliteの TIMESTAMP型での CURRENT_TIMESTAMPは UTCで記録されるので、UTC TimeZoneで Date型に変換する
     * @param sqliteTimeStamp SQLiteの タイムスタンプ文字列
     * @return 変換されたDate値
     */
    public static Date toUtcDate(String sqliteTimeStamp) {
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
