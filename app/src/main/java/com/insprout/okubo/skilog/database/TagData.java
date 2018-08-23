package com.insprout.okubo.skilog.database;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.Date;


/**
 * Created by okubo on 2018/01/30.
 * データベースのレコードデータを格納するクラス
 */

public class TagData implements IModelSQLite {
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

    public TagData() {
    }

    public TagData(Date date, String tag) {
        mDate = date;
        mTag = tag;
    }


    ////////////////////////////////////////////////////////////////////////////////////////
    //
    // 格納データの getter
    //

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public String getTag() {
        return mTag;
    }

    public void setTag(String tag) {
        mTag = tag;
    }

    public Date getCreated() {
        return mCreated;
    }

    public void setCreated(Date created) {
        mCreated = created;
    }

    public Date getUpdated() {
        return mUpdated;
    }

    public void setUpdated(Date updated) {
        mUpdated = updated;
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
    public ContentValues getRecord() {
        ContentValues record = new ContentValues();
        record.put(DbConfiguration.COL_2_1, DbSQLite.formatUtcDateTime(mDate));
        record.put(DbConfiguration.COL_2_2, mTag);
        record.put(DbConfiguration.COL_2_4, DbSQLite.formatUtcDateTime(new Date(System.currentTimeMillis())));
        return record;
    }

    @Override
    public IModelSQLite fromDatabase(Cursor cursor) {
        TagData data = new TagData();
        data.setId(cursor.getLong(0));
        data.setDate(DbSQLite.toUtcDate(cursor.getString(1)));
        data.setTag(cursor.getString(2));
        data.setCreated(DbSQLite.toUtcDate(cursor.getString(3)));
        data.setUpdated(DbSQLite.toUtcDate(cursor.getString(4)));
        return data;
    }
}
