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

    public TagData() {
    }

    public TagData(Date date, String tag) {
        mDate = date;
        mTag = tag;
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

    @Override
    public String toString() {
        return getTag();
    }

}
