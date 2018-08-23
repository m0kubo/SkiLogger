package com.insprout.okubo.skilog.database;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.Date;

/**
 * Created by okubo on 2018/01/30.
 * データベースのレコードデータを格納するクラス
 */

public class ModelSkiLog implements IModelSQLite {
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

    public ModelSkiLog() {
    }

    public ModelSkiLog(float altitude, float ascTotal, float descTotal, int count) {
        mAltitude = altitude;
        mAscTotal = ascTotal;
        mDescTotal = descTotal;
        mCount = count;
    }

    public ModelSkiLog(long id, float altitude, float ascTotal, float descTotal, int count, Date created) {
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
    public ContentValues getRecord() {
        ContentValues record = new ContentValues();
        record.put(DbConfiguration.COL_1_1, mAltitude);
        record.put(DbConfiguration.COL_1_2, mAscTotal);
        record.put(DbConfiguration.COL_1_3, mDescTotal);
        record.put(DbConfiguration.COL_1_4, mCount);
        return record;
    }

    @Override
    public IModelSQLite fromDatabase(Cursor cursor) {
        return new ModelSkiLog(
                cursor.getLong(cursor.getColumnIndex(DbConfiguration.COL_1_0)),
                cursor.getFloat(cursor.getColumnIndex(DbConfiguration.COL_1_1)),
                cursor.getFloat(cursor.getColumnIndex(DbConfiguration.COL_1_2)),
                cursor.getFloat(cursor.getColumnIndex(DbConfiguration.COL_1_3)),
                cursor.getInt(cursor.getColumnIndex(DbConfiguration.COL_1_4)),
                DbSQLite.getSQLiteDate(cursor, DbConfiguration.COL_1_5)
        );
    }
}
