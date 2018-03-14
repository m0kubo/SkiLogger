package com.insprout.okubo.skilog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.DashPathEffect;
import android.graphics.RectF;
import android.location.Location;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.database.SkiLogData;
import com.insprout.okubo.skilog.database.TagData;
import com.insprout.okubo.skilog.model.ResponsePlaceData;
import com.insprout.okubo.skilog.setting.Const;
import com.insprout.okubo.skilog.setting.Settings;
import com.insprout.okubo.skilog.util.AppUtils;
import com.insprout.okubo.skilog.util.DialogUtils;
import com.insprout.okubo.skilog.util.MiscUtils;
import com.insprout.okubo.skilog.util.SdkUtils;
import com.insprout.okubo.skilog.util.UiUtils;
import com.insprout.okubo.skilog.webapi.RequestUrlTask;
import com.insprout.okubo.skilog.webapi.WebApiUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class ChartPagerActivity extends AppCompatActivity {
    private final static int RP_LOCATION = 100;

    private final static int RC_DELETE_LOG = 100;
    private final static int RC_LIST_TAG = 200;
    private final static int RC_ADD_TAG = 201;
    private final static int RC_ADD_TAG_INPUT = 300;
    private final static int RC_ADD_TAG_SELECTION = 301;
    private final static int RC_ADD_TAG_LOCATION = 302;
    private final static int RC_DELETE_TAG = 400;

    private final static String TAG = "chart";
    private final static String EXTRA_PARAM1 = "intent.extra.PARAM1";
    private final static int MAX_DATA_COUNT = 0;

    private ServiceMessenger mServiceMessenger;

    ///////////////////////////
    private ViewPager mViewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.getThemeStyle(this));
        setContentView(R.layout.activity_chart_pager);

        initVars();                                             // 変数などの初期化
        initView();                                             // View関連の初期化
    }


    private void initVars() {

        // Serviceプロセスとの 通信クラス作成
        mServiceMessenger = new ServiceMessenger(this, new ServiceMessenger.OnServiceMessageListener() {
            @Override
            public void onReceiveMessage(Message msg) {
                switch (msg.what) {
                    case ServiceMessenger.MSG_REPLY_LONG_ARRAY:
//                        Date targetDate = getTargetDate(mDateIndex);
//                        if (targetDate == null) return;
//
                        long[] data = (long[]) msg.obj;
                        if (data[0] <= 0) return;
//
//                        Date timeAm00 = MiscUtils.getDate(new Date(data[0]));
//                        Log.d(TAG, "received data: date=" + timeAm00);
//                        if (MiscUtils.isSameDate(timeAm00, targetDate)) {
//                            appendData((data[0] - timeAm00.getTime()) / (60 * 60 * 1000.0f), data[1] * 0.001f, data[2] * 0.001f, -data[3] * 0.001f, (int)data[4]);
//                        }
                        break;
                }
            }
        });
    }

    private void initView() {
        mViewPager = findViewById(R.id.vp_chart);
        mViewPager.setAdapter(new ChartPagerAdapter(this));
    }


    @Override
    public void onResume() {
        super.onResume();

        // チャートの描画を開始する
//        updateChart();
        if (SkiLogService.isRunning(this)) mServiceMessenger.bind();
    }

    @Override
    public void onPause() {
        if (SkiLogService.isRunning(this)) mServiceMessenger.unbind();
        super.onPause();
    }





    public void onClick(View view) {
        int id = view.getId();
        switch(id) {
//            case R.id.btn_negative:
//                if (mDateIndex > 0) mDateIndex--;
//                updateUi(mDateIndex);
//                updateChart();
//                break;
//
//            case R.id.btn_positive:
//                if (mDateIndex < mDateList.size() - 1) mDateIndex++;
//                updateUi(mDateIndex);
//                updateChart();
//                break;
//
//            case R.id.btn_chart2:
//                UiUtils.setSelected(this, R.id.btn_chart1, false);
//                UiUtils.setSelected(this, R.id.btn_chart2, true);
//                BarChartActivity.startActivity(this);
//                finish();
//                break;
        }
    }


    ////////////////////////////////////////////////////////////////////
    //
    // Optionメニュー関連
    //

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.line_chart, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
//        Date targetDate = getTargetDate(mDateIndex);
//
//        MenuItem tagsMenu = menu.findItem(R.id.menu_list_tags);
//        MenuItem deleteMenu = menu.findItem(R.id.menu_delete_logs);
//
//        // 日付に依存するメニューの 有効/無効状態を設定
//        if (targetDate != null) {
//            // 削除メニュー
//            deleteMenu.setEnabled(true);
//            deleteMenu.setTitle(getString(R.string.fmt_menu_delete_logs, AppUtils.toDateString(targetDate)));
//
//            // 付与されているタグ一覧メニュー
//            mTags = DbUtils.selectTags(this, targetDate);
//            tagsMenu.setEnabled(mTags != null && !mTags.isEmpty());
//
//        } else {
//            // 削除メニュー
//            deleteMenu.setEnabled(false);
//            deleteMenu.setTitle(R.string.menu_delete_logs);
//
//            // 付与されているタグ一覧メニュー
//            mTags = null;
//            tagsMenu.setEnabled(false);
//        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

//            case R.id.menu_delete_logs:
//                confirmDeleteLogs();
//                return true;
//
//            case R.id.menu_list_tags:
//                listTags();
//                return true;
//
//            case R.id.menu_add_tags:
//                showAddTagDialog();
//                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        switch (requestCode) {
//            case RP_LOCATION:
//                // PERMISSIONが すべて付与されたか確認する
//                if (!SdkUtils.isGranted(grantResults)) {
//                    // 必要な PERMISSIONは付与されなかった
//                    // タグ追加方法選択に戻す
//                    showAddTagDialog();
//                    return;
//                }
//
//                measureLocation();
//                return;
//        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    ////////////////////////////////////////////////////////////////////
    //
    // Activity起動 staticメソッド
    //

    public static void startActivity(Activity context) {
        Intent intent = new Intent(context, ChartPagerActivity.class);
        context.startActivity(intent);
    }
}
