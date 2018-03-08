package com.insprout.okubo.skilog.model;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by okubo on 2018/03/08.
 * Google Place apiの レスポンスの jsonを 格納するクラス
 */

public class ResponsePlaceData {

    @SerializedName("status")
    private String mStatus;

    @SerializedName("results")
    private List<PlaceData> mPlaces;


    public String getStatus() {
        return mStatus;
    }

    public boolean isStatusOk() {
        return ("OK".equalsIgnoreCase(mStatus));
    }

    public List<PlaceData> getPlaces() {
        return mPlaces;
    }

    /**
     * json文字列からこのクラスのインスタンスを生成する静的メソッド
     * 与えられたjsonが不正な場合はnullを返す
     * @param json json文字列
     * @return 生成されたインスタンス
     */
    public static ResponsePlaceData fromJson(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return new Gson().fromJson(json, ResponsePlaceData.class);

        } catch(JsonSyntaxException e) {
            return null;
        }
    }
}
