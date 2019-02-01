package com.insprout.okubo.skilog;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import com.insprout.okubo.skilog.setting.Const;

import java.util.ArrayList;
import java.util.List;

public class PhotoViewerActivity extends AppCompatActivity {

    private List<Uri> mPhotos = null;

    private ViewPager mViewPager;
    private ImageViewPagerAdapter mAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_viewer);

        initVars();
        initView();
    }

    private void initVars() {
        mPhotos = getIntent().getParcelableArrayListExtra(Const.EXTRA_PARAM1);
    }

    private void initView() {
        // タイトルバーに backボタンを表示する
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);

        mAdapter = new ImageViewPagerAdapter(this, mPhotos);
        mViewPager = findViewById(R.id.viewpager);
        mViewPager.setAdapter(mAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // タイトルバーの backボタン処理
            case android.R.id.home:
                finish();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public static void startActivity(Context context, ArrayList<Uri> photos) {
        Intent intent = new Intent(context, PhotoViewerActivity.class);
        intent.putExtra(Const.EXTRA_PARAM1, photos);
        context.startActivity(intent);
    }
}
