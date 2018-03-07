package com.insprout.okubo.skilog.util;

import android.content.Context;

import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.database.TagData;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by okubo on 2018/03/07.
 * アプリ固有の処理を Utilsクラスにまとめる
 */

public class AppUtils {
    public static String toDateString(Date date) {
        if (date == null) return "";
        DateFormat mDateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        return mDateFormat.format(date);
    }

    public static List<TagData> getTags(Context context) {
        return getTags(context, null);
    }

    public static List<TagData> getTags(Context context, List<TagData> exclude) {
        List<TagData> tags = new ArrayList<>();
        List<TagData> tagsAll = DbUtils.selectTags(context);

        if (tagsAll == null || tagsAll.isEmpty()) {
            return tags;

        } else {
            for (TagData tagData : tagsAll) {
                String tag = tagData.getTag();
                if (tag != null && !containsTag(tagData, tags) && !containsTag(tagData, exclude)) tags.add(tagData);
            }
        }
        return tags;
    }

    private static boolean containsTag(TagData tag, List<TagData> tags) {
        if (tag == null || tags == null) return false;
        String text = tag.getTag();
        if (text == null) return false;

        for (TagData tagData : tags) {
            if (text.equals(tagData.getTag())) {
                return true;
            }
        }
        return false;
    }

}
