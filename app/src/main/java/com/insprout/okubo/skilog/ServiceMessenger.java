package com.insprout.okubo.skilog;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.ArrayList;
import java.util.EventListener;

/**
 * Created by okubo on 2018/01/25.
 */

public class ServiceMessenger implements ServiceConnection {
    private static final String TAG = "ServiceMessenger";

    // メッセージ送信用 イベントコード
    private static final int MSG_SET_CALLBACK = 1;
    private static final int MSG_UNSET_CALLBACK = 2;

    // メッセージ返信用イベントコード
    public static final int MSG_REPLY_STRING = 1;
    public static final int MSG_REPLY_INT = 2;
    public static final int MSG_REPLY_FLOAT = 3;
    public static final int MSG_REPLY_STRING_ARRAY = 11;
    public static final int MSG_REPLY_INT_ARRAY = 12;
    public static final int MSG_REPLY_FLOAT_ARRAY = 13;


    private Context mContext;
    private Intent mServiceIntent;
    private Messenger mSendMessenger = null;
    private Messenger mCallbackMessenger = null;
    private SetCallbackHandler mCallbackHandler;                // コールバック用のリスナーを Serviceに送信するHandler
    private boolean mConnected = false;


    public ServiceMessenger(Context context, OnServiceMessageListener listener) {
        mContext = context;

        mServiceIntent = new Intent(context, SkiLogService.class);
        mServiceIntent.setPackage(mContext.getPackageName());
        mCallbackHandler = new SetCallbackHandler(listener);
        mCallbackMessenger = new Messenger(mCallbackHandler);
    }

    public void bind() {
        Log.d(TAG, "Try binding");
        //mContext.bindService(mServiceIntent, this, Context.BIND_ALLOW_OOM_MANAGEMENT);
        mContext.bindService(mServiceIntent, this, Context.BIND_ABOVE_CLIENT);
    }

    public void unbind() {
        Log.d(TAG, "Try unbinding");
        if (!mConnected) return;
        Message msg = Message.obtain(null, MSG_UNSET_CALLBACK);
        msg.replyTo = mCallbackMessenger;
        try {
            mSendMessenger.send(msg);
            mConnected = false;
        } catch (RemoteException e) {
            //e.printStackTrace();
        }

        mContext.unbindService(this);
    }

    public void setOnServiceEventListener(OnServiceMessageListener listener) {
        mCallbackHandler.setOnEventListener(listener);
    }


    private boolean sendMessage(int what) {
        Message msg = Message.obtain(null, what);
        try {
            mSendMessenger.send(msg);
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }


    //////////////////////////////////////////////////////////
    //
    // ServiceConnection用 implements
    //

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        Log.d(TAG, "onServiceConnected");

        mSendMessenger = new Messenger(iBinder);
        try {
            // Activityが受け取るコールバックを Serviceに登録する
            Message msg = Message.obtain(null, MSG_SET_CALLBACK);
            msg.replyTo = mCallbackMessenger;
            mSendMessenger.send(msg);
            mConnected = true;

        } catch (RemoteException e) {
            mConnected = false;
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        // サービスが停止された場合に呼び出される
        // 注) このメソッドは サービスがunbindされてもよばれない。
        mContext.unbindService(this);
    }


    //////////////////////////////////////////////////
    //
    // Activity - Service間通信のためのHandlerクラス
    //

    /**
     * Serviceプロセスから Activityのコールバックへ返信するためのHandlerクラス
     * (Handler のメモリーリーク対策のために staticクラスにする)
     */
    static class ReplyMessageHandler extends Handler {

        private final ArrayList<Messenger> mCallbackMessengers = new ArrayList<>();   // 返信用メッセンジャー(複数)

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ServiceMessenger.MSG_SET_CALLBACK:
                    // 返信リスナー登録
                    mCallbackMessengers.add(msg.replyTo);
                    break;

                case ServiceMessenger.MSG_UNSET_CALLBACK:
                    // 返信リスナー登録解除
                    mCallbackMessengers.remove(msg.replyTo);
                    break;
            }
        }


        // 返信するデータの型ごとにメソッドを分ける

        public void replyMessage(String message) {
            try {
                for (Messenger callback : mCallbackMessengers) {
                    callback.send(Message.obtain(null, ServiceMessenger.MSG_REPLY_STRING, message));
                }
            } catch (RemoteException e) {
                //e.printStackTrace();
            }
        }

        public void replyMessage(float[] data) {
            try {
                for (Messenger callback : mCallbackMessengers) {
                    callback.send(Message.obtain(null, ServiceMessenger.MSG_REPLY_FLOAT_ARRAY, data));
                }
            } catch (RemoteException e) {
                //e.printStackTrace();
            }
        }
    }

    /**
     * Serviceプロセスへ Activityへのコールバックを登録するためのHandlerクラス
     * (Handler のメモリーリーク対策のために staticクラスにする)
     */
    private static class SetCallbackHandler extends Handler {

        private OnServiceMessageListener mListener;

        public SetCallbackHandler(OnServiceMessageListener listener) {
            mListener = listener;
        }

        @Override
        public void handleMessage(Message msg) {
            if (mListener != null) {
                mListener.onReceiveMessage(msg);
            }
        }

        public void setOnEventListener(OnServiceMessageListener listener) {
            mListener = listener;
        }
    }

    public interface OnServiceMessageListener extends EventListener {
        void onReceiveMessage(Message msg);
    }

}
