package com.insprout.okubo.skilog.util;

import android.content.Context;

import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.database.TagData;
import com.insprout.okubo.skilog.model.PlaceData;
import com.insprout.okubo.skilog.model.ResponsePlaceData;

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
        return DbUtils.selectDistinctTags(context);
    }

    public static List<TagData> getTags(Context context, List<TagData> exclude) {
        List<TagData> tags = new ArrayList<>();
        List<TagData> tagsAll = DbUtils.selectDistinctTags(context);

        if (tagsAll != null && !tagsAll.isEmpty()) {
            for (TagData tagData : tagsAll) {
                String tag = tagData.getTag();
                if (tag != null && !containsTag(tagData, exclude)) tags.add(tagData);
            }
        }
        return tags;
    }

    private static boolean containsTag(TagData tag, List<TagData> tags) {
        if (tag == null || tags == null) return false;
        String tagText = tag.getTag();
        if (tagText == null) return false;

        for (TagData tagData : tags) {
            if (tagText.equals(tagData.getTag())) {
                return true;
            }
        }
        return false;
    }

    public static String[] toStringArray(List<?> list) {
        return toStringArray(list, -1);
    }

    public static String[] toStringArray(List<?> list, int limit) {
        if (list == null) return null;

        int arraySize = list.size();
        if (limit >= 1 && limit < arraySize ) arraySize = limit;
        String[] array = new String[ arraySize ];
        for (int i=0; i<arraySize; i++) {
            Object item = list.get(i);
            if (item instanceof PlaceData) {
                array[i] = ((PlaceData)item).getName();

            } else if (item instanceof TagData) {
                array[i] = ((TagData)item).getTag();

            } else {
                array[i] = item.toString();
            }
        }
        return array;
    }

    public static String[] toStringArray(ResponsePlaceData data) {
        if (data == null) return null;
        if (!data.isStatusOk()) return null;

        List<PlaceData> places = data.getPlaces();
        if (places.size() == 0) return null;

        return toStringArray(places);
    }

}
