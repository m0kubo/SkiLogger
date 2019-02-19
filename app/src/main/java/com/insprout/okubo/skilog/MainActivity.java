package com.insprout.okubo.skilog;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.insprout.okubo.skilog.database.DbUtils;
import com.insprout.okubo.skilog.setting.Const;
import com.insprout.okubo.skilog.setting.Settings;
import com.insprout.okubo.skilog.util.DialogUi;
import com.insprout.okubo.skilog.util.SdkUtils;
import com.insprout.okubo.skilog.util.SensorUtils;
import com.insprout.okubo.skilog.util.UiUtils;

import java.io.File;
import java.io.FileFilter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, DialogUi.DialogEventListener {
    private final static int RC_CHANGE_THEME = 1;
    private final static int RC_DATA_EXPORT = 2;
    private final static int RC_DATA_IMPORT = 3;
    private final static int RC_DATA_DELETE = 4;
    private final static int RC_DATA_IMPORT_CONFIRM = 5;
    private final static int RC_DATA_DELETE_CONFIRM = 6;
    private final static int RC_PROGRESS_DIALOG = 10;
//    private final static int RC_FINISH_APP = 101;

    private ServiceMessenger mServiceMessenger;

    private Sensor mSensor = null;
    private TextView mTvAltitude;
    private TextView mTvTotalAsc;
    private TextView mTvTotalDesc;
    private TextView mTvCount;

    private int mThemeIndex;
    private String[] mThemeArray;
    private File mBackupFolder = null;
    private File[] mBackupFolderList = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(Settings.getThemeStyle(this));
        setContentView(R.layout.activity_main);

        initVars();                                             // 変数などの初期化
        initView();                                             // View関連の初期化

        mSensor = SensorUtils.getPressureSensor(this);
        if (mSensor == null) {
            // 気圧センサーがない場合はアプリ終了
            new AlertDialog.Builder(this)
                    .setTitle(R.string.title_missing_sensor)
                    .setMessage(R.string.msg_missing_sensor)
                    .setCancelable(false)
                    .setPositiveButton(R.string.btn_finish_app, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // アプリ終了
                            finish();
                        }
                    })
                    .show();
        }
    }

    private void initVars() {
        SkiLogService.registerNotifyChannel(this);              // Android8.0対応 notificationチャンネルの登録

        mThemeArray = getResources().getStringArray(R.array.app_themes);

        // Serviceプロセスとの 通信クラス作成
        mServiceMessenger = new ServiceMessenger(this, new ServiceMessenger.OnServiceMessageListener() {
            @Override
            public void onReceiveMessage(Message msg) {
                switch (msg.what) {
                    case ServiceMessenger.MSG_REPLY_LONG_ARRAY:
                        long[] data = (long[]) msg.obj;
                        mTvAltitude.setText(formattedAltitude(data[1]));
                        mTvTotalAsc.setText(formattedAltitude(data[2]));
                        mTvTotalDesc.setText(formattedAltitude(-data[3]));
                        mTvCount.setText(String.valueOf(data[4]));
                        break;
                }
            }
        });
    }

    private void initView() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.drawable.ic_settings);
        }
        UiUtils.setDrawables(this, R.id.btn_positive, R.drawable.ic_record, R.drawable.bg_circle2_large);
        UiUtils.setDrawables(this, R.id.btn_negative, R.mipmap.ic_pause_white_36dp, R.drawable.bg_circle2_large);

        mTvAltitude = findViewById(R.id.tv_altitude);
        mTvTotalAsc = findViewById(R.id.tv_total_asc);
        mTvTotalDesc = findViewById(R.id.tv_total_desc);
        mTvCount = findViewById(R.id.tv_count_lift);
    }


    @Override
    public void onResume() {
        super.onResume();

        // サービスの実行状況に合わせて、Messengerや ボタンなどを設定する
        boolean svcRunning = SkiLogService.isRunning(this);
        if (svcRunning) mServiceMessenger.bind();
        updateUi(svcRunning);
    }

    @Override
    public void onPause() {
        mServiceMessenger.unbind();
        super.onPause();
    }

// 確認ダイアログを出さずに、そのまま終了するように仕様変更
//    @Override
//    public void onBackPressed() {
//        // アプリ終了確認ダイアログを出す
//        new DialogUi.Builder(this)
//                .setMessage(R.string.msg_close_application)
//                .setPositiveButton(R.string.btn_finish_app)
//                .setNegativeButton(R.string.btn_cancel)
//                .setRequestCode(RC_FINISH_APP)
//                .show();
//    }

    private void startService() {
        Log.d("WatchService", "startService()");
        updateUi(true);
        UiUtils.setText(this, R.id.tv_status, R.string.label_status_starting);
        SkiLogService.startService(this);

        try {
            mServiceMessenger.bind();
        } catch(Exception e) {
            updateUi(false);
        }
        // 不足のエラーで Serviceが開始できなかった場合のために、一定時間後にServiceの起動状態を確認して ボタンに反映する
        confirmServiceStatus();
    }

    private void stopService() {
        Log.d("WatchService", "stopService()");
        updateUi(false);
        SkiLogService.stopService(this);

        mServiceMessenger.unbind();
    }


    // ミリメートル単位の高度値を 表示用の文字列に変換する
    private String formattedAltitude(long altitude) {
        return getString(R.string.fmt_meter, altitude * 0.001f);
    }

    private void updateUi(boolean serviceRunning) {
        // ステータス表示
        UiUtils.setText(this, R.id.tv_status, (serviceRunning ? R.string.label_status_start : R.string.label_status_stop));

        // サービス起動・停止ボタンの 有効無効
        UiUtils.setEnabled(this, R.id.btn_positive, (mSensor!=null && !serviceRunning));
        UiUtils.setEnabled(this, R.id.btn_negative, (mSensor!=null && serviceRunning));

        boolean  readyData = serviceRunning || (DbUtils.countLogs(this) >= 1);
        updateChartButton(this, readyData);
    }

    private static void updateChartButton(Activity activity, boolean logsReady) {
        UiUtils.setEnabled(activity, R.id.btn_chart1, logsReady);
        UiUtils.setEnabled(activity, R.id.btn_chart2, logsReady);
        UiUtils.setSelected(activity, R.id.btn_chart1, false);
        UiUtils.setSelected(activity, R.id.btn_chart2, false);
    }

    private void confirmServiceStatus() {
        // 不測のエラーで Serviceが開始できなかった場合のために、一定時間後にServiceの起動状態を確認して ボタンに反映する
        new Handler().postDelayed(new Runnable() {
             @Override
             public void run() {
                 boolean isRunning = SkiLogService.isRunning(MainActivity.this);
                 updateUi(isRunning);
             }
        },
        1000);
    }


    // タイトルメニュー用 設定

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_change_theme:
                mThemeIndex = Settings.getThemeIndex(this);
                new DialogUi.Builder(this)
                        .setTitle(R.string.menu_theme)
                        .setSingleChoiceItems(mThemeArray, mThemeIndex)
                        .setPositiveButton()
                        .setNegativeButton()
                        .setRequestCode(RC_CHANGE_THEME)
                        .show();
                return true;

            case R.id.menu_export_data:
                if (SdkUtils.requestRuntimePermissions(this, Const.PERMISSIONS_EXPORT, RC_DATA_EXPORT)) {
                    // 権限がない場合、続きは onRequestPermissionsResult()から継続
                    showExportDialog();
                }
                return true;

            case R.id.menu_import_data:
                if (SdkUtils.requestRuntimePermissions(this, Const.PERMISSIONS_EXPORT, RC_DATA_EXPORT)) {
                    // 権限がない場合、続きは onRequestPermissionsResult()から継続
                    showImportDialog();
                }
                return true;

            case R.id.menu_delete_data:
                if (SdkUtils.requestRuntimePermissions(this, Const.PERMISSIONS_EXPORT, RC_DATA_DELETE)) {
                    // 権限がない場合、続きは onRequestPermissionsResult()から継続
                    showDeleteBackupDialog();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showExportDialog() {
        mBackupFolder = newBackupFolder();
        new DialogUi.Builder(this)
                .setTitle(R.string.menu_export)
                .setMessage(getString(R.string.msg_export_log_fmt, mBackupFolder.toString()))
                .setPositiveButton()
                .setNegativeButton()
                .setRequestCode(RC_DATA_EXPORT)
                .show();
    }

    private void showImportDialog() {
        // ログ記録サービス実行中なら警告メッセージを表示
        if (SkiLogService.isRunning(this)) {
            new DialogUi.Builder(this)
                    .setMessage(R.string.wrn_import_svc_running)
                    .setPositiveButton()
                    .show();
            return;
        }

        mBackupFolderList = getExportedFolderList();
        if (mBackupFolderList == null) return;

        new DialogUi.Builder(this)
                .setTitle(R.string.msg_select_import_log)
                .setSingleChoiceItems(toStringArray(mBackupFolderList))
                .setPositiveButton()
                .setNegativeButton()
                .setRequestCode(RC_DATA_IMPORT)
                .show();
    }

    private void showDeleteBackupDialog() {
        mBackupFolderList = getExportedFolderList();
        if (mBackupFolderList == null) return;

        new DialogUi.Builder(this)
                .setTitle(R.string.msg_select_delete_backup)
                .setSingleChoiceItems(toStringArray(mBackupFolderList))
                .setPositiveButton()
                .setNegativeButton()
                .setRequestCode(RC_DATA_DELETE)
                .show();
    }

    private File[] getExportedFolderList() {
        File[] exportedFolders = getExportRoot().listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                // ディレクトリで、その直下に logs.csvファイルが存在するものをexportされたディレクトリとみなす
                if (!file.isDirectory()) return false;
                // 指定のフォルダ内に ログ情報のCSVファイルがあるかも確認する
                File csvFile = new File(file, Const.FILENAME_LOGS_CSV);
                return csvFile.exists() && csvFile.isFile();
            }
        });
        if (exportedFolders == null || exportedFolders.length == 0) {
            // エクスポートされたデータがない場合
            showNoExported();
            return null;
        }
        // フォルダリストを作成時間降順にする
        Arrays.sort(exportedFolders, new Comparator<File>() {
            @Override
            public int compare(File file1, File file2) {
                long timeStamp1 = file1.lastModified();
                long timeStamp2 = file2.lastModified();
                if (timeStamp1 == timeStamp2) return 0;
                return timeStamp1 < timeStamp2 ? 1 : -1;
            }
        });
        return exportedFolders;
    }

    private String[] toStringArray(File[] files) {
        if (files == null || files.length == 0) return new String[0];
        String[] list = new String[files.length];
        for (int i = 0; i<files.length; i++) {
            list[i] = files[i].getName();
        }
        return list;
    }

    private void showNoExported() {
        new DialogUi.Builder(this)
                .setMessage(R.string.msg_no_exported_logs)
                .setPositiveButton()
                .show();
    }


    private File getExportRoot() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), Const.APP_FOLDER_NAME);
    }

    private File newBackupFolder() {
        SimpleDateFormat mDateFormat = new SimpleDateFormat(Const.DATE_FORMAT_EXPORT_FOLDER, Locale.getDefault());
        return new File(getExportRoot(), mDateFormat.format(new Date(System.currentTimeMillis())));
    }

    private void confirmImportData(File backupFolder) {
        if (backupFolder == null) return;
        mBackupFolder = backupFolder;
        String msg = getString(R.string.msg_confirm_import_backup_fmt, mBackupFolder.getName());
        new DialogUi.Builder(this)
                .setMessage(msg)
                .setPositiveButton(R.string.btn_restore)
                .setNegativeButton()
                .setRequestCode(RC_DATA_IMPORT_CONFIRM)
                .show();
    }

    private void confirmDeleteData(File backupFolder) {
        if (backupFolder == null) return;
        mBackupFolder = backupFolder;
        String msg = getString(R.string.msg_confirm_delete_backup_fmt, mBackupFolder.getName());
        new DialogUi.Builder(this)
                .setMessage(msg)
                .setPositiveButton(R.string.btn_delete)
                .setNegativeButton()
                .setRequestCode(RC_DATA_DELETE_CONFIRM)
                .show();
    }

    private void deleteData(File backupFolder) {
        if (deleteFolderWithChildren(backupFolder)) {
            Toast.makeText(this, R.string.msg_delete_backup, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.err_delete_backup, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean deleteFolderWithChildren(File file) {
        // 存在しない場合は処理終了
        if (!file.exists()) return true;

        // 対象がディレクトリの場合は再帰処理
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                if (!deleteFolderWithChildren(child)) return false;
            }
        }
        // 対象がファイルもしくは配下が空のディレクトリの場合は削除する
        return file.delete();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch(id) {
            case R.id.btn_positive:
                startService();
                break;

            case R.id.btn_negative:
                stopService();
                break;

            case R.id.btn_chart1:
                UiUtils.setSelected(this, R.id.btn_chart1, true);
                //ChartPagerActivity.startActivity(this, 1);
                LineChartActivity.startActivity(this);
                break;

            case R.id.btn_chart2:
                UiUtils.setSelected(this, R.id.btn_chart2, true);
                //ChartPagerActivity.startActivity(this, 0);
                BarChartActivity.startActivity(this);
                break;
        }
    }


    @Override
    public void onDialogEvent(int requestCode, AlertDialog dialog, int which, View view) {
        switch(requestCode) {
            case RC_CHANGE_THEME:
                if (which == DialogUi.EVENT_BUTTON_POSITIVE) {
                    if (view instanceof ListView) {
                        int index = ((ListView) view).getCheckedItemPosition();
                        if (index != mThemeIndex) {
                            Settings.putThemeIndex(this, index);

                            Intent intent = new Intent(this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                    }
                }
                break;

            case RC_DATA_EXPORT:
                if (which == DialogUi.EVENT_BUTTON_POSITIVE) {
                    if (mBackupFolder != null) {
                        new BackupTask(this, BackupTask.EXPORT_BACKUP).execute(mBackupFolder);
                    }
                }
                break;

            case RC_DATA_DELETE:
            case RC_DATA_IMPORT:
                if (view instanceof ListView) {
                    int pos = ((ListView) view).getCheckedItemPosition();
                    switch (which) {
                        case DialogUi.EVENT_BUTTON_POSITIVE:
                            if (pos < 0) {
                                Toast.makeText(this, R.string.wrn_no_data_selected, Toast.LENGTH_SHORT).show();
                            } else if (pos < mBackupFolderList.length) {
                                switch (requestCode) {
                                    case RC_DATA_IMPORT:
                                        confirmImportData(mBackupFolderList[pos]);
                                        break;
                                    case RC_DATA_DELETE:
                                        confirmDeleteData(mBackupFolderList[pos]);
                                        break;
                                }
                            }

                        default:
                            // 選択状態によって OKボタンを有効化/無効化する
                            Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                            if (button != null) button.setEnabled(pos >= 0);    // EVENT_DIALOG_CREATEDの時点では buttonはまだnullなので注意
                            break;
                    }
                }
                break;

            case RC_DATA_IMPORT_CONFIRM:
                if (which == DialogUi.EVENT_BUTTON_POSITIVE) {
                    if (mBackupFolder != null && mBackupFolder.isDirectory()) {
                        new BackupTask(this, BackupTask.IMPORT_BACKUP).execute(mBackupFolder);
                    }
                }
                break;

            case RC_DATA_DELETE_CONFIRM:
                if (which == DialogUi.EVENT_BUTTON_POSITIVE) {
                    deleteData(mBackupFolder);
                }
                break;

//            case RC_FINISH_APP:
//                if (which == DialogUi.EVENT_BUTTON_POSITIVE) {
//                    // アプリ終了する
//                    finish();
//                }
//                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case RC_DATA_EXPORT:
                if (SdkUtils.isGranted(grantResults)) {
                    showExportDialog();
                }
                break;

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                break;
        }
    }

    private static class BackupTask extends AsyncTask<File, Void, Void> {
        public final static int EXPORT_BACKUP = 0;
        public final static int IMPORT_BACKUP = 1;

        private WeakReference<Activity> activityReference;
        private int backupType;
        private int importedLogCount = 0;
        private String message = null;

        BackupTask(Activity activity, int type) {
            activityReference = new WeakReference<>(activity);
            backupType = type;
        }

        @Override
        protected Void doInBackground(File... file) {
            if (file.length == 0 || file[0] == null) return null;

            Activity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return null;

            switch(backupType) {
                case EXPORT_BACKUP:
                    File exportLogFile = new File(file[0], Const.FILENAME_LOGS_CSV);
                    int exportLogCount = DbUtils.exportLogs(activity, exportLogFile);
                    if (exportLogCount < 0) {
                        message = activity.getString(R.string.err_export_logs);
                        return null;
                    }

                    File exportTagFile = new File(file[0], Const.FILENAME_TAGS_CSV);
                    int exportTagCount = DbUtils.exportTags(activity, exportTagFile);
                    if (exportTagCount < 0) {
                        message = activity.getString(R.string.wrn_export_logs_fmt, exportLogCount);
                        return null;
                    }

                    message = activity.getString(R.string.msg_export_logs_fmt, exportLogCount, exportTagCount);
                    break;

                case IMPORT_BACKUP:
                    if (file[0] == null || !file[0].isDirectory()) return null;
                    int importedTagCount = 0;
                    importedLogCount = 0;
                    File logsFile = new File(file[0], Const.FILENAME_LOGS_CSV);
                    if (logsFile.exists() && logsFile.isFile()) {
                        importedLogCount = DbUtils.importLogs(activity, logsFile);
                        if (importedLogCount < 0) {
                            Toast.makeText(activity, R.string.err_import_logs, Toast.LENGTH_SHORT).show();
                            return null;
                        }
                    }

                    File tagsFile = new File(file[0], Const.FILENAME_TAGS_CSV);
                    message = activity.getString(R.string.wrn_import_logs_fmt, importedLogCount);
                    if (tagsFile.exists() && tagsFile.isFile()) {
                        importedTagCount = DbUtils.importTags(activity, tagsFile);
                        if (importedTagCount >= 0) {
                            // タグのインポートも成功
                            message = activity.getString(R.string.msg_import_logs_fmt, importedLogCount, importedTagCount);
                        }
                    }
                    break;
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Activity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            switch(backupType) {
                case EXPORT_BACKUP:
                    new DialogUi.Builder(activity, DialogUi.STYLE_PROGRESS_DIALOG)
                            .setMessage(R.string.msg_exporting_logs)
                            .setRequestCode(RC_PROGRESS_DIALOG)
                            .show();
                    break;
                case IMPORT_BACKUP:
                    new DialogUi.Builder(activity, DialogUi.STYLE_PROGRESS_DIALOG)
                            .setMessage(R.string.msg_importing_logs)
                            .setRequestCode(RC_PROGRESS_DIALOG)
                            .show();
                    break;
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            Activity activity = activityReference.get();
            if (activity == null || activity.isFinishing()) return;

            DialogUi.dismissDialog(activity, RC_PROGRESS_DIALOG);
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            if (backupType == IMPORT_BACKUP) {
                updateChartButton(activity, importedLogCount >= 1);
            }
        }
    }

}
