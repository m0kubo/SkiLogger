package com.insprout.okubo.skilog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.insprout.okubo.skilog.chart.SummaryChart;
import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.model.TagDb;
import com.insprout.okubo.skilog.setting.Settings;
import com.insprout.okubo.skilog.util.AppUtils;
import com.insprout.okubo.skilog.util.ContentsUtils;
import com.insprout.okubo.skilog.util.DialogUi;
import com.insprout.okubo.skilog.util.MiscUtils;
import com.insprout.okubo.skilog.util.UiUtils;

import java.util.Date;
import java.util.List;


public class BarChartActivity extends BaseActivity implements View.OnClickListener, DialogUi.DialogEventListener {
//    private final static int RC_DELETE_LOG = 1;
    private final static int RC_SELECT_TAG = 2;

    private ServiceMessenger mServiceMessenger;

    private SummaryChart mSummaryChart;
    private List<TagDb> mAllTags;
    private int mIndexTag = -1;
    private Uri mPhotoUri = null;

    private boolean mValueSelected = false;
    //private long mTimeToast = 0;


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
        setupFilteringTag();

        // タイトルバーに backボタンを表示する
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.drawable.ic_bar_chart);
        }

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

        // チャートの表示を更新する
        updateChart();
    }


    // チャートの表示を更新する
    private void updateChart() {
        mValueSelected = false;     // 選択解除の表示を行わせない
        mSummaryChart.drawChart();
        UiUtils.setText(this, R.id.tv_count, getString(R.string.fmt_count_summaries, mSummaryChart.getCount()));
        updateView();
    }


    private void updateView() {
        setTitle(mSummaryChart.getSubject());
        // 前データ、次データへのボタンの 有効無効
        UiUtils.setEnabled(this, R.id.btn_negative, mSummaryChart.hasPreviousPage());
        UiUtils.setEnabled(this, R.id.btn_positive, mSummaryChart.hasNextPage());
        displayValue(null);
    }

    private void displayValue(Entry entry) {
        mPhotoUri = null;
        String text = null;
//        String toast;
        if (entry != null) {
            String dateString = mSummaryChart.getXAxisLabelFull(entry.getX());
            text = getString(R.string.fmt_value_accumulate, dateString);
            // 計測期間内に撮影された画像があるか確認する
            Date[] period = DbUtils.selectTimePeriods(this, mSummaryChart.getLogDate((int)entry.getX()));
            if (period != null && period.length == 2) {
                // 撮影した写真がすぐグラフに反映されるように修正
                Date endTime = period[1];
                if (MiscUtils.isToday(endTime) && SkiLogService.isRunning(this)) endTime = new Date(System.currentTimeMillis());
                List<Uri> photoList = ContentsUtils.getImageList(this, period[0], endTime);
                if (photoList.size() >= 1) {
                    mPhotoUri = photoList.get(0);
                    text += getString(R.string.fmt_photo_count, photoList.size());
                }
            }
            if (!mValueSelected) {
                Toast.makeText(this, R.string.msg_date_selected, Toast.LENGTH_LONG).show();
                mValueSelected = true;
            }
        }
        UiUtils.setText(this, R.id.tv_chart_value, text);
        UiUtils.setEnabled(this, R.id.btn_detail, mPhotoUri != null);
    }

    private void setupFilteringTag() {
        mAllTags = AppUtils.getTags(this);
        UiUtils.setEnabled(this, R.id.btn_tag, (mAllTags != null && !mAllTags.isEmpty()));
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch(id) {
            case R.id.btn_tag:
                selectTag();
                break;

            case R.id.btn_detail:
                if (mPhotoUri != null) UiUtils.intentActionView(this, mPhotoUri);
                break;

            default:
                super.onClick(view);
                break;
        }
    }


    ////////////////////////////////////////////////////////////////////
    //
    // 親クラスのメソッドをOverrideする
    //


    @Override
    protected Date getTargetDate() {
        return mSummaryChart.getSelectedDate();
    }

    @Override
    protected void redrawChart(Date deletedLogDate) {
        mSummaryChart.drawChart();
    }

    @Override
    protected void redrawChart(String deletedTag) {
        if (deletedTag != null && deletedTag.equals(mSummaryChart.getFilter())) {
            mSummaryChart.drawChart();
        }
        setupFilteringTag();
    }


    ////////////////////////////////////////////////////////////////////
    //
    // Dialog関連
    //

    // 絞り込み用 タグ選択ダイアログ表示
    private void selectTag() {
        // tag一覧
        // tagがない場合 ボタン無効になっている筈だが念のためチェック
        if (mAllTags == null || mAllTags.isEmpty()) {
            return;
        }
        // 選択用リストを作成
        String[] arrayTag = new String[ mAllTags.size() + 1 ];
        for (int i = 0; i< mAllTags.size(); i++) {
            arrayTag[i] = mAllTags.get(i).getTag();
        }
        arrayTag[ arrayTag.length - 1 ] = getString(R.string.menu_reset_tag);
        new DialogUi.Builder(this)
                .setTitle(R.string.title_select_tag)
                .setSingleChoiceItems(arrayTag, mIndexTag)
                .setPositiveButton()
                .setNegativeButton()
                .setRequestCode(RC_SELECT_TAG)
                .show();
    }


    @Override
    public void onDialogEvent(int requestCode, AlertDialog dialog, int which, View view) {
        switch (requestCode) {
            case RC_SELECT_TAG:
                if (which == DialogUi.EVENT_BUTTON_POSITIVE) {
                    // 絞り込み処理 実行
                    if (view instanceof ListView) {
                        int pos = ((ListView)view).getCheckedItemPosition();
                        if (mAllTags != null && pos >= 0 && pos < mAllTags.size()) {
                            mIndexTag = pos;
                            mSummaryChart.setFilter(mAllTags.get(mIndexTag).getTag());

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
                        // チャートの表示を更新する
                        updateChart();
                    }
                }
                break;

            default:
                super.onDialogEvent(requestCode, dialog, which, view);
                break;
        }
    }


    public static void startActivity(Activity context) {
        Intent intent = new Intent(context, BarChartActivity.class);
        context.startActivity(intent);
    }

}
