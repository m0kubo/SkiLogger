package com.insprout.okubo.skilog.model;

import android.content.ContentValues;
import android.database.Cursor;

import com.insprout.okubo.skilog.database.DbConfiguration;
import com.insprout.okubo.skilog.database.DbSQLite;

import java.util.Date;
import java.util.List;
import java.util.Locale;


/**
 * Created by okubo on 2018/01/30.
 * データベースのレコードデータを格納するクラス
 */

public class TagDb implements DbSQLite.IModelSQLite {
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

    public TagDb() {
    }

    public TagDb(Date date, String tag) {
        mDate = date;
        mTag = tag;
    }

    public TagDb(long id, Date date, String tag, Date created, Date updated) {
        mId = id;
        mDate = date;
        mTag = tag;
        mCreated = created;
        mUpdated = updated;
    }


    public static String join(String glue, List<TagDb> tags) {
        if (tags == null) return "";
        StringBuilder builder = new StringBuilder();
        for(int i=0; i<tags.size(); i++) {
            if (i >= 1) builder.append(glue);
            builder.append(tags.get(i).toString());
        }
        return builder.toString();
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
    public String toCsvString() {
        return String.format(
                Locale.getDefault(),
                "%d,%d,%s,%d,%d",
                mId,
                (mDate != null ? mDate.getTime() : 0L),
                mTag != null ? mTag : "",
                (mCreated != null ? mCreated.getTime() : 0L),
                (mUpdated != null ? mUpdated.getTime() : 0L));
    }

    @Override
    public DbSQLite.IModelSQLite fromCsvString(String csv) {
        String column[] = csv != null ? csv.split(",") : new String[0];
        return new TagDb(
                column.length >= 1 ? DbSQLite.toLong(column[0], 0) : 0L,
                new Date(column.length >= 2 ? DbSQLite.toLong(column[1], 0) : 0L),
                column.length >= 3 ? column[2] : "",
                new Date(column.length >= 4 ? DbSQLite.toLong(column[3], 0) : 0L),
                new Date(column.length >= 5 ? DbSQLite.toLong(column[4], 0) : 0L)
        );
    }

    @Override
    public ContentValues toRecord() {
        ContentValues record = new ContentValues();
        // _idは 自動採番なので設定しない
        record.put(DbConfiguration.COL_2_1, DbSQLite.formatUtcDateTime(mDate));
        record.put(DbConfiguration.COL_2_2, mTag);
        if (mCreated != null) record.put(DbConfiguration.COL_2_3, DbSQLite.formatUtcDateTime(mCreated));
        record.put(DbConfiguration.COL_2_4, DbSQLite.formatUtcDateTime(new Date(System.currentTimeMillis())));
        return record;
    }

    @Override
    public DbSQLite.IModelSQLite fromDatabase(Cursor cursor) {
        return new TagDb(
                cursor.getLong(0),
                DbSQLite.toUtcDate(cursor.getString(1)),
                cursor.getString(2),
                DbSQLite.toUtcDate(cursor.getString(3)),
                DbSQLite.toUtcDate(cursor.getString(4))
        );
    }
}
