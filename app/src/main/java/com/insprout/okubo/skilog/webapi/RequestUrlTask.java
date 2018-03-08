package com.insprout.okubo.skilog.webapi;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Created by okubo on 2018/03/08.
 * URLクラスを利用して、httpリクエストを行う
 */

public class RequestUrlTask extends AsyncTask<Void, Void, String> {
    private final String mUrl;
    private final OnResponseListener mEvent;

    public RequestUrlTask(String url, OnResponseListener listener) {
        mUrl = url;
        mEvent = listener;
    }

    @Override
    protected String doInBackground(Void... params) {
        if (mUrl == null) return null;

        StringBuilder result = new StringBuilder();
        InputStream stream = null;
        try {
            stream = new URL(mUrl).openStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            int count = 0;
            String line;
            while((line = reader.readLine()) != null) {
                if (count++ != 0) result.append("\n");
                result.append(line);
            }

        } catch (IOException ignored) {
            //
        } finally {
            try {
                if (stream != null) stream.close();
            } catch (IOException ignored) {
                //
            }
        }

        return result.toString();
    }

    @Override
    protected void onPostExecute(String responseBody) {
        if (mEvent != null) mEvent.onResponse(responseBody);
    }

    public interface OnResponseListener {
        void onResponse(String responseBody);
    }

}

