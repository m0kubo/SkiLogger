package com.insprout.okubo.skilog;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.insprout.okubo.skilog.chart.SummaryChart;

import java.util.Date;


/**
 * Created by okubo on 2018/03/13.
 */

public class ChartPagerAdapter extends PagerAdapter {
    private final static int ITEM_COUNT = 2;

    private Context mContext;
    private LayoutInflater mInflater;

    private SummaryChart mChart1;
    private ImageButton mBtnNext1 = null;
    private ImageButton mBtnPrev1 = null;
    private ImageButton mBtnTag1 = null;
    private View.OnClickListener mTagButtonListener;


    public ChartPagerAdapter(Context context) {
        this(context, null);
    }

    public ChartPagerAdapter(Context context, View.OnClickListener listener) {
        mContext = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mTagButtonListener = listener;
    }


    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View layout;

        switch (position) {
            case 1:
                layout = this.mInflater.inflate(R.layout.cell_line_chart, container, false);
                break;

            case 0:
            default:
                layout = this.mInflater.inflate(R.layout.cell_bar_chart, container, false);

                mBtnNext1 = layout.findViewById(R.id.btn_next);
                mBtnNext1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mChart1.goNextPage();
                        enableButtons(0);
                    }
                });
                mBtnPrev1 = layout.findViewById(R.id.btn_prev);
                mBtnPrev1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mChart1.goPreviousPage();
                        enableButtons(0);
                    }
                });
                mBtnTag1 = layout.findViewById(R.id.btn_tag);
                mBtnTag1.setVisibility(View.VISIBLE);
                mBtnTag1.setOnClickListener(mTagButtonListener);

                final TextView tvChartValue = layout.findViewById(R.id.tv_chart_value);
                BarChart barChart = layout.findViewById(R.id.chart);
                mChart1 = new SummaryChart(mContext, barChart, new OnChartValueSelectedListener() {
                    @Override
                    public void onValueSelected(Entry entry, Highlight h) {
                        displayValue(entry);
                    }

                    @Override
                    public void onNothingSelected() {
                        displayValue(null);
                    }

                    private void displayValue(Entry entry) {
                        String text = null;
                        if (entry != null) {
                            text = mContext.getString(R.string.fmt_value_accumulate,
                                    mChart1.getXAxisLabelFull(entry.getX()),
                                    mChart1.getYAxisLabel(entry.getY()));
                        }
                        tvChartValue.setText(text);
                    }
                });
                break;
        }

        drawChart(position);

        container.addView(layout);
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
        return ITEM_COUNT;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }


    public void drawChart(int position) {
        switch (position) {
            case 0:
                if (mChart1 != null) {
                    mChart1.drawChart();
                }
                break;

            case 1:
                break;
        }

        enableButtons(position);
    }

    private void enableButtons(int position) {
        switch (position) {
            case 0:
                mBtnNext1.setEnabled(mChart1 != null && mChart1.hasNextPage());
                mBtnPrev1.setEnabled(mChart1 != null && mChart1.hasPreviousPage());
                break;

            case 1:
                break;
        }
    }


    public void setFilter(String filteringTag) {
        mChart1.setFilter(filteringTag);
    }

    public String getSubject(int position) {
        switch (position) {
            case 0:
                if (mChart1 != null) return mChart1.getSubject();
                break;

            case 1:
                break;
        }
        return null;
    }

    public Date getSelectedDate(int position) {
        switch (position) {
            case 0:
                if (mChart1 != null) return mChart1.getSelectedDate();
                break;

            case 1:
                break;
        }
        return null;
    }

    public void setButtonEnabled(int index, boolean enabled) {
        switch(index) {
            case 0:
                if (mBtnTag1 != null) mBtnTag1.setEnabled(enabled);
                break;
        }
    }

    public void appendChartValue(long time, float altitude, float accumulateAsc, float accumulateDesc, int runCount) {
        if (mChart1 != null) {
            mChart1.appendChartValue(time, altitude, accumulateAsc, accumulateDesc, runCount);
        }
    }

}
