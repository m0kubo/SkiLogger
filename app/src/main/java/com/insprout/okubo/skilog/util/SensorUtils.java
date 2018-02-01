package com.insprout.okubo.skilog.util;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.List;

/**
 * Created by okubo on 2018/01/25.
 */

public class SensorUtils {

    /**
     * 気圧センサーを取得する
     * 端末に気圧センサーが搭載されていない場合は nullを返す
     * @param context コンテキスト
     * @return 気圧センサー
     */
    public static Sensor getPressureSensor(Context context) {
        SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_PRESSURE);
            if (!sensors.isEmpty()) return sensors.get(0);
        }
        return null;
    }

    public static void registerListener(Context context, SensorEventListener listener, Sensor sensor, int sampling) {
        if (sensor == null) return;
        SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            sensorManager.registerListener(listener, sensor, sampling);
        }
    }

    public static void unregisterListener(Context context, SensorEventListener listener) {
        SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            sensorManager.unregisterListener(listener);
        }
    }

    /**
     * 気圧から標高を計算する。
     * ただし、海面気圧は 1気圧(1013.25hPa)、温度は 0℃とする
     * @param hPa 気圧
     * @return 標高 (メートル)
     */
    public static float getAltitude(float hPa) {
        float P0 = 1013.25f;            // 海面気圧(高度0ｍでの気圧)。 1013.25 = 1気圧
        float TEMP = 0.0f;        // 気温 (雪山想定なので 0℃と仮定)

        return getAltitude(hPa, P0, TEMP);
    }

    /**
     * 気圧、海面気圧、温度から標高を計算する
     * @param hPa 気圧
     * @param p0 海抜0ｍでの気圧
     * @param temperature 気温
     * @return 標高 (メートル)
     */
    public static float getAltitude(float hPa, float p0, float temperature) {
        return (float)((Math.pow(p0 / hPa, 1/5.257f) - 1) * (temperature + 273.15f) / 0.0065f);
    }
}
