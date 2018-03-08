package com.insprout.okubo.skilog.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/**
 * Created by okubo on 2018/03/05.
 * AlertDialogを利用するための ユーティリティクラス
 */

public class DialogUtils {
    /**
     * 引数受け渡し用のキー
     */
    private static final String KEY_REQUEST_CODE = "dialog.REQUEST_CODE";
    private static final String KEY_DIALOG_TITLE = "dialog.TITLE";
    private static final String KEY_DIALOG_TEXT = "dialog.TEXT";
    private static final String KEY_DIALOG_LAYOUT = "dialog.LAYOUT_ID";
    private static final String KEY_DIALOG_POSITIVE_BUTTON_TEXT = "button.positive.TEXT";
    private static final String KEY_DIALOG_NEGATIVE_BUTTON_TEXT = "button.negative.TEXT";
    private static final String KEY_DIALOG_NEUTRAL_BUTTON_TEXT = "button.neutral.TEXT";
    private static final String KEY_CHOICE_ARRAY = "choice.ARRAY";
    private static final String KEY_CHOICE_SELECTED = "choice.SELECTED";
    private static final String KEY_PROGRESS_STYLE = "progress.STYLE";

    private static int STYLE_ALERT_DIALOG = 0;
    private static int STYLE_PROGRESS_DIALOG = 1;

    public static final int RC_NO_LISTENER = -1;

    public static final int LIST_TYPE_NO_CHECKBOX = -2;
    public static final int LIST_TYPE_NOT_SELECTED = -1;

    public static final int EVENT_BUTTON_POSITIVE = DialogInterface.BUTTON_POSITIVE;
    public static final int EVENT_BUTTON_NEGATIVE = DialogInterface.BUTTON_NEGATIVE;
    public static final int EVENT_BUTTON_NEUTRAL = DialogInterface.BUTTON_NEUTRAL;
    public static final int EVENT_DIALOG_CREATED = -100;
    public static final int EVENT_DIALOG_SHOWN = -101;

    private static final int ID_STRING_DEFAULT_OK = android.R.string.ok;
    private static final int ID_STRING_DEFAULT_CANCEL = android.R.string.cancel;

    private static final String TAG_PREFIX = "DIALOG_";


    //////////////////////////
    //
    // interface
    //

    public interface DialogEventListener {
        /**
         * AlertDialogの イベントを通知する
         * Listenerに渡される objResponseは Dialogの形態によって内容が異なる
         * - メッセージと ダイアログ標準ボタンのみの場合、常にnull
         * - 選択リスト(Single Choice)が指定されている場合、選択リストの ListViewオブジェクト
         * - カスタムViewが 設定されている場合、その Viewオブジェクト
         */
        void onDialogEvent(int requestCode, AlertDialog dialog, int which, View view);
    }


    ////////////////////////////////////
    //
    // ボタンと メッセージのみのダイアログ

    /**
     * OK/Cancel/Neutralの ボタンを持つダイアログを表示する
     * @param activity 呼び出すActivity
     * @param title ダイアログのタイトル文字列 (表示しない場合は nullを指定する)
     * @param message メッセージ文字列 (表示しない場合は nullを指定する)
     * @param labelOk OKボタンのラベル文字列 (ボタンを表示しない場合は nullを指定する)
     * @param labelCancel Cancelボタンのラベル文字列 (ボタンを表示しない場合は nullを指定する)
     * @param labelNeutral Neutralボタンのラベル文字列 (ボタンを表示しない場合は nullを指定する)
     * @param requestCode Dialogの イベントリスナーに返されるリクエストコード
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showButtonsDialog(Activity activity, String title, String message, String labelOk, String labelCancel, String labelNeutral, int requestCode) {
        DialogFragment dialog = BaseDialogFragment.newInstance(requestCode, title, message, labelOk, labelCancel, labelNeutral);
        dialog.show(activity.getFragmentManager(), buildTag(requestCode));
        return dialog;
    }

    /**
     * OKボタンのみの メッセージダイアログを表示する
     * @param activity 呼び出すActivity
     * @param titleId ダイアログのタイトルのリソースID (表示しない場合は 0を指定する)
     * @param messageId メッセージのリソースID (表示しない場合は 0を指定する)
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showOkDialog(Activity activity, int titleId, int messageId) {
        return showOkCancelDialog(activity, titleId, messageId, ID_STRING_DEFAULT_OK, 0, RC_NO_LISTENER);
    }

    /**
     * OK/Cancelの ボタンを持つダイアログを表示する
     * @param activity 呼び出すActivity
     * @param titleId ダイアログのタイトルのリソースID (表示しない場合は 0を指定する)
     * @param messageId メッセージのリソースID (表示しない場合は 0を指定する)
     * @param okTextId OKボタンのラベルのリソースID (ボタンを表示しない場合は 0を指定する)
     * @param cancelTextId キャンセルボタンのラベルのリソースID (ボタンを表示しない場合は 0を指定する)
     * @param requestCode Dialogの イベントリスナーに返されるリクエストコード
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showOkCancelDialog(Activity activity, int titleId, int messageId, int okTextId, int cancelTextId, int requestCode) {
        String title = getString(activity, titleId);
        String message = getString(activity, messageId);
        String labelOk = getString(activity, okTextId);
        String labelCancel = getString(activity, cancelTextId);
        return showButtonsDialog(activity, title, message, labelOk, labelCancel, null, requestCode);
    }

    /**
     *
     * @param activity 呼び出すActivity
     * @param title ダイアログのタイトル文字列 (表示しない場合は nullを指定する)
     * @param message メッセージ文字列 (表示しない場合は nullを指定する)
     * @param requestCode Dialogの イベントリスナーに返されるリクエストコード
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showOkCancelDialog(Activity activity, String title, String message, int requestCode) {
        String labelOk = getString(activity, android.R.string.ok);
        String labelCancel = getString(activity, android.R.string.cancel);
        return showButtonsDialog(activity, title, message, labelOk, labelCancel, null, requestCode);
    }


    ////////////////////////////////////
    //
    // ボタンと Item選択のダイアログ
    //

    /**
     * Itemリストを持つダイアログを表示する
     * @param activity 呼び出すActivity
     * @param title ダイアログのタイトル文字列 (表示しない場合は nullを指定する)
     * @param list 選択(単一選択)リスト
     * @param selected 初期選択番号 (未選択の場合は-1を設定する)
     * @param labelOk OKボタンのラベル文字列 (ボタンを表示しない場合は nullを指定する)
     * @param labelCancel Cancelボタンのラベル文字列 (ボタンを表示しない場合は nullを指定する)
     * @param requestCode Dialogの イベントリスナーに返されるリクエストコード
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showItemSelectDialog(final Activity activity, String title, String[] list, int selected, String labelOk, String labelCancel, int requestCode) {
        DialogFragment dialog = BaseDialogFragment.newInstance(requestCode, title, list, selected, labelOk, labelCancel);
        dialog.show(activity.getFragmentManager(), TAG_PREFIX + Integer.toHexString(requestCode));
        return dialog;
    }


    /**
     * CheckBoxなしItemリストを持つダイアログを表示する
     * @param activity 呼び出すActivity
     * @param title ダイアログのタイトル文字列 (表示しない場合は nullを指定する)
     * @param list 選択(単一選択)リスト
     * @param requestCode Dialogの イベントリスナーに返されるリクエストコード
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showItemListDialog(final Activity activity, String title, String[] list, String labelCancel, int requestCode) {
        return showItemSelectDialog(activity, title, list, LIST_TYPE_NO_CHECKBOX, null, labelCancel, requestCode);
    }

    /**
     * CheckBoxなしItemリストを持つダイアログを表示する
     * @param activity 呼び出すActivity
     * @param titleId ダイアログのタイトルのリソースID (表示しない場合は 0を指定する)
     * @param list 選択(単一選択)リスト
     * @param cancelTextId キャンセルボタンのラベルのリソースID (ボタンを表示しない場合は 0を指定する)
     * @param requestCode Dialogの イベントリスナーに返されるリクエストコード
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showItemListDialog(final Activity activity, int titleId, String[] list, int cancelTextId, int requestCode) {
        String title = getString(activity, titleId);
        String labelCancel = getString(activity, cancelTextId);
        return showItemListDialog(activity, title, list, labelCancel, requestCode);
    }


    /**
     * CheckBox付きItem選択(単一選択)リストを持つダイアログを表示する
     * @param activity 呼び出すActivity
     * @param titleId ダイアログのタイトルのリソースID (表示しない場合は 0を指定する)
     * @param list 選択(単一選択)リスト
     * @param selected 初期選択番号 (未選択の場合は-1を設定する)
     * @param okTextId OKボタンのラベルのリソースID (ボタンを表示しない場合は 0を指定する)
     * @param cancelTextId キャンセルボタンのラベルのリソースID (ボタンを表示しない場合は 0を指定する)
     * @param requestCode Dialogの イベントリスナーに返されるリクエストコード
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showItemSelectDialog(final Activity activity, int titleId, String[] list, int selected, int okTextId, int cancelTextId, int requestCode) {
        String title = getString(activity, titleId);
        String labelOk = getString(activity, okTextId);
        String labelCancel = getString(activity, cancelTextId);
        return showItemSelectDialog(activity, title, list, selected, labelOk, labelCancel, requestCode);
    }

    /**
     * CheckBox付きItem選択(単一選択)リストを持つダイアログを表示する
     * @param activity 呼び出すActivity
     * @param titleId ダイアログのタイトルのリソースID (表示しない場合は 0を指定する)
     * @param list 選択(単一選択)リスト
     * @param selected 初期選択番号 (未選択の場合は-1を設定する)
     * @param requestCode Dialogの イベントリスナーに返されるリクエストコード
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showItemSelectDialog(final Activity activity, int titleId, String[] list, int selected, int requestCode) {
        return showItemSelectDialog(activity, titleId, list, selected, ID_STRING_DEFAULT_OK, ID_STRING_DEFAULT_CANCEL, requestCode);
    }


    ////////////////////////////////////
    //
    // カスタムレイアウトのダイアログ
    //

    /**
     * カスタムレイアウトで Viewを指定したダイアログを表示する
     * @param activity 呼び出すActivity
     * @param title ダイアログのタイトル文字列 (表示しない場合は nullを指定する)
     * @param message メッセージ文字列 (表示しない場合は nullを指定する)
     * @param layoutId カスタムレイアウトのリソースID
     * @param labelOk OKボタンのラベル文字列 (ボタンを表示しない場合は nullを指定する)
     * @param labelCancel Cancelボタンのラベル文字列 (ボタンを表示しない場合は nullを指定する)
     * @param requestCode Dialogの イベントリスナーに返されるリクエストコード
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showCustomDialog(final Activity activity, String title, String message, int layoutId, String labelOk, String labelCancel, int requestCode) {
        DialogFragment dialog = BaseDialogFragment.newInstance(requestCode, title, message, layoutId, labelOk, labelCancel);
        dialog.show(activity.getFragmentManager(), TAG_PREFIX + Integer.toHexString(requestCode));
        return dialog;
    }

    /**
     * カスタムレイアウトで Viewを指定したダイアログを表示する
     * @param activity 呼び出すActivity
     * @param titleId ダイアログのタイトルのリソースID (表示しない場合は 0を指定する)
     * @param messageId メッセージのリソースID (表示しない場合は 0を指定する)
     * @param layoutId カスタムレイアウトのリソースID
     * @param okTextId OKボタンのラベルのリソースID (ボタンを表示しない場合は 0を指定する)
     * @param cancelTextId キャンセルボタンのラベルのリソースID (ボタンを表示しない場合は 0を指定する)
     * @param requestCode Dialogの イベントリスナーに返されるリクエストコード
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showCustomDialog(final Activity activity, int titleId, int messageId, int layoutId, int okTextId, int cancelTextId, int requestCode) {
        String title = getString(activity, titleId);
        String message = getString(activity, messageId);
        String buttonOk = getString(activity, okTextId);
        String buttonCancel = getString(activity, cancelTextId);
        return showCustomDialog(activity, title, message, layoutId, buttonOk, buttonCancel, requestCode);
    }


    ////////////////////////////////////
    //
    // プログレスダイアログ
    //

    /**
     * OK/Cancel/Neutralの ボタンを持つダイアログを表示する
     * @param activity 呼び出すActivity
     * @param title ダイアログのタイトル文字列 (表示しない場合は nullを指定する)
     * @param message メッセージ文字列 (表示しない場合は nullを指定する)
     * @param requestCode Dialogの イベントリスナーに返されるリクエストコード
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showProgressDialog(Activity activity, String title, String message, int requestCode) {
        DialogFragment dialog = BaseDialogFragment.newInstance(requestCode, title, message);
        dialog.show(activity.getFragmentManager(), buildTag(requestCode));
        return dialog;
    }

    /**
     * OKボタンのみの メッセージダイアログを表示する
     * @param activity 呼び出すActivity
     * @param title ダイアログのタイトル文字列 (表示しない場合は nullを指定する)
     * @param message メッセージ文字列 (表示しない場合は nullを指定する)
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showProgressDialog(Activity activity, String title, String message) {
        return showProgressDialog(activity, title, message, RC_NO_LISTENER);
    }

    /**
     * OKボタンのみの メッセージダイアログを表示する
     * @param activity 呼び出すActivity
     * @param titleId ダイアログのタイトルのリソースID (表示しない場合は 0を指定する)
     * @param messageId メッセージのリソースID (表示しない場合は 0を指定する)
     * @return 生成されたDialogFragmentオブジェクト
     */
    public static DialogFragment showProgressDialog(Activity activity, int titleId, int messageId) {
        String title = getString(activity, titleId);
        String message = getString(activity, messageId);
        return showProgressDialog(activity, title, message);
    }

    /**
     * Dialog作成時に与えたリクエストコードを指定して Dialogを 閉じる
     * @param activity 呼び出すActivity
     * @param requestCode 作成時に設定した リクエストコード
     */
    public static void dismissDialog(Activity activity, int requestCode) {
        FragmentManager manager = activity.getFragmentManager();
        DialogFragment dialog = (DialogFragment) manager.findFragmentByTag(buildTag(requestCode));
        if (dialog != null) dialog.dismissAllowingStateLoss();
    }


    ////////////////////////////////////
    //
    // 基本DialogFragmentクラス
    //

    public static class BaseDialogFragment extends DialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnCancelListener, DialogInterface.OnShowListener {
        private final static int LAYOUT_ID_DEFAULT = -1;
        private final static float DIP_PADDING_PROGRESS = 15.0f;

        private AlertDialog mAlertDialog = null;
        private int mStyle = STYLE_ALERT_DIALOG;
        private View mCustomView = null;
        private DialogEventListener mListener = null;
        private int mRequestCode;
        private String[] mChoiceList;


        /**
         * メッセージと 標準ボタンのみのDialogを作成する
         * @param requestCode コールバック用Listenerに返す識別値。どのダイアログからのコールバックかを識別する
         * @param title Dialogのタイトル
         * @param message Dialogのメッセージ
         * @param labelOk Positiveボタンの表記
         * @param labelCancel Negativeボタンの表記
         * @param labelNeutral Neutralボタンの表記
         * @return DialogFragmentのインスタンス
         */
        public static BaseDialogFragment newInstance(int requestCode, String title, String message, String labelOk, String labelCancel, String labelNeutral) {
            BaseDialogFragment dialogFragment = new BaseDialogFragment();
            Bundle args = new Bundle();
            args.putInt(KEY_REQUEST_CODE, requestCode);
            args.putString(KEY_DIALOG_TITLE, title);
            args.putString(KEY_DIALOG_TEXT, message);
            args.putString(KEY_DIALOG_POSITIVE_BUTTON_TEXT, labelOk);
            args.putString(KEY_DIALOG_NEGATIVE_BUTTON_TEXT, labelCancel);
            args.putString(KEY_DIALOG_NEUTRAL_BUTTON_TEXT, labelNeutral);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }

        /**
         * SingleChoiceリストを持つDialogを作成する
         * @param requestCode コールバック用Listenerに返す識別値。どのダイアログからのコールバックかを識別する
         * @param title Dialogのタイトル
         * @param selectArray 選択用リスト
         * @param selected 初期選択位置。未選択の場合は -1を渡す
         * @param labelOk Positiveボタンの表記
         * @param labelCancel Negativeボタンの表記
         * @return DialogFragmentのインスタンス
         */
        public static BaseDialogFragment newInstance(int requestCode, String title, String[] selectArray, int selected, String labelOk, String labelCancel) {
            BaseDialogFragment dialogFragment = new BaseDialogFragment();
            Bundle args = new Bundle();
            args.putInt(KEY_REQUEST_CODE, requestCode);
            args.putString(KEY_DIALOG_TITLE, title);
            args.putStringArray(KEY_CHOICE_ARRAY, selectArray);
            args.putInt(KEY_CHOICE_SELECTED, selected);
            args.putString(KEY_DIALOG_POSITIVE_BUTTON_TEXT, labelOk);
            args.putString(KEY_DIALOG_NEGATIVE_BUTTON_TEXT, labelCancel);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }

        /**
         * Layoutを指定してDialogを作成する
         * @param requestCode コールバック用Listenerに返す識別値。どのダイアログからのコールバックかを識別する
         * @param title Dialogのタイトル
         * @param message Dialogのメッセージ
         * @param layoutId カスタムViewの layout ID
         * @param labelOk Positiveボタンの表記
         * @param labelCancel Negativeボタンの表記
         * @return DialogFragmentのインスタンス
         */
        public static BaseDialogFragment newInstance(int requestCode, String title, String message, int layoutId, String labelOk, String labelCancel) {
            BaseDialogFragment dialogFragment = new BaseDialogFragment();
            Bundle args = new Bundle();
            args.putInt(KEY_REQUEST_CODE, requestCode);
            args.putString(KEY_DIALOG_TITLE, title);
            args.putString(KEY_DIALOG_TEXT, message);
            args.putInt(KEY_DIALOG_LAYOUT, layoutId);
            args.putString(KEY_DIALOG_POSITIVE_BUTTON_TEXT, labelOk);
            args.putString(KEY_DIALOG_NEGATIVE_BUTTON_TEXT, labelCancel);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }

        /**
         * ProgressDialog風の Dialogを作成する。
         * (API level 26で ProgressDialogが 非推奨になったので、互換部品を用意)
         * @param requestCode コールバック用Listenerに返す識別値。どのダイアログからのコールバックかを識別する
         * @param title Dialogのタイトル
         * @param message Dialogのメッセージ
         * @return DialogFragmentのインスタンス
         */
        public static BaseDialogFragment newInstance(int requestCode, String title, String message) {
            BaseDialogFragment dialogFragment = new BaseDialogFragment();
            Bundle args = new Bundle();
            args.putInt(KEY_REQUEST_CODE, requestCode);
            args.putString(KEY_DIALOG_TITLE, title);
            args.putString(KEY_DIALOG_TEXT, message);
            args.putInt(KEY_PROGRESS_STYLE, STYLE_PROGRESS_DIALOG);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            if (activity instanceof DialogEventListener) {
                mListener = (DialogEventListener) activity;
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mRequestCode = getArguments().getInt(KEY_REQUEST_CODE);
            mStyle = getArguments().getInt(KEY_PROGRESS_STYLE, STYLE_ALERT_DIALOG);
            String title = getArguments().getString(KEY_DIALOG_TITLE);
            String message = getArguments().getString(KEY_DIALOG_TEXT);
            String buttonOk = getArguments().getString(KEY_DIALOG_POSITIVE_BUTTON_TEXT);
            String buttonCancel = getArguments().getString(KEY_DIALOG_NEGATIVE_BUTTON_TEXT);
            String buttonNeutral = getArguments().getString(KEY_DIALOG_NEUTRAL_BUTTON_TEXT);

            mChoiceList = getArguments().getStringArray(KEY_CHOICE_ARRAY);
            int selected = getArguments().getInt(KEY_CHOICE_SELECTED, LIST_TYPE_NOT_SELECTED);
            int layoutId = getArguments().getInt(KEY_DIALOG_LAYOUT, LAYOUT_ID_DEFAULT);
            LayoutInflater inflater;

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            if (title != null) builder.setTitle(title);
            if (mStyle == STYLE_PROGRESS_DIALOG) {
                builder.setView(buildProgressDialog(getActivity(), message));
                // messageは ProgressDialog(風の)Viewで表示するので、AlertDialogオリジナルの setMessage()を明示的にスキップする。
                message = null;

            } else if (layoutId != LAYOUT_ID_DEFAULT
                    && (inflater = (LayoutInflater)getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE)) != null) {
                // カスタムViewを設定
                mCustomView = inflater.inflate(layoutId, null);
                builder.setView(mCustomView);

            } else if (mChoiceList != null) {
                // 選択リストを設定
                if (selected == LIST_TYPE_NO_CHECKBOX) {
                    // checkboxなしListViewを表示
                    builder.setItems(mChoiceList, this);
                } else {
                    // checkboxつきListViewを表示
                    builder.setSingleChoiceItems(mChoiceList, selected, this);
                }
                // setMessage()を実行すると setSingleChoiceItems()が無視されるので、
                // setSingleChoiceItems()を行った場合は、setMessage()を明示的にスキップする様にする。
                message = null;
            }
            if (message != null) builder.setMessage(message);   // messageと カスタムViewは両立する

            // ダイアログボタンを設定
            if (buttonOk != null) builder.setPositiveButton(buttonOk, this);
            if (buttonCancel != null) builder.setNegativeButton(buttonCancel, this);
            if (buttonNeutral != null) builder.setNeutralButton(buttonNeutral, this);
            this.setCancelable(buttonCancel != null);        // キャンセルボタンがない場合は、Backキーによるキャンセルも無効
            mAlertDialog = builder.create();

            mAlertDialog.setCanceledOnTouchOutside(false);
            // Dialogが createされた事をListenerに通知する。(主にカスタムViewの初期化処理のため)
            callbackToListener(EVENT_DIALOG_CREATED);
            mAlertDialog.setOnShowListener(this);

            return mAlertDialog;
        }

        @Override
        public void dismiss() {
            dismissAllowingStateLoss();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            callbackToListener(which);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            callbackToListener(EVENT_BUTTON_NEGATIVE);
        }

        private void callbackToListener(int which) {
            if (mListener == null) return;

            View viewCallback = null;
            // Dialogの形態によって、callbackで返す オブジェクトを切り替える
            if (mCustomView != null) {
                // カスタムlayoutが指定されている場合は、そのViewを返す
                viewCallback = mCustomView;

            } else if (mChoiceList != null) {
                viewCallback = mAlertDialog.getListView();
            }
            mListener.onDialogEvent(mRequestCode, mAlertDialog, which, viewCallback);
        }

        @Override
        public void onShow(DialogInterface dialogInterface) {
            callbackToListener(EVENT_DIALOG_SHOWN);
        }

        private View buildProgressDialog(Context context, String message) {
            int pxPadding = (int)(DIP_PADDING_PROGRESS * getResources().getDisplayMetrics().density);

            // ダイアログのレイアウトを設定
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setVerticalGravity(Gravity.CENTER_VERTICAL);
            layout.setPadding(pxPadding, pxPadding, pxPadding, pxPadding);

            // プログレス表示 設定
            ProgressBar progress = new ProgressBar(context);
            layout.addView(progress);

            // メッセージ 設定
            TextView tv = new TextView(context);
            tv.setText(message);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tv.setPadding(pxPadding, 0, 0, 0);          // progress表示と メッセージの間もマージンをあける
            layout.addView(tv);

            return layout;
        }
    }


    //////////////////////////
    //
    // private functions

    private static String getString(Context context, int resourceId) {
        if (resourceId <= 0) return null;
        return context.getString(resourceId);
    }

    private static String buildTag(int requestCode) {
        return TAG_PREFIX + Integer.toHexString(requestCode);
    }

}
