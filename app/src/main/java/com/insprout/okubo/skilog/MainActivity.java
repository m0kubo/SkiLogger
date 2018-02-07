package com.insprout.okubo.skilog;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.database.SkiLogData;
import com.insprout.okubo.skilog.util.SdkUtils;
import com.insprout.okubo.skilog.util.SensorUtils;
import com.insprout.okubo.skilog.util.UiUtils;

import java.util.Date;
import java.util.List;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ServiceMessenger mServiceMessenger;

    private Sensor mSensor = null;
    private TextView mTvAltitude;
    private TextView mTvTotalAsc;
    private TextView mTvTotalDesc;
    private TextView mTvCount;

    private boolean mReadyData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initVars();                                             // 変数などの初期化
        initView();                                             // View関連の初期化

        mSensor = SensorUtils.getPressureSensor(this);
        if (mSensor == null) {
            // 気圧センサーがない場合はアプリ終了
            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_missing_sensor)
                    .setMessage(R.string.msg_missing_sensor)
                    .setCancelable(false)
                    .setPositiveButton(R.string.btn_finish_app, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // アプリ終了
                            finish();
                        }
                    })
                    .show();
        }
    }

    @Override
    protected void onDestroy() {
        mServiceMessenger.unbind();
        super.onDestroy();
    }

    private void initVars() {
        SkiLogService.registerNotifyChannel(this);              // Android8.0対応 notificationチャンネルの登録

        mReadyData = (DbUtils.count(this) >= 1);                  // 記録データが存在するかどうか

        // Serviceプロセスとの 通信クラス作成
        mServiceMessenger = new ServiceMessenger(this, new ServiceMessenger.OnServiceMessageListener() {
            @Override
            public void onReceiveMessage(Message msg) {
                switch (msg.what) {
                    case ServiceMessenger.MSG_REPLY_STRING:
                        // 文字列型データの受信
                        mTvAltitude.setText((String)msg.obj);
                        break;

                    case ServiceMessenger.MSG_REPLY_FLOAT_ARRAY:
                        float[] data = (float[]) msg.obj;
                        mTvAltitude.setText(formattedAltitude(data[0]));
                        mTvTotalAsc.setText(formattedAltitude(data[1]));
                        mTvTotalDesc.setText(formattedAltitude(-data[2]));
                        mTvCount.setText(String.valueOf((int)data[3]));
                        break;
                }
            }
        });
    }

    private void initView() {
        mTvAltitude = findViewById(R.id.tv_altitude);
        mTvTotalAsc = findViewById(R.id.tv_total_asc);
        mTvTotalDesc = findViewById(R.id.tv_total_desc);
        mTvCount = findViewById(R.id.tv_count_lift);
    }


    @Override
    public void onResume() {
        super.onResume();

        // サービスの実行状況に合わせて、Messengerや ボタンなどを設定する
        boolean svcRunning = SkiLogService.isRunning(this);
        if (svcRunning) mServiceMessenger.bind();
        updateUi(svcRunning);
    }

    @Override
    public void onPause() {
        mServiceMessenger.unbind();
        super.onPause();
    }

    private void startService() {
        Log.d("WatchService", "startService()");
        updateUi(true);
        SkiLogService.startService(this);

        mServiceMessenger.bind();
        // 不足のエラーで Serviceが開始できなかった場合のために、一定時間後にServiceの起動状態を確認して ボタンに反映する
        confirmServiceStatus();
    }

    private void stopService() {
        Log.d("WatchService", "stopService()");
        updateUi(false);
        SkiLogService.stopService(this);

        mServiceMessenger.unbind();
    }


    private String formattedAltitude(float altitude) {
        return getString(R.string.fmt_meter, (int)(altitude + 0.5f));
    }

    private void updateUi(boolean serviceRunning) {
        // ステータス表示
        UiUtils.setText(this, R.id.tv_status, (serviceRunning ? R.string.label_status_logging : R.string.label_status_stop));

        // サービス起動・停止ボタンの 有効無効
        UiUtils.enableView(this, R.id.btn_start_svc, (mSensor!=null && !serviceRunning));
        UiUtils.enableView(this, R.id.btn_stop_svc, (mSensor!=null && serviceRunning));

        UiUtils.enableView(this, R.id.btn_chart, mReadyData);
        UiUtils.enableView(this, R.id.btn_chart2, mReadyData);
    }

    private void confirmServiceStatus() {
        // 不測のエラーで Serviceが開始できなかった場合のために、一定時間後にServiceの起動状態を確認して ボタンに反映する
        new Handler().postDelayed(new Runnable() {
             @Override
             public void run() {
                 boolean isRunning = SkiLogService.isRunning(MainActivity.this);
                 if (isRunning) mReadyData = true;              // サービスが起動できたら、新規データがあると見做す
                 updateUi(isRunning);
             }
        },
        1000);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch(id) {
            case R.id.btn_start_svc:
                startService();
                SdkUtils.requestDisableDozeModeIfNeeded(this);
                break;

            case R.id.btn_stop_svc:
                stopService();
                break;

            case R.id.btn_chart:
                LineChartActivity.startActivity(this);
                break;

            case R.id.btn_chart2:
                BarChartActivity.startActivity(this);
                break;
        }
    }

    private void getData() {
        long count = DbUtils.count(this);
        Log.d("database", "レコード数:" + count);

//        List<SkiLogData> data = DbUtils.listByRawQuery(
//                this,
//                "SELECT * FROM ski_log WHERE created IN (SELECT MAX(created) FROM ski_log GROUP BY date(created,'+9 hours') )",
//                null);
        List<SkiLogData> data = DbUtils.select(this, new Date(System.currentTimeMillis()));
//        TimeZone.getTimeZone("Europe/Berlin").
//        for (SkiLogData log : data) {
        for (int i=0; i<data.size(); i++) {
            SkiLogData log = data.get(i);
            String msg = "" + i + ":" + log.getCreated().toString() + " 高度:" + log.getAltitude() + " 上昇:" + log.getAscTotal() + " 下降:" + log.getDescTotal() + " RUN:" + log.getCount();
            Log.d("database", msg);
        }
    }

}
