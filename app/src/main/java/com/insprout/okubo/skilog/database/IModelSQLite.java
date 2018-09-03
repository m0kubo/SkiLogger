package com.insprout.okubo.skilog.database;

import android.content.ContentValues;
import android.database.Cursor;

public interface IModelSQLite {
    String getTable();

    String getPrimaryKeyName();

    String getPrimaryKeyValue();

    ContentValues toRecord();

    IModelSQLite fromDatabase(Cursor cursor);
}
