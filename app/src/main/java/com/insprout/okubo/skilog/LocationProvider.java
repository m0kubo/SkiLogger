package com.insprout.okubo.skilog;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;

import java.util.logging.Logger;

/**
 * Created by okubo on 2018/03/08.
 * 位置情報を取得する
 */

public class LocationProvider implements LocationListener {

    private Context mContext;
    private LocationManager mLocationManager;
    private Handler mHandler;

    private OnLocatedListener listener;

    public LocationProvider(Context context, OnLocatedListener listener) {
        mContext = context;
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mHandler = null;
        this.listener = listener;
    }

    /**
     * networkプロバイダーで、位置測位を試みる
     * RUNTIMEパーミッション ACCESS_COARSE_LOCATION権限の確認も行う
     */
    public void requestMeasure() {

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
//            criteria.setPowerRequirement(Criteria.POWER_LOW);

            String provider = mLocationManager.getBestProvider(criteria, true);
            if (provider != null) {
                // timeout用の Handlerを設定する
                if (mHandler != null) {
                    // Handlerを作り直すので、Runnableが登録されていた場合は、removeしておく
                    mHandler.removeCallbacks(mTimeoutTask);
                }
                mHandler = new Handler();
                mHandler.postDelayed(mTimeoutTask, 30000);

                mLocationManager.requestLocationUpdates(provider, 1000, 1, this);

                // 計測が正常に開始
                return;
            }
        }

        // プロバイダーが無効の場合は、位置測位しない
        listener.onLocationChanged(null);
    }

    private int checkSelfPermission(String permission) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return mContext.checkSelfPermission(permission);
        } else {
            return PackageManager.PERMISSION_GRANTED;
        }
    }

    public void cancelMeasure() {
        mLocationManager.removeUpdates(this);
    }

    final private Runnable mTimeoutTask = new Runnable() {
        @Override
        public void run() {
            cancelMeasure();
            listener.onLocationChanged(null);
        }
    };

    ////////////////////////////////////////////////////
    //
    // LocationListenerの implement
    //

    @Override
    public void onLocationChanged(Location location) {
        if (mHandler != null) {
            mHandler.removeCallbacks(mTimeoutTask);
            mHandler = null;
        }
        cancelMeasure();
        listener.onLocationChanged(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }


    ////////////////////////////////////////////////////
    //
    // static メソッド
    //

    public static boolean isEnabled(Context context) {
        return isProviderEnabled(context, LocationManager.GPS_PROVIDER) || isProviderEnabled(context, LocationManager.NETWORK_PROVIDER);
    }

    private static boolean isProviderEnabled(Context context, String provider) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return (locationManager != null && locationManager.isProviderEnabled(provider));
    }


    ////////////////////////////////////////////////////
    //
    // interface
    //

    public interface OnLocatedListener {
        void onLocationChanged(Location location);
    }
}
