package com.insprout.okubo.skilog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.DashPathEffect;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RadioGroup;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;

import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.database.SkiLogData;
import com.insprout.okubo.skilog.util.MiscUtils;
import com.insprout.okubo.skilog.util.UiUtils;
import com.insprout.okubo.skilog.util.SdkUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class LineChartActivity extends AppCompatActivity implements View.OnClickListener {
    private final static String TAG = "chart";
    private final static int MAX_DATA_COUNT = 0;

    private ServiceMessenger mServiceMessenger;
    private int mDateIndex = -1;
    private List<Date> mDateList;
    private DateFormat mDateFormat;
    private RadioGroup mRgChartType;

    private int mColor;
    private int mColorAsc;
    private int mColorDesc;

    private LineChart mChart;
    private ArrayList<Entry> mChartValues11;
    private ArrayList<Entry> mChartValues21;
    private ArrayList<Entry> mChartValues22;
    private RectF mChartAxis1;
    private RectF mChartAxis2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_line_chart);

        initVars();                                             // 変数などの初期化
        initView();                                             // View関連の初期化
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
        Date targetDate = getTargetDate(mDateIndex);
        MenuItem deleteMenu = menu.findItem(R.id.menu_delete_logs);
        if (targetDate != null) {
            deleteMenu.setEnabled(true);
            deleteMenu.setTitle(getString(R.string.fmt_menu_delete_logs, mDateFormat.format(targetDate)));

        } else {
            deleteMenu.setEnabled(false);
            deleteMenu.setTitle(R.string.menu_delete_logs);
        }
        return true;
    }


    @Override
    public void onResume() {
        super.onResume();

        // サービスの実行状況に合わせて、Messengerや ボタンなどを設定する
        if (SkiLogService.isRunning(this)) mServiceMessenger.bind();
    }

    @Override
    public void onPause() {
        if (SkiLogService.isRunning(this)) mServiceMessenger.unbind();
        super.onPause();
    }


    private void initVars() {
        mDateFormat = DateFormat.getDateInstance(DateFormat.SHORT);

        mDateList = new ArrayList<>();
        List<SkiLogData>data = DbUtils.selectLogSummaries(this, 0, MAX_DATA_COUNT);
        if (data == null || data.isEmpty()) {
            mDateIndex = -1;

        } else {
            // 取得したログの 日付情報のリストを作成する
            for(SkiLogData log : data) {
                mDateList.add(log.getCreated());
            }
            mDateIndex = mDateList.size() - 1;
        }
        updateUi(mDateIndex);

        // チャートの色を設定
        mColor = SdkUtils.getColor(this, R.color.colorAltitude);
        mColorAsc = SdkUtils.getColor(this, R.color.colorAccumulateAsc);
        mColorDesc = SdkUtils.getColor(this, R.color.colorAccumulateDesc);

        // Serviceプロセスとの 通信クラス作成
        mServiceMessenger = new ServiceMessenger(this, new ServiceMessenger.OnServiceMessageListener() {
            @Override
            public void onReceiveMessage(Message msg) {
                switch (msg.what) {
                    case ServiceMessenger.MSG_REPLY_LONG_ARRAY:
                        Date targetDate = getTargetDate(mDateIndex);
                        if (targetDate == null) return;

                        long[] data = (long[]) msg.obj;
                        if (data[0] <= 0) return;

                        Date timeAm00 = MiscUtils.getDate(new Date(data[0]));
                        Log.d(TAG, "received data: date=" + timeAm00);
                        if (MiscUtils.isSameDate(timeAm00, targetDate)) {
                            appendData((data[0] - timeAm00.getTime()) / (60 * 60 * 1000.0f), data[1] * 0.001f, data[2] * 0.001f, -data[3] * 0.001f);
                        }
                        break;
                }
            }
        });
    }

    private void initView() {
        UiUtils.setSelected(this, R.id.btn_chart1, true);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

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

    private void updateUi(int dateIndex) {
        Date date = getTargetDate(dateIndex);
        setTitle(getString(R.string.fmt_title_chart, mDateFormat.format(date != null ? date : new Date(System.currentTimeMillis()))));

        // 前データ、次データへのボタンの 有効無効
        UiUtils.enableView(this, R.id.btn_negative, dateIndex >= 1);
        UiUtils.enableView(this, R.id.btn_positive, dateIndex < mDateList.size() - 1);
    }

    private Date getTargetDate(int dateIndex) {
        // タイトルに データの日付を表示する
        if (dateIndex >= 0 && dateIndex < mDateList.size()) {
            return mDateList.get(dateIndex);
        } else {
            return null;
        }
    }

    private boolean setupChartValues(int dateIndex) {
        mChartValues11 = new ArrayList<>();
        mChartValues21 = new ArrayList<>();
        mChartValues22 = new ArrayList<>();

        // DBから 指定日のデータを取得する
        Date targetDate = getTargetDate(dateIndex);
        if (targetDate == null) return false;

        List<SkiLogData> data = DbUtils.select(this, targetDate);
        if (targetDate == null || data == null || data.size() == 0) {
            return false;
        }

        // 取得したデータをチャート用の データクラスに格納する。また データの最大値/最小値も記録しておく
        float minX = 24.0f;//Float.POSITIVE_INFINITY;
        float maxX = 0.0f;//Float.NEGATIVE_INFINITY;
        float minY1 = 0.0f;
        float maxY1 = 0.0f;
        float minY2 = 0.0f;
        float maxY2 = 0.0f;

        // チャート用の データクラスに格納する
        long timeAm0 = MiscUtils.getDate(targetDate).getTime();
        for (SkiLogData log : data) {
//            String msg = "" + log.getCreated().toString() + " 高度:" + log.getAltitude() + " 上昇:" + log.getAscTotal() + " 下降:" + log.getDescTotal() + " RUN:" + log.getCount();
//            Log.d("database", msg);

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

        return true;
    }

    private void appendData(float hours, float altitude, float ascent, float descent) {
        Log.d(TAG, "appendData: hour=" + hours);
        Log.d(TAG, "appendData: altitude=" + altitude);
        Log.d(TAG, "appendData: ascent=" + ascent);
        Log.d(TAG, "appendData: descent=" + descent);
        mChartValues11.add(new Entry(hours, altitude));
        mChartValues21.add(new Entry(hours, ascent));
        mChartValues22.add(new Entry(hours, descent));

        // 必要があればチャートの表示目盛り更新
        float maxX = (float)Math.ceil(hours);
        float maxY = MiscUtils.maxValue(ascent, descent);
        float minY = MiscUtils.minValue(ascent, descent);
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
        int chartId = mRgChartType.getCheckedRadioButtonId();
        switch (chartId) {
            case R.id.btn_accumulate:
                Log.d(TAG, "appendData: btn_accumulate");
                // 積算チャート
                if (data.getDataSetCount() != 2) return;
                //新しいデータを追加
                data.addEntry(new Entry(hours, ascent), 0);
                data.addEntry(new Entry(hours, descent), 1);
                data.notifyDataChanged();
                mChart.getXAxis().setAxisMaximum(mChartAxis2.right);
                mChart.getAxisLeft().setAxisMaximum(mChartAxis2.top);
                mChart.getAxisLeft().setAxisMinimum(mChartAxis2.bottom);
                break;

            default:
                // 高度チャート
                Log.d(TAG, "appendData: btn_altitude");
                if (data.getDataSetCount() != 1) return;
                //新しいデータを追加
                data.addEntry(new Entry(hours, altitude), 0);
                data.notifyDataChanged();
                mChart.getXAxis().setAxisMaximum(mChartAxis1.right);
                mChart.getAxisLeft().setAxisMaximum(mChartAxis1.top);
                mChart.getAxisLeft().setAxisMinimum(mChartAxis1.bottom);
                break;
        }

        //更新を通知
        //mChart.setVisibleXRangeMaximum(maxX);
        mChart.notifyDataSetChanged();
        mChart.moveViewToX(hours);
    }


    private void drawChartAltitude() {
        mChart.clear();

        // 表示データを取得する
        if (!setupChartValues(mDateIndex)) {
            return;
        }

        // チャートの 軸表示設定
        setupAxis(mChartAxis1);

        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        // add the datasets
        String label = getString(R.string.label_altitude);
        dataSets.add(newLineDataSet(mChartValues11, label, mColor, true));
        // create a data object with the datasets
        LineData lineData = new LineData(dataSets);

        // set data
        mChart.setData(lineData);

        //mChart.animateX(2500);
        mChart.invalidate();
    }

    private void drawChartAccumulate() {
        mChart.clear();

        // 表示データを取得する
        if (!setupChartValues(mDateIndex)) {
            return;
        }

        // チャートの 軸表示設定
        setupAxis(mChartAxis2);

        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();
        // add the datasets
        String label = getString(R.string.label_accumulate);
        dataSets.add(newLineDataSet(mChartValues21, label, mColorAsc, false));
        dataSets.add(newLineDataSet(mChartValues22, label, mColorDesc, false));
        // create a data object with the datasets
        LineData lineData = new LineData(dataSets);

        // set data
        mChart.setData(lineData);

        //mChart.animateX(2500);
        mChart.invalidate();
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
        if (!setupChartValues(mDateIndex)) {
            mChart.clear();
            return;
        }

        LineData lineData = mChart.getData();
        if (lineData == null) {
            // Chart.clear()などが行われるとLineDataは nullになっているので、その場合は新規にチャートを描く
            drawChartAltitude();
            return;
        }

        // チャートの 軸表示設定
        setupAxis(mChartAxis1);

        ((LineDataSet)lineData.getDataSetByIndex(0)).setValues(mChartValues11);
        mChart.getData().notifyDataChanged();
        mChart.notifyDataSetChanged();

        //mChart.animateX(2500);
        mChart.invalidate();
    }

    private void updateChartAccumulate() {
        // 表示データを取得する
        if (!setupChartValues(mDateIndex)) {
            mChart.clear();
            return;
        }

        LineData lineData = mChart.getData();
        if (lineData == null) {
            // Chart.clear()などが行われるとLineDataは nullになっているので、その場合は新規にチャートを描く
            drawChartAccumulate();
            return;
        }

        // チャートの 軸表示設定
        setupAxis(mChartAxis2);

        ((LineDataSet)lineData.getDataSetByIndex(0)).setValues(mChartValues21);
        ((LineDataSet)lineData.getDataSetByIndex(1)).setValues(mChartValues22);
        mChart.getData().notifyDataChanged();
        mChart.notifyDataSetChanged();

        //mChart.animateX(2500);
        mChart.invalidate();
    }

    private LineDataSet newLineDataSet(List<Entry>yValues, String label, int color, boolean lineFilled) {
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
                // X軸の valueは 0時(am0:00)からの経過時間を示す。1.5で 1時間30分
                int hour = (int)value;
                int minutes = (int)((value - hour) * 60);
                return getString(R.string.fmt_time, hour, minutes);
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
                return getString(R.string.fmt_meter, (int)value);
            }
        });
    }

    private void deleteLogs(Date targetDate) {
        boolean res = DbUtils.deleteLogs(this, targetDate);
        if (res) {
            // 対象日付を削除する。
            mDateList.remove(mDateIndex);
            if (mDateIndex >= mDateList.size()) {
                mDateIndex--;
            }
            // チャートの表示を更新する
            updateUi(mDateIndex);
            updateChart();
        }
    }

    private void confirmDeleteLogs() {
        if (mDateIndex < 0) return;

        final Date targetDate = getTargetDate(mDateIndex);
        if (targetDate == null) return;
        // データ削除
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_delete_logs)
                .setMessage(getString(R.string.fmt_msg_delete_logs,  mDateFormat.format(targetDate)))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // アプリ終了
                        deleteLogs(targetDate);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch(id) {
            case R.id.btn_negative:
                if (mDateIndex > 0) mDateIndex--;
                updateUi(mDateIndex);
                updateChart();
                break;

            case R.id.btn_positive:
                if (mDateIndex < mDateList.size() - 1) mDateIndex++;
                updateUi(mDateIndex);
                updateChart();
                break;

            case R.id.btn_chart2:
                UiUtils.setSelected(this, R.id.btn_chart1, false);
                UiUtils.setSelected(this, R.id.btn_chart2, true);
                BarChartActivity.startActivity(this);
                finish();
                break;
        }
    }


    public static void startActivity(Activity context) {
        Intent intent = new Intent(context, LineChartActivity.class);
        context.startActivity(intent);
    }

}
