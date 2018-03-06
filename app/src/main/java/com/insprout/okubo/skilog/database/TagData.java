package com.insprout.okubo.skilog.database;

import java.util.Date;

/**
 * Created by okubo on 2018/01/30.
 * データベースのレコードデータを格納するクラス
 */

public class TagData {

    private long mId;
    private Date mDate;
    private String mTag;
    private Date mCreated;
    private Date mUpdated;
    /*
    private static final String TABLE_2 = "ski_tag";
    private static final String COL_2_0 = "_id";                                // TABLE_2の 第0カラム(PRIMARY KEY)
    private static final String COL_2_1 = "date";                               // TABLE_2の 第1カラム
    private static final String COL_2_2 = "tag";                                // TABLE_2の 第2カラム
    private static final String COL_2_3 = "created";                            // TABLE_2の 第3カラム
    private static final String COL_2_4 = "updated";                            // TABLE_2の 第4カラム
    private static final String SQLITE_COUNT_2 = "COUNT(" + COL_2_0 + ")";      // sqliteは COUNT(*)だと遅い
    * */

    public TagData() {
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public String getIdStr() {
        return Long.toString(mId);
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

}
