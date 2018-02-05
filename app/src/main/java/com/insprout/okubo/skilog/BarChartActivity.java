package com.insprout.okubo.skilog;

import android.app.Activity;
import android.content.Intent;
import android.graphics.DashPathEffect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RadioGroup;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.database.SkiLogData;
import com.insprout.okubo.skilog.util.SdkUtils;
import com.insprout.okubo.skilog.util.TimeUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class BarChartActivity extends AppCompatActivity implements View.OnClickListener {

    private Date mTargetDate;

    private BarChart chart;
//    private LineDataSet[] mChartDataSet1 = new LineDataSet[ 1 ];
//    private LineDataSet[] mChartDataSet2 = new LineDataSet[ 2 ];
//    private RectF mChartAxis1;
//    private RectF mChartAxis2;
    List<SkiLogData> mData;
    private float mMaxValue = Float.NEGATIVE_INFINITY;
    private float mMinValue = Float.POSITIVE_INFINITY;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bar_chart);

        chart = (BarChart) findViewById(R.id.bar_chart);

        List<IBarDataSet> logs = getBarData();
        if (logs == null) return;

        BarData data = new BarData(logs);
        chart.setData(data);

        //Y軸(左)
        YAxis left = chart.getAxisLeft();
        left.setAxisMinimum(mMinValue);
        left.setAxisMaximum(mMaxValue);
//        left.setLabelCount(5);
        left.setDrawTopYLabelEntry(true);
        //整数表示に
        left.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return "" + (int)value;
            }
        });

        //Y軸(右)
        YAxis right = chart.getAxisRight();
        right.setDrawLabels(false);
        right.setDrawGridLines(false);
        right.setDrawZeroLine(true);
        right.setDrawTopYLabelEntry(true);

        //X軸
        XAxis xAxis = chart.getXAxis();
        //X軸に表示するLabelのリスト(最初の""は原点の位置)
        final String[] labels = {"","国語", "数学", "英語"};

        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        XAxis bottomAxis = chart.getXAxis();
        bottomAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        bottomAxis.setDrawLabels(true);
        bottomAxis.setDrawGridLines(false);
        bottomAxis.setDrawAxisLine(true);

        //グラフ上の表示
        chart.setDrawValueAboveBar(true);
        chart.getDescription().setEnabled(false);
        chart.setClickable(false);

        //凡例
        chart.getLegend().setEnabled(false);

        chart.setScaleEnabled(false);
        //アニメーション
//        chart.animateY(1200, Easing.EasingOption.Linear);
    }

    //棒グラフのデータを取得
    private List<IBarDataSet> getBarData() {
        mData = DbUtils.selectDailyLogs(this);
        if (mData == null || mData.isEmpty()) return null;

        //表示させるデータ
        mMaxValue = Float.NEGATIVE_INFINITY;
        mMinValue = Float.POSITIVE_INFINITY;
        ArrayList<BarEntry> entries = new ArrayList<>();
//        for (SkiLogData log : data) {
        for (int i = 0; i<mData.size(); i++) {
            SkiLogData log = mData.get(i);
            float accumulate = -log.getDescTotal();
            entries.add(new BarEntry(i, accumulate));
            mMaxValue = maxValue(mMaxValue, accumulate);
            mMinValue = minValue(mMinValue, accumulate);
        }

        List<IBarDataSet> bars = new ArrayList<>();
        BarDataSet dataSet = new BarDataSet(entries, "bar");

        //整数で表示
        dataSet.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return "" + (int) value;
            }
        });
        //ハイライトさせない
        dataSet.setHighlightEnabled(false);

        //Barの色をセット
        dataSet.setColor(SdkUtils.getColor(this, R.color.colorAccumulateDesc));
        //dataSet.setColors(new int[]{ R.color.colorAccumulateAsc, R.color.colorAccumulateDesc }, this);
        bars.add(dataSet);

        return bars;
    }

    @Override
    public void onClick(View view) {
//        int id = view.getId();
//        switch(id) {
//            case R.id.btn_prev:
//                mTargetDate = TimeUtils.addDays(mTargetDate, -1);
//                setTitle(mTargetDate);
//                updateChart();
//                break;
//
//            case R.id.btn_next:
//                mTargetDate = TimeUtils.addDays(mTargetDate, 1);
//                setTitle(mTargetDate);
//                updateChart();
//                break;
//        }
    }

    private float minValue(float... values) {
        if (values.length == 0) return Float.NEGATIVE_INFINITY;

        float min = Float.POSITIVE_INFINITY;
        for (float value : values) {
            if (value < min) min = value;
        }
        return min;
    }

    private float maxValue(float... values) {
        if (values.length == 0) return Float.POSITIVE_INFINITY;

        float max = Float.NEGATIVE_INFINITY;
        for (float value : values) {
            if (value > max) max = value;
        }
        return max;
    }

    public static void startActivity(Activity context) {
        Intent intent = new Intent(context, BarChartActivity.class);
        context.startActivity(intent);
    }

}
