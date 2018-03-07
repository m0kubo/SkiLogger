package com.insprout.okubo.skilog.util;

import android.content.Context;

import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.database.TagData;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by okubo on 2018/03/07.
 * アプリ固有の処理を Utilsクラスにまとめる
 */

public class AppUtils {

    public static List<TagData> getTags(Context context) {
        List<TagData> tags = new ArrayList<>();
        List<TagData> tagsAll = DbUtils.selectTags(context);

        if (tagsAll == null || tagsAll.isEmpty()) {
            return tags;

        } else {
            for (TagData tagData : tagsAll) {
                String tag = tagData.getTag();
                if (tag == null) continue;

                boolean duplicated = false;
                for (TagData tagData2 : tags) {
                    if (tag.equals(tagData2.getTag())) {
                        duplicated = true;
                        break;
                    }
                }
                if (!duplicated) tags.add(tagData);
            }
        }
        return tags;
    }

}
