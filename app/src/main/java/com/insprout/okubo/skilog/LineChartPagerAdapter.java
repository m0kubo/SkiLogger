package com.insprout.okubo.skilog;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.insprout.okubo.skilog.chart.AltitudeChart;

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
    //private Map<Date, Uri> mPhoto = new HashMap<>();
    private PhotoUriListener mListener = null;


    public LineChartPagerAdapter(Activity activity, List<Date> dates) {
        this(activity, dates, null);
    }

    public LineChartPagerAdapter(Activity activity, List<Date> dates, PhotoUriListener listener) {
        mContext = activity;
        mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDateList = dates;
        mListener = listener;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View layout = mInflater.inflate(R.layout.cell_line_chart2, container, false);
        container.addView(layout);

        final Date date = mDateList.get(position);

        LineChart lineChartView = layout.findViewById(R.id.line_chart);
        final TextView tvValue = layout.findViewById(R.id.tv_chart_value);
        final AltitudeChart dailyChart = new AltitudeChart(mContext, lineChartView, date);
        final OnChartValueSelectedListener listener = new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(Entry e, Highlight h) {
                // ポイント地点の情報を表示
//                tvValue.setText(mContext.getString(R.string.fmt_value_altitude, dailyChart.getXAxisLabel(e.getX()), dailyChart.getYAxisLabel(e.getY())));
                Uri photo = dailyChart.getPhotoUri(e);
                tvValue.setText(mContext.getString(
                        photo != null ? R.string.fmt_value_photo : R.string.fmt_value_altitude,
                        dailyChart.getXAxisLabel(e.getX()),
                        dailyChart.getYAxisLabel(e.getY())
                ));
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

//    private class LineChartTask extends AsyncTask<Void, Void, Void> {
//
//        public LineChartTask() {
//
//        }
//
//        @Override
//        protected Void doInBackground(Void... voids) {
//            return null;
//        }
//    }

}

