package com.insprout.okubo.skilog;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.insprout.okubo.skilog.chart.SummaryChart;
import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.model.TagDb;
import com.insprout.okubo.skilog.setting.Const;
import com.insprout.okubo.skilog.setting.Settings;
import com.insprout.okubo.skilog.util.ContentsUtils;
import com.insprout.okubo.skilog.util.DialogUi;
import com.insprout.okubo.skilog.util.MiscUtils;
import com.insprout.okubo.skilog.util.SdkUtils;
import com.insprout.okubo.skilog.util.UiUtils;

import java.util.Date;
import java.util.List;

import androidx.appcompat.app.ActionBar;


public class BarChartActivity extends BaseActivity implements View.OnClickListener, DialogUi.DialogEventListener {

    private ServiceMessenger mServiceMessenger;

    private SummaryChart mSummaryChart;
    private ImageView mIvPhoto;
    private Uri mPhotoUri = null;

    private boolean mValueSelected = false;
    //private long mTimeToast = 0;


    @Override
    public void onResume() {
        super.onResume();
        if (SkiLogService.isRunning(this)) mServiceMessenger.bind();
    }

    @Override
    public void onPause() {
        if (SkiLogService.isRunning(this)) mServiceMessenger.unbind();
        super.onPause();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.getThemeStyle(this));
        setContentView(R.layout.activity_bar_chart);

        initVars();                                             // 変数などの初期化
        initView();                                             // View関連の初期化
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

                        mSummaryChart.appendChartValue(data[0], data[1] * 0.001f, data[2] * 0.001f, data[3] * 0.001f, (int)data[4]);
                        break;
                }
            }
        });
    }

    private void initView() {
        onInitialize();
        UiUtils.setVisibility(this, R.id.btn_chart2, View.GONE);

        // タイトルバーに backボタンを表示する
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
//            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.drawable.ic_bar_chart);
        }
        mIvPhoto = findViewById(R.id.iv_photo);
        mIvPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPhotoUri != null) {
                    UiUtils.intentActionView(BarChartActivity.this, mPhotoUri);
                }
            }
        });

        BarChart barChart = findViewById(R.id.bar_chart);
        mSummaryChart = new SummaryChart(this, barChart, new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry entry, Highlight h) {
                showValue(entry);
            }

            @Override
            public void onNothingSelected() {
                showValue(null);
            }
        });

        // チャートの表示を更新する
        updateChart();
    }


    // チャートの表示を更新する
    private void updateChart() {
//        mValueSelected = false;     // 選択解除の表示を行わせない
        mSummaryChart.drawChart();
        UiUtils.setText(this, R.id.tv_count, getString(R.string.fmt_count_summaries, mSummaryChart.getCount()));

        setTitle(mSummaryChart.getSubject());
        showValue(null);
    }

    private void showValue(Entry entry) {
        mPhotoUri = null;
        String text = null;
        int gravityPhoto = Gravity.NO_GRAVITY;

        if (entry != null) {
            Date date = mSummaryChart.getLogDate(entry.getX());
            text = getString(R.string.fmt_value_accumulate, MiscUtils.toDateString(date));
            // キーワードを表示する
            // 付与されているタグ一覧
            showTags(date);

            // 計測期間内に撮影された画像があるか確認する
            Date[] period = DbUtils.selectTimePeriods(this, date);
            if (period != null && period.length == 2) {
                // 撮影した写真がすぐグラフに反映されるように修正
                Date endTime = period[1];
                if (MiscUtils.isToday(endTime) && SkiLogService.isRunning(this)) endTime = new Date(System.currentTimeMillis());

                if (SdkUtils.checkSelfPermission(this, Const.PERMISSIONS_CONTENTS)) {
                    List<Uri> photoList = ContentsUtils.getImageList(this, period[0], endTime);
                    if (photoList.size() >= 1) {
                        mPhotoUri = photoList.get(0);
                        text += getString(R.string.fmt_photo_count, photoList.size());

                        BarChart chart = mSummaryChart.getChart();
                        float visibleCountHalf = 4f;//(int)Math.ceil(chart.getVisibleXRange() / 2);
                        if (entry.getX() >= visibleCountHalf && entry.getX() >= chart.getXChartMax() - visibleCountHalf) {
                            gravityPhoto = Gravity.START;
                        } else {
                            gravityPhoto = Gravity.END;
                        }
                    }
                }
            }
            if (!mValueSelected) {
                Toast.makeText(this, R.string.msg_date_selected, Toast.LENGTH_LONG).show();
                mValueSelected = true;
            }
        } else {
            showTags(null);
        }

        if (mPhotoUri != null) {
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)mIvPhoto.getLayoutParams();
            params.gravity = gravityPhoto;
            mIvPhoto.setLayoutParams(params);
            mIvPhoto.setVisibility(View.VISIBLE);
            mIvPhoto.setImageBitmap(ContentsUtils.getThumbnail(this, mPhotoUri, MediaStore.Images.Thumbnails.MICRO_KIND));
        } else {
            // サムネイルを非表示
            mIvPhoto.setVisibility(View.GONE);
            mIvPhoto.setImageDrawable(null);
        }

        UiUtils.setText(this, R.id.tv_chart_value, text);
        UiUtils.setEnabled(this, R.id.btn_detail, mPhotoUri != null);
    }

    private void showTags(Date date) {
        String tags = "";
        if (date != null) {
            List<TagDb> tagsOnTarget = DbUtils.selectTags(this, date);
            tags = TagDb.join(getString(R.string.glue_join), tagsOnTarget);
        }
        UiUtils.setText(this, R.id.tv_keywords, tags);
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch(id) {
            case R.id.btn_detail:
                if (mPhotoUri != null) UiUtils.intentActionView(this, mPhotoUri);
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
        return mSummaryChart.getSelectedDate();
    }

    @Override
    protected void notifyChartChanged() {
        updateChart();
    }

    @Override
    protected void notifyLogsDeleted(Date deletedLogDate) {
        updateChart();
    }

    @Override
    protected void notifyTagRemoved(TagDb deletedTag) {
        if (deletedTag != null) {
            showTags(deletedTag.getDate());
            String tag = deletedTag.getTag();

            // 絞り込み中に、その条件となっているタグが消された場合は、グラフを更新する
            if (tag != null && tag.equals(mSummaryChart.getFilter())) {
                // グラフを再描画すると、計測日の選択は解除される
                updateChart();
            }
        }
    }

    @Override
    protected void notifyTagAdded(TagDb addedTag) {
        if (addedTag != null) {
            showTags(addedTag.getDate());
            String tag = addedTag.getTag();
        }
    }

    @Override
    protected void notifyFilterSpecified(String filter) {
        if (filter != null) {
            setTitle(getString(R.string.fmt_title_tag, filter));
        } else {
            setTitle(R.string.title_chart_desc);
        }
        mSummaryChart.setFilter(filter);
        updateChart();
    }


    public static void startActivity(Activity context) {
        Intent intent = new Intent(context, BarChartActivity.class);
        context.startActivity(intent);
    }

}
