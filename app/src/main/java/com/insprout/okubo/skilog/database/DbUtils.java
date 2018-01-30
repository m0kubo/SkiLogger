package com.insprout.okubo.skilog.database;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by okubo on 2018/01/30.
 * データベース操作用 ユーティリティクラス
 */

public class DbUtils {

    public static List<SkiLogData> select(Context context) {
        List<SkiLogData> res = new ArrayList<>();

        SkiLogDb fbkDatabase = null;
        try {
            fbkDatabase = new SkiLogDb(context);
            res = fbkDatabase.selectFromTable1();

        } catch(Exception ex) {
            return res;

        } finally {
            if (fbkDatabase != null) fbkDatabase.close();
        }
        return res;
    }


    public static long insert(Context context, SkiLogData data) {
        long id = 0;

        SkiLogDb database = null;
        try {
            database = new SkiLogDb(context);
            id = database.insert(data);

        } finally {
            if (database != null) database.close();
        }
        return id;
    }

    public static long count(Context context) {
        long res = 0;

        SkiLogDb fbkDatabase = null;
        try {
            fbkDatabase = new SkiLogDb(context);
            res = fbkDatabase.countFromTable1();

        } catch(Exception ex) {
            return res;

        } finally {
            if (fbkDatabase != null) fbkDatabase.close();
        }
        return res;
    }

}
