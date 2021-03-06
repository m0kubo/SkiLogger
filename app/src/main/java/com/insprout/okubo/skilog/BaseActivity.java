package com.insprout.okubo.skilog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.net.Uri;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.model.ResponsePlaceData;
import com.insprout.okubo.skilog.model.TagDb;
import com.insprout.okubo.skilog.setting.Const;
import com.insprout.okubo.skilog.util.AppUtils;
import com.insprout.okubo.skilog.util.DialogUi;
import com.insprout.okubo.skilog.util.SdkUtils;
import com.insprout.okubo.skilog.util.UiUtils;
import com.insprout.okubo.skilog.webapi.RequestUrlTask;
import com.insprout.okubo.skilog.webapi.WebApiUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity implements View.OnClickListener, DialogUi.DialogEventListener {
    protected final static int RP_LOCATION = 100;
    protected final static int RP_CONTENTS = 101;

    private final static int RC_DELETE_LOG = 100;
    private final static int RC_SELECT_TAG = 200;
    private final static int RC_LIST_TAG = 201;
    private final static int RC_ADD_TAG = 202;
    private final static int RC_ADD_TAG_INPUT = 300;
    private final static int RC_ADD_TAG_SELECTION = 301;
    private final static int RC_DELETE_TAG = 400;
    final static int RC_DIALOG_PHOTO = 901;


    ///////////////////////////
    private List<TagDb> mTagsOnTarget;
    private Date mTargetDate = null;
    private TagDb mTargetTag = null;
    private List<Uri> mPhotoList = null;
    private List<TagDb> mAllTags;
    private int mIndexTag = -1;


    @Override
    public void onResume() {
        super.onResume();
        SdkUtils.requestRuntimePermissions(this, Const.PERMISSIONS_CONTENTS, RP_CONTENTS);
    }

    protected Date getTargetDate() {
        return null;
    }


    protected void notifyChartChanged() {
    }

    protected void notifyLogsDeleted(Date deletedLogDate) {
    }

    protected void notifyTagAdded(TagDb tag) {
    }

    protected void notifyTagRemoved(TagDb tag) {
    }

    protected void notifyFilterSpecified(String filter) {
    }

    protected void onInitialize() {
        setupFilteringTag();
    }


    protected void startLineChartActivity() {
        UiUtils.setSelected(this, R.id.btn_chart1, true);
        UiUtils.setSelected(this, R.id.btn_chart2, false);
        Date target = getTargetDate();
        if (target != null) {
            Toast.makeText(
                    this,
                    getString(R.string.fmt_toast_daily_chart, AppUtils.toDateString(target)),
                    Toast.LENGTH_SHORT
            ).show();
        }
        LineChartActivity.startActivity(this, target);
        finish();
    }


    private void setupFilteringTag() {
        mAllTags = AppUtils.getTags(this);
        UiUtils.setEnabled(this, R.id.btn_tag, (mAllTags != null && !mAllTags.isEmpty()));
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch(id) {
            case R.id.btn_back:
                finish();
                break;

            case R.id.btn_chart1:
                startLineChartActivity();
                break;

            case R.id.btn_chart2:
                // サマリー画面表示
                UiUtils.setSelected(this, R.id.btn_chart1, false);
                UiUtils.setSelected(this, R.id.btn_chart2, true);
                BarChartActivity.startActivity(this);
                finish();
                break;

            case R.id.btn_tag:
                selectTag();
                break;
        }
    }



    ////////////////////////////////////////////////////////////////////
    //
    // Optionメニュー関連
    //

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.line_chart, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // 削除メニューの状態を設定
        mTargetDate = getTargetDate();
        if (mTargetDate != null) {
            UiUtils.setTitle(menu, R.id.menu_target_date, getString(R.string.menu_targeted_fmt, AppUtils.toDateString(mTargetDate)));
            mTagsOnTarget = DbUtils.selectTags(this, mTargetDate);

        } else {
            UiUtils.setTitle(menu, R.id.menu_target_date, R.string.menu_not_targeted);
            mTagsOnTarget = null;
        }
        UiUtils.setEnabled(menu, R.id.menu_delete_logs, (mTargetDate != null));
        UiUtils.setEnabled(menu, R.id.menu_list_tags, (mTagsOnTarget != null && !mTagsOnTarget.isEmpty()));
        UiUtils.setEnabled(menu, R.id.menu_add_tags, (mTargetDate != null));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            // タイトルバーの backボタン処理
            case android.R.id.home:
                finish();
                return true;

            case R.id.menu_delete_logs:
                confirmDeleteLogs();
                return true;

            case R.id.menu_list_tags:
                listTags();
                return true;

            case R.id.menu_add_tags:
                showAddTagDialog();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void confirmDeleteLogs() {
        if (mTargetDate == null) return;

        // データ削除
        String title = getString(R.string.title_delete_logs);
        String message = getString(R.string.fmt_delete_daily_logs,  AppUtils.toDateString(mTargetDate));
        new DialogUi.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton()
                .setNegativeButton()
                .setRequestCode(RC_DELETE_LOG)
                .show();
    }

    private void deleteLogs() {
        if (mTargetDate == null) return;       // 念のためチェック

        // 指定日のログをDBから削除する
        boolean res = DbUtils.deleteLogs(this, mTargetDate);
        if (res) {
            // 指定された日に紐づいたタグ情報もDBから削除する
            DbUtils.deleteTags(this, mTargetDate);

            // チャートの表示を更新する
            notifyLogsDeleted(mTargetDate);
        }
    }

    private void showAddTagDialog() {
        String[] submenu = getResources().getStringArray(R.array.menu_add_tag);
        new DialogUi.Builder(this)
                .setItems(submenu)
                .setNegativeButton(R.string.btn_cancel)
                .setRequestCode(RC_ADD_TAG)
                .show();
    }

    private void listTags() {
        // 付与されているタグ一覧メニュー
        List<TagDb> tagsOnTarget = DbUtils.selectTags(this, mTargetDate);
        if (tagsOnTarget == null || tagsOnTarget.isEmpty()) return;

        // 選択用リストを作成
        String[] arrayTag = AppUtils.toStringArray(mTagsOnTarget);
        String title = getString(R.string.fmt_title_list_tags, AppUtils.toDateString(mTargetDate));
        new DialogUi.Builder(this)
                .setTitle(title)
                .setSingleChoiceItems(arrayTag, -1)
                .setPositiveButton(R.string.btn_delete)
                .setNegativeButton(R.string.btn_close)
                .setRequestCode(RC_LIST_TAG)
                .show();
    }

    private void inputTag() {
        new DialogUi.Builder(this)
                .setTitle(R.string.title_input_tag)
                .setView(R.layout.dlg_edittext)
                .setPositiveButton(R.string.btn_ok)
                .setNegativeButton(R.string.btn_cancel)
                .setRequestCode(RC_ADD_TAG_INPUT)
                .show();
    }

//    private void showPhotoViewDialog(List<Uri> photos) {
//        mPhotoList = photos;
//        if (photos != null && photos.size() > 0) {
//            new DialogUi.Builder(this)
//                    .setMessage(getString(R.string.fmt_photo_viewer, photos.size()))
//                    .setPositiveButton()
//                    .setNegativeButton()
//                    .setRequestCode(RC_DIALOG_PHOTO)
//                    .show();
//        }
//    }

    private void selectTagFromHistory() {
        List<TagDb> tagsCandidate = AppUtils.getTags(this, mTagsOnTarget);
        if (tagsCandidate == null || tagsCandidate.isEmpty()) {
            Toast.makeText(this, R.string.msg_no_more_tags, Toast.LENGTH_SHORT).show();

        } else {
            new DialogUi.Builder(this)
                    .setTitle(R.string.title_input_tag)
                    .setSingleChoiceItems(AppUtils.toStringArray(tagsCandidate), -1)
                    .setPositiveButton(R.string.btn_ok)
                    .setNegativeButton(R.string.btn_cancel)
                    .setRequestCode(RC_ADD_TAG_SELECTION)
                    .show();
        }
    }

    // 絞り込み用 タグ選択ダイアログ表示
    private void selectTag() {
        // tag一覧
        // tagがない場合 ボタン無効になっている筈だが念のためチェック
        if (mAllTags == null || mAllTags.isEmpty()) {
            return;
        }
        // 選択用リストを作成
        String[] arrayTag = new String[ mAllTags.size() + 1 ];
        for (int i = 0; i< mAllTags.size(); i++) {
            arrayTag[i] = mAllTags.get(i).getTag();
        }
        arrayTag[ arrayTag.length - 1 ] = getString(R.string.menu_reset_tag);
        new DialogUi.Builder(this)
                .setTitle(R.string.title_select_tag)
                .setSingleChoiceItems(arrayTag, mIndexTag)
                .setPositiveButton()
                .setNegativeButton()
                .setRequestCode(RC_SELECT_TAG)
                .show();
    }


    private void measureLocation() {
        if (!LocationProvider.isEnabled(this)) {
            Toast.makeText(BaseActivity.this, R.string.msg_gps_not_available, Toast.LENGTH_SHORT).show();
            // タグ追加のダイアログを再表示しておく
            showAddTagDialog();
            return;
        }

        if (!SdkUtils.requestRuntimePermissions(this, Const.PERMISSIONS_LOCATION, RP_LOCATION)) {
            // 権限がない場合、続きは onRequestPermissionsResult()から継続
            return;
        }

        final DialogFragment dialog = new DialogUi.Builder(this, DialogUi.STYLE_PROGRESS_DIALOG).setMessage(R.string.msg_getting_tags).show();
        new LocationProvider(this, new LocationProvider.OnLocatedListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (location == null) {
                    dialog.dismissAllowingStateLoss();
                    Toast.makeText(BaseActivity.this, R.string.msg_fail_to_google_place_api, Toast.LENGTH_SHORT).show();

                } else {
                    WebApiUtils.googlePlaceApi(getString(R.string.google_maps_key), location, new RequestUrlTask.OnResponseListener() {
                        @Override
                        public void onResponse(String responseBody) {
                            dialog.dismissAllowingStateLoss();
                            if (responseBody == null) return;
                            ResponsePlaceData places = ResponsePlaceData.fromJson(responseBody);
                            if (places == null || places.getPlaces() == null) {
                                // api取得失敗
                                Toast.makeText(BaseActivity.this, R.string.msg_fail_to_google_place_api, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (places.getPlaces().isEmpty()) {
                                // 0件
                                Toast.makeText(BaseActivity.this, R.string.msg_no_place_tags, Toast.LENGTH_SHORT).show();
                                return;
                            }

                            new DialogUi.Builder(BaseActivity.this)
                                    .setTitle(R.string.title_input_tag)
                                    .setSingleChoiceItems(AppUtils.toStringArray(places.getPlaces(), Const.MAX_TAG_CANDIDATE_BY_LOCATION), -1)
                                    .setPositiveButton(R.string.btn_ok)
                                    .setNegativeButton(R.string.btn_cancel)
                                    .setRequestCode(RC_ADD_TAG_SELECTION)
                                    .show();
                        }
                    }).execute();

                }
            }
        }
        ).requestMeasure();
    }


    @Override
    public void onDialogEvent(int requestCode, AlertDialog dialog, int which, View view) {
        switch (requestCode) {
            case RC_DELETE_LOG:
                if (which == DialogUi.EVENT_BUTTON_POSITIVE) {
                    deleteLogs();
                }
                break;

            case RC_ADD_TAG:
                switch(which) {
                    case 0:
                        inputTag();
                        break;
                    case 1:
                        selectTagFromHistory();
                        break;
                    case 2:
                        measureLocation();
                        break;
                }
                break;

            case RC_LIST_TAG:
                // タグ一覧ダイアログのコールバック
                if (view instanceof ListView) {      // 念のためチェック
                    int pos = ((ListView)view).getCheckedItemPosition();
                    switch (which) {
                        case DialogUi.EVENT_BUTTON_POSITIVE:
                            // 削除ボタンが押された
                            if (pos >= 0 && pos < mTagsOnTarget.size()) {
                                mTargetTag = mTagsOnTarget.get(pos);
                                // 確認ダイアログを表示する
                                new DialogUi.Builder(this)
                                        .setTitle(R.string.msg_delete_tags)
                                        .setMessage(mTargetTag.getTag())
                                        .setPositiveButton()
                                        .setNegativeButton()
                                        .setRequestCode(RC_DELETE_TAG)
                                        .show();
                            }
                            break;

                        default:
                            // 選択状態によって OKボタンを有効化/無効化する
                            Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                            if (button != null) button.setEnabled(pos >= 0);    // EVENT_DIALOG_CREATEDの時点では buttonはまだnullなので注意
                            break;
                    }
                }
                break;

            case RC_DELETE_TAG:
                // タグ削除
                switch(which) {
                    case DialogUi.EVENT_BUTTON_POSITIVE:
                        // 対象のタグデータをDBから削除する
                        if (mTargetTag != null) {
                            boolean result = DbUtils.deleteTag(this, mTargetTag);
                            if (result) {
                                String msg = getString(R.string.fmt_msg_deleted_tags,  AppUtils.toDateString(mTargetTag.getDate()), mTargetTag.getTag());
                                Toast.makeText(this, msg ,Toast.LENGTH_SHORT).show();
                                setupFilteringTag();

                                // 削除されたタグが絞り込み表示に指定されていた場合は、チャートを再描画する
                                notifyTagRemoved(mTargetTag);
                            }
                        }
                        mTargetTag = null;
                        break;

                    case DialogUi.EVENT_BUTTON_NEGATIVE:
                        // タグ削除キャンセル
                        mTargetTag = null;
                        break;
                }
                break;

            case RC_ADD_TAG_INPUT:
                // タグ直接入力
                EditText editText = ((View)view).findViewById(R.id._et_dlg);
                switch (which) {
                    case DialogUi.EVENT_DIALOG_SHOWN:
                        // キーボードを自動で開く
                        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (inputMethodManager != null) inputMethodManager.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
                        break;

                    case DialogUi.EVENT_BUTTON_POSITIVE:
                        // 入力されたタグを登録
                        if (editText != null) {
                            String tag = editText.getText().toString();
                            if (!tag.isEmpty() && mTargetDate != null) {
                                TagDb newTag = new TagDb(mTargetDate, tag);
                                DbUtils.insertTag(this, newTag);

                                // 絞り込み用のタグリスト再取得
                                setupFilteringTag();
                                notifyTagAdded(newTag);
                            }
                        }
                        break;
                }
                break;

            case RC_ADD_TAG_SELECTION:
                // 履歴から選択
                if (view instanceof ListView) {      // 念のためチェック
                    int pos = ((ListView) view).getCheckedItemPosition();

                    switch (which) {
                        case DialogUi.EVENT_DIALOG_SHOWN:
                            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(pos >= 0);
                            break;

                        case DialogUi.EVENT_BUTTON_POSITIVE:
                            // 入力されたタグを登録
                            String tag = ((ListView) view).getAdapter().getItem(pos).toString();
                            if (!tag.isEmpty() && mTargetDate != null) {
                                TagDb newTag = new TagDb(mTargetDate, tag);
                                DbUtils.insertTag(this, newTag);

                                // 絞り込み用のタグリスト再取得
                                setupFilteringTag();
                                notifyTagAdded(newTag);
                            }
                            break;

                        default:
                            if (pos >= 0) {
                                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                            }
                            break;
                    }
                }
                break;

            case RC_SELECT_TAG:
                if (which == DialogUi.EVENT_BUTTON_POSITIVE) {
                    // 絞り込み処理 実行
                    if (view instanceof ListView) {
                        mIndexTag = -1;
                        String filter = null;
                        int pos = ((ListView)view).getCheckedItemPosition();
                        if (mAllTags != null && pos >= 0 && pos < mAllTags.size()) {
                            mIndexTag = pos;
                            filter = mAllTags.get(mIndexTag).getTag();
                        }
                        // チャートの表示を更新する
                        notifyFilterSpecified(filter);
                    }
                }
                break;


            case RC_DIALOG_PHOTO:
                if (which == DialogUi.EVENT_BUTTON_POSITIVE) {
                    if (mPhotoList != null && mPhotoList.size() >= 1) {
                        PhotoViewerActivity.startActivity(BaseActivity.this, (ArrayList<Uri>) mPhotoList);
                    }
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RP_LOCATION:
                // PERMISSIONが すべて付与されたか確認する
                if (!SdkUtils.isGranted(grantResults)) {
                    // 必要な PERMISSIONは付与されなかった
                    // タグ追加方法選択に戻す
                    showAddTagDialog();
                    return;
                }
                measureLocation();
                return;

            case RP_CONTENTS:
                if (SdkUtils.isGranted(grantResults)) {
                    notifyChartChanged();
                }
                break;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


}
