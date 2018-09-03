package com.insprout.okubo.skilog.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbConfiguration extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_NAME = "skilogger.db";


    public static final String TABLE_1 = "ski_log";
    public static final String COL_1_0 = "_id";                                // TABLE_1の 第0カラム(PRIMARY KEY)
    public static final String COL_1_1 = "altitude";                           // TABLE_1の 第1カラム
    public static final String COL_1_2 = "asc_total";                          // TABLE_1の 第2カラム
    public static final String COL_1_3 = "desc_total";                         // TABLE_1の 第3カラム
    public static final String COL_1_4 = "count";                              // TABLE_1の 第4カラム
    public static final String COL_1_5 = "created";                            // TABLE_1の 第5カラム

    public static final String TABLE_2 = "ski_tag";
    public static final String COL_2_0 = "_id";                                // TABLE_2の 第0カラム(PRIMARY KEY)
    public static final String COL_2_1 = "date";                               // TABLE_2の 第1カラム
    public static final String COL_2_2 = "tag";                                // TABLE_2の 第2カラム
    public static final String COL_2_3 = "created";                            // TABLE_2の 第3カラム
    public static final String COL_2_4 = "updated";                            // TABLE_2の 第4カラム

    /**
     * DB および テーブル作成用 ヘルパークラス
     */
    public DbConfiguration(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // テーブルを作成する。DBファイルが存在しない場合に呼ばれる
        // DBファイルはあるが、その中にTABLEが存在しない場合には呼ばれないので注意
        // TABLE_1作成
        createTable1(db);
        // TABLE_2作成
        createTable2(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion <= 3) {
            // DATABASE_VERSIONが 3以前からの アップグレードの場合は、TABLE_2が存在しないので作成する
            createTable2(db);
        }
    }

    private void createTable1(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE " + TABLE_1 + " ("
                        + COL_1_0 + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                        + COL_1_1 + " REAL NOT NULL DEFAULT 0, "
                        + COL_1_2 + " REAL NOT NULL DEFAULT 0, "
                        + COL_1_3 + " REAL NOT NULL DEFAULT 0, "
                        + COL_1_4 + " INTEGER NOT NULL DEFAULT 0, "
                        + COL_1_5 + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP )" );
    }

    private void createTable2(SQLiteDatabase db) {
        // セカンダリーのテーブルを作成する。
        db.execSQL(
                "CREATE TABLE " + TABLE_2 + " ("
                        + COL_2_0 + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                        + COL_2_1 + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + COL_2_2 + " TEXT, "
                        + COL_2_3 + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                        + COL_2_4 + " TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP )");
    }

}
