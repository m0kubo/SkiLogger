package com.insprout.okubo.skilog;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.insprout.okubo.skilog.chart.DailyChart;
import com.insprout.okubo.skilog.chart.SummaryChart;

import java.util.Date;
import java.util.EventListener;


/**
 * Created by okubo on 2018/03/13.
 */

public class ChartPagerAdapter extends PagerAdapter {
    private final static int ITEM_COUNT = 2;

    private Context mContext;
    private LayoutInflater mInflater;

    private SummaryChart mChart1;
    private TextView mTvValue1;
    private ImageButton mBtnNext1;
    private ImageButton mBtnPrev1;
    private ImageButton mBtnOption1;
    private OnChartEventListener mChartEventListener;

    private DailyChart mChart2;
    private TextView mTvValue2;
    private ImageButton mBtnNext2;
    private ImageButton mBtnPrev2;
    private RadioGroup mRadioGroup2;


    public ChartPagerAdapter(Context context) {
        this(context, null);
    }

    public ChartPagerAdapter(Context context, OnChartEventListener listener) {
        mContext = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mChartEventListener = listener;
    }


    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View layout;

        switch (position) {
            case 1:
                layout = this.mInflater.inflate(R.layout.cell_line_chart, container, false);

                mTvValue2 = layout.findViewById(R.id.tv_chart_value);
                LineChart lineChart = layout.findViewById(R.id.line_chart);
                mChart2 = new DailyChart(mContext, lineChart, new OnChartValueSelectedListener() {
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
                                    mChart2.getXAxisLabel(entry.getX()),
                                    mChart2.getYAxisLabel(entry.getY()));
                        }
                        mTvValue2.setText(text);
                    }
                });

                mBtnNext2 = layout.findViewById(R.id.btn_next);
                mBtnNext2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mChart2.goNextPage();
                        updateUi(1);
                    }
                });
                mBtnPrev2 = layout.findViewById(R.id.btn_prev);
                mBtnPrev2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mChart2.goPreviousPage();
                        updateUi(1);
                    }
                });

                mRadioGroup2 = layout.findViewById(R.id.rg_chart_type);
                mRadioGroup2.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup radioGroup, int id) {
                        switch (id) {
                            case R.id.btn_altitude:
                                mChart2.setChartType(0);
                                break;
                            case R.id.btn_accumulate:
                                mChart2.setChartType(1);
                                break;
                        }
                    }
                });
                break;

            case 0:
            default:
                layout = this.mInflater.inflate(R.layout.cell_bar_chart, container, false);

                mTvValue1 = layout.findViewById(R.id.tv_chart_value);
                BarChart barChart = layout.findViewById(R.id.bar_chart);
                mChart1 = new SummaryChart(mContext, barChart, new OnChartValueSelectedListener() {
                    @Override
                    public void onValueSelected(Entry entry, Highlight h) {
                        displayValue(entry);
                        Date date = mChart1.getSelectedDate();
                        if (date != null && mChart2 != null) {
                            mChart2.setSelectedDate(date);
                            String msg = mContext.getString(R.string.fmt_title_chart, mChart1.getXAxisLabelFull(entry.getX()));
                            Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
                            if (mChartEventListener != null) {
                                if (mChartEventListener != null) {
                                    Integer pageIndex = 1;
                                    mChartEventListener.onChartEvent(0, TYPE_PAGE_SELECTED, pageIndex);
                                }
                            }
                        }
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
                        mTvValue1.setText(text);
                    }
                });

                mBtnNext1 = layout.findViewById(R.id.btn_next);
                mBtnNext1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mChart1.goNextPage();
                        updateUi(0);
                    }
                });
                mBtnPrev1 = layout.findViewById(R.id.btn_prev);
                mBtnPrev1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mChart1.goPreviousPage();
                        updateUi(0);
                    }
                });
                mBtnOption1 = layout.findViewById(R.id.btn_tag);
                mBtnOption1.setVisibility(View.VISIBLE);
                mBtnOption1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (mChartEventListener != null) {
                            mChartEventListener.onChartEvent(0, TYPE_VIEW_CLICKED, mBtnOption1);
                        }
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


    public void delete(Date date) {
        if (mChart2 != null) {
            mChart2.delete(date);
        }
    }


    public void drawChart(int position) {
        switch (position) {
            case 0:
                if (mChart1 != null) {
                    mChart1.drawChart();
                }
                break;

            case 1:
                if (mChart2 != null) {
                    mChart2.drawChart();
                }
                break;
        }

        updateUi(position);
    }

    private void updateUi(int position) {
        switch (position) {
            case 0:
                if (mBtnNext1 != null) mBtnNext1.setEnabled(mChart1 != null && mChart1.hasNextPage());
                if (mBtnPrev1 != null) mBtnPrev1.setEnabled(mChart1 != null && mChart1.hasPreviousPage());
                break;

            case 1:
                if (mBtnNext2 != null) mBtnNext2.setEnabled(mChart2 != null && mChart2.hasNextPage());
                if (mBtnPrev2 != null) mBtnPrev2.setEnabled(mChart2 != null && mChart2.hasPreviousPage());
                break;
        }

        if (mChartEventListener != null) {
            mChartEventListener.onChartEvent(position, TYPE_TITLE_UPDATED, getSubject(position));
        }
    }


    public void setFilter(String filteringTag) {
        mChart1.setFilter(filteringTag);
    }

    public String getFiler() {
        return mChart1.getFilter();
    }

    public String getSubject(int position) {
        switch (position) {
            case 0:
                if (mChart1 != null) return mChart1.getSubject();
                break;

            case 1:
                if (mChart2 != null) return mChart2.getSubject();
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
                if (mChart2 != null) return mChart2.getSelectedDate();
                break;
        }
        return null;
    }

    public void setViewEnabled(int position, int id, boolean enabled) {
        switch(position) {
            case 0:
                switch(id) {
                    case R.id.btn_tag :
                        if (mBtnOption1 != null) mBtnOption1.setEnabled(enabled);
                        break;
                }
                break;
        }
    }

    public void appendChartValue(long time, float altitude, float ascent, float descent, int runCount) {
        if (mChart1 != null) {
            mChart1.appendChartValue(time, altitude, ascent, descent, runCount);
        }
        if (mChart2 != null) {
            mChart2.appendChartValue(time, altitude, ascent, descent, runCount);
        }
    }


    //////////////////////////////////////////////////////////////////////////////////
    //
    // Interface
    //

    public final static int TYPE_VIEW_CLICKED = 100;
    public final static int TYPE_PAGE_SELECTED = 110;
    public final static int TYPE_TITLE_UPDATED = 200;

    public interface OnChartEventListener extends EventListener {
        void onChartEvent(int position, int eventType, Object obj);
    }

}
