package com.insprout.okubo.skilog;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.insprout.okubo.skilog.chart.AltitudeChart;
import com.insprout.okubo.skilog.chart.DailyChart;
import com.insprout.okubo.skilog.util.CacheUtils;
import com.insprout.okubo.skilog.util.ContentsUtils;

import java.util.Date;
import java.util.List;

public class LineChartPagerAdapter extends PagerAdapter {

    private LayoutInflater mInflater;

    private Context mContext;
    private List<Date> mDateList = null;
//    private Point mDisplaySize = new Point();


    public LineChartPagerAdapter(Activity activity, List<Date> dates) {
        mContext = activity;
        mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDateList = dates;

//        activity.getWindowManager().getDefaultDisplay().getRealSize(mDisplaySize);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View layout = mInflater.inflate(R.layout.cell_line_chart2, container, false);
        container.addView(layout);

        Date date = mDateList.get(position);

        final LineChart lineChart = layout.findViewById(R.id.line_chart);
        final TextView tvValue = layout.findViewById(R.id.tv_chart_value);
        final AltitudeChart mDailyChart = new AltitudeChart(mContext, lineChart, date, new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                // ポイント地点の情報を表示
//                displayValues(mRgChartType.getCheckedRadioButtonId(), e);
//                this.
//                if (mDailyChart != null) {
//
//                }
//                tvValue.setText(mContext.getString(R.string.fmt_value_altitude, mDailyChart.getXAxisLabel(e.getX()), mDailyChart.getYAxisLabel(e.getY())));
            }

            @Override
            public void onNothingSelected() {
            }
        });
        mDailyChart.drawChart();

        return layout;
    }


    // 削除されるタイミングで呼ばれる。
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ViewPager viewPager = (ViewPager)container;
        viewPager.removeView((View)object);
    }

    @Override
    public int getCount() {
        return mDateList != null ? mDateList.size() : 0;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }
}

