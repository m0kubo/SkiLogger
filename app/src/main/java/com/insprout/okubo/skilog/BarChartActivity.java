package com.insprout.okubo.skilog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.insprout.okubo.skilog.chart.SummaryChart;
import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.database.TagData;
import com.insprout.okubo.skilog.setting.Settings;
import com.insprout.okubo.skilog.util.AppUtils;
import com.insprout.okubo.skilog.util.DialogUi;
import com.insprout.okubo.skilog.util.UiUtils;

import java.util.Date;
import java.util.List;


public class BarChartActivity extends AppCompatActivity implements View.OnClickListener, DialogUi.DialogEventListener {
    private final static int RC_DELETE_LOG = 1;
    private final static int RC_SELECT_TAG = 2;

    private ServiceMessenger mServiceMessenger;

    private SummaryChart mSummaryChart;
    private List<TagData> mTags;
    private int mIndexTag = -1;
    private Date mTargetDate = null;


    @Override
    public void onResume() {
        super.onResume();
        if (SkiLogService.isRunning(this)) mServiceMessenger.bind();
    }

    @Override
    public void onPause() {
        if (SkiLogService.isRunning(this)) mServiceMessenger.unbind();
        super.onPause();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.getThemeStyle(this));
        setContentView(R.layout.activity_bar_chart);

        initVars();                                             // 変数などの初期化
        initView();                                             // View関連の初期化
    }

    private void initVars() {
        mTags = AppUtils.getTags(this);                         // 絞り込み用のタグリスト取得

        // Serviceプロセスとの 通信クラス作成
        mServiceMessenger = new ServiceMessenger(this, new ServiceMessenger.OnServiceMessageListener() {
            @Override
            public void onReceiveMessage(Message msg) {
                switch (msg.what) {
                    case ServiceMessenger.MSG_REPLY_LONG_ARRAY:
                        long[] data = (long[]) msg.obj;
                        if (data[0] <= 0) return;

                        mSummaryChart.appendChartValue(data[0], data[1] * 0.001f, data[2] * 0.001f, data[3] * 0.001f, (int)data[4]);
                        break;
                }
            }
        });
    }

    private void initView() {
        UiUtils.setSelected(this, R.id.btn_chart2, true);

        // タイトルバーに backボタンを表示する
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        BarChart barChart = findViewById(R.id.bar_chart);
        mSummaryChart = new SummaryChart(this, barChart, new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry entry, Highlight h) {
                displayValue(entry);
            }

            @Override
            public void onNothingSelected() {
                displayValue(null);
            }
        });
        mSummaryChart.drawChart();
        updateUi();
    }


    private void updateUi() {
        setTitle(mSummaryChart.getSubject());
        // 前データ、次データへのボタンの 有効無効
        UiUtils.setEnabled(this, R.id.btn_negative, mSummaryChart.hasPreviousPage());
        UiUtils.setEnabled(this, R.id.btn_positive, mSummaryChart.hasNextPage());
        UiUtils.setEnabled(this, R.id.btn_tag, !(mTags == null || mTags.isEmpty()));
        displayValue(null);
    }

    private void displayValue(Entry entry) {
        String text = null;
        if (entry != null) {
            text = getString(R.string.fmt_value_accumulate,
                    mSummaryChart.getXAxisLabelFull(entry.getX()),
                    mSummaryChart.getYAxisLabel(entry.getY()));
        }
        UiUtils.setText(this, R.id.tv_chart_value, text);
    }



    ////////////////////////////////////////////////////////////////////
    //
    // Optionメニュー関連
    //

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.titlebar, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // 削除メニューの状態を設定
        MenuItem deleteMenu = menu.findItem(R.id.menu_delete_logs);
        mTargetDate = mSummaryChart.getSelectedDate();
        if (mTargetDate != null) {
            deleteMenu.setEnabled(true);
            deleteMenu.setTitle(getString(R.string.fmt_menu_delete_logs, AppUtils.toDateString(mTargetDate)));

        } else {
            deleteMenu.setEnabled(false);
            deleteMenu.setTitle(R.string.menu_delete_logs);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;

            case R.id.menu_delete_logs:
                confirmDeleteLogs();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmDeleteLogs() {
        if (mTargetDate == null) return;       // 念のためチェック
        // データ削除
        String title = getString(R.string.title_delete_logs);
        String message = getString(R.string.fmt_delete_daily_logs,  AppUtils.toDateString(mTargetDate));
        DialogUi.showOkCancelDialog(this, title, message, RC_DELETE_LOG);
    }


    private void deleteLogs() {
        if (mTargetDate == null) return;       // 念のためチェック

        // 指定日のログをDBから削除する
        boolean res = DbUtils.deleteLogs(this, mTargetDate);
        if (res) {
            // 紐づいたタグ情報もDBから削除する
            DbUtils.deleteTags(this, mTargetDate);

            // チャートの表示を更新する
            mSummaryChart.drawChart();
            updateUi();
        }
    }

    private void selectTag() {
        // tag一覧
        // tagがない場合 ボタン無効になっている筈だが念のためチェック
        if (mTags == null || mTags.isEmpty()) {
            return;
        }
        // 選択用リストを作成
        String[] arrayTag = new String[ mTags.size() + 1 ];
        for (int i=0; i<mTags.size(); i++) {
            arrayTag[i] = mTags.get(i).getTag();
        }
        arrayTag[ arrayTag.length - 1 ] = getString(R.string.menu_reset_tag);
        DialogUi.showItemSelectDialog(this, R.string.title_select_tag, arrayTag, mIndexTag, RC_SELECT_TAG);
    }

    @Override
    public void onDialogEvent(int requestCode, AlertDialog dialog, int which, View view) {
        switch (requestCode) {
            case RC_DELETE_LOG:
                if (which == DialogUi.EVENT_BUTTON_POSITIVE) {
                    deleteLogs();
                }
                break;

            case RC_SELECT_TAG:
                if (which == DialogUi.EVENT_BUTTON_POSITIVE) {
                    if (view instanceof ListView) {
                        int pos = ((ListView)view).getCheckedItemPosition();
                        if (mTags != null && pos >= 0 && pos < mTags.size()) {
                            mIndexTag = pos;
                            mSummaryChart.setFilter(mTags.get(mIndexTag).getTag());

                        } else {
                            if (mIndexTag >= 0) {
                                Intent intent = new Intent(this, BarChartActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                //finish();
                                return;
                            }
                            mIndexTag = -1;
                            mSummaryChart.setFilter(null);
                        }
                        mSummaryChart.drawChart();
                        updateUi();
                    }
                }
                break;
        }
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch(id) {
            case R.id.btn_negative:
                mSummaryChart.goPreviousPage();
                updateUi();
                break;

            case R.id.btn_positive:
                mSummaryChart.goNextPage();
                updateUi();
                break;

            case R.id.btn_chart1:
                UiUtils.setSelected(this, R.id.btn_chart1, true);
                UiUtils.setSelected(this, R.id.btn_chart2, false);
                LineChartActivity.startActivity(this);
                finish();
                break;

            case R.id.btn_tag:
                selectTag();
                break;
        }
    }

    public static void startActivity(Activity context) {
        Intent intent = new Intent(context, BarChartActivity.class);
        context.startActivity(intent);
    }

}
