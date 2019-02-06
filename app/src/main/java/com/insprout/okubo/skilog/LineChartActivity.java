package com.insprout.okubo.skilog;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.View;

import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.model.SkiLogDb;
import com.insprout.okubo.skilog.setting.Settings;
import com.insprout.okubo.skilog.util.AppUtils;
import com.insprout.okubo.skilog.util.DialogUi;
import com.insprout.okubo.skilog.util.MiscUtils;
import com.insprout.okubo.skilog.util.UiUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class LineChartActivity extends BaseActivity implements View.OnClickListener, DialogUi.DialogEventListener {
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
    private LineChartPagerAdapter mChartPagerAdapter;
    private int mTargetIndex = 0;
    private List<Date> mDateList = new ArrayList<>();
    private Map<Date, Uri> mUriMap = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.getThemeStyle(this));
        setContentView(R.layout.activity_line_chart2);

        initVars();                                             // 変数などの初期化
        initView();                                             // View関連の初期化
    }


    @Override
    public void onResume() {
        super.onResume();

        // チャートの描画を開始する
        mChartPagerAdapter.updateChart();
        if (SkiLogService.isRunning(this)) mServiceMessenger.bind();
    }

    @Override
    public void onPause() {
        if (SkiLogService.isRunning(this)) mServiceMessenger.unbind();
        super.onPause();
    }


    private void initVars() {
        mDateList.clear();
        List<SkiLogDb>data = DbUtils.selectLogSummaries(this, 0, 0);
        if (data != null && !data.isEmpty()) {
            mTargetIndex = data.size() - 1;     // 初期表示するページ
            // 初期表示する日付が指定されていた場合は、そのページを初期ページにする
            Date target = (Date) getIntent().getSerializableExtra(EXTRA_PARAM1);

            // 取得したログの 日付情報のリストを作成する
            for(int i = 0; i<data.size(); i++) {
                SkiLogDb log = data.get(i);
                mDateList.add(log.getCreated());
                if (MiscUtils.isSameDate(target, log.getCreated())) mTargetIndex = i;
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

                        mChartPagerAdapter.appendChartValue(data[0], data[1] * 0.001f, data[2] * 0.001f, data[3] * 0.001f, (int)data[4]);
                        break;
                }
            }
        });
    }

    private void initView() {
        UiUtils.setSelected(this, R.id.btn_chart1, true);

        // タイトルバーに backボタンを表示する
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.drawable.ic_line_chart);
        }

        // チャートの表示設定
        mChartPagerAdapter = new LineChartPagerAdapter(this, mDateList, new LineChartPagerAdapter.PhotoUriListener() {
            @Override
            public void photoPicked(Date date, Uri photo) {
                mUriMap.put(date, photo);
                enablePhotoButton(mViewPager.getCurrentItem());
            }
        });
        mViewPager = findViewById(R.id.viewpager);
        mViewPager.setAdapter(mChartPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                updateView(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });

        updateView(0);
        setPage(mTargetIndex, false);    // 最新日を表示
    }

    private void updateView(int position) {
        int pageSize = mDateList.size();
        if (position >= 0 && position < pageSize) {
            setTitle(getString(R.string.fmt_title_chart, AppUtils.toDateString(mDateList.get(position))));
        }
        UiUtils.setEnabled(this, R.id.btn_prev, position >= 1);
        UiUtils.setEnabled(this, R.id.btn_next, position + 1 < pageSize);
        enablePhotoButton(position);
    }

    private void enablePhotoButton(int position) {
        UiUtils.setEnabled(this, R.id.btn_detail, getPhotoUri(position) != null);
    }

    private Uri getPhotoUri(int position) {
        return (position >= 0 && position < mDateList.size()) ? mUriMap.get(mDateList.get(position)) : null;
    }


    private void setPage(int index, boolean smoothScroll) {
        if (index >= 0 && index < mDateList.size()) mViewPager.setCurrentItem(index, smoothScroll);
    }

    private void goPrevPage() {
        int index = mViewPager.getCurrentItem() - 1;
        setPage(index, true);
    }

    private void goNextPage() {
        int index = mViewPager.getCurrentItem() + 1;
        setPage(index, true);
    }

    private Date getSelectedDate() {
        int position = mViewPager.getCurrentItem();
        return (position >= 0 && position < mDateList.size()) ? mDateList.get(position) : null;
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch(id) {
            case R.id.btn_prev:
                goPrevPage();;
                break;

            case R.id.btn_next:
                goNextPage();
                break;

            case R.id.btn_detail:
                Uri photo = getPhotoUri(mViewPager.getCurrentItem());
                if (photo != null) UiUtils.intentActionView(this, photo);
                break;

            default:
                super.onClick(view);
                break;
        }
    }


    ////////////////////////////////////////////////////////////////////
    //
    // 親クラスのメソッドをOverrideする
    //

    @Override
    protected Date getTargetDate() {
        return getSelectedDate();
    }

    @Override
    protected void redrawChart(String deletedTag) {
        // タグの削除については何もしない
    }

    @Override
    protected void redrawChart(Date deletedLogDate) {
        // 表示ページ数が変更になるので Activityごと再描画する
        Intent intent = new Intent(this, LineChartActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (deletedLogDate != null) {
            // 初期ページの処理
            // 表示中のデータが消去されるので次の日付を初期表示ページにする
            int index = mDateList.indexOf(deletedLogDate) + 1;
            if (index >= 1 && index < mDateList.size()) {
                intent.putExtra(EXTRA_PARAM1, mDateList.get(index));
            }
        }
        startActivity(intent);
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
