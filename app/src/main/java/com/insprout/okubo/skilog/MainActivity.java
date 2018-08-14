package com.insprout.okubo.skilog;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.setting.Settings;
import com.insprout.okubo.skilog.util.DialogUi;
import com.insprout.okubo.skilog.util.SdkUtils;
import com.insprout.okubo.skilog.util.SensorUtils;
import com.insprout.okubo.skilog.util.UiUtils;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, DialogUi.DialogEventListener {
    private final static int RC_CHANGE_THEME = 1;
    private final static int REQ_CODE_FINISH_APP = 101;

    private ServiceMessenger mServiceMessenger;

    private Sensor mSensor = null;
    private TextView mTvAltitude;
    private TextView mTvTotalAsc;
    private TextView mTvTotalDesc;
    private TextView mTvCount;

    private boolean mReadyData = false;
    private int mThemeIndex;
    private String[] mThemeArray;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.getThemeStyle(this));
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

    private void initVars() {
        SkiLogService.registerNotifyChannel(this);              // Android8.0対応 notificationチャンネルの登録

        mThemeArray = getResources().getStringArray(R.array.app_themes);
        mReadyData = (DbUtils.countLogs(this) >= 1);                  // 記録データが存在するかどうか

        // Serviceプロセスとの 通信クラス作成
        mServiceMessenger = new ServiceMessenger(this, new ServiceMessenger.OnServiceMessageListener() {
            @Override
            public void onReceiveMessage(Message msg) {
                switch (msg.what) {
                    case ServiceMessenger.MSG_REPLY_LONG_ARRAY:
                        long[] data = (long[]) msg.obj;
                        mTvAltitude.setText(formattedAltitude(data[1]));
                        mTvTotalAsc.setText(formattedAltitude(data[2]));
                        mTvTotalDesc.setText(formattedAltitude(-data[3]));
                        mTvCount.setText(String.valueOf(data[4]));
                        break;
                }
            }
        });
    }

    private void initView() {
        UiUtils.setDrawables(this, R.id.btn_positive, R.drawable.ic_record, R.drawable.bg_circle2_large);
        UiUtils.setDrawables(this, R.id.btn_negative, R.mipmap.ic_pause_white_36dp, R.drawable.bg_circle2_large);

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

    @Override
    public void onBackPressed() {
        // アプリ終了確認ダイアログを出す
        new DialogUi.Builder(this)
                .setMessage(R.string.msg_close_application)
                .setPositiveButton(R.string.btn_finish_app)
                .setNegativeButton(R.string.btn_cancel)
                .setRequestCode(REQ_CODE_FINISH_APP)
                .show();
    }

    private void startService() {
        Log.d("WatchService", "startService()");
        updateUi(true);
        UiUtils.setText(this, R.id.tv_status, R.string.label_status_starting);
        SkiLogService.startService(this);

        try {
            mServiceMessenger.bind();
        } catch(Exception e) {
            updateUi(false);
        }
        // 不足のエラーで Serviceが開始できなかった場合のために、一定時間後にServiceの起動状態を確認して ボタンに反映する
        confirmServiceStatus();
    }

    private void stopService() {
        Log.d("WatchService", "stopService()");
        updateUi(false);
        SkiLogService.stopService(this);

        mServiceMessenger.unbind();
    }


    // ミリメートル単位の高度値を 表示用の文字列に変換する
    private String formattedAltitude(long altitude) {
        return getString(R.string.fmt_meter, altitude * 0.001f);
    }

    private void updateUi(boolean serviceRunning) {
        // ステータス表示
        UiUtils.setText(this, R.id.tv_status, (serviceRunning ? R.string.label_status_start : R.string.label_status_stop));

        // サービス起動・停止ボタンの 有効無効
        UiUtils.setEnabled(this, R.id.btn_positive, (mSensor!=null && !serviceRunning));
        UiUtils.setEnabled(this, R.id.btn_negative, (mSensor!=null && serviceRunning));

        UiUtils.setEnabled(this, R.id.btn_chart1, mReadyData);
        UiUtils.setEnabled(this, R.id.btn_chart2, mReadyData);
        UiUtils.setSelected(this, R.id.btn_chart1, false);
        UiUtils.setSelected(this, R.id.btn_chart2, false);
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


    // タイトルメニュー用 設定

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_change_theme:
                mThemeIndex = Settings.getThemeIndex(this);
                new DialogUi.Builder(this)
                        .setTitle(R.string.menu_theme)
                        .setSingleChoiceItems(mThemeArray, mThemeIndex)
                        .setPositiveButton()
                        .setNegativeButton()
                        .setRequestCode(RC_CHANGE_THEME)
                        .show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onDialogEvent(int requestCode, AlertDialog dialog, int which, View view) {
        switch(requestCode) {
            case RC_CHANGE_THEME:
                if (which == DialogUi.EVENT_BUTTON_POSITIVE) {
                    if (view instanceof ListView) {
                        int index = ((ListView) view).getCheckedItemPosition();
                        if (index != mThemeIndex) {
                            Settings.putThemeIndex(this, index);

                            Intent intent = new Intent(this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }
                }
                break;

            case REQ_CODE_FINISH_APP:
                if (which == DialogUi.EVENT_BUTTON_POSITIVE) {
                    // アプリ終了する
                    finish();
                }
                break;
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch(id) {
            case R.id.btn_positive:
                startService();
//                SdkUtils.requestDisableDozeModeIfNeeded(this);
                break;

            case R.id.btn_negative:
                stopService();
                break;

            case R.id.btn_chart1:
                UiUtils.setSelected(this, R.id.btn_chart1, true);
//                LineChartActivity.startActivity(this);
                ChartPagerActivity.startActivity(this, 1);
                break;

            case R.id.btn_chart2:
                UiUtils.setSelected(this, R.id.btn_chart2, true);
//                BarChartActivity.startActivity(this);
                ChartPagerActivity.startActivity(this, 0);
                break;
        }
    }


}
