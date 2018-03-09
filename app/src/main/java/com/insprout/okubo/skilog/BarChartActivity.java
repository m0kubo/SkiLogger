package com.insprout.okubo.skilog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.database.SkiLogData;
import com.insprout.okubo.skilog.database.TagData;
import com.insprout.okubo.skilog.setting.Settings;
import com.insprout.okubo.skilog.util.AppUtils;
import com.insprout.okubo.skilog.util.DialogUtils;
import com.insprout.okubo.skilog.util.MiscUtils;
import com.insprout.okubo.skilog.util.SdkUtils;
import com.insprout.okubo.skilog.util.UiUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class BarChartActivity extends AppCompatActivity implements View.OnClickListener, DialogUtils.DialogEventListener {
    private final static int RC_DELETE_LOG = 1;
    private final static int RC_SELECT_TAG = 2;
    private final static int START_MONTH_OF_SEASON = 9;         // 月の指定は 0～11。(0⇒1月  11⇒12月)

    private ServiceMessenger mServiceMessenger;
    private Date mThisSeasonFrom;
    private Date mDateOldest;
    private Date mDateFrom, mDateTo;
    private SimpleDateFormat mDateFormat;

    private List<SkiLogData> mSkiLogs;
    private BarChart mChart;
    private List<String> mXAxisLabels;                              //X軸に表示するLabelのリスト
    private float mYAxisMax = 0f;
    private float mYAxisMin = 0f;
    private float mChartTextSize;
    private String mChartLabel;
    private int mColorForeground;

    private String mSearchTag = null;
    private int mIndexTag = -1;
    private List<TagData> mTags;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.getThemeStyle(this));
        setContentView(R.layout.activity_bar_chart);

        initVars();                                             // 変数などの初期化
        initView();                                             // View関連の初期化
    }


    @Override
    public void onResume() {
        super.onResume();
        if (SkiLogService.isRunning(this)) mServiceMessenger.bind();
        mChart.highlightValue(-1, 0, false);            // ハイライト(選択)を解除
    }

    @Override
    public void onPause() {
        if (SkiLogService.isRunning(this)) mServiceMessenger.unbind();
        super.onPause();
    }


    // タイトルメニュー用 設定

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.titlebar, menu);
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

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // 削除メニューの状態を設定
        MenuItem deleteMenu = menu.findItem(R.id.menu_delete_logs);
        if (mDateFrom != null) {
            deleteMenu.setEnabled(true);
            deleteMenu.setTitle(getString(R.string.fmt_menu_delete_logs, getTitleString()));

        } else {
            deleteMenu.setEnabled(false);
            deleteMenu.setTitle(R.string.menu_delete_logs);
        }
        return true;
    }

    private void initVars() {
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorForeground, typedValue, true);
        mColorForeground = SdkUtils.getColor(this, typedValue.resourceId);

        mTags = AppUtils.getTags(this);
        mThisSeasonFrom = getStartDateOfSeason(new Date(System.currentTimeMillis()));
        mDateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());

        // スキーシーズンの 開始日付と終了日付を設定する
        mDateFrom = new Date(mThisSeasonFrom.getTime());
        mDateTo = MiscUtils.addYears(mDateFrom, 1);
        List<SkiLogData> logs = DbUtils.selectLogSummaries(this, 0, 1);    // 最古の1件を取得する
        // データの 下限日付を取得しておく
        if (logs != null && !logs.isEmpty()) {
            mDateOldest = logs.get(0).getCreated();
        } else {
            mDateOldest = mThisSeasonFrom;
        }
        mChartTextSize = SdkUtils.getSpDimension(this, R.dimen.text_size_chart_axis);
        mChartLabel = getString(R.string.label_graph_desc);

        // Serviceプロセスとの 通信クラス作成
        mServiceMessenger = new ServiceMessenger(this, new ServiceMessenger.OnServiceMessageListener() {
            @Override
            public void onReceiveMessage(Message msg) {
                switch (msg.what) {
                    case ServiceMessenger.MSG_REPLY_LONG_ARRAY:
                        long[] data = (long[]) msg.obj;
                        if (data[0] <= 0) return;

                        updateChart(data[0], -data[3] * 0.001f);
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

        mChart = findViewById(R.id.bar_chart);
        setupChart();
        updateUi();
    }

    // タイトル表記を返す。
    // 年は年度での年表 (スキーシーズンは年末年始をまたぐので)
    private String getTitleString() {
        int[] date = MiscUtils.getDateValues(mDateFrom);
        if (date[1] >= START_MONTH_OF_SEASON) date[0]++;
        return getString(R.string.fmt_title_chart2, date[0]);
    }

    private void updateUi() {
        if (mSearchTag != null) {
            // タグで絞り込み表示
            setTitle(getString(R.string.fmt_title_tag, mSearchTag));
            // 前データ、次データへのボタンの 有効無効
            UiUtils.setEnabled(this, R.id.btn_negative, false);
            UiUtils.setEnabled(this, R.id.btn_positive, false);

        } else {
            // シーズン表示
            setTitle(getTitleString());
            // 前データ、次データへのボタンの 有効無効
            UiUtils.setEnabled(this, R.id.btn_negative, mDateOldest.before(mDateFrom));
            UiUtils.setEnabled(this, R.id.btn_positive, mDateFrom.before(mThisSeasonFrom));

        }
        UiUtils.setEnabled(this, R.id.btn_tag, !(mTags == null || mTags.isEmpty()));
    }

    private Date getStartDateOfSeason(Date date) {
        int year = MiscUtils.getYear(date);
        int month = MiscUtils.getMonth(date);
        if (month < START_MONTH_OF_SEASON) year--;

        return MiscUtils.toDate(year, START_MONTH_OF_SEASON, 1);
    }

    private void setupChart() {
        if (mSearchTag != null) {
            // tagで絞る
            mSkiLogs = DbUtils.selectLogSummaries(this, mSearchTag);
        } else {
            // 期間で絞る
            mSkiLogs = DbUtils.selectLogSummaries(this, mDateFrom, mDateTo);
        }
        if (mSkiLogs == null || mSkiLogs.isEmpty()) return;

        List<IBarDataSet> logs = getBarData();
        if (logs == null) return;

        BarData data = new BarData(logs);
        mChart.clear();
        mChart.setData(data);

        mChart.getXAxis().setTextColor(mColorForeground);
        mChart.getXAxis().setTextSize(mChartTextSize);          // 縦軸のラベルの文字サイズ
        mChart.getAxisLeft().setTextColor(mColorForeground);
        mChart.getAxisLeft().setTextSize(mChartTextSize);       // 縦軸のラベルの文字サイズ

        float textSize = SdkUtils.getSpDimension(this, R.dimen.text_size_regular);
        mChart.getLegend().setTextColor(mColorForeground);
        mChart.getLegend().setTextSize(textSize);

        //Y軸(左)
        YAxis left = mChart.getAxisLeft();
        left.setAxisMinimum(mYAxisMin);
        left.setAxisMaximum(mYAxisMax);
//        left.setLabelCount(5);
        left.setDrawTopYLabelEntry(true);
        //整数表示に
        left.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                // 縦軸の valueは高度
                return getString(R.string.fmt_meter, value);
            }
        });

        //Y軸(右)
        YAxis yAxis = mChart.getAxisRight();
        yAxis.setDrawLabels(false);
        yAxis.setDrawGridLines(false);
//        yAxis.setDrawZeroLine(true);
//        yAxis.setDrawTopYLabelEntry(true);

        //X軸
        XAxis xAxis = mChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(mXAxisLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawLabels(true);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);

        int dataCount = mXAxisLabels.size();
        if (dataCount <= 6) {
            mChart.setVisibleXRangeMinimum(6.0f);               // 一度に表示する棒グラフの数 (少ないと棒の幅が広すぎるため設定)
            xAxis.setLabelCount(dataCount);
            if (dataCount <= 1) xAxis.setAxisMinimum(-0.6f);    // データが一件の際、なぜかラベルが 2つ書かれる(0軸にも書かれる)ので、パッチ処理
        } else {
            mChart.setVisibleXRangeMaximum(7.5f);               // 一度に表示する棒グラフの数 (スクロールアウトしているのがわかる様に 端数を指定)
            mChart.moveViewToX((float)dataCount - 0.5f);
        }

        //グラフ上の表示
        mChart.setDrawValueAboveBar(true);
        mChart.getDescription().setEnabled(false);
        mChart.setClickable(true);
        mChart.setHighlightPerTapEnabled(true);
        mChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                int index = (int)(e.getX() + 0.5f);
                if (mSkiLogs == null || mSkiLogs.size() <= index) return;

                // 日別チャートを表示する
                LineChartActivity.startActivity(BarChartActivity.this, mSkiLogs.get(index).getCreated());
                finish();
            }

            @Override
            public void onNothingSelected() {
            }
        });

        //凡例
//        mChart.getLegend().setEnabled(false);

        mChart.setScaleEnabled(false);
        //アニメーション
//        mChart.animateY(1200, Easing.EasingOption.Linear);

        mChart.getOnTouchListener().setLastHighlighted(null);
    }

    //棒グラフのデータを取得
    private List<IBarDataSet> getBarData() {
        if (mSkiLogs == null || mSkiLogs.isEmpty()) return null;

        //表示させるデータ
        List<BarEntry> entries = new ArrayList<>();

        // 横軸(日付)/縦軸(高度)の表示を設定
        mXAxisLabels = new ArrayList<>();
        mYAxisMax = 0.0f;
        mYAxisMin = 0.0f;

        for (int i=0; i<mSkiLogs.size(); i++) {
            SkiLogData log = mSkiLogs.get(i);
            mXAxisLabels.add(mDateFormat.format(log.getCreated()));

            float accumulate = -log.getDescTotal();
            entries.add(new BarEntry(i, accumulate));
            mYAxisMax = MiscUtils.maxValue(mYAxisMax, accumulate);
            mYAxisMin = MiscUtils.minValue(mYAxisMin, accumulate);
        }
        // 縦軸は 100ｍごとの区切りに補正しておく
        int boundary = 100;
        mYAxisMax = (float)(Math.ceil(mYAxisMax / boundary) * boundary);
        mYAxisMin = (float)(Math.floor(mYAxisMin / boundary) * boundary);

        List<IBarDataSet> bars = new ArrayList<>();
        BarDataSet dataSet = new BarDataSet(entries, mChartLabel);
        dataSet.setValueTextColor(mColorForeground);

        dataSet.setValueTextSize(mChartTextSize);
        //整数で表示
        dataSet.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return getString(R.string.fmt_meter, value);
            }
        });
        // 棒グラフの tapを検出するためにハイライトを有効にする
        dataSet.setHighlightEnabled(true);

        //Barの色をセット
        dataSet.setColor(SdkUtils.getColor(this, R.color.colorAccumulateDesc));
        bars.add(dataSet);

        return bars;
    }

    private void updateChart(long time, float accumulate) {
        if (mXAxisLabels == null) return;
        int pos = mXAxisLabels.indexOf(mDateFormat.format(time));
        if (pos < 0) return;

        // 棒グラフの長さを更新
        BarData barData = mChart.getBarData();
        if (barData == null || barData.getDataSetCount() < 1) return;
        IBarDataSet dataSet = barData.getDataSetByIndex(0);
        BarEntry entry = dataSet.getEntryForIndex(pos);
        entry.setY(accumulate);

        // Y座標の表示範囲を更新
        int boundary = 100;
        if (accumulate > mYAxisMax) {
            mYAxisMax = (float)(Math.ceil(accumulate / boundary) * boundary);
            mChart.getAxisLeft().setAxisMaximum(mYAxisMax);
        }
        if (accumulate < mYAxisMin) {
            mYAxisMin = (float)(Math.floor(accumulate / boundary) * boundary);
            mChart.getAxisLeft().setAxisMinimum(mYAxisMin);
        }

        mChart.highlightValue(null);
        //更新を通知
        barData.notifyDataChanged();
        mChart.notifyDataSetChanged();

        mChart.invalidate();
    }


    private void deleteLogs() {
        if (mDateFrom == null || mDateTo == null) return;       // 念のためチェック
        // 該当期間のログ情報をDBから削除する
        boolean res = DbUtils.deleteLogs(this, mDateFrom, mDateTo);
        if (res) {
            // 紐づいたタグ情報もDBから削除する
            DbUtils.deleteTags(this, mDateFrom, mDateTo);

            if (!mDateOldest.before(mDateFrom)) {
                // データの 参照期間を更新しておく
                List<SkiLogData> logs = DbUtils.selectLogSummaries(this, 0, 1);    // 最古の1件を取得する
                if (logs != null && !logs.isEmpty()) {
                    mDateOldest = logs.get(0).getCreated();
                } else {
                    mDateOldest = mThisSeasonFrom;
                }
            }
            // チャートの表示を更新する
            //setupChart();
            mChart.clear();     //  年度グラフは、最古記録以降の年度は データ0件の年度も表示するので、削除しても期間は移動させない
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
        DialogUtils.showItemSelectDialog(this, R.string.title_select_tag, arrayTag, mIndexTag, RC_SELECT_TAG);
    }

    private void confirmDeleteLogs() {
        if (mDateFrom == null || mDateTo == null) return;       // 念のためチェック
        // データ削除
        String title = getString(R.string.title_delete_logs);
        String message = getString(R.string.fmt_delete_seasonal_logs, getTitleString());
        DialogUtils.showOkCancelDialog(this, title, message, RC_DELETE_LOG);
    }

    @Override
    public void onDialogEvent(int requestCode, AlertDialog dialog, int which, View view) {
        switch (requestCode) {
            case RC_DELETE_LOG:
                if (which == DialogUtils.EVENT_BUTTON_POSITIVE) {
                    deleteLogs();
                }
                break;

            case RC_SELECT_TAG:
                if (which == DialogUtils.EVENT_BUTTON_POSITIVE) {
                    if (view instanceof ListView) {
                        int pos = ((ListView)view).getCheckedItemPosition();
                        if (mTags != null && pos >= 0 && pos < mTags.size()) {
                            mIndexTag = pos;
                            mSearchTag = mTags.get(mIndexTag).getTag();
                        } else {
                            if (mIndexTag >= 0) {
                                Intent intent = new Intent(this, BarChartActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                //finish();
                                return;
                            }
                            mIndexTag = -1;
                            mSearchTag = null;
                        }
                        setupChart();
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
                mDateFrom = MiscUtils.addYears(mDateFrom, -1);
                mDateTo = MiscUtils.addYears(mDateTo, -1);
                updateUi();
                setupChart();
                break;

            case R.id.btn_positive:
                mDateFrom = MiscUtils.addYears(mDateFrom, 1);
                mDateTo = MiscUtils.addYears(mDateTo, 1);
                updateUi();
                setupChart();
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
