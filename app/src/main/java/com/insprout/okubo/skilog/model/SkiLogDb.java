package com.insprout.okubo.skilog.model;

import android.content.ContentValues;
import android.database.Cursor;

import com.insprout.okubo.skilog.database.DbConfiguration;
import com.insprout.okubo.skilog.database.DbSQLite;

import java.util.Date;
import java.util.Locale;

/**
 * Created by okubo on 2018/01/30.
 * データベースのレコードデータを格納するクラス
 */

public class SkiLogDb implements DbSQLite.IModelSQLite {
    public final static String TABLE_NAME = DbConfiguration.TABLE_1;

    private long mId;
    private float mAltitude;
    private float mAscTotal;
    private float mDescTotal;
    private int mCount;
    private Date mCreated;


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    // コンストラクタ
    //

    public SkiLogDb() {
    }

    public SkiLogDb(float altitude, float ascTotal, float descTotal, int count) {
        mAltitude = altitude;
        mAscTotal = ascTotal;
        mDescTotal = descTotal;
        mCount = count;
    }

    public SkiLogDb(long id, float altitude, float ascTotal, float descTotal, int count, Date created) {
        this(altitude, ascTotal, descTotal, count);
        mId = id;
        mCreated = created;
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    // 格納データの getter
    //

    public long getId() {
        return mId;
    }

    public float getAltitude() {
        return mAltitude;
    }

    public float getAscTotal() {
        return mAscTotal;
    }

    public float getDescTotal() {
        return mDescTotal;
    }

    public int getCount() {
        return mCount;
    }

    public Date getCreated() {
        return mCreated;
    }

    public void setCreated(Date created) {
        mCreated = created;
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    // Interfaceの 実装
    //

    @Override
    public String getTable() {
        return DbConfiguration.TABLE_1;
    }

    @Override
    public String getPrimaryKeyName() {
        return DbConfiguration.COL_1_0;
    }

    @Override
    public String getPrimaryKeyValue() {
        return Long.toString(mId);
    }

    @Override
    public String toCsvString() {
        return String.format(
                Locale.ENGLISH,
                "%d,%f,%f,%f,%d,%d",
                mId,
                mAltitude,
                mAscTotal,
                mDescTotal,
                mCount,
                (mCreated != null ? mCreated.getTime() : 0L));
    }

    @Override
    public DbSQLite.IModelSQLite fromCsvString(String csv) {
        String column[] = csv != null ? csv.split(",") : new String[0];
        return new SkiLogDb(
                column.length >= 1 ? DbSQLite.toLong(column[0], 0) : 0L,
                column.length >= 2 ? DbSQLite.toFloat(column[1], 0f) : 0f,
                column.length >= 3 ? DbSQLite.toFloat(column[2], 0f) : 0f,
                column.length >= 4 ? DbSQLite.toFloat(column[3], 0f) : 0f,
                column.length >= 5 ? DbSQLite.toInt(column[4], 0) : 0,
                new Date(column.length >= 6 ? DbSQLite.toLong(column[5], 0L) : 0L)
        );
    }

    @Override
    public ContentValues toRecord() {
        ContentValues record = new ContentValues();
        // _idは 自動採番なので設定しない
        record.put(DbConfiguration.COL_1_1, mAltitude);
        record.put(DbConfiguration.COL_1_2, mAscTotal);
        record.put(DbConfiguration.COL_1_3, mDescTotal);
        record.put(DbConfiguration.COL_1_4, mCount);
        if (mCreated != null) record.put(DbConfiguration.COL_1_5, DbSQLite.formatUtcDateTime(mCreated));
        return record;
    }

    @Override
    public DbSQLite.IModelSQLite fromDatabase(Cursor cursor) {
        return new SkiLogDb(
                cursor.getLong(cursor.getColumnIndex(DbConfiguration.COL_1_0)),
                cursor.getFloat(cursor.getColumnIndex(DbConfiguration.COL_1_1)),
                cursor.getFloat(cursor.getColumnIndex(DbConfiguration.COL_1_2)),
                cursor.getFloat(cursor.getColumnIndex(DbConfiguration.COL_1_3)),
                cursor.getInt(cursor.getColumnIndex(DbConfiguration.COL_1_4)),
                DbSQLite.getSQLiteDate(cursor, DbConfiguration.COL_1_5)
        );
    }
}
