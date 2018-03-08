package com.insprout.okubo.skilog.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by okubo on 2018/03/08.
 * Google Place apiの レスポンスjsonの "results"句を 格納するクラス
 */

public class PlaceData {

    @SerializedName("id")
    private String mId;

    @SerializedName("place_id")
    private String mPlaceId;

    @SerializedName("name")
    private String mName;

    @SerializedName("icon")
    private String mUrlIcon;

    @SerializedName("rating")
    private float mRating;

    @SerializedName("types")
    private List<String> mTypes;

    @SerializedName("vicinity")
    private String mVicinity;

    @Override
    public String toString() {
        return mName;
    }
}
