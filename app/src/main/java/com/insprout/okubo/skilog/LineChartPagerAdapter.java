package com.insprout.okubo.skilog;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.insprout.okubo.skilog.chart.AltitudeChart;
import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.model.TagDb;
import com.insprout.okubo.skilog.util.AppUtils;
import com.insprout.okubo.skilog.util.ContentsUtils;
import com.insprout.okubo.skilog.util.MiscUtils;
import com.insprout.okubo.skilog.util.UiUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LineChartPagerAdapter extends PagerAdapter {

    public interface PhotoUriListener {
        void photoPicked(Date date, Uri photo);
    }

    private LayoutInflater mInflater;

    private Context mContext;
    private List<Date> mDateList;
    private PhotoUriListener mListener = null;
    private boolean mHasToday = false;
    private AltitudeChart mChartOfToday = null;
    private Map<Date, TextView> mMapTvKeyword = new HashMap<>();


    public LineChartPagerAdapter(Activity activity, List<Date> dates) {
        this(activity, dates, null);
    }

    public LineChartPagerAdapter(Activity activity, List<Date> dates, PhotoUriListener listener) {
        mContext = activity;
        mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDateList = dates;
        mListener = listener;
        mMapTvKeyword.clear();
        // 日付リストが 本日のものかチェックしておく
        mHasToday = (mDateList != null && !mDateList.isEmpty() && MiscUtils.isToday(mDateList.get(mDateList.size() - 1)));
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View layout = mInflater.inflate(R.layout.cell_line_chart2, container, false);
        container.addView(layout);

        final Date date = mDateList.get(position);

        if (getCount() >= 1) {
            TextView tv = layout.findViewById(R.id.tv_count);
            tv.setText(mContext.getString(R.string.fmt_pager, position + 1, getCount()));
        }
        TextView tvDate = layout.findViewById(R.id.tv_date);
        tvDate.setText(AppUtils.toDateString(date));
        final LineChart lineChartView = layout.findViewById(R.id.line_chart);
        final TextView tvValue = layout.findViewById(R.id.tv_chart_value);
        final ImageView ivPhoto = layout.findViewById(R.id.iv_photo);
        ivPhoto.setVisibility(View.GONE);
        ivPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (view.getTag() instanceof Uri) {
                    UiUtils.intentActionView(mContext, (Uri)view.getTag());
                }
            }
        });
        final AltitudeChart dailyChart = new AltitudeChart(mContext, lineChartView, date);
        if (mHasToday && (position == getCount() - 1)) mChartOfToday = dailyChart;   // 当日のチャートを保持
        final OnChartValueSelectedListener listener = new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                // ポイント地点の情報を表示
                Uri photo = dailyChart.getPhotoUri(e);
                tvValue.setText(mContext.getString(
                        photo != null ? R.string.fmt_value_photo : R.string.fmt_value_altitude,
                        dailyChart.getXAxisLabel(e.getX()),
                        dailyChart.getYAxisLabel(e.getY())
                ));
                ivPhoto.setTag(photo);
                if (photo != null) {
                    // サムネイルを右寄せにするか 左寄せにするか
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)ivPhoto.getLayoutParams();
                    if (e.getX() > (lineChartView.getXChartMin() + lineChartView.getXChartMax()) / 2) {
                        params.gravity = Gravity.START;
                    } else {
                        params.gravity = Gravity.END;
                    }
                    ivPhoto.setLayoutParams(params);
                    ivPhoto.setVisibility(View.VISIBLE);
                    ivPhoto.setImageBitmap(ContentsUtils.getThumbnail(mContext, photo, MediaStore.Images.Thumbnails.MICRO_KIND));
                } else {
                    ivPhoto.setVisibility(View.GONE);
                    ivPhoto.setImageDrawable(null);
                }
                if (photo != null && mListener != null) mListener.photoPicked(date, photo);
            }

            @Override
            public void onNothingSelected() {
            }
        };
        dailyChart.setOnChartValueSelectedListener(listener);
        //dailyChart.drawChart();
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                dailyChart.drawChart();
                if (mListener != null) mListener.photoPicked(date, dailyChart.getPhotoUri());
            }
        });

        // 紐付けられたキーワードを表示する
        mMapTvKeyword.put(date, (TextView) layout.findViewById(R.id.tv_keywords));
        notifyTagChanged(date);

        return layout;
    }


    // 削除されるタイミングで呼ばれる。
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ViewPager viewPager = (ViewPager)container;
        viewPager.removeView((View)object);

        mMapTvKeyword.remove(mDateList.get(position));
        if (position == getCount() - 1) mChartOfToday = null;   // 当日のチャートが破棄された
    }

    @Override
    public int getCount() {
        return mDateList != null ? mDateList.size() : 0;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }

    public void updateChart() {
        // 当日のチャートが存在していれば、表示を最新にする
        if (mChartOfToday != null) {
            mChartOfToday.drawChart();
        }
    }

    public void appendChartValue(long time, float altitude, float ascent, float descent, int runCount) {
        // 当日のチャートが存在していれば、新規データをチャートに反映する
        if (mChartOfToday != null) {
            mChartOfToday.appendChartValue(time, altitude, ascent, descent, runCount);
        }
    }

    public void notifyTagChanged(Date date) {
        TextView tv = mMapTvKeyword.get(date);
        if (tv != null) {
            List<TagDb> tagsOnTarget = DbUtils.selectTags(mContext, date);
            String tags = TagDb.join(mContext.getString(R.string.glue_join), tagsOnTarget);
            tv.setText(tags);
        }
    }

}

