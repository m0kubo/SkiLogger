package com.insprout.okubo.skilog.chart;

import android.content.Context;
import android.util.TypedValue;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.Chart;
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
import com.insprout.okubo.skilog.R;
import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.model.SkiLogDb;
import com.insprout.okubo.skilog.util.AppUtils;
import com.insprout.okubo.skilog.util.MiscUtils;
import com.insprout.okubo.skilog.util.SdkUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by okubo on 2018/03/14.
 */

public class SummaryChart {
    private final static int START_MONTH_OF_SEASON = 9;         // 月の指定は 0～11。(0⇒1月  11⇒12月)

    private Context mContext;
    private BarChart mChart;
    private OnChartValueSelectedListener mValueSelectedListener;
    private int mColorForeground;
    private int[] mBarColors;

    private List<SkiLogDb> mSkiLogs;
    private List<String> mXAxisLabels;                              //X軸に表示するLabelのリスト
    private float mYAxisMax = 0f;
    private float mYAxisMin = 0f;
    private float mChartTextSize;

    private Date mThisSeasonFrom;
    private Date mDateOldest;
    private Date mDateFrom, mDateTo;
    private SimpleDateFormat mDateFormat;

    private String mSearchTag = null;

    public SummaryChart(Context context, Chart barChart, OnChartValueSelectedListener listener) {
        mContext = context;
        mChart = (BarChart)barChart;
        mValueSelectedListener = listener;

        initVars();
    }

    private void initVars() {
        // テーマ切り替えに対応するため、フォアグランド色はテーマから取得する
        TypedValue typedValue = new TypedValue();
        mContext.getTheme().resolveAttribute(android.R.attr.colorForeground, typedValue, true);
        mColorForeground = SdkUtils.getColor(mContext, typedValue.resourceId);

        mThisSeasonFrom = getStartDateOfSeason(new Date(System.currentTimeMillis()));
        mDateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());

        // スキーシーズンの 開始日付と終了日付を設定する
        mDateFrom = new Date(mThisSeasonFrom.getTime());
        mDateTo = MiscUtils.addYears(mDateFrom, 1);
        List<SkiLogDb> logs = DbUtils.selectLogSummaries(mContext, 0, 1);    // 最古の1件を取得する
        // データの 下限日付を取得しておく
        if (logs != null && !logs.isEmpty()) {
            mDateOldest = logs.get(0).getCreated();
        } else {
            mDateOldest = mThisSeasonFrom;
        }
        mChartTextSize = SdkUtils.getSpDimension(mContext, R.dimen.text_size_chart_axis);
    }


    public Date getSelectedDate() {
        Highlight[] items = mChart.getHighlighted();
        if (items == null || items.length == 0) return null;

        int index = (int)(items[ items.length-1 ].getX());
        if (index >= 0 && index < mSkiLogs.size()) {
            return mSkiLogs.get(index).getCreated();
        }
        return null;
    }

    public void setFilter(String filteringTag) {
        mSearchTag = filteringTag;
    }

    public String getFilter() {
        return mSearchTag;
    }

    public boolean hasNextPage() {
        if (mSearchTag != null) return false;
        return mDateFrom.before(mThisSeasonFrom);
    }

    public boolean hasPreviousPage() {
        if (mSearchTag != null) return false;
        return mDateOldest.before(mDateFrom);
    }

    public void goNextPage() {
        mDateFrom = MiscUtils.addYears(mDateFrom, 1);
        mDateTo = MiscUtils.addYears(mDateTo, 1);
        drawChart();
    }

    public void goPreviousPage() {
        mDateFrom = MiscUtils.addYears(mDateFrom, -1);
        mDateTo = MiscUtils.addYears(mDateTo, -1);
        drawChart();
    }

    public String getXAxisLabel(float value) {
        int index = (int)value;
        if (index >= 0 && index < mXAxisLabels.size()) return mXAxisLabels.get(index);
        return null;
    }

    public String getXAxisLabelFull(float value) {
        Date date = getLogDate((int)value);
        if (date == null) return null;
        SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        return mDateFormat.format(date);
    }

    public Date getLogDate(float value) {
        return getLogDate((int)value);
    }

    public Date getLogDate(int index) {
        return mSkiLogs != null && index >= 0 && index < mSkiLogs.size() ? mSkiLogs.get(index).getCreated() : null;
    }

    public Date[] getLogDates() {
        if (mSkiLogs == null) return null;
        Date[] dates = new Date[mSkiLogs.size()];
        for(int i=0; i<mSkiLogs.size(); i++) {
            dates[i] = mSkiLogs.get(i).getCreated();
        }
        return dates;
    }

    public String getYAxisLabel(float value) {
        // 縦軸の valueは高度
        return AppUtils.getFormattedMeter(mContext, value);
    }

    public void clearChart() {
        mChart.clear();

        if (mValueSelectedListener != null) {
            mValueSelectedListener.onNothingSelected();
        }
    }

    public void updateChart() {
        drawChart();
    }

    public void drawChart() {
        clearChart();

        if (mSearchTag != null) {
            // tagで絞る
            mSkiLogs = DbUtils.selectLogSummaries(mContext, mSearchTag);
        } else {
            // 全件取得
            mSkiLogs = DbUtils.selectLogSummaries(mContext, 0, -1);
        }
        if (mSkiLogs == null || mSkiLogs.isEmpty()) return;
        // 棒グラフの色を設定する。年が変わると色を変える
        mBarColors = new int[mSkiLogs.size()];
        int year = -1;
        int index = 0;
        int colorId = R.color.colorAccumulateDesc;
        // 最新のデータが同じ色になるように、現在から過去のデータに向かって色を設定する
        for (int i = mSkiLogs.size() - 1; i >= 0; i--) {
            SkiLogDb log = mSkiLogs.get(i);
            if (MiscUtils.getYear(log.getCreated()) != year) {
                year = MiscUtils.getYear(log.getCreated());
                colorId = (index++ % 2 == 0 ? R.color.colorAccumulateDesc : R.color.colorAccumulateDesc2);
            }
            mBarColors[i] = colorId;
        }

        List<IBarDataSet> logs = getBarData();
        if (logs == null) return;

        BarData data = new BarData(logs);
        mChart.setData(data);

        mChart.getXAxis().setTextColor(mColorForeground);
        mChart.getXAxis().setTextSize(mChartTextSize);          // 縦軸のラベルの文字サイズ
        mChart.getAxisLeft().setTextColor(mColorForeground);
        mChart.getAxisLeft().setTextSize(mChartTextSize);       // 縦軸のラベルの文字サイズ

        float textSize = SdkUtils.getSpDimension(mContext, R.dimen.text_size_regular);
        mChart.getLegend().setTextColor(mColorForeground);
        mChart.getLegend().setTextSize(textSize);

        //Y軸(左)
        YAxis left = mChart.getAxisLeft();
        left.setAxisMinimum(mYAxisMin);
        left.setAxisMaximum(mYAxisMax);
        left.setDrawTopYLabelEntry(true);
        //整数表示に
        left.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                // 縦軸の valueは高度
                return getYAxisLabel(value);
            }
        });

        //Y軸(右)
        YAxis yAxis = mChart.getAxisRight();
        yAxis.setDrawLabels(false);
        yAxis.setDrawGridLines(false);

        //X軸
        XAxis xAxis = mChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(mXAxisLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawLabels(true);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);

        int MAX_X_LABELS = 7;
        mChart.setVisibleXRange(MAX_X_LABELS, MAX_X_LABELS + 0.5f);     // 一度に表示する棒グラフの数 (スクロールアウトしているのがわかる様に 端数を指定)
        int dataCount = mXAxisLabels.size();
        if (dataCount > MAX_X_LABELS) {
            mChart.moveViewToX((float) dataCount - MAX_X_LABELS);       // 末尾が表示されるようにスクロール
        } else {
            xAxis.setAxisMinimum(-0.7f);    // デフォルトの -0.5のままだと 最初のラベルが2重に描かれてしまう
            xAxis.setLabelCount(dataCount);
        }

        //グラフ上の表示
        mChart.setDrawValueAboveBar(true);
        mChart.getDescription().setEnabled(false);
        mChart.setClickable(true);
        mChart.setHighlightPerTapEnabled(true);
        mChart.setOnChartValueSelectedListener(mValueSelectedListener);

        //凡例
        mChart.getLegend().setEnabled(false);

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
            SkiLogDb log = mSkiLogs.get(i);
            mXAxisLabels.add(mDateFormat.format(log.getCreated()));

            float accumulate = Math.abs(log.getDescTotal());
            entries.add(new BarEntry(i, accumulate));
            mYAxisMax = MiscUtils.maxValue(mYAxisMax, accumulate);
            mYAxisMin = MiscUtils.minValue(mYAxisMin, accumulate);
        }
        // 縦軸は 100ｍごとの区切りに補正しておく
        int boundary = 100;
        mYAxisMax = (float)(Math.ceil(mYAxisMax / boundary) * boundary);
        mYAxisMin = (float)(Math.floor(mYAxisMin / boundary) * boundary);

        List<IBarDataSet> bars = new ArrayList<>();
        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setValueTextColor(mColorForeground);

        dataSet.setValueTextSize(mChartTextSize);
        //整数で表示
        dataSet.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return getYAxisLabel(value);
            }
        });
        // 棒グラフの tapを検出するためにハイライトを有効にする
        dataSet.setHighlightEnabled(true);

        //Barの色をセット
        //dataSet.setColor(SdkUtils.getColor(mContext, R.color.colorAccumulateDesc));
        dataSet.setColors(mBarColors, mContext);
        bars.add(dataSet);

        return bars;
    }

    public void appendChartValue(long time, float altitude, float accumulateAsc, float accumulateDesc, int runCount) {
        // 追加されたデータが どの棒グラフのものかを判別
        int index = indexOfLogs(new Date(time));
        if (index < 0) return;

        float accumulate = Math.abs(accumulateDesc);        // 下降積算データは負の値なので、絶対値に直す
        // 棒グラフの長さを更新
        BarData barData = mChart.getBarData();
        if (barData == null || barData.getDataSetCount() < 1) return;
        IBarDataSet dataSet = barData.getDataSetByIndex(0);
        BarEntry entry = dataSet.getEntryForIndex(index);
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

    // データの件数を取得する
    // drawChart()実行後に有効
    public int getCount() {
        return mSkiLogs != null ? mSkiLogs.size() : 0;
    }

    private int indexOfLogs(Date date) {
        if (date == null || mSkiLogs == null || mSkiLogs.size() == 0) return -1;
        for (int i=0; i<mSkiLogs.size(); i++) {
            if (MiscUtils.isSameDate(date, mSkiLogs.get(i).getCreated())) return i;
        }
        return -1;
    }

    // タイトル表記を返す。
    public String getSubject() {
        if (mSearchTag != null) {
            // タグで絞り込み表示
            return mContext.getString(R.string.fmt_title_tag, mSearchTag);

        } else {
            // シーズン表示
            return mContext.getString(R.string.title_chart_desc);
        }
    }


    private Date getStartDateOfSeason(Date date) {
        int year = MiscUtils.getYear(date);
        int month = MiscUtils.getMonth(date);
        if (month < START_MONTH_OF_SEASON) year--;

        return MiscUtils.toDate(year, START_MONTH_OF_SEASON, 1);
    }

}
