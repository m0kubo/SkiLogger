package com.insprout.okubo.skilog.database;

import java.util.Date;

/**
 * Created by okubo on 2018/01/30.
 * データベースのレコードデータを格納するクラス
 */

public class SkiLogData {

    private long mId;
    private float mAltitude;
    private float mAscTotal;
    private float mDescTotal;
    private int mCount;
    private Date mCreated;

    public SkiLogData() {
    }

    public SkiLogData(float altitude, float ascTotal, float descTotal, int count) {
        mAltitude = altitude;
        mAscTotal = ascTotal;
        mDescTotal = descTotal;
        mCount = count;
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

    public float getAltitude() {
        return mAltitude;
    }

    public void setAltitude(float altitude) {
        mAltitude = altitude;
    }

    public float getAscTotal() {
        return mAscTotal;
    }

    public void setAscTotal(float ascTotal) {
        mAscTotal = ascTotal;
    }

    public float getDescTotal() {
        return mDescTotal;
    }

    public void setDescTotal(float descTotal) {
        mDescTotal = descTotal;
    }

    public int getCount() {
        return mCount;
    }

    public void setCount(int count) {
        mCount = count;
    }

    public Date getCreated() {
        return mCreated;
    }

    public void setCreated(Date created) {
        mCreated = created;
    }
}
