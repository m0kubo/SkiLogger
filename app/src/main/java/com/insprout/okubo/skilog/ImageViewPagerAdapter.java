package com.insprout.okubo.skilog;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.insprout.okubo.skilog.util.CacheUtils;
import com.insprout.okubo.skilog.util.ContentsUtils;

import java.util.List;

public class ImageViewPagerAdapter extends PagerAdapter {

    private LayoutInflater mInflater;

    private Context mContext;
    private List<Uri> mPhotoList = null;
    private Point mDisplaySize = new Point();


    public ImageViewPagerAdapter(Activity activity, List<Uri> photos) {
        mContext = activity;
        mInflater = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mPhotoList = photos;

        activity.getWindowManager().getDefaultDisplay().getRealSize(mDisplaySize);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View layout = mInflater.inflate(R.layout.cell_photo_list, container, false);
        container.addView(layout);

        final ImageView iv = layout.findViewById(R.id.iv_image);
        iv.setImageBitmap(getBitmap(mPhotoList.get(position)));

        return layout;
    }

    private Bitmap getBitmap(Uri uri) {
        if (uri == null) return null;
        String imagePath = uri.getPath();
        Bitmap bitmap = CacheUtils.getBitmap(imagePath);
        if (bitmap == null) {
            bitmap = compressBitmap(ContentsUtils.getBitmap(mContext, uri));
            CacheUtils.putBitmap(imagePath, bitmap);
        }
        return bitmap;
    }

    private Bitmap compressBitmap(Bitmap bitmap) {
        if (bitmap == null) return null;
        int edgeSize = Math.min(bitmap.getHeight(), bitmap.getWidth());
        if (edgeSize == 0 || mDisplaySize.x == 0) return bitmap;

        float ratio = (float)Math.min(bitmap.getHeight(), bitmap.getWidth()) / mDisplaySize.x;
        if (ratio < 1.4f) return bitmap;

        // 写真サイズが、画面サイズよりも大きい場合は、画面サイズに縮小する
        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, Math.round(bitmap.getWidth() / ratio), Math.round(bitmap.getHeight() / ratio), true);
        bitmap.recycle();
        bitmap = null;
        return newBitmap;
    }

    // 削除されるタイミングで呼ばれる。
    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        ViewPager viewPager = (ViewPager)container;
        viewPager.removeView((View)object);
    }

    @Override
    public int getCount() {
        return mPhotoList != null ? mPhotoList.size() : 0;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    }
}

