package com.insprout.okubo.skilog.database;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.Date;


/**
 * Created by okubo on 2018/01/30.
 * データベースのレコードデータを格納するクラス
 */

public class ModelTag implements IModelSQLite {
    public final static String TABLE_NAME = DbConfiguration.TABLE_2;

    private long mId;
    private Date mDate;
    private String mTag;
    private Date mCreated;
    private Date mUpdated;


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    // コンストラクタ
    //

    public ModelTag() {
    }

    public ModelTag(Date date, String tag) {
        mDate = date;
        mTag = tag;
    }

    public ModelTag(long id, Date date, String tag, Date created, Date updated) {
        mId = id;
        mDate = date;
        mTag = tag;
        mCreated = created;
        mUpdated = updated;
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    // 格納データの getter
    //

    public long getId() {
        return mId;
    }

    public Date getDate() {
        return mDate;
    }

    public String getTag() {
        return mTag;
    }

    public Date getCreated() {
        return mCreated;
    }

    public Date getUpdated() {
        return mUpdated;
    }

    @Override
    public String toString() {
        return getTag();
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    // Interfaceの 実装
    //

    @Override
    public String getTable() {
        return DbConfiguration.TABLE_2;
    }

    @Override
    public String getPrimaryKeyName() {
        return DbConfiguration.COL_2_0;
    }

    @Override
    public String getPrimaryKeyValue() {
        return Long.toString(mId);
    }

    @Override
    public ContentValues toRecord() {
        ContentValues record = new ContentValues();
        record.put(DbConfiguration.COL_2_1, DbSQLite.formatUtcDateTime(mDate));
        record.put(DbConfiguration.COL_2_2, mTag);
        record.put(DbConfiguration.COL_2_4, DbSQLite.formatUtcDateTime(new Date(System.currentTimeMillis())));
        return record;
    }

    @Override
    public IModelSQLite fromDatabase(Cursor cursor) {
        return new ModelTag(
                cursor.getLong(0),
                DbSQLite.toUtcDate(cursor.getString(1)),
                cursor.getString(2),
                DbSQLite.toUtcDate(cursor.getString(3)),
                DbSQLite.toUtcDate(cursor.getString(4))
        );
    }
}
