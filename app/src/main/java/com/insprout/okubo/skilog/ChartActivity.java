package com.insprout.okubo.skilog;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.RectF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.RadioGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;

import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.database.SkiLogData;
import com.insprout.okubo.skilog.util.SdkUtils;
import com.insprout.okubo.skilog.util.TimeUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class ChartActivity extends AppCompatActivity implements View.OnClickListener {

    private Date mTargetDate;
    private RadioGroup mRgChartType;

    private LineChart mChart;
    private LineDataSet[] mChartDataSet1 = new LineDataSet[ 1 ];
    private LineDataSet[] mChartDataSet2 = new LineDataSet[ 2 ];
    private RectF mChartAxis1;
    private RectF mChartAxis2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chart);

        initVars();                                             // 変数などの初期化
        initView();                                             // View関連の初期化
    }


    private void initVars() {
        mTargetDate = new Date(System.currentTimeMillis());
        setTitle(mTargetDate);

        // 高度のチャート
        int color = Color.BLUE;
        mChartDataSet1[0] = newLineDataSet(new ArrayList<Entry>(), getString(R.string.label_altitude), color);
        mChartDataSet1[0].setDrawFilled(true);
        mChartDataSet1[0].setFillColor(Color.BLUE);

        // 上昇・下降積算のチャート
        int colorAsc = Color.BLACK;
        int colorDesc = SdkUtils.getColor(this, R.color.colorAccent);
        mChartDataSet2[0] = newLineDataSet(new ArrayList<Entry>(), getString(R.string.label_graph_asc), colorAsc);
        mChartDataSet2[0].setDrawFilled(false);
        mChartDataSet2[1] = newLineDataSet(new ArrayList<Entry>(), getString(R.string.label_graph_desc), colorDesc);
        mChartDataSet2[1].setDrawFilled(false);
    }

    private void initView() {

        mRgChartType = findViewById(R.id.rg_chart_type);
        mRgChartType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                switch(id) {
                    case R.id.btn_altitude:
                        drawChartAltitude();
                        break;
                    case R.id.btn_accumulate:
                        drawChartAccumulate();
                        break;
                }
            }
        });

        // チャートの表示設定
        mChart = findViewById(R.id.line_chart);
        // Grid背景色
        mChart.setDrawGridBackground(true);
        // no description text
        mChart.getDescription().setEnabled(true);
        // 右側の目盛り
        mChart.getAxisRight().setEnabled(false);

        float textSize = getResources().getDimension(R.dimen.text_size_chart_axis);
        mChart.getXAxis().setTextSize(textSize);
        mChart.getAxisLeft().setTextSize(textSize);

        // チャートの描画を開始する
        mRgChartType.check(R.id.btn_altitude);
    }

    private boolean setupChartValues(Date targetDate) {
        // 3種のチャート用データを設定する
        mChartDataSet1[0].getValues().clear();                  // 高度チャート用データ
        mChartDataSet2[0].getValues().clear();                  // 上昇積算チャート用データ
        mChartDataSet2[1].getValues().clear();                  // 下降積算チャート用データ

        // DBから 指定日のデータを取得する
        List<SkiLogData> data = DbUtils.select(this, targetDate);
        if (targetDate == null || data == null || data.size() == 0) {
            return false;
        }

        // 取得したデータをチャート用の データクラスに格納する。また データの最大値/最小値も記録しておく
        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY1 = Float.POSITIVE_INFINITY;
        float maxY1 = Float.NEGATIVE_INFINITY;
        float minY2 = Float.POSITIVE_INFINITY;
        float maxY2 = Float.NEGATIVE_INFINITY;

        // チャート用の データクラスに格納する
        long timeAm0 = TimeUtils.getDate(targetDate).getTime();
        for (SkiLogData log : data) {
//            String msg = "" + log.getCreated().toString() + " 高度:" + log.getAltitude() + " 上昇:" + log.getAscTotal() + " 下降:" + log.getDescTotal() + " RUN:" + log.getCount();
//            Log.d("database", msg);

            // X軸は 対象日の午前0時からの経過時間とする
            float time = (log.getCreated().getTime() - timeAm0) / (60 * 60 * 1000.0f);
            float altitude = log.getAltitude();
            float ascent = log.getAscTotal();
            float descent = -log.getDescTotal();
            mChartDataSet1[0].getValues().add(new Entry(time, altitude, null, null));
            mChartDataSet2[0].getValues().add(new Entry(time, ascent, null, null));
            mChartDataSet2[1].getValues().add(new Entry(time, descent, null, null));

            // ついでにデータの最大値/最小値を記録しておく
            maxX = maxValue(maxX, time);
            minX = minValue(minX, time);
            maxY1 = maxValue(maxY1, altitude);
            minY1 = minValue(minY1, altitude);
            maxY2 = maxValue(maxY2, ascent, descent);
            minY2 = minValue(minY2, ascent, descent);
        }

        // チャートの表示領域を記録
        // X軸は 1時間ごとの区切りに補正しておく
        maxX = (float)Math.ceil(maxX);
        minX = (float)Math.floor(minX);
        // Y軸は 100ｍごとの区切りに補正しておく
        int boundary = 100;
        mChartAxis1 = new RectF(minX, (float)(Math.ceil(maxY1 / boundary) * boundary), maxX, (float)(Math.floor(minY1 / boundary) * boundary));
        mChartAxis2 = new RectF(minX, (float)(Math.ceil(maxY2 / boundary) * boundary), maxX, (float)(Math.floor(minY2 / boundary) * boundary));

        return true;
    }

    private void drawChartAltitude() {
        mChart.clear();

        // 表示データを取得する
        if (setupChartValues(mTargetDate)) {
            // 高度チャートを描画する
            drawChart(mChartDataSet1, mChartAxis1);
        }
    }


    private void drawChartAccumulate() {
        mChart.clear();

        // 表示データを取得する
        if (setupChartValues(mTargetDate)) {
            // 積算チャート(2種類)を描画する
            drawChart(mChartDataSet2, mChartAxis2);
        }
    }

    private void updateChart() {
        int chartId = mRgChartType.getCheckedRadioButtonId();
        switch (chartId) {
            case R.id.btn_altitude:
                updateChartAltitude();
                break;

            case R.id.btn_accumulate:
                updateChartAccumulate();
                break;

            case -1:
                // RadioButtonがどれも選択されていない場合
                drawChartAltitude();
                break;
        }
    }

    private void updateChartAltitude() {
        // 表示データを取得する
        if (setupChartValues(mTargetDate)) {
            // 高度チャートを描画する
            updateChart(mChartDataSet1, mChartAxis1);
        } else {
            mChart.clear();
        }
    }

    private void updateChartAccumulate() {
        // 表示データを取得する
        if (setupChartValues(mTargetDate)) {
            // 積算チャート(2種類)を描画する
            updateChart(mChartDataSet2, mChartAxis2);
        } else {
            mChart.clear();
        }
    }


    private void drawChart(LineDataSet[] dataSets, RectF axis) {
        if (dataSets == null) return;
        for (LineDataSet dataSet : dataSets) {
            if (dataSet.getValues().isEmpty()) return;
        }

        mChart.clear();
        // チャートの 軸表示設定
        setupAxis(axis);

        ArrayList<ILineDataSet> lineDataSets = new ArrayList<ILineDataSet>();
        // add the datasets
        lineDataSets.addAll(Arrays.asList(dataSets));
        // create a data object with the datasets
        LineData lineData = new LineData(lineDataSets);

        // set data
        mChart.setData(lineData);

        //mChart.animateX(2500);
        mChart.invalidate();
    }

    /**
     * Chartには 必要な LineDataSetが設定されている前提で、チャートを更新する
     */
    private void updateChart(LineDataSet[] dataSets, RectF axis) {
        if (dataSets == null) return;
        for (LineDataSet dataSet : dataSets) {
            if (dataSet.getValues().isEmpty()) return;
        }

        if (mChart.getData() == null) {
            // Chart.clear()などが行われるとLineDataは nullになっているので、その場合は新規にチャートを描く
            drawChart(dataSets, axis);
            return;
        }

        // チャートの 軸表示設定
        setupAxis(axis);

        mChart.getData().notifyDataChanged();
        mChart.notifyDataSetChanged();

        //mChart.animateX(2500);
        mChart.invalidate();
    }

    private LineDataSet newLineDataSet(List<Entry>yValues, String label, int color) {
        LineDataSet dataSet = new LineDataSet(yValues, label);

        dataSet.setDrawIcons(false);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(1f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(0f);
        dataSet.setFormLineWidth(1f);
        dataSet.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
        dataSet.setFormSize(15.f);

        dataSet.setColor(color);
        dataSet.setCircleColor(color);
//        dataSet.setDrawFilled(false);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(color);

        return dataSet;
    }

    private void setupAxis(RectF axis) {
        // Grid縦軸を破線
        XAxis xAxis = mChart.getXAxis();
        xAxis.setAxisMaximum(axis.right);
        xAxis.setAxisMinimum(axis.left);
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = mChart.getAxisLeft();
        // Y軸最大最小設定
        leftAxis.setAxisMaximum(axis.top);
        leftAxis.setAxisMinimum(axis.bottom);
        // Grid横軸を破線
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(true);
    }

    private void setTitle(Date date) {
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        setTitle(getString(R.string.fmt_title_chart,df.format(date)));
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch(id) {
            case R.id.btn_prev:
                mTargetDate = TimeUtils.addDays(mTargetDate, -1);
                setTitle(mTargetDate);
                updateChart();
                break;

            case R.id.btn_next:
                mTargetDate = TimeUtils.addDays(mTargetDate, 1);
                setTitle(mTargetDate);
                updateChart();
                break;
        }
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
        Intent intent = new Intent(context, ChartActivity.class);
        context.startActivity(intent);
    }

}
