package com.insprout.okubo.skilog;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Messenger;
import android.support.annotation.Nullable;
import android.util.Log;

import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.database.SkiLogData;

import java.util.Date;
import java.util.List;

/**
 * Created by okubo on 2018/01/25.
 * 気圧センサーにより、高度を測定するサービス
 * 機能的には シンプルなので、onStartCommand()で 特にアクションなどはチェックしない
 *
 * この Serviceプロセスが稼働していれば、高度測定中とみなす
 */

public class AltitudeLogService extends Service implements SensorEventListener {

    private static String CHANNEL_ID_SERVICE = "ID_SVC_ALTITUDE";
    private static final int ID_SERVICE_ONGOING = 100;


    private Messenger mReplyMessenger;                          // Activityに返信するためのメッセンジャー
    private ServiceMessenger.ReplyMessageHandler mReplyHandler; // 返信のためのHandler

    private Sensor mSensor = null;


    //////////////////////////
    //
    // Service 基本処理
    //

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mReplyMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }


    @Override
    public void onCreate() {
        super.onCreate();

        mReplyHandler = new ServiceMessenger.ReplyMessageHandler();
        mReplyMessenger = new Messenger(mReplyHandler);

        mSensor = SensorUtils.getPressureSensor(this);          // 気圧センサー取得
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (mSensor != null) {
            setForegroundMode(this);
            // 気圧センサーがあれば、リスナーを登録する
            SensorUtils.registerListener(this, this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

        } else {
            // 気圧センサーがないのであれば、サービスを停止する
            stopSelf();
        }

        // START_STICKYの場合は 再起動時にintentに nullが渡される場合がある。
        // intentが nullだと困る場合があるなら、START_STICKY_COMPATIBILITYなどを指定する
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // サービス破棄時に 気圧センサーリスナーを開放しておく
        if (mSensor != null) SensorUtils.unregisterListener(this, this);

        super.onDestroy();
    }

    /**
     * このサービスを foregroundモードにする
     * android 8.0対応
     * @param context
     */
    private void setForegroundMode(Context context) {
        String title = getString(R.string.channel_altitude_service);
//        String message = getString(R.string.msg_svc_ongoing);
        String message = "" + new Date(System.currentTimeMillis());
        Notification.Builder builder;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // android8.0以降で サービスを startForegroundService()で起動する場合は、サービスをforegroundにする
            builder = new Notification.Builder(context, CHANNEL_ID_SERVICE);

        } else {
            builder = new Notification.Builder(context);
        }
        builder.setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.mipmap.ic_landscape_white_24dp)
                .setOngoing(true);
        startForeground(ID_SERVICE_ONGOING, builder.build());
    }


    //////////////////////////////////////////////////////////////////////
    //
    // Service 制御関連 メソッド
    //

    /**
     * このServiceが 実行中であるかどうかを返す
     * @param context コンテキスト
     * @return 結果
     */
    public static boolean isRunning(Context context) {
        List<ActivityManager.RunningServiceInfo> serviceList;
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return false;
        try {
            serviceList = manager.getRunningServices(Integer.MAX_VALUE);
        } catch(SecurityException e) {
            return false;
        }

        for (ActivityManager.RunningServiceInfo serviceInfo : serviceList) {
            // 自身のサービスが実行中かリストから確認する
            if (AltitudeLogService.class.getName().equals(serviceInfo.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    /**
     * このサービスを開始する
     * android 8.0対応
     * @param context コンテキスト
     */
    public static void startService(Context context) {
        // サービス起動
        Intent serviceIntent = new Intent(context, AltitudeLogService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }

    /**
     * このサービスを停止する
     * @param context コンテキスト
     */
    public static void stopService(Context context) {
        // サービス起動
        Intent serviceIntent = new Intent(context, AltitudeLogService.class);
        context.stopService(serviceIntent);
    }


    /**
     * notificationチャンネルを登録する (android8.0以降用)
     * @param context コンテキスト
     * @param importance 初期設定値
     */
    public static void registerNotifyChannel(Context context, int importance) {
        if (context == null) return;

        // 以下は android8.0 以降のみ有効
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = context.getString(R.string.channel_altitude_service);
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID_SERVICE, channelName, importance);
            channel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);       // ロック画面で通知を表示するかどうか

            // 端末にチャンネルを登録する。端末設定で表示される
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;
            manager.createNotificationChannel(channel);
        }
    }

    /**
     * notificationチャンネルを登録する (android8.0以降用)
     * @param context コンテキスト
     */
    public static void registerNotifyChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerNotifyChannel(context, NotificationManager.IMPORTANCE_DEFAULT);
        }
    }


    //////////////////////////////////////////////////////////
    //
    // 気圧センサー用 implements
    //

    private final static float INVALID_ALTITUDE = -99999.9f;
    private final static float THRESHOLD_ALTITUDE = 5.0f;
    private final static float THRESHOLD_LIFT_COUNT = 50.0f;

    private float mPrevAltitude = INVALID_ALTITUDE;
    private float mLiftAltitude = INVALID_ALTITUDE;
    private float mTotalDesc = 0.0f;
    private float mTotalAsc = 0.0f;
    private int mRunCount = 0;
    private int mLiftDelta = 0;

    private float[] mReplyData = new float[ 4 ];


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float[] val = sensorEvent.values.clone();

        if (val.length >= 1) {
            // 取得した気圧をログに出力する
            float altitude = SensorUtils.getAltitude(val[0]);
            Log.d("pressure", "高度=" + altitude + "ｍ 気圧=" + val[0] + "hPa");
            logAltitude(altitude);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    // 高度を記録する
    private void logAltitude(float altitude) {
        // 上昇累積/下降累積 算出
        if (mPrevAltitude == INVALID_ALTITUDE) {
            // 初回
            mPrevAltitude = altitude;

            DbUtils.insert(this, new SkiLogData(altitude, mTotalAsc, mTotalDesc, mRunCount));

        } else {
            float delta = altitude - mPrevAltitude;             // 高度差分
            if (delta >= THRESHOLD_ALTITUDE) {
                // 閾値以上に 登った
                mPrevAltitude = altitude;                       // 高度を記録
                mTotalAsc += delta;                             // 登った高度を積算

                DbUtils.insert(this, new SkiLogData(altitude, mTotalAsc, mTotalDesc, mRunCount));
            } else if (delta <= -THRESHOLD_ALTITUDE) {
                // 閾値以上に 降りた
                mPrevAltitude = altitude;                       // 高度を記録
                mTotalDesc += delta;                            // 降りた高度を積算 (下降分は負の値)

                DbUtils.insert(this, new SkiLogData(altitude, mTotalAsc, mTotalDesc, mRunCount));
            }
        }

        // リフト回数算出
        if (mLiftAltitude == INVALID_ALTITUDE) {
            // 初回
            mLiftAltitude = altitude;

        } else {
            int delta = (int)(altitude - mLiftAltitude);        // リフト乗車高度差分

            if (delta <= -THRESHOLD_LIFT_COUNT && mLiftDelta >= 0) {
                // 閾値以上に 降りた
                mRunCount++;                                    // 滑走回数カウント
                mLiftAltitude = altitude;                       // リフト最低地点(乗車高度用)を記録
                mLiftDelta = delta;                             // 下降中(負の値)

                DbUtils.insert(this, new SkiLogData(altitude, mTotalAsc, mTotalDesc, mRunCount));

            } else if (delta >= THRESHOLD_LIFT_COUNT && mLiftDelta <= 0) {
                // 閾値以上に 登った
                mLiftAltitude = altitude;                       // リフト最高地点(下車高度用)を記録
                mLiftDelta = delta;                             // 上昇中(正の値)
            }

            if (mLiftDelta > 0) {
                // 上昇中なら リフト最高地点(下車高度用)を更新
                if (mLiftAltitude < altitude) mLiftAltitude = altitude;
            } else if (mLiftDelta < 0) {
                // 下降中なら リフト最低地点(乗車高度用)を更新
                if (mLiftAltitude > altitude) mLiftAltitude = altitude;
            }
        }


        // 表示用の値を 配列で一度にActivityへ送る
        mReplyData[0] = altitude;
        mReplyData[1] = mTotalAsc;
        mReplyData[2] = mTotalDesc;
        mReplyData[3] = (float) mRunCount;
        mReplyHandler.replyMessage(mReplyData);
    }
}
