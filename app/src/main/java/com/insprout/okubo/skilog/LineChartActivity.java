package com.insprout.okubo.skilog;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.model.SkiLogDb;
import com.insprout.okubo.skilog.model.TagDb;
import com.insprout.okubo.skilog.model.ResponsePlaceData;
import com.insprout.okubo.skilog.setting.Const;
import com.insprout.okubo.skilog.setting.Settings;
import com.insprout.okubo.skilog.util.AppUtils;
import com.insprout.okubo.skilog.util.DialogUi;
import com.insprout.okubo.skilog.util.UiUtils;
import com.insprout.okubo.skilog.util.SdkUtils;
import com.insprout.okubo.skilog.webapi.RequestUrlTask;
import com.insprout.okubo.skilog.webapi.WebApiUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class LineChartActivity extends AppCompatActivity implements View.OnClickListener, DialogUi.DialogEventListener {
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

    private ViewPager mViewPager;
    private List<Date> mDateList;
    private List<TagDb> mAllTags;
    private TagDb mTargetTag = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.getThemeStyle(this));
        setContentView(R.layout.activity_bar_chart2);

        initVars();                                             // 変数などの初期化
        initView();                                             // View関連の初期化
    }


    @Override
    public void onResume() {
        super.onResume();

        // チャートの描画を開始する
//        mDailyChart.updateChart();
        if (SkiLogService.isRunning(this)) mServiceMessenger.bind();
    }

    @Override
    public void onPause() {
        if (SkiLogService.isRunning(this)) mServiceMessenger.unbind();
        super.onPause();
    }


    private void initVars() {

        mDateList = new ArrayList<>();
        List<SkiLogDb>data = DbUtils.selectLogSummaries(this, 0, 0);
        if (data != null && !data.isEmpty()) {
            // 取得したログの 日付情報のリストを作成する
            for(int i = 0; i<data.size(); i++) {
                SkiLogDb log = data.get(i);
                mDateList.add(log.getCreated());
            }
        }


        // Serviceプロセスとの 通信クラス作成
        mServiceMessenger = new ServiceMessenger(this, new ServiceMessenger.OnServiceMessageListener() {
            @Override
            public void onReceiveMessage(Message msg) {
                switch (msg.what) {
                    case ServiceMessenger.MSG_REPLY_LONG_ARRAY:
                        long[] data = (long[]) msg.obj;
                        if (data[0] <= 0) return;

//                        mDailyChart.appendChartValue(data[0], data[1] * 0.001f, data[2] * 0.001f, data[3] * 0.001f, (int)data[4]);
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

        // チャートの表示設定
        LineChartPagerAdapter adapter = new LineChartPagerAdapter(this, mDateList);
        mViewPager = findViewById(R.id.viewpager);
        mViewPager.setAdapter(adapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                setTitleByDate(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
        setTitleByDate(0);
        if (mDateList != null && mDateList.size() >= 1) mViewPager.setCurrentItem(mDateList.size() - 1);    // 最新日を表示

        updateUi();
    }

    private void setTitleByDate(int position) {
        if (mDateList != null && position >= 0 && position < mDateList.size()) {
            setTitle(getString(R.string.fmt_title_chart, AppUtils.toDateString(mDateList.get(position))));
        }
    }


//    private void displayValues(int chartId, Entry entry) {
//        String msg = null;
//
//        if (entry != null) {
//            switch (chartId) {
//                case R.id.btn_altitude:
//                    msg = getString(R.string.fmt_value_altitude, mDailyChart.getXAxisLabel(entry.getX()), mDailyChart.getYAxisLabel(entry.getY()));
//                    break;
//
//                case R.id.btn_accumulate:
//                    msg = getString(R.string.fmt_value_accumulate, mDailyChart.getXAxisLabel(entry.getX()), mDailyChart.getYAxisLabel(entry.getY()));
//                    break;
//            }
//        }
//        UiUtils.setText(LineChartActivity.this, R.id.tv_chart_value, msg);
//    }


    private void updateUi() {
//        setTitle(mDailyChart.getSubject());

        // 前データ、次データへのボタンの 有効無効
//        UiUtils.setEnabled(this, R.id.btn_negative, mDailyChart.hasPreviousPage());
//        UiUtils.setEnabled(this, R.id.btn_positive, mDailyChart.hasNextPage());
    }

    private Date getSelectedDate() {
        int position = mViewPager.getCurrentItem();
        if (mDateList != null && position >= 0 && position < mDateList.size()) {
            return mDateList.get(position);
        }
        return null;
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch(id) {
//            case R.id.btn_negative:
//                mDailyChart.goPreviousPage();
//                updateUi();
//                break;
//
//            case R.id.btn_positive:
//                mDailyChart.goNextPage();
//                updateUi();
//                break;

            case R.id.btn_chart2:
                // サマリー画面表示
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
        Date targetDate = getSelectedDate();

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
        Date targetDate = getSelectedDate();
        if (targetDate == null) return;

        // データ削除
        String title = getString(R.string.title_delete_logs);
        String message = getString(R.string.fmt_delete_daily_logs,  AppUtils.toDateString(targetDate));
        new DialogUi.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton()
                .setNegativeButton()
                .setRequestCode(RC_DELETE_LOG)
                .show();
    }

    private void deleteLogs(Date targetDate) {
        // 指定日のログをDBから削除する
        boolean res = DbUtils.deleteLogs(this, targetDate);
        if (res) {
            // 紐づいたタグ情報もDBから削除する
            DbUtils.deleteTags(this, targetDate);

            // 指定日付を 選択可能日から削除する。
//            mDailyChart.delete(targetDate);
//            mDailyChart.updateChart();
            updateUi();
        }
    }

    private void showAddTagDialog() {
        String[] submenu = getResources().getStringArray(R.array.menu_add_tag);
        new DialogUi.Builder(this)
                .setItems(submenu)
                .setNegativeButton(R.string.btn_cancel)
                .setRequestCode(RC_ADD_TAG)
                .show();
    }

    private void listTags() {
        if (mAllTags == null || mAllTags.isEmpty()) return;

        // 選択用リストを作成
        String[] arrayTag = AppUtils.toStringArray(mAllTags);
        String title = getString(R.string.fmt_title_list_tags, AppUtils.toDateString(getSelectedDate()));
        new DialogUi.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(arrayTag, -1)
                .setPositiveButton(R.string.btn_delete)
                .setNegativeButton(R.string.btn_close)
                .setRequestCode(RC_LIST_TAG)
                .show();
    }

    private void inputTag() {
        new DialogUi.Builder(this)
                .setTitle(R.string.title_input_tag)
                .setView(R.layout.dlg_edittext)
                .setPositiveButton(R.string.btn_ok)
                .setNegativeButton(R.string.btn_cancel)
                .setRequestCode(RC_ADD_TAG_INPUT)
                .show();
    }

    private void selectTagFromHistory() {
        List<TagDb> tagsCandidate = AppUtils.getTags(this, mAllTags);
        if (tagsCandidate == null || tagsCandidate.isEmpty()) {
            Toast.makeText(this, R.string.msg_no_more_tags, Toast.LENGTH_SHORT).show();

        } else {
            new DialogUi.Builder(this)
                    .setTitle(R.string.title_input_tag)
                    .setSingleChoiceItems(AppUtils.toStringArray(tagsCandidate), -1)
                    .setPositiveButton(R.string.btn_ok)
                    .setNegativeButton(R.string.btn_cancel)
                    .setRequestCode(RC_ADD_TAG_SELECTION)
                    .show();
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

        final DialogFragment dialog = new DialogUi.Builder(this, DialogUi.STYLE_PROGRESS_DIALOG).setMessage(R.string.msg_getting_tags).show();
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

                            new DialogUi.Builder(LineChartActivity.this)
                                    .setTitle(R.string.title_input_tag)
                                    .setSingleChoiceItems(AppUtils.toStringArray(places.getPlaces(), Const.MAX_TAG_CANDIDATE_BY_LOCATION), -1)
                                    .setPositiveButton(R.string.btn_ok)
                                    .setNegativeButton(R.string.btn_cancel)
                                    .setRequestCode(RC_ADD_TAG_SELECTION)
                                    .show();
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
                if (which == DialogUi.EVENT_BUTTON_POSITIVE) {
                    deleteLogs(getSelectedDate());
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
                        case DialogUi.EVENT_DIALOG_SHOWN:
                            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(pos >= 0);
                            break;

                        case DialogUi.EVENT_BUTTON_POSITIVE:
                            // 削除ボタンが押された
                            if (pos >= 0 && pos < mAllTags.size()) {
                                mTargetTag = mAllTags.get(pos);
                                // 確認ダイアログを表示する
                                new DialogUi.Builder(this)
                                        .setTitle(R.string.msg_delete_tags)
                                        .setMessage(mTargetTag.getTag())
                                        .setPositiveButton()
                                        .setNegativeButton()
                                        .setRequestCode(RC_DELETE_TAG)
                                        .show();
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
                    case DialogUi.EVENT_BUTTON_POSITIVE:
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

                    case DialogUi.EVENT_BUTTON_NEGATIVE:
                        // タグ削除キャンセル
                        mTargetTag = null;
                        break;
                }
                break;

            case RC_ADD_TAG_INPUT:
                // タグ直接入力
                EditText editText = ((View)view).findViewById(R.id._et_dlg);
                switch (which) {
                    case DialogUi.EVENT_DIALOG_SHOWN:
                        // キーボードを自動で開く
                        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (inputMethodManager != null) inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                        break;

                    case DialogUi.EVENT_BUTTON_POSITIVE:
                        // 入力されたタグを登録
                        if (editText != null) {
                            Date targetDate = getSelectedDate();
                            String tag = editText.getText().toString();
                            if (!tag.isEmpty() && targetDate != null) {
                                DbUtils.insertTag(this, new TagDb(targetDate, tag));
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
                        case DialogUi.EVENT_DIALOG_SHOWN:
                            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(pos >= 0);
                            break;

                        case DialogUi.EVENT_BUTTON_POSITIVE:
                            // 入力されたタグを登録
                            Date targetDate = getSelectedDate();
                            String tag = ((ListView) view).getAdapter().getItem(pos).toString();
                            if (!tag.isEmpty() && targetDate != null) {
                                DbUtils.insertTag(this, new TagDb(targetDate, tag));
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
