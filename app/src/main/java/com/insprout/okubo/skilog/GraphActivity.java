package com.insprout.okubo.skilog;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

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
import java.util.Date;
import java.util.List;


public class GraphActivity extends AppCompatActivity implements View.OnClickListener {

    private String mTitle;
    private Date mTargetDate;

    private LineChart mChart;
    private ArrayList<Entry> mChartValues;
    private float mChartMinX;
    private float mChartMaxX;
    private float mChartMinY;
    private float mChartMaxY;
    private ArrayList<Entry> mChartValues2;
    private ArrayList<Entry> mChartValues3;
    private float mChartMinY2;
    private float mChartMaxY2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        initVars();                                             // 変数などの初期化
        initView();                                             // View関連の初期化
    }


    private void initVars() {
        mTitle = getString(R.string.title_asc);
        mTargetDate = new Date(System.currentTimeMillis());
        setTitle(mTitle, mTargetDate);
    }

    private void initView() {

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

        // add data
        //drawChart();
        drawChartAsc();
    }

    private void setupChartValues(Date targetDate) {
        // DBから 指定日のデータを取得する
        List<SkiLogData> data = DbUtils.select(this, targetDate);
        if (targetDate == null || data == null || data.size() == 0) {
            mChartValues = null;
            mChartValues2 = null;
            mChartValues3 = null;
            return;
        }

        long timeStart = TimeUtils.getDate(targetDate).getTime();

        // チャート用の データクラスに格納する
        mChartMinX = Float.POSITIVE_INFINITY;
        mChartMaxX = Float.NEGATIVE_INFINITY;
        mChartMinY = Float.POSITIVE_INFINITY;
        mChartMaxY = Float.NEGATIVE_INFINITY;
        mChartMinY2 = Float.POSITIVE_INFINITY;
        mChartMaxY2 = Float.NEGATIVE_INFINITY;

        mChartValues = new ArrayList<>();
        mChartValues2 = new ArrayList<>();
        mChartValues3 = new ArrayList<>();
        for (SkiLogData log : data) {
//            String msg = "" + log.getCreated().toString() + " 高度:" + log.getAltitude() + " 上昇:" + log.getAscTotal() + " 下降:" + log.getDescTotal() + " RUN:" + log.getCount();
//            Log.d("database", msg);

            float time = (log.getCreated().getTime() - timeStart) / (60 * 60 * 1000.0f);
            float altitude = log.getAltitude();
            float ascent = log.getAscTotal();
            float descent = -log.getDescTotal();

            // ついでにグラフの最大値/最小値を記録しておく
            if (time > mChartMaxX) mChartMaxX = time;
            if (time < mChartMinX) mChartMinX = time;
            if (altitude > mChartMaxY) mChartMaxY = altitude;
            if (altitude < mChartMinY) mChartMinY = altitude;
            if (ascent > mChartMaxY2) mChartMaxY2 = ascent;
            if (ascent < mChartMinY2) mChartMinY2 = ascent;
            if (descent > mChartMaxY2) mChartMaxY2 = descent;
            if (descent < mChartMinY2) mChartMinY2 = descent;
            mChartValues.add(new Entry(time, altitude, null, null));
            mChartValues2.add(new Entry(time, ascent, null, null));
            mChartValues3.add(new Entry(time, descent, null, null));

        }

        // X軸は 1時間ごとの区切りに補正しておく
        mChartMaxX = (float) Math.ceil(mChartMaxX);
        mChartMinX = (float) Math.floor(mChartMinX);
        // Y軸は 100ｍごとの区切りに補正しておく
        int boundary = 100;
        mChartMaxY = (float) (Math.ceil(mChartMaxY / boundary) * boundary);
        mChartMinY = (float) (Math.floor(mChartMinY / boundary) * boundary);
        mChartMaxY2 = (float) (Math.ceil(mChartMaxY2 / boundary) * boundary);
        mChartMinY2 = (float) (Math.floor(mChartMinY2 / boundary) * boundary);
    }

    private void drawChart() {
        // 表示データを取得する
        String label = getString(R.string.label_altitude);
        setupChartValues(mTargetDate);
        if (mChartValues == null) {
            mChart.clear();
            return;
        }

        // Grid縦軸を破線
        XAxis xAxis = mChart.getXAxis();
        xAxis.setAxisMaximum(mChartMaxX);
        xAxis.setAxisMinimum(mChartMinX);
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = mChart.getAxisLeft();
        // Y軸最大最小設定
        leftAxis.setAxisMaximum(mChartMaxY);
        leftAxis.setAxisMinimum(mChartMinY);
        // Grid横軸を破線
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(true);

        LineDataSet set1;

        if (mChart.getData() != null &&
                mChart.getData().getDataSetCount() > 0) {

            set1 = (LineDataSet) mChart.getData().getDataSetByIndex(0);
            set1.setValues(mChartValues);
            mChart.getData().notifyDataChanged();
            mChart.notifyDataSetChanged();

        } else {
            // create a dataset and give it a type
//            set1 = new LineDataSet(mChartValues, label);
//
//            set1.setDrawIcons(false);
//            set1.setColor(Color.BLACK);
//            set1.setCircleColor(Color.BLACK);
//            set1.setLineWidth(1f);
//            set1.setCircleRadius(3f);
//            set1.setDrawCircleHole(false);
//            set1.setValueTextSize(0f);
//            set1.setFormLineWidth(1f);
//            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
//            set1.setFormSize(15.f);

            set1 = newLineDataSet(mChartValues, label, Color.BLUE);

            set1.setDrawFilled(true);
            set1.setFillColor(Color.BLUE);

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1); // add the datasets

            // create a data object with the datasets
            LineData lineData = new LineData(dataSets);

            // set data
            mChart.setData(lineData);
        }

        //mChart.animateX(2500);
        mChart.invalidate();

        // dont forget to refresh the drawing
        // mChart.invalidate();
    }

    private void drawChartAsc() {
        // 表示データを取得する
        setupChartValues(mTargetDate);
        if (mChartValues2 == null || mChartValues3 == null) {
            mChart.clear();
            return;
        }

        // Grid縦軸を破線
        XAxis xAxis = mChart.getXAxis();
        xAxis.setAxisMaximum(mChartMaxX);
        xAxis.setAxisMinimum(mChartMinX);
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis leftAxis = mChart.getAxisLeft();
        // Y軸最大最小設定
        leftAxis.setAxisMaximum(mChartMaxY2);
        leftAxis.setAxisMinimum(mChartMinY2);
        // Grid横軸を破線
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setDrawZeroLine(true);

        int colorAsc = Color.BLACK;
        int colorDesc = SdkUtils.getColor(this, R.color.colorAccent);
        String label = getString(R.string.label_graph_asc);
        String label2 = getString(R.string.label_graph_desc);

        LineDataSet set1;
        LineDataSet set2;

        if (mChart.getData() == null) {
            // create a dataset and give it a type
            set1 = newLineDataSet(mChartValues2, label, colorAsc);
            set2 = newLineDataSet(mChartValues3, label2, colorDesc);

            ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
            dataSets.add(set1); // add the datasets
            dataSets.add(set2); // add the datasets

            // create a data object with the datasets
            LineData lineData = new LineData(dataSets);

            // set data
            mChart.setData(lineData);

        } else {
            int dataCount = mChart.getData().getDataSetCount();
            switch (dataCount) {
                case -1:
                    break;

                case 0:
                    set1 = newLineDataSet(mChartValues2, label, colorAsc);
                    set2 = newLineDataSet(mChartValues3, label2, colorDesc);
                    mChart.getData().addDataSet(set1);
                    mChart.getData().addDataSet(set2);
                    mChart.getData().notifyDataChanged();
                    mChart.notifyDataSetChanged();
                    break;

                case 1:
                    set1 = (LineDataSet) mChart.getData().getDataSetByIndex(0);
                    set1.setValues(mChartValues2);
                    set2 = newLineDataSet(mChartValues3, label2, colorDesc);
                    mChart.getData().addDataSet(set2);
                    mChart.getData().notifyDataChanged();
                    mChart.notifyDataSetChanged();
                    break;

                default:
                    set1 = (LineDataSet) mChart.getData().getDataSetByIndex(0);
                    set1.setValues(mChartValues2);
                    set2 = (LineDataSet) mChart.getData().getDataSetByIndex(1);
                    set2.setValues(mChartValues3);
                    //
                    for (int i = dataCount - 1; i >= 2; i--) {
                        mChart.getData().removeDataSet(i);
                    }
                    mChart.getData().notifyDataChanged();
                    mChart.notifyDataSetChanged();
                    break;
            }
        }

        //mChart.animateX(2500);
        mChart.invalidate();

        // dont forget to refresh the drawing
        // mChart.invalidate();
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
        dataSet.setDrawFilled(false);
//        dataSet.setDrawFilled(true);
//        dataSet.setFillColor(color);

        return dataSet;
    }

    private void setTitle(String title, Date date) {
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        setTitle(title + df.format(date));
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch(id) {
            case R.id.btn_prev:
                mTargetDate = TimeUtils.addDays(mTargetDate, -1);
                setTitle(mTitle, mTargetDate);
//                drawChart();
                drawChartAsc();
                break;

            case R.id.btn_next:
                mTargetDate = TimeUtils.addDays(mTargetDate, 1);
                setTitle(mTitle, mTargetDate);
//                drawChart();
                drawChartAsc();
                break;
        }
    }


    public static void startActivity(Activity context) {
        Intent intent = new Intent(context, GraphActivity.class);
        context.startActivity(intent);
    }

}
