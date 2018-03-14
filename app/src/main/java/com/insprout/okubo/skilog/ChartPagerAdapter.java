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


/**
 * Created by okubo on 2018/03/13.
 */

public class ChartPagerAdapter extends PagerAdapter {
    private final static int ITEM_COUNT = 2;

    private Context mContext;
    private LayoutInflater mInflater;

    private SummaryChart mChart1;


    public ChartPagerAdapter(Context context) {
        mContext = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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

                final ImageButton btnChart1 = layout.findViewById(R.id.btn_chart1);
                final ImageButton btnChart2 = layout.findViewById(R.id.btn_chart2);
                final TextView tvChartValue = layout.findViewById(R.id.tv_chart_value);
        //        btnChart1.setVisibility(View.GONE);
                btnChart2.setVisibility(View.GONE);

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
                mChart1.drawChart();

                break;
        }

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

}
