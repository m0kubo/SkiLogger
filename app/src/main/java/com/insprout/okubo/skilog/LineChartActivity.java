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
import android.support.v7.app.ActionBar;
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
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.insprout.okubo.skilog.chart.DailyChart;
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


public class LineChartActivity extends AppCompatActivity implements View.OnClickListener, DialogUtils.DialogEventListener {
    private final static int RP_LOCATION = 100;

    private final static int RC_DELETE_LOG = 100;
    private final static int RC_LIST_TAG = 200;
    private final static int RC_ADD_TAG = 201;
    private final static int RC_ADD_TAG_INPUT = 300;
    private final static int RC_ADD_TAG_SELECTION = 301;
    private final static int RC_ADD_TAG_LOCATION = 302;
    private final static int RC_DELETE_TAG = 400;

    private final static String EXTRA_PARAM1 = "intent.extra.PARAM1";

    private ServiceMessenger mServiceMessenger;
    private RadioGroup mRgChartType;

    private DailyChart mDailyChart;
    private List<TagData> mAllTags;
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
        mDailyChart.updateChart();
        if (SkiLogService.isRunning(this)) mServiceMessenger.bind();
    }

    @Override
    public void onPause() {
        if (SkiLogService.isRunning(this)) mServiceMessenger.unbind();
        super.onPause();
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

                        mDailyChart.appendChartValue(data[0], data[1] * 0.001f, data[2] * 0.001f, data[3] * 0.001f, (int)data[4]);
                        break;
                }
            }
        });
    }

    private void initView() {
        UiUtils.setSelected(this, R.id.btn_chart1, true);

        // タイトルバーに backボタンを表示する
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        mRgChartType = findViewById(R.id.rg_chart_type);
        mRgChartType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                // ポイント地点の情報をクリア
                displayValues(id, null);
                // チャートの種類が変更されたので、新規にチャートを書き直す
                switch(id) {
                    case R.id.btn_altitude:
                        mDailyChart.setChartType(0);
                        break;
                    case R.id.btn_accumulate:
                        mDailyChart.setChartType(1);
                        break;
                }
            }
        });

        // チャートの表示設定
        LineChart lineChart = findViewById(R.id.line_chart);
        mDailyChart = new DailyChart(this, lineChart, new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                // ポイント地点の情報を表示
                displayValues(mRgChartType.getCheckedRadioButtonId(), e);
            }

            @Override
            public void onNothingSelected() {
            }
        });

        mDailyChart.drawChart();
        updateUi();
    }


    private void displayValues(int chartId, Entry entry) {
        String msg = null;

        if (entry != null) {
            switch (chartId) {
                case R.id.btn_altitude:
                    msg = getString(R.string.fmt_value_altitude, mDailyChart.getXAxisLabel(entry.getX()), mDailyChart.getYAxisLabel(entry.getY()));
                    break;

                case R.id.btn_accumulate:
                    msg = getString(R.string.fmt_value_accumulate, mDailyChart.getXAxisLabel(entry.getX()), mDailyChart.getYAxisLabel(entry.getY()));
                    break;
            }
        }
        UiUtils.setText(LineChartActivity.this, R.id.tv_chart_value, msg);
    }


    private void updateUi() {
        setTitle(mDailyChart.getSubject());

        // 前データ、次データへのボタンの 有効無効
        UiUtils.setEnabled(this, R.id.btn_negative, mDailyChart.hasPreviousPage());
        UiUtils.setEnabled(this, R.id.btn_positive, mDailyChart.hasNextPage());
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch(id) {
            case R.id.btn_negative:
                mDailyChart.goPreviousPage();
                updateUi();
                break;

            case R.id.btn_positive:
                mDailyChart.goNextPage();
                updateUi();
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
        Date targetDate = mDailyChart.getSelectedDate();

        MenuItem tagsMenu = menu.findItem(R.id.menu_list_tags);
        MenuItem deleteMenu = menu.findItem(R.id.menu_delete_logs);

        // 日付に依存するメニューの 有効/無効状態を設定
        if (targetDate != null) {
            // 削除メニュー
            deleteMenu.setEnabled(true);
            deleteMenu.setTitle(getString(R.string.fmt_menu_delete_logs, AppUtils.toDateString(targetDate)));

            // 付与されているタグ一覧メニュー
            mAllTags = DbUtils.selectTags(this, targetDate);
            tagsMenu.setEnabled(mAllTags != null && !mAllTags.isEmpty());

        } else {
            // 削除メニュー
            deleteMenu.setEnabled(false);
            deleteMenu.setTitle(R.string.menu_delete_logs);

            // 付与されているタグ一覧メニュー
            mAllTags = null;
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
        Date targetDate = mDailyChart.getSelectedDate();
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
//            mDateList.remove(mDateIndex);
//            if (mDateIndex >= mDateList.size()) {
//                mDateIndex--;
//            }
            // チャートの表示を更新する
            mDailyChart.setSelectedDate(targetDate);
            mDailyChart.updateChart();
            updateUi();
        }
    }

    private void showAddTagDialog() {
        String[] submenu = getResources().getStringArray(R.array.menu_add_tag);
        DialogUtils.showItemListDialog(this, 0, submenu, R.string.btn_cancel, RC_ADD_TAG);
    }

    private void listTags() {
        if (mAllTags == null || mAllTags.isEmpty()) return;

        // 選択用リストを作成
        String[] arrayTag = AppUtils.toStringArray(mAllTags);
        String title = getString(R.string.fmt_title_list_tags, AppUtils.toDateString(mDailyChart.getSelectedDate()));
        DialogFragment dialog = DialogUtils.showItemSelectDialog(this, title, arrayTag, -1, getString(R.string.btn_delete), getString(R.string.btn_close), RC_LIST_TAG);
    }

    private void inputTag() {
        DialogUtils.showCustomDialog(this, R.string.title_input_tag, 0, R.layout.dlg_edittext, R.string.btn_ok, R.string.btn_cancel, RC_ADD_TAG_INPUT);
    }

    private void selectTagFromHistory() {
        List<TagData> tagsCandidate = AppUtils.getTags(this, mAllTags);
        if (tagsCandidate == null || tagsCandidate.isEmpty()) {
            Toast.makeText(this, R.string.msg_no_more_tags, Toast.LENGTH_SHORT).show();

        } else {
            DialogUtils.showItemSelectDialog(this, R.string.title_input_tag, AppUtils.toStringArray(tagsCandidate), -1, R.string.btn_ok, R.string.btn_cancel, RC_ADD_TAG_SELECTION);
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

                            DialogUtils.showItemSelectDialog(LineChartActivity.this, R.string.title_input_tag, AppUtils.toStringArray(places.getPlaces(), Const.MAX_TAG_CANDIDATE_BY_LOCATION), -1, R.string.btn_ok, R.string.btn_cancel, RC_ADD_TAG_SELECTION);
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
                    deleteLogs(mDailyChart.getSelectedDate());
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
                            if (pos >= 0 && pos < mAllTags.size()) {
                                mTargetTag = mAllTags.get(pos);
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
                            Date targetDate = mDailyChart.getSelectedDate();
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
                            Date targetDate = mDailyChart.getSelectedDate();
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
