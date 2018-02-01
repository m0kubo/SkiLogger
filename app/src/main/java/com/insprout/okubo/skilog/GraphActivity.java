package com.insprout.okubo.skilog;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import com.insprout.okubo.skilog.util.TimeUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class GraphActivity extends AppCompatActivity implements View.OnClickListener {

    private Date mTargetDate;

    private LineChart mChart;
    private ArrayList<Entry> mChartValues;
    private float mChartMinX;
    private float mChartMaxX;
    private float mChartMinY;
    private float mChartMaxY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        initVars();                                             // 変数などの初期化
        initView();                                             // View関連の初期化
    }


    private void initVars() {
        mTargetDate = new Date(System.currentTimeMillis());
        setTitle(mTargetDate);
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
        drawChart();
    }

    private void setupChartValues(Date targetDate) {
        // DBから 指定日のデータを取得する
        List<SkiLogData> data = DbUtils.select(this, targetDate);
        if (targetDate == null || data == null || data.size() == 0) {
            mChartValues = null;
            return;
        }

//        long timeStart = TimeUtils.toDate(TimeUtils.getYear(targetDate), TimeUtils.getMonth(targetDate), TimeUtils.getDay(targetDate)).getTime();
        long timeStart = TimeUtils.getAM00(targetDate).getTime();
        Date t2 = TimeUtils.getAM00(null);
        if (t2 == null) {}

        // チャート用の データクラスに格納する
        mChartMinX = Float.POSITIVE_INFINITY;
        mChartMaxX = Float.NEGATIVE_INFINITY;
        mChartMinY = Float.POSITIVE_INFINITY;
        mChartMaxY = Float.NEGATIVE_INFINITY;

        mChartValues = new ArrayList<>();
        for (SkiLogData log : data) {
//            String msg = "" + log.getCreated().toString() + " 高度:" + log.getAltitude() + " 上昇:" + log.getAscTotal() + " 下降:" + log.getDescTotal() + " RUN:" + log.getCount();
//            Log.d("database", msg);

            float time = (log.getCreated().getTime() - timeStart) / (60 * 60 * 1000.0f);
            float altitude = log.getAltitude();
            // ついでにグラフの最大値/最小値を記録しておく
            if (time > mChartMaxX) mChartMaxX = time;
            if (time < mChartMinX) mChartMinX = time;
            if (altitude > mChartMaxY) mChartMaxY = altitude;
            if (altitude < mChartMinY) mChartMinY = altitude;
            mChartValues.add(new Entry(time, altitude, null, null));
        }

        // X軸は 1時間ごとの区切りに補正しておく
        mChartMaxX = (float)Math.ceil(mChartMaxX);
        mChartMinX = (float)Math.floor(mChartMinX);
        // Y軸は 100ｍごとの区切りに補正しておく
        int boundary = 100;
        mChartMaxY = (float)(Math.ceil(mChartMaxY / boundary) * boundary);
        mChartMinY = (float)(Math.floor(mChartMinY / boundary) * boundary);
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
            set1 = new LineDataSet(mChartValues, label);

            set1.setDrawIcons(false);
            set1.setColor(Color.BLACK);
            set1.setCircleColor(Color.BLACK);
            set1.setLineWidth(1f);
            set1.setCircleRadius(3f);
            set1.setDrawCircleHole(false);
            set1.setValueTextSize(0f);
            set1.setDrawFilled(true);
            set1.setFormLineWidth(1f);
            set1.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
            set1.setFormSize(15.f);

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

    private void setTitle(Date date) {
        DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT);
        setTitle(df.format(date));
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch(id) {
            case R.id.btn_prev:
                mTargetDate = TimeUtils.addDays(mTargetDate, -1);
                setTitle(mTargetDate);
                drawChart();
                break;

            case R.id.btn_next:
                mTargetDate = TimeUtils.addDays(mTargetDate, 1);
                setTitle(mTargetDate);
                drawChart();
                break;
        }
    }



    public static void startActivity(Activity context) {
        Intent intent = new Intent(context, GraphActivity.class);
        context.startActivity(intent);
    }

}
