package com.insprout.okubo.skilog.chart;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.DashPathEffect;
import android.graphics.RectF;
import android.net.Uri;
import android.util.TypedValue;

import com.github.mikephil.charting.charts.Chart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.insprout.okubo.skilog.R;
import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.model.SkiLogDb;
import com.insprout.okubo.skilog.util.AppUtils;
import com.insprout.okubo.skilog.util.ContentsUtils;
import com.insprout.okubo.skilog.util.MiscUtils;
import com.insprout.okubo.skilog.util.SdkUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created by okubo on 2018/03/15.
 */

public class DailyChart {
    public final static int TYPE_ALTITUDE = 0;
    public final static int TYPE_ACCUMULATE = 1;

    private Context mContext;
    private LineChart mChart;
    private OnChartValueSelectedListener mValueSelectedListener;
    private int mColorForeground;

    private int mDateIndex = -1;
    private List<Date> mDateList;
    private int mChartType = TYPE_ALTITUDE;

    private int mColor;
    private int mColorPhoto;
    private int mColorAsc;
    private int mColorDesc;

    private ArrayList<Entry> mChartValues11;
    private ArrayList<Entry> mChartValues12;    // 写真用
    private ArrayList<Entry> mChartValues21;
    private ArrayList<Entry> mChartValues22;
    private RectF mChartAxis1;
    private RectF mChartAxis2;
    private String mChartLabel11;
    private String mChartLabel12;
    private String mChartLabel21;
    private String mChartLabel22;

    private float mAccumulateAsc = 0f;
    private float mAccumulateDesc = 0f;
    private int mRunCount = 0;


    public DailyChart(Context context, Chart lineChart) {
        this(context, lineChart, null);
    }

    public DailyChart (Context context, Chart lineChart, OnChartValueSelectedListener listener) {
        mContext = context;
        mChart = (LineChart)lineChart;
        mValueSelectedListener = listener;

        initVars();
    }

    private void initVars() {
        mDateList = new ArrayList<>();
        List<SkiLogDb>data = DbUtils.selectLogSummaries(mContext, 0, 0);
        if (data == null || data.isEmpty()) {
            mDateIndex = -1;

        } else {
            mDateIndex = -1;
            // 取得したログの 日付情報のリストを作成する
            for(int i = 0; i<data.size(); i++) {
                SkiLogDb log = data.get(i);
                mDateList.add(log.getCreated());
            }

            if (mDateIndex < 0) mDateIndex = mDateList.size() - 1;
        }

        // チャートの色/ラベルを設定
        TypedValue typedValue = new TypedValue();
        mContext.getTheme().resolveAttribute(android.R.attr.colorForeground, typedValue, true);
        mColorForeground = SdkUtils.getColor(mContext, typedValue.resourceId);

        mColor = SdkUtils.getColor(mContext, R.color.colorAltitude);
        mColorPhoto = SdkUtils.getColor(mContext, R.color.orange);
        mColorAsc = mColorForeground;
        mColorDesc = SdkUtils.getColor(mContext, R.color.colorAccumulateDesc);
        mChartLabel11 = mContext.getString(R.string.label_altitude);
        mChartLabel12 = mContext.getString(R.string.label_photo);
        mChartLabel21 = mContext.getString(R.string.label_graph_asc);
        mChartLabel22 = mContext.getString(R.string.label_graph_desc);


        // チャートの表示設定
        // Grid背景色
        mChart.setDrawGridBackground(false);

        // 右側の目盛り
        mChart.getAxisRight().setEnabled(false);

        float textSize;
        textSize = SdkUtils.getSpDimension(mContext, R.dimen.text_size_chart_axis);
        mChart.getXAxis().setTextColor(mColorForeground);
        mChart.getXAxis().setTextSize(textSize);                // 縦軸のラベルの文字サイズ
        mChart.getAxisLeft().setTextColor(mColorForeground);
        mChart.getAxisLeft().setTextSize(textSize);             // 縦軸のラベルの文字サイズ

        // Description設定
        textSize = SdkUtils.getSpDimension(mContext, R.dimen.text_size_regular);
        mChart.getDescription().setEnabled(true);
        mChart.getDescription().setTextColor(mColorForeground);
        mChart.getDescription().setTextSize(textSize);
        mChart.getDescription().setXOffset(5);
        mChart.getDescription().setYOffset(5);
        mChart.getDescription().setText("");

        mChart.getLegend().setTextColor(mColorForeground);
        mChart.getLegend().setTextSize(textSize);

        mChart.setOnChartValueSelectedListener(mValueSelectedListener);
    }


    public Date getSelectedDate() {
        if (mDateIndex >= 0 && mDateIndex < mDateList.size()) {
            return mDateList.get(mDateIndex);
        } else {
            return null;
        }
    }

    public void setSelectedDate(Date date) {
        if (date == null || mDateList == null) return;

//        for(int i=0; i<mDateList.size(); i++) {
//            if (MiscUtils.isSameDate(date, mDateList.get(i))) {
//                mDateIndex = i;
//                return;
//            }
//        }
        for(int i=mDateList.size()-1; i>=0; i--) {
            if (!MiscUtils.getDate(mDateList.get(i)).after(date)) {
                mDateIndex = i;
                return;
            }
        }
        mDateIndex = 0;
    }

    public void delete(Date date) {
        if (date == null || mDateList == null) return;

        for (int i=mDateList.size()-1; i>=0; i--) {
            if (MiscUtils.isSameDate(date, mDateList.get(i))) mDateList.remove(i);
        }

        if (mDateIndex >= mDateList.size()) mDateIndex = mDateList.size() - 1;
    }

//    public void setFilter(String filteringTag) {
//    }

    public boolean hasNextPage() {
        return (mDateIndex < mDateList.size() - 1);
    }

    public boolean hasPreviousPage() {
        return (mDateIndex >= 1);
    }

    public void goNextPage() {
        if (mDateIndex < mDateList.size() - 1) mDateIndex++;
        updateChart();
    }

    public void goPreviousPage() {
        if (mDateIndex > 0) mDateIndex--;
        updateChart();
    }


    public String getXAxisLabel(float value) {
        // X軸の valueは 0時(am0:00)からの経過時間を示す。1.5で 1時間30分
        int hour = (int)value;
        int minutes = (int)((value - hour) * 60);
        return mContext.getString(R.string.fmt_time, hour, minutes);
    }

//    public String getXAxisLabelFull(float value) {
//        return getXAxisLabel(value);
//    }

    public String getYAxisLabel(float value) {
        // 縦軸の valueは高度
        return AppUtils.getFormattedMeter(mContext, value);
    }

    public void clearChart() {
        mChart.clear();
        nothingSelected();
    }

    private void nothingSelected() {
        if (mValueSelectedListener != null) {
            mValueSelectedListener.onNothingSelected();
        }
    }

    public void drawChart() {
        drawNewChart(mChartType);
    }


    // タイトル表記を返す。
    public String getSubject() {
        Date date = getSelectedDate();
        return mContext.getString(R.string.fmt_title_chart, AppUtils.toDateString(date != null ? date : new Date(System.currentTimeMillis())));
    }

//////////////////////////////////////////////////////////////////////////////////////////

    private boolean setupChartValues() {
        mChartValues11 = new ArrayList<>();
        mChartValues12 = new ArrayList<>();
        mChartValues21 = new ArrayList<>();
        mChartValues22 = new ArrayList<>();
        mAccumulateAsc = 0f;
        mAccumulateDesc = 0f;
        mRunCount = 0;

        // DBから 指定日のデータを取得する
        Date targetDate = getSelectedDate();
        if (targetDate == null) return false;

        List<SkiLogDb> data = DbUtils.selectLogs(mContext, targetDate);
        if (data == null || data.size() == 0) {
            return false;
        }

        // 取得したデータをチャート用の データクラスに格納する。また データの最大値/最小値も記録しておく
        float minX = 24.0f;//Float.POSITIVE_INFINITY;
        float maxX = 0.0f;//Float.NEGATIVE_INFINITY;
        float minY1 = 0.0f;
        float maxY1 = 0.0f;
        float minY2 = 0.0f;
        float maxY2 = 0.0f;
        SkiLogDb lastData = data.get(data.size()-1);
        mAccumulateAsc = lastData.getAscTotal();
        mAccumulateDesc = -lastData.getDescTotal();
        mRunCount = lastData.getCount();

        // チャート用の データクラスに格納する
        long timeAm0 = MiscUtils.getDate(targetDate).getTime();
        for (SkiLogDb log : data) {
            // X軸は 対象日の午前0時からの経過時間とする
            float hours = (log.getCreated().getTime() - timeAm0) / (60 * 60 * 1000.0f);
            float altitude = log.getAltitude();
            float ascent = log.getAscTotal();
            float descent = -log.getDescTotal();
            mChartValues11.add(new Entry(hours, altitude));
            mChartValues21.add(new Entry(hours, ascent));
            mChartValues22.add(new Entry(hours, descent));

            // ついでにデータの最大値、最小値を記録しておく (チャートの軸表示用)
            maxX = MiscUtils.maxValue(maxX, hours);
            minX = MiscUtils.minValue(minX, hours);
            maxY1 = MiscUtils.maxValue(maxY1, altitude);
            minY1 = MiscUtils.minValue(minY1, altitude);
            maxY2 = MiscUtils.maxValue(maxY2, ascent, descent);
            minY2 = MiscUtils.minValue(minY2, ascent, descent);
        }

        // チャートの表示領域を記録
        // X軸は 1時間ごとの区切りに補正しておく
        maxX = (float)Math.ceil(maxX);
        minX = (float)Math.floor(minX);
        // Y軸は 100ｍごとの区切りに補正しておく
        int boundary = 100;
        mChartAxis1 = new RectF(minX, (float)(Math.ceil(maxY1 / boundary) * boundary), maxX, (float)(Math.floor(minY1 / boundary) * boundary));
        mChartAxis2 = new RectF(minX, (float)(Math.ceil(maxY2 / boundary) * boundary), maxX, (float)(Math.floor(minY2 / boundary) * boundary));

        // 写真データを取得する
        List<Uri> photos = ContentsUtils.getImageList(mContext, data.get(0).getCreated(), data.get(data.size() - 1).getCreated());
        if (photos.size() >= 1) {
            int photoCount = photos.size();
            Date[] photoTimes = new Date[photos.size()];
            for (int i = 0; i<photos.size(); i++) {
                photoTimes[i] = ContentsUtils.getDate(mContext, photos.get(i));
            }

            for (int i = data.size() - 1; i >= 0; i--) {
                SkiLogDb log = data.get(i);
                Date logTime = log.getCreated();
                for (int j = photos.size() - 1; j>=0 ;j--) {
                    if (photoTimes[j] != null && photoTimes[j].after(logTime)) {
                        float hours = (photoTimes[j].getTime() - timeAm0) / (60 * 60 * 1000.0f);
                        mChartValues12.add(0, new Entry(hours, log.getAltitude()));
                        photoTimes[j] = null;
                        photoCount--;
                    }
                }
                if (photoCount <= 0) break;
            }
        }

        return true;
    }

    // チャート表示中に、Serviceプロセスからデータが送られてきた際に そのデータを追加表示する
    public void appendChartValue(long time, float altitude, float ascent, float descent, int runCount) {
        Date target = getSelectedDate();
        if (!MiscUtils.isSameDate(new Date(time), target)) return;

        Date timeAm00 = MiscUtils.getDate(new Date(time));
        float hours = (time - timeAm00.getTime()) / (60 * 60 * 1000.0f);
        float desc = -descent;
        mChartValues11.add(new Entry(hours, altitude));
        mChartValues21.add(new Entry(hours, ascent));
        mChartValues22.add(new Entry(hours, desc));
        mAccumulateAsc = ascent;
        mAccumulateDesc = desc;
        mRunCount = runCount;

        // 必要があればチャートの表示目盛り更新
        float maxX = (float)Math.ceil(hours);
        float maxY = MiscUtils.maxValue(ascent, desc);
        float minY = MiscUtils.minValue(ascent, desc);
        int boundary = 100;
        if (maxX > mChartAxis1.right) mChartAxis1.right = maxX;
        if (altitude > mChartAxis1.top) mChartAxis1.top = (float)(Math.floor(altitude / boundary) * boundary);
        if (altitude < mChartAxis1.bottom) mChartAxis1.bottom = (float)(Math.floor(altitude / boundary) * boundary);
        if (maxX > mChartAxis2.right) mChartAxis2.right = maxX;
        if (maxY > mChartAxis2.top) mChartAxis2.top = (float)(Math.floor(maxY / boundary) * boundary);
        if (minY < mChartAxis2.bottom) mChartAxis2.bottom = (float)(Math.floor(minY / boundary) * boundary);

        LineData data = mChart.getData();
        if (data == null) return;

        // 表示中のチャート種により処理を分ける
        switch (mChartType) {
            case TYPE_ACCUMULATE:
                // 積算チャート
                if (data.getDataSetCount() != 2) return;
                //新しいデータを追加
                data.addEntry(new Entry(hours, ascent), 0);
                data.addEntry(new Entry(hours, desc), 1);
                mChart.getXAxis().setAxisMaximum(mChartAxis2.right);
                mChart.getAxisLeft().setAxisMaximum(mChartAxis2.top);
                mChart.getAxisLeft().setAxisMinimum(mChartAxis2.bottom);
                break;

            default:
                // 高度チャート
                if (data.getDataSetCount() != 1) return;
                //新しいデータを追加
                data.addEntry(new Entry(hours, altitude), 0);
                mChart.getXAxis().setAxisMaximum(mChartAxis1.right);
                mChart.getAxisLeft().setAxisMaximum(mChartAxis1.top);
                mChart.getAxisLeft().setAxisMinimum(mChartAxis1.bottom);
                // Descriptionは 滑走本数
                break;
        }

        setDescription();
        //更新を通知
        data.notifyDataChanged();
        mChart.notifyDataSetChanged();

        mChart.invalidate();
    }

    private void drawNewChart(int chartId) {
        clearChart();

        // 表示データを取得する
        if (!setupChartValues()) {
            return;
        }

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        switch(chartId) {
            case TYPE_ALTITUDE:
                // 高度チャートの 軸表示設定
                setupAxis(mChartAxis1);
                // add the dataSets
                dataSets.add(newLineDataSet(mChartValues11, mChartLabel11, mColor, true));
                dataSets.add(newLineDataSet(mChartValues12, mChartLabel12, mColorPhoto, false, true));
                break;

            case TYPE_ACCUMULATE:
                // 積算チャートの 軸表示設定
                setupAxis(mChartAxis2);
                // add the dataSets
                dataSets.add(newLineDataSet(mChartValues21, mChartLabel21, mColorAsc, false));
                dataSets.add(newLineDataSet(mChartValues22, mChartLabel22, mColorDesc, false));
                break;

            default:
                return;
        }

        setDescription();
        // create a data object with the dataSets
        LineData lineData = new LineData(dataSets);
        // set data
        mChart.setData(lineData);
        //mChart.animateX(2500);
        mChart.invalidate();
    }


    // 同じチャートタイプで、違う期間のチャートを描画する
    // (LineDataSetを再利用して、別のチャートを描画する)
    public void updateChart() {
        // 表示データを取得する
        if (!setupChartValues()) {
            // 表示データなし
            clearChart();
            return;
        }

        LineData lineData = mChart.getData();
        if (lineData == null) {
            // Chart.clear()などが行われるとLineDataは nullになっているので、その場合は新規にチャートを描く
            drawNewChart(mChartType);
            return;
        }

        switch (mChartType) {
            case TYPE_ALTITUDE:
                // チャートの 軸表示設定
                setupAxis(mChartAxis1);
                ((LineDataSet)lineData.getDataSetByIndex(0)).setValues(mChartValues11);
                if (mChartValues12.size() >= 1) ((LineDataSet)lineData.getDataSetByIndex(1)).setValues(mChartValues12);
                break;

            case TYPE_ACCUMULATE:
                // チャートの 軸表示設定
                setupAxis(mChartAxis2);
                ((LineDataSet)lineData.getDataSetByIndex(0)).setValues(mChartValues21);
                ((LineDataSet)lineData.getDataSetByIndex(1)).setValues(mChartValues22);
                break;
        }

        setDescription();
        mChart.getData().notifyDataChanged();
        mChart.notifyDataSetChanged();

        //mChart.animateX(2500);
        mChart.invalidate();

        // 別のチャートを描画したので、selectionは未選択状態にする
        nothingSelected();
    }


    private LineDataSet newLineDataSet(List<Entry>yValues, String label, int color, boolean lineFilled) {
        return newLineDataSet(yValues, label, color, lineFilled, false);
    }
    private LineDataSet newLineDataSet(List<Entry>yValues, String label, int color, boolean lineFilled, boolean marker) {
        LineDataSet dataSet = new LineDataSet(yValues, label);

        dataSet.setDrawIcons(false);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(marker ? 6f : 1f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(0f);
        dataSet.setFormLineWidth(1f);
        dataSet.setFormLineDashEffect(new DashPathEffect(new float[]{10f, 5f}, 0f));
        dataSet.setFormSize(15.f);

        dataSet.setColor(color);
        dataSet.setCircleColor(color);
        if (lineFilled) {
            dataSet.setDrawFilled(true);
            dataSet.setFillColor(color);
        } else {
            dataSet.setDrawFilled(false);
        }

        return dataSet;
    }

    private void setupAxis(RectF axis) {
        // Grid縦軸を破線
        XAxis xAxis = mChart.getXAxis();
        xAxis.setAxisMaximum(axis.right);
        xAxis.setAxisMinimum(axis.left);
        xAxis.enableGridDashedLine(10f, 10f, 0f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                return getXAxisLabel(value);
            }
        });

        YAxis yAxis = mChart.getAxisLeft();
        // Y軸最大最小設定
        yAxis.setAxisMaximum(axis.top);
        yAxis.setAxisMinimum(axis.bottom);
        // Grid横軸を破線
        yAxis.enableGridDashedLine(10f, 10f, 0f);
        yAxis.setDrawZeroLine(true);
        yAxis.setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, AxisBase axis) {
                // Y軸の valueは高度を示す
                return getYAxisLabel(value);
            }
        });
    }


    private void setDescription() {
        mChart.getDescription().setText(mContext.getString(R.string.fmt_ski_log, mRunCount, Math.abs(mAccumulateDesc)));
    }


    /////////////////////////

    public void setChartType(int type) {
        switch(type) {
            case TYPE_ALTITUDE:
            case TYPE_ACCUMULATE:
                mChartType = type;
                // チャート種が変更された場合は、チャートを更新する
                drawNewChart(mChartType);
                break;
        }
    }

    public int getChartType() {
        return mChartType;
    }

}
