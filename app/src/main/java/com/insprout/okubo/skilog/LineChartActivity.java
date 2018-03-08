package com.insprout.okubo.skilog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.DashPathEffect;
import android.graphics.RectF;
import android.location.Location;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Toast;

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
import com.insprout.okubo.skilog.database.TagData;
import com.insprout.okubo.skilog.model.ResponsePlaceData;
import com.insprout.okubo.skilog.setting.Const;
import com.insprout.okubo.skilog.setting.Settings;
import com.insprout.okubo.skilog.util.AppUtils;
import com.insprout.okubo.skilog.util.DialogUtils;
import com.insprout.okubo.skilog.util.MiscUtils;
import com.insprout.okubo.skilog.util.UiUtils;
import com.insprout.okubo.skilog.util.SdkUtils;
import com.insprout.okubo.skilog.webapi.RequestUrlTask;
import com.insprout.okubo.skilog.webapi.WebApiUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class LineChartActivity extends AppCompatActivity implements View.OnClickListener, DialogUtils.DialogEventListener {
    private final static int RP_LOCATION = 100;

    private final static int RC_DELETE_LOG = 100;
    private final static int RC_LIST_TAG = 200;
    private final static int RC_ADD_TAG = 201;
    private final static int RC_ADD_TAG_INPUT = 300;
    private final static int RC_ADD_TAG_SELECTION = 301;
    private final static int RC_ADD_TAG_LOCATION = 302;
    private final static int RC_DELETE_TAG = 400;

    private final static String TAG = "chart";
    private final static String EXTRA_PARAM1 = "intent.extra.PARAM1";
    private final static int MAX_DATA_COUNT = 0;

    private ServiceMessenger mServiceMessenger;
    private int mDateIndex = -1;
    private List<Date> mDateList;
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
    private String mChartLabel11;
    private String mChartLabel21;
    private String mChartLabel22;
    private int mColorForeground;

    private float mAccumulateAsc = 0f;
    private float mAccumulateDesc = 0f;
    private int mRunCount = 0;

    private List<TagData> mTags;
    private TagData mTargetTag = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.getThemeStyle(this));
        setContentView(R.layout.activity_line_chart);

        initVars();                                             // 変数などの初期化
        initView();                                             // View関連の初期化
    }


    @Override
    public void onResume() {
        super.onResume();

        // チャートの描画を開始する
        updateChart();
        if (SkiLogService.isRunning(this)) mServiceMessenger.bind();
    }

    @Override
    public void onPause() {
        if (SkiLogService.isRunning(this)) mServiceMessenger.unbind();
        super.onPause();
    }


    private void initVars() {

        mDateList = new ArrayList<>();
        List<SkiLogData>data = DbUtils.selectLogSummaries(this, 0, MAX_DATA_COUNT);
        if (data == null || data.isEmpty()) {
            mDateIndex = -1;

        } else {
            Date target = (Date)getIntent().getSerializableExtra(EXTRA_PARAM1);
            mDateIndex = -1;
            // 取得したログの 日付情報のリストを作成する
            for(int i = 0; i<data.size(); i++) {
                SkiLogData log = data.get(i);
                mDateList.add(log.getCreated());
                if (MiscUtils.isSameDate(log.getCreated(), target)) mDateIndex = i;
            }

            if (mDateIndex < 0) mDateIndex = mDateList.size() - 1;
        }
        updateUi(mDateIndex);

        // チャートの色/ラベルを設定
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.colorForeground, typedValue, true);
        mColorForeground = SdkUtils.getColor(this, typedValue.resourceId);

        mColor = SdkUtils.getColor(this, R.color.colorAltitude);
        mColorAsc = mColorForeground;
        mColorDesc = SdkUtils.getColor(this, R.color.colorAccumulateDesc);
        mChartLabel11 = getString(R.string.label_altitude);
        mChartLabel21 = getString(R.string.label_graph_asc);
        mChartLabel22 = getString(R.string.label_graph_desc);

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
                            appendData((data[0] - timeAm00.getTime()) / (60 * 60 * 1000.0f), data[1] * 0.001f, data[2] * 0.001f, -data[3] * 0.001f, (int)data[4]);
                        }
                        break;
                }
            }
        });
    }

    private void initView() {
        UiUtils.setSelected(this, R.id.btn_chart1, true);

//        // タイトルバーに backボタンを表示する
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        mRgChartType = findViewById(R.id.rg_chart_type);
        mRgChartType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                // チャートの種類が変更されたので、新規にチャートを書き直す
                drawNewChart(id);
            }
        });

        // チャートの表示設定
        mChart = findViewById(R.id.line_chart);
        // Grid背景色
        mChart.setDrawGridBackground(false);

        // 右側の目盛り
        mChart.getAxisRight().setEnabled(false);

        float textSize;
        textSize = SdkUtils.getSpDimension(this, R.dimen.text_size_chart_axis);
        mChart.getXAxis().setTextColor(mColorForeground);
        mChart.getXAxis().setTextSize(textSize);                // 縦軸のラベルの文字サイズ
        mChart.getAxisLeft().setTextColor(mColorForeground);
        mChart.getAxisLeft().setTextSize(textSize);             // 縦軸のラベルの文字サイズ

        // Description設定
        textSize = SdkUtils.getSpDimension(this, R.dimen.text_size_regular);
        mChart.getDescription().setEnabled(true);
        mChart.getDescription().setTextColor(mColorForeground);
        mChart.getDescription().setTextSize(textSize);
        mChart.getDescription().setXOffset(5);
        mChart.getDescription().setYOffset(5);
        mChart.getDescription().setText("");

        mChart.getLegend().setTextColor(mColorForeground);
        mChart.getLegend().setTextSize(textSize);
    }

    private void updateUi(int dateIndex) {
        Date date = getTargetDate(dateIndex);
        setTitle(getString(R.string.fmt_title_chart, AppUtils.toDateString(date != null ? date : new Date(System.currentTimeMillis()))));

        // 前データ、次データへのボタンの 有効無効
        UiUtils.setEnabled(this, R.id.btn_negative, dateIndex >= 1);
        UiUtils.setEnabled(this, R.id.btn_positive, dateIndex < mDateList.size() - 1);
    }

    private Date getTargetDate(int dateIndex) {
        // タイトルに データの日付を表示する
        if (dateIndex >= 0 && dateIndex < mDateList.size()) {
            return mDateList.get(dateIndex);
        } else {
            return null;
        }
    }

    private void getTagList(int dateIndex) {
        // タグリスト取得
        mTags = DbUtils.selectTags(this, getTargetDate(dateIndex));
    }

    private boolean setupChartValues(int dateIndex) {
        mChartValues11 = new ArrayList<>();
        mChartValues21 = new ArrayList<>();
        mChartValues22 = new ArrayList<>();
        mAccumulateAsc = 0f;
        mAccumulateDesc = 0f;
        mRunCount = 0;

        // DBから 指定日のデータを取得する
        Date targetDate = getTargetDate(dateIndex);
        if (targetDate == null) return false;

        List<SkiLogData> data = DbUtils.selectLogs(this, targetDate);
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
        SkiLogData lastData = data.get(data.size()-1);
        mAccumulateAsc = lastData.getAscTotal();
        mAccumulateDesc = -lastData.getDescTotal();
        mRunCount = lastData.getCount();

        // チャート用の データクラスに格納する
        long timeAm0 = MiscUtils.getDate(targetDate).getTime();
        for (SkiLogData log : data) {
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

    // チャート表示中に、Serviceプロセスからデータが送られてきた際に そのデータを追加表示する
    private void appendData(float hours, float altitude, float ascent, float descent, int runCount) {
        mChartValues11.add(new Entry(hours, altitude));
        mChartValues21.add(new Entry(hours, ascent));
        mChartValues22.add(new Entry(hours, descent));
        mAccumulateAsc = ascent;
        mAccumulateDesc = -descent;
        mRunCount = runCount;

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
        mChart.clear();

        // 表示データを取得する
        if (!setupChartValues(mDateIndex)) {
            return;
        }

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        switch(chartId) {
            case R.id.btn_altitude:
                // 高度チャートの 軸表示設定
                setupAxis(mChartAxis1);
                // add the dataSets
                dataSets.add(newLineDataSet(mChartValues11, mChartLabel11, mColor, true));
                break;

            case R.id.btn_accumulate:
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


    private void updateChart() {
        // 表示データを取得する
        if (!setupChartValues(mDateIndex)) {
            // 表示データなし
            mChart.clear();
            return;
        }

        int chartId = mRgChartType.getCheckedRadioButtonId();
        LineData lineData = mChart.getData();
        if (lineData == null) {
            // Chart.clear()などが行われるとLineDataは nullになっているので、その場合は新規にチャートを描く
            drawNewChart(chartId);
            return;
        }

        switch (chartId) {
            case R.id.btn_altitude:
                // チャートの 軸表示設定
                setupAxis(mChartAxis1);
                ((LineDataSet)lineData.getDataSetByIndex(0)).setValues(mChartValues11);
                break;

            case R.id.btn_accumulate:
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
                return getString(R.string.fmt_meter, value);
            }
        });
    }

    private void setDescription() {
        mChart.getDescription().setText(getString(R.string.fmt_ski_log, mRunCount, mAccumulateDesc));
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


    ////////////////////////////////////////////////////////////////////
    //
    // Optionメニュー関連
    //

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.line_chart, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Date targetDate = getTargetDate(mDateIndex);

        MenuItem tagsMenu = menu.findItem(R.id.menu_list_tags);
        MenuItem deleteMenu = menu.findItem(R.id.menu_delete_logs);

        // 日付に依存するメニューの 有効/無効状態を設定
        if (targetDate != null) {
            // 削除メニュー
            deleteMenu.setEnabled(true);
            deleteMenu.setTitle(getString(R.string.fmt_menu_delete_logs, AppUtils.toDateString(targetDate)));

            // 付与されているタグ一覧メニュー
            mTags = DbUtils.selectTags(this, targetDate);
            tagsMenu.setEnabled(mTags != null && !mTags.isEmpty());

        } else {
            // 削除メニュー
            deleteMenu.setEnabled(false);
            deleteMenu.setTitle(R.string.menu_delete_logs);

            // 付与されているタグ一覧メニュー
            mTags = null;
            tagsMenu.setEnabled(false);
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

            case R.id.menu_list_tags:
                listTags();
                return true;

            case R.id.menu_add_tags:
                showAddTagDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void confirmDeleteLogs() {
        if (mDateIndex < 0) return;
        Date targetDate = getTargetDate(mDateIndex);
        if (targetDate == null) return;

        // データ削除
        String title = getString(R.string.title_delete_logs);
        String message = getString(R.string.fmt_delete_daily_logs,  AppUtils.toDateString(targetDate));
        DialogUtils.showOkCancelDialog(this, title, message, RC_DELETE_LOG);
    }

    private void deleteLogs(Date targetDate) {
        // 指定日のログをDBから削除する
        boolean res = DbUtils.deleteLogs(this, targetDate);
        if (res) {
            // 紐づいたタグ情報もDBから削除する
            DbUtils.deleteTags(this, targetDate);

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

    private void showAddTagDialog() {
        String[] submenu = getResources().getStringArray(R.array.menu_add_tag);
        DialogUtils.showItemListDialog(this, 0, submenu, R.string.btn_cancel, RC_ADD_TAG);
    }

    private void listTags() {
        if (mTags == null || mTags.isEmpty()) return;

        // 選択用リストを作成
        String[] arrayTag = MiscUtils.toStringArray(mTags);
        String title = getString(R.string.fmt_title_list_tags, AppUtils.toDateString(getTargetDate(mDateIndex)));
        DialogFragment dialog = DialogUtils.showItemSelectDialog(this, title, arrayTag, -1, getString(R.string.btn_delete), getString(R.string.btn_close), RC_LIST_TAG);
    }

    private void inputTag() {
        DialogUtils.showCustomDialog(this, R.string.title_input_tag, 0, R.layout.dlg_edittext, R.string.btn_ok, R.string.btn_cancel, RC_ADD_TAG_INPUT);
    }

    private void selectTagFromHistory() {
        List<TagData> tagsCandidate = AppUtils.getTags(this, mTags);
        if (tagsCandidate == null || tagsCandidate.isEmpty()) {
            Toast.makeText(this, R.string.msg_no_more_tags, Toast.LENGTH_SHORT).show();

        } else {
            DialogUtils.showItemSelectDialog(this, R.string.title_input_tag, MiscUtils.toStringArray(tagsCandidate), -1, R.string.btn_ok, R.string.btn_cancel, RC_ADD_TAG_SELECTION);
        }
    }


    private void measureLocation() {
        if (!LocationProvider.isEnabled(this)) {
            Toast.makeText(LineChartActivity.this, R.string.msg_gps_not_available, Toast.LENGTH_SHORT).show();
            // タグ追加のダイアログを再表示しておく
            showAddTagDialog();
            return;
        }

        if (!SdkUtils.requestRuntimePermissions(this, Const.PERMISSIONS_LOCATION, RP_LOCATION)) {
            // 権限がない場合、続きは onRequestPermissionsResult()から継続
            return;
        }

        final DialogFragment dialog = DialogUtils.showProgressDialog(this, 0, R.string.msg_getting_tags);
        new LocationProvider(this, new LocationProvider.OnLocatedListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location == null) {
                    dialog.dismiss();
                    Toast.makeText(LineChartActivity.this, R.string.msg_fail_to_google_place_api, Toast.LENGTH_SHORT).show();

                } else {
                    WebApiUtils.googlePlaceApi(getString(R.string.google_maps_key), location, new RequestUrlTask.OnResponseListener() {
                        @Override
                        public void onResponse(String responseBody) {
                            dialog.dismiss();
                            if (responseBody == null) return;
                            ResponsePlaceData places = ResponsePlaceData.fromJson(responseBody);
                            if (places == null || places.getPlaces() == null) {
                                // api取得失敗
                                Toast.makeText(LineChartActivity.this, R.string.msg_fail_to_google_place_api, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (places.getPlaces().isEmpty()) {
                                // 0件
                                Toast.makeText(LineChartActivity.this, R.string.msg_no_place_tags, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            DialogUtils.showItemSelectDialog(LineChartActivity.this, R.string.title_input_tag, MiscUtils.toStringArray(places.getPlaces(), 10), -1, R.string.btn_ok, R.string.btn_cancel, RC_ADD_TAG_SELECTION);
                        }
                    }).execute();

                }
            }
        }
        ).requestMeasure();
    }


    @Override
    public void onDialogEvent(int requestCode, AlertDialog dialog, int which, View view) {
        switch (requestCode) {
            case RC_DELETE_LOG:
                if (which == DialogUtils.EVENT_BUTTON_POSITIVE) {
                    deleteLogs(getTargetDate(mDateIndex));
                }
                break;

            case RC_ADD_TAG:
                switch(which) {
                    case 0:
                        inputTag();
                        break;
                    case 1:
                        selectTagFromHistory();
                        break;
                    case 2:
                        measureLocation();
                        break;
                }
                break;

            case RC_LIST_TAG:
                // タグ一覧ダイアログのコールバック
                if (view instanceof ListView) {      // 念のためチェック
                    int pos = ((ListView)view).getCheckedItemPosition();

                    switch (which) {
                        case DialogUtils.EVENT_DIALOG_SHOWN:
                            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(pos >= 0);
                            break;

                        case DialogUtils.EVENT_BUTTON_POSITIVE:
                            // 削除ボタンが押された
                            if (pos >= 0 && pos < mTags.size()) {
                                mTargetTag = mTags.get(pos);
                                // 確認ダイアログを表示する
                                DialogUtils.showOkCancelDialog(this, getString(R.string.msg_delete_tags), mTargetTag.getTag(), RC_DELETE_TAG);
                            }
                            break;

                        default:
                            if (pos >= 0) {
                                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                            }
                            break;
                    }
                }
                break;

            case RC_DELETE_TAG:
                // タグ削除
                switch(which) {
                    case DialogUtils.EVENT_BUTTON_POSITIVE:
                        // 対象のタグデータをDBから削除する
                        if (mTargetTag != null) {
                            boolean result = DbUtils.deleteTag(this, mTargetTag);
                            if (result) {
                                String msg = getString(R.string.fmt_msg_deleted_tags,  AppUtils.toDateString(mTargetTag.getDate()), mTargetTag.getTag());
                                Toast.makeText(this, msg ,Toast.LENGTH_SHORT).show();
                            }
                        }
                        mTargetTag = null;
                        break;

                    case DialogUtils.EVENT_BUTTON_NEGATIVE:
                        // タグ削除キャンセル
                        mTargetTag = null;
                        break;
                }
                break;

            case RC_ADD_TAG_INPUT:
                // タグ直接入力
                EditText editText = ((View)view).findViewById(R.id._et_dlg);
                switch (which) {
                    case DialogUtils.EVENT_DIALOG_SHOWN:
                        // キーボードを自動で開く
                        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (inputMethodManager != null) inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                        break;

                    case DialogUtils.EVENT_BUTTON_POSITIVE:
                        // 入力されたタグを登録
                        if (editText != null) {
                            Date targetDate = getTargetDate(mDateIndex);
                            String tag = editText.getText().toString();
                            if (!tag.isEmpty() && targetDate != null) {
                                DbUtils.insertTag(this, new TagData(targetDate, tag));
                            }
                        }
                        break;
                }
                break;

            case RC_ADD_TAG_SELECTION:
                // 履歴から選択
                if (view instanceof ListView) {      // 念のためチェック
                    int pos = ((ListView) view).getCheckedItemPosition();

                    switch (which) {
                        case DialogUtils.EVENT_DIALOG_SHOWN:
                            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(pos >= 0);
                            break;

                        case DialogUtils.EVENT_BUTTON_POSITIVE:
                            // 入力されたタグを登録
                            Date targetDate = getTargetDate(mDateIndex);
                            String tag = ((ListView) view).getAdapter().getItem(pos).toString();
                            if (!tag.isEmpty() && targetDate != null) {
                                DbUtils.insertTag(this, new TagData(targetDate, tag));
                            }
                            break;

                        default:
                            if (pos >= 0) {
                                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                            }
                            break;
                    }
                }
                break;

            case RC_ADD_TAG_LOCATION:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RP_LOCATION:
                // PERMISSIONが すべて付与されたか確認する
                if (!SdkUtils.isGranted(grantResults)) {
                    // 必要な PERMISSIONは付与されなかった
                    // タグ追加方法選択に戻す
                    showAddTagDialog();
                    return;
                }

                measureLocation();
                return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    ////////////////////////////////////////////////////////////////////
    //
    // Activity起動 staticメソッド
    //

    public static void startActivity(Activity context) {
        startActivity(context, null);
    }

    public static void startActivity(Activity context, Date targetDate) {
        Intent intent = new Intent(context, LineChartActivity.class);
        if (targetDate != null) {
            intent.putExtra(EXTRA_PARAM1, targetDate);
        }
        context.startActivity(intent);
    }
}
