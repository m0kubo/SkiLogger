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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.database.TagData;
import com.insprout.okubo.skilog.model.ResponsePlaceData;
import com.insprout.okubo.skilog.setting.Const;
import com.insprout.okubo.skilog.setting.Settings;
import com.insprout.okubo.skilog.util.AppUtils;
import com.insprout.okubo.skilog.util.DialogUtils;
import com.insprout.okubo.skilog.util.SdkUtils;
import com.insprout.okubo.skilog.webapi.RequestUrlTask;
import com.insprout.okubo.skilog.webapi.WebApiUtils;

import java.util.Date;
import java.util.List;


public class ChartPagerActivity extends AppCompatActivity implements DialogUtils.DialogEventListener {
    private final static int RP_LOCATION = 100;

    private final static int RC_DELETE_LOG = 100;
    private final static int RC_SELECT_TAG = 200;
    private final static int RC_LIST_TAG = 201;
    private final static int RC_ADD_TAG = 202;
    private final static int RC_ADD_TAG_INPUT = 300;
    private final static int RC_ADD_TAG_SELECTION = 301;
    private final static int RC_DELETE_TAG = 400;

    private final static String EXTRA_PARAM1 = "intent.extra.PARAM1";

    private ServiceMessenger mServiceMessenger;

    ///////////////////////////
    private ViewPager mViewPager;
    private int mDefaultPage = 0;
    private ChartPagerAdapter mChartPagerAdapter;
    private RadioGroup mRadioGroup;

    private List<TagData> mAllTags;
    private List<TagData> mTagsOnTarget;
    private int mIndexTag = -1;
    private Date mTargetDate = null;
    private TagData mTargetTag = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.getThemeStyle(this));
        setContentView(R.layout.activity_chart_pager);

        initVars();                                             // 変数などの初期化
        initView();                                             // View関連の初期化
    }


    private void initVars() {
        mDefaultPage = getIntent().getIntExtra(EXTRA_PARAM1, 0);

        // Serviceプロセスとの 通信クラス作成
        mServiceMessenger = new ServiceMessenger(this, new ServiceMessenger.OnServiceMessageListener() {
            @Override
            public void onReceiveMessage(Message msg) {
                switch (msg.what) {
                    case ServiceMessenger.MSG_REPLY_LONG_ARRAY:
                        Date targetDate = mChartPagerAdapter.getSelectedDate(mViewPager.getCurrentItem());
                        if (targetDate == null) return;

                        long[] data = (long[]) msg.obj;
                        if (data[0] <= 0) return;

                        mChartPagerAdapter.appendChartValue(
                                data[0],
                                data[1] * 0.001f,
                                data[2] * 0.001f,
                                data[3] * 0.001f,
                                (int)data[4]);
                        break;
                }
            }
        });
    }

    private void initView() {
        // タイトルバーに backボタンを表示する
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        mViewPager = findViewById(R.id.vp_chart);
        mChartPagerAdapter = new ChartPagerAdapter(this, new ChartPagerAdapter.OnChartEventListener() {
            @Override
            public void onChartEvent(int position, int eventType, Object obj) {
                switch (eventType) {
                    case ChartPagerAdapter.TYPE_VIEW_CLICKED:
                        if (obj instanceof View) {
                            if (((View)obj).getId() == R.id.btn_tag) selectFilteringTag();
                        }
                        break;

                    case ChartPagerAdapter.TYPE_TITLE_UPDATED:
                        if (obj instanceof String) setTitle((String)obj);
                        break;
                }
            }
        });

        int count = mChartPagerAdapter.getCount();    // ページ数を取得する
        mRadioGroup = (RadioGroup) findViewById(R.id.group);
        // ページの数だけ indicatorを登録する
        for (int i=0; i<count; i++) {
            RadioButton button = new RadioButton(this);
            button.setButtonDrawable(R.drawable.indicator);
            button.setEnabled(false);
            mRadioGroup.addView(button);
        }

        mViewPager.setAdapter(mChartPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            boolean is1stTime = true;

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                // スクロールが完了して呼ばれる
                // こちらは、ViewPagerが表示された時点でも呼ばれる。
                if (is1stTime) {
                    // 表示直後のイベントのみこちらで拾う
                    updateUi(position);
                }
                is1stTime = false;
            }

            @Override
            public void onPageSelected(int position) {
                // 半分以上スクロールした時点で呼ばれる。onPageScrolled()より反応が早い。
                // ただしこちらは、ViewPagerが表示されただけでは呼ばれない。
                updateUi(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        // 初期ページ選択
        if (mDefaultPage >= 1 && mDefaultPage < mChartPagerAdapter.getCount()) {
            mViewPager.setCurrentItem(mDefaultPage);
        }

        // 絞り込み用のタグリスト取得
        setupFilteringTag();
    }

    private void setIndicator(int position) {
        if (position >= 0 && position < mRadioGroup.getChildCount()) {
            RadioButton button = (RadioButton) mRadioGroup.getChildAt(position);
            button.setChecked(true);

        } else {
            mRadioGroup.clearCheck();
        }
    }

    private void updateUi() {
        updateUi(mViewPager.getCurrentItem());
    }

    private void updateUi(int pageIndex) {
        setTitle(mChartPagerAdapter.getSubject(pageIndex));
        setIndicator(pageIndex);
        mChartPagerAdapter.setViewEnabled(0, R.id.btn_tag, (mAllTags != null && !mAllTags.isEmpty()));
    }


    @Override
    public void onResume() {
        super.onResume();

        // チャートの描画を開始する
//        updateChart();
        if (SkiLogService.isRunning(this)) mServiceMessenger.bind();
    }

    @Override
    public void onPause() {
        if (SkiLogService.isRunning(this)) mServiceMessenger.unbind();
        super.onPause();
    }


    private void setupFilteringTag() {
        mAllTags = AppUtils.getTags(this);
        mChartPagerAdapter.setViewEnabled(0, R.id.btn_tag, (mAllTags != null && !mAllTags.isEmpty()));
    }

    private void selectFilteringTag() {
        // tag一覧
        // tagがない場合 ボタン無効になっている筈だが念のためチェック
        if (mAllTags == null || mAllTags.isEmpty()) {
            return;
        }
        // 選択用リストを作成
        String[] arrayTag = new String[ mAllTags.size() + 1 ];
        for (int i = 0; i< mAllTags.size(); i++) {
            arrayTag[i] = mAllTags.get(i).getTag();
        }
        arrayTag[ arrayTag.length - 1 ] = getString(R.string.menu_reset_tag);
        DialogUtils.showItemSelectDialog(this, R.string.title_select_tag, arrayTag, mIndexTag, RC_SELECT_TAG);
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
        // 削除メニューの状態を設定
        MenuItem menuSubject = menu.findItem(R.id.menu_target_date);
        menuSubject.setVisible(true);
        mTargetDate = mChartPagerAdapter.getSelectedDate(mViewPager.getCurrentItem());
        if (mTargetDate != null) {
            menuSubject.setTitle(getString(R.string.menu_targeted_fmt, AppUtils.toDateString(mTargetDate)));
            mTagsOnTarget = DbUtils.selectTags(this, mTargetDate);

        } else {
            menuSubject.setTitle(R.string.menu_not_targeted);
            mTagsOnTarget = null;
        }
        menu.findItem(R.id.menu_delete_logs).setEnabled(mTargetDate != null);
        menu.findItem(R.id.menu_list_tags).setEnabled(mTagsOnTarget != null && !mTagsOnTarget.isEmpty());
        menu.findItem(R.id.menu_add_tags).setEnabled(mTargetDate != null);
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
        if (mTargetDate == null) return;

        // データ削除
        String title = getString(R.string.title_delete_logs);
        String message = getString(R.string.fmt_delete_daily_logs,  AppUtils.toDateString(mTargetDate));
        DialogUtils.showOkCancelDialog(this, title, message, RC_DELETE_LOG);
    }

    private void deleteLogs() {
        if (mTargetDate == null) return;       // 念のためチェック

        // 指定日のログをDBから削除する
        boolean res = DbUtils.deleteLogs(this, mTargetDate);
        if (res) {
            // 指定された日に紐づいたタグ情報もDBから削除する
            DbUtils.deleteTags(this, mTargetDate);

            // チャートの表示を更新する
            mChartPagerAdapter.delete(mTargetDate);
            mChartPagerAdapter.drawChart(mViewPager.getCurrentItem());
            updateUi();
        }
    }

    private void showAddTagDialog() {
        String[] submenu = getResources().getStringArray(R.array.menu_add_tag);
        DialogUtils.showItemListDialog(this, 0, submenu, R.string.btn_cancel, RC_ADD_TAG);
    }

    private void listTags() {
        // 付与されているタグ一覧メニュー
        List<TagData> tagsOnTarget = DbUtils.selectTags(this, mTargetDate);
        if (tagsOnTarget == null || tagsOnTarget.isEmpty()) return;

        // 選択用リストを作成
        String[] arrayTag = AppUtils.toStringArray(mTagsOnTarget);
        String title = getString(R.string.fmt_title_list_tags, AppUtils.toDateString(mTargetDate));
        DialogUtils.showItemSelectDialog(this, title, arrayTag, -1, getString(R.string.btn_delete), getString(R.string.btn_close), RC_LIST_TAG);
    }

    private void inputTag() {
        DialogUtils.showCustomDialog(this, R.string.title_input_tag, 0, R.layout.dlg_edittext, R.string.btn_ok, R.string.btn_cancel, RC_ADD_TAG_INPUT);
    }

    private void selectTagFromHistory() {
        List<TagData> tagsCandidate = AppUtils.getTags(this, mTagsOnTarget);
        if (tagsCandidate == null || tagsCandidate.isEmpty()) {
            Toast.makeText(this, R.string.msg_no_more_tags, Toast.LENGTH_SHORT).show();

        } else {
            DialogUtils.showItemSelectDialog(this, R.string.title_input_tag, AppUtils.toStringArray(tagsCandidate), -1, R.string.btn_ok, R.string.btn_cancel, RC_ADD_TAG_SELECTION);
        }
    }


    private void measureLocation() {
        if (!LocationProvider.isEnabled(this)) {
            Toast.makeText(ChartPagerActivity.this, R.string.msg_gps_not_available, Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(ChartPagerActivity.this, R.string.msg_fail_to_google_place_api, Toast.LENGTH_SHORT).show();

                } else {
                    WebApiUtils.googlePlaceApi(getString(R.string.google_maps_key), location, new RequestUrlTask.OnResponseListener() {
                        @Override
                        public void onResponse(String responseBody) {
                            dialog.dismiss();
                            if (responseBody == null) return;
                            ResponsePlaceData places = ResponsePlaceData.fromJson(responseBody);
                            if (places == null || places.getPlaces() == null) {
                                // api取得失敗
                                Toast.makeText(ChartPagerActivity.this, R.string.msg_fail_to_google_place_api, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (places.getPlaces().isEmpty()) {
                                // 0件
                                Toast.makeText(ChartPagerActivity.this, R.string.msg_no_place_tags, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            DialogUtils.showItemSelectDialog(ChartPagerActivity.this, R.string.title_input_tag, AppUtils.toStringArray(places.getPlaces(), Const.MAX_TAG_CANDIDATE_BY_LOCATION), -1, R.string.btn_ok, R.string.btn_cancel, RC_ADD_TAG_SELECTION);
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
                    deleteLogs();
                }
                break;

            case RC_SELECT_TAG:
                if (which == DialogUtils.EVENT_BUTTON_POSITIVE) {
                    if (view instanceof ListView) {
                        int pos = ((ListView)view).getCheckedItemPosition();
                        if (mAllTags != null && pos >= 0 && pos < mAllTags.size()) {
                            mIndexTag = pos;
                            mChartPagerAdapter.setFilter(mAllTags.get(mIndexTag).getTag());

                        } else {
                            mIndexTag = -1;
                            mChartPagerAdapter.setFilter(null);
                        }
                        mChartPagerAdapter.drawChart(mViewPager.getCurrentItem());
                        updateUi();
                    }
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
                            if (pos >= 0 && pos < mTagsOnTarget.size()) {
                                mTargetTag = mTagsOnTarget.get(pos);
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

                                // 削除されたタグが絞り込み表示に指定されていた場合は、チャートを再描画する
                                if (mTargetTag.getTag().equals(mChartPagerAdapter.getFiler())) {
                                    mChartPagerAdapter.drawChart(mViewPager.getCurrentItem());
                                    updateUi();
                                }
                                // 絞り込み用のタグリスト再取得して、View(タグボタンの有効/無効)更新
                                setupFilteringTag();
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
                            String tag = editText.getText().toString();
                            if (!tag.isEmpty() && mTargetDate != null) {
                                DbUtils.insertTag(this, new TagData(mTargetDate, tag));

                                // 絞り込み用のタグリスト再取得
                                setupFilteringTag();
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
                            String tag = ((ListView) view).getAdapter().getItem(pos).toString();
                            if (!tag.isEmpty() && mTargetDate != null) {
                                DbUtils.insertTag(this, new TagData(mTargetDate, tag));

                                // 絞り込み用のタグリスト再取得
                                setupFilteringTag();
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

    public static void startActivity(Activity context, int page) {
        Intent intent = new Intent(context, ChartPagerActivity.class);
        intent.putExtra(EXTRA_PARAM1, page);
        context.startActivity(intent);
    }
}
