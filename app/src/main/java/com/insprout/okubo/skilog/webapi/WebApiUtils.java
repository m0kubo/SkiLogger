package com.insprout.okubo.skilog.webapi;

import android.location.Location;

import com.insprout.okubo.skilog.R;

import java.util.Locale;

/**
 * Created by okubo on 2018/03/08.
 * web api関連の ユーティリティクラス
 */

public class WebApiUtils {

    public static RequestUrlTask googlePlaceApi(String apiKey, Location location, RequestUrlTask.OnResponseListener listener) {
        return googlePlaceApi(apiKey, location.getLatitude(), location.getLongitude(), listener);
    }

    public static RequestUrlTask googlePlaceApi(String apiKey, double latitude, double longitude, RequestUrlTask.OnResponseListener listener) {
        // google place APIを使用して 付近の施設情報を取得する
        final String FMT_PLACE_API = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%f,%f&radius=5000&type=point_of_interest&key=%s";
        String url = String.format(Locale.ENGLISH, FMT_PLACE_API, latitude, longitude, apiKey);

        return new RequestUrlTask(url, listener);
    }

}
