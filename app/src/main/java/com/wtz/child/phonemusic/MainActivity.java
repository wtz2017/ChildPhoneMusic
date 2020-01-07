package com.wtz.child.phonemusic;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.wtz.child.phonemusic.adapter.MusicGridAdapter;
import com.wtz.child.phonemusic.data.Item;
import com.wtz.child.phonemusic.utils.LogUtils;
import com.wtz.child.phonemusic.utils.Md5Util;
import com.wtz.child.phonemusic.utils.PermissionChecker;
import com.wtz.child.phonemusic.utils.PermissionHandler;
import com.wtz.child.phonemusic.utils.ScreenUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
    private static final String TAG = "Phone_Music_Home";

    private TextView mCurrentPathView;

    private static final int GRIDVIEW_VERTICAL_SPACE_DIP = 12;
    private static final int GRIDVIEW_HORIZONTAL_SPACE_DIP = 12;
    private int mColumnCounts;
    private int mColumnWidth;
    private int mItemWidth;
    private int mItemHeight;

    private GridView mGridView;
    private MusicGridAdapter mGridAdapter;
    private ArrayList<Item> mTotalList = new ArrayList<>();
    private ArrayList<Item> mAudioList = new ArrayList<>();

    private static final Map<String, String> SUFFIX = new HashMap<>();
    static {
        SUFFIX.put(".mp3", ".mp3");
        SUFFIX.put(".aac", ".aac");
        SUFFIX.put(".wma", ".wma");
        SUFFIX.put(".m4a", ".m4a");
        SUFFIX.put(".ac3", ".ac3");
        SUFFIX.put(".dts", ".dts");
        SUFFIX.put(".wav", ".wav");
        SUFFIX.put(".flac", ".flac");
        SUFFIX.put(".ape", ".ape");
    }

    private SharedPreferences mSp;
    private String mLastAudioPath;
    private String mRootPath;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private PermissionHandler mPermissionHandler;

    private MenuItem mStopPlayMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogUtils.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        mSp = Preferences.getInstance().getSP();
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mBroadcastReceiver, new IntentFilter(App.ACTION_PLAY_STOPPED));

        configView();

        mPermissionHandler = new PermissionHandler(this, mPermissionHandleListener);
        mPermissionHandler.handleCommonPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // 由于是桌面应用的主 Activity，且设置了 singleInstance 模式，按 Home 键时会调用此方法
        LogUtils.d(TAG, "onNewIntent " + intent);
        super.onNewIntent(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.action_bar_menu_items, menu);
        mStopPlayMenu = menu.findItem(R.id.menu_item_stop);
        mStopPlayMenu.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_stop:
                stopPlay();
                return true;
            case R.id.menu_item_setting:
                showAuthenticationDialog(this);
                return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        LogUtils.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        LogUtils.d(TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        LogUtils.w(TAG, "onConfigurationChanged newConfig:" + newConfig);
        super.onConfigurationChanged(newConfig);
        configView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        LogUtils.d(TAG, "onRequestPermissionsResult requestCode=" + requestCode);
        mPermissionHandler.handleActivityRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        LogUtils.d(TAG, "onActivityResult requestCode=" + requestCode + ", resultCode=" + resultCode
                + ", data=" + data);
        mPermissionHandler.handleActivityResult(requestCode);
    }

    private PermissionHandler.PermissionHandleListener mPermissionHandleListener =
            new PermissionHandler.PermissionHandleListener() {
                @Override
                public void onPermissionResult(String permission, PermissionChecker.PermissionState state) {
                    if (state == PermissionChecker.PermissionState.ALLOWED) {
                        initAudioDir();
                        updateGridview();
                    } else {
                        LogUtils.e(TAG, "onPermissionResult " + permission + " is not allowed!");
                    }
                }
            };

    private void configView() {
        setContentView(R.layout.activity_main);
        configGrid();
        initViews();
    }

    private void configGrid() {
        if (ScreenUtils.isPortrait(this)) {
            // 竖屏
            mColumnCounts = 3;
        } else {
            // 横屏
            mColumnCounts = 5;
        }
        int[] wh = ScreenUtils.getScreenPixels(MainActivity.this);
        mColumnWidth = wh[0] / mColumnCounts;

        int horizontalSapce = ScreenUtils.dip2px(MainActivity.this, GRIDVIEW_HORIZONTAL_SPACE_DIP);
        mItemWidth = mColumnWidth - horizontalSapce * 2;

        int nameHeight = (int) getResources().getDimension(R.dimen.sp_20);
        int margin = (int) getResources().getDimension(R.dimen.dp_6);
        mItemHeight = mItemWidth + margin + nameHeight;
    }

    private void initViews() {
        mCurrentPathView = findViewById(R.id.tv_path);
        mCurrentPathView.setText(mLastAudioPath);

        mGridView = findViewById(R.id.gridView_music);
        mGridView.setNumColumns(mColumnCounts);
        mGridView.setColumnWidth(mColumnWidth);
        int verticalSapce = ScreenUtils.dip2px(MainActivity.this, GRIDVIEW_VERTICAL_SPACE_DIP);
        mGridView.setVerticalSpacing(verticalSapce);

        if (mGridAdapter == null) {
            mGridAdapter = new MusicGridAdapter(MainActivity.this, mTotalList, mItemWidth, mItemHeight, mHandler);
        } else {
            mGridAdapter.updateLayout(mTotalList, mItemWidth, mItemHeight);
        }
        mGridView.setAdapter(mGridAdapter);
        mGridView.setOnItemClickListener(this);
    }

    private void initAudioDir() {
        mRootPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        LogUtils.d(TAG, "mRootPath=" + mRootPath);
        mLastAudioPath = mSp.getString(Preferences.KEY_LAST_AUDIO_PATH, mRootPath);
        mCurrentPathView.setText(mLastAudioPath);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        LogUtils.d(TAG, "onKeyDown " + keyCode + "," + event.getAction());
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (mRootPath == null || mRootPath.equals(mLastAudioPath)) {
                    return true;
                }
                int end = mLastAudioPath.lastIndexOf(File.separator);
                if (end == -1 || end == 0) {
                    mLastAudioPath = mRootPath;
                } else {
                    mLastAudioPath = mLastAudioPath.substring(0, end);
                }
                mCurrentPathView.setText(mLastAudioPath);
                updateGridview();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Item item = (Item) mGridAdapter.getItem(position);
        LogUtils.d(TAG, "mGridView onItemClick position=" + position + ", path=" + item.path);
        if (item.type == Item.TYPE_AUDIO) {
            play(item);
        } else if (item.type == Item.TYPE_DIR) {
            mLastAudioPath = item.path;
            mCurrentPathView.setText(mLastAudioPath);
            updateGridview();
        }
    }

    private void play(Item item) {
        if (mAudioList.isEmpty()) {
            Toast.makeText(MainActivity.this, "请选择音乐", Toast.LENGTH_SHORT).show();
            return;
        }
        int index = mAudioList.indexOf(item);
        Intent i = new Intent(MainActivity.this, MusicPlayer.class);
        i.putExtra(MusicPlayer.KEY_MUSIC_LIST, mAudioList);
        i.putExtra(MusicPlayer.KEY_MUSIC_INDEX, index);
        startActivity(i);
        mStopPlayMenu.setVisible(true);
    }

    private void updateGridview() {
        LogUtils.d(TAG, "updateGridview path: " + mLastAudioPath);
        new Thread(new Runnable() {
            @Override
            public void run() {
                parseTargetDir(mLastAudioPath);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mGridAdapter.updateData(mTotalList);
                    }
                });
            }
        }).start();
    }

    private void parseTargetDir(String targetPath) {
        LogUtils.d(TAG, "parseTargetDir: " + targetPath);
        if (TextUtils.isEmpty(targetPath)) {
            LogUtils.e(TAG, "target path is null");
            return;
        }

        File dir = new File(targetPath);
        if (!dir.exists() || !dir.isDirectory()) {
            LogUtils.e(TAG, "target dir not exist:" + targetPath);
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            LogUtils.e(TAG, "target File[] files is null");
            return;
        }

        int index;
        String suffix;
        String path;
        mTotalList.clear();
        mAudioList.clear();
        for (File file : files) {
            path = file.getAbsolutePath();
            if (file.isDirectory()) {
                mTotalList.add(new Item(Item.TYPE_DIR, file.getName(), path));
            } else {
                index = path.lastIndexOf(".");
                if (index > 0 && index < path.length() - 1) {
                    suffix = path.substring(index);
                    if (SUFFIX.containsKey(suffix.toLowerCase())) {
                        Item audio = new Item(Item.TYPE_AUDIO, stripFileName(file), path);
                        mTotalList.add(audio);
                        mAudioList.add(audio);
                    }
                }
            }
        }
        if (mTotalList.size() > 0) {
            Collections.sort(mTotalList);
        }
        if (mAudioList.size() > 0) {
            Collections.sort(mAudioList);
        }
    }

    private String stripFileName(File file) {
        String orign = file.getName();
        int end = orign.lastIndexOf(".");
        if (end == -1 || end == 0) {
            end = orign.length();
        }

        return orign.substring(0, end);
    }

    private void saveVideoPath(String path) {
        if (mSp == null) {
            return;
        }
        SharedPreferences.Editor editor = mSp.edit();
        editor.putString(Preferences.KEY_LAST_AUDIO_PATH, path);
        editor.apply();
    }

    private void showAuthenticationDialog(Context context) {
        final Dialog dialog = new Dialog(context, R.style.NormalDialogStyle);
        View view = View.inflate(context, R.layout.dialog_authentication, null);
        final EditText etPassWord = view.findViewById(R.id.et_password);
        Button cancel = view.findViewById(R.id.btn_cancel);
        Button confirm = view.findViewById(R.id.btn_confirm);
        dialog.setContentView(view);

        int[] screenSize = ScreenUtils.getScreenPixels(context);
        view.setMinimumHeight((int) (screenSize[1] * 0.23f));
        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = (int) (screenSize[0] * 0.80f);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        dialogWindow.setAttributes(lp);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Editable pwdEdit = etPassWord.getText();
                if (pwdEdit == null) {
                    Toast.makeText(v.getContext(), "请输入密码", Toast.LENGTH_SHORT).show();
                    return;
                }
                String pwdStr = pwdEdit.toString();
                if (TextUtils.isEmpty(pwdStr)) {
                    Toast.makeText(v.getContext(), "请输入密码", Toast.LENGTH_SHORT).show();
                    return;
                }

                String pwdMd5 = Md5Util.getStringMD5(pwdStr);
                String realMd5 = mSp.getString(Preferences.KEY_PASS_WORD, "");
                if (TextUtils.isEmpty(realMd5)) {
                    realMd5 = Md5Util.getStringMD5(Preferences.DEFAULT_PASS_WORD);
                }
                if (!realMd5.equals(pwdMd5)) {
                    Toast.makeText(v.getContext(), "密码错误", Toast.LENGTH_SHORT).show();
                    return;
                }

                dialog.dismiss();
                showSettingDialog(v.getContext());
            }
        });

        //使得点击对话框外部不消失对话框
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void showSettingDialog(Context context) {
        final Dialog dialog = new Dialog(context, R.style.NormalDialogStyle);
        View view = View.inflate(context, R.layout.dialog_setting, null);
        Button setTime = view.findViewById(R.id.btn_set_time);
        Button setPwd = view.findViewById(R.id.btn_set_pwd);
        dialog.setContentView(view);

        int[] screenSize = ScreenUtils.getScreenPixels(context);
        view.setMinimumHeight((int) (screenSize[1] * 0.23f));
        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = (int) (screenSize[0] * 0.80f);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        dialogWindow.setAttributes(lp);

        setTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showTimeSettingDialog(v.getContext());
            }
        });
        setPwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                showPwdSettingDialog(v.getContext());
            }
        });

        //使得点击对话框外部不消失对话框
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void showTimeSettingDialog(Context context) {
        final Dialog dialog = new Dialog(context, R.style.NormalDialogStyle);

        View view = View.inflate(context, R.layout.dialog_time_setting, null);
        final EditText etMaxPlayTime = view.findViewById(R.id.et_max_play_time);
        final EditText etPlanRestTime = view.findViewById(R.id.et_plan_rest_time);
        Button cancel = view.findViewById(R.id.btn_cancel);
        Button confirm = view.findViewById(R.id.btn_confirm);
        dialog.setContentView(view);

        etMaxPlayTime.setText("" + TimeManager.getInstance().getMaxPlayTimeMinutes());
        etPlanRestTime.setText("" + TimeManager.getInstance().getPlanRestTimeMinutes());

        int[] screenSize = ScreenUtils.getScreenPixels(context);
        view.setMinimumHeight((int) (screenSize[1] * 0.23f));
        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = (int) (screenSize[0] * 0.80f);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        dialogWindow.setAttributes(lp);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String maxPlayTimeStr;
                if (etMaxPlayTime.getText() != null
                        && !TextUtils.isEmpty((maxPlayTimeStr = etMaxPlayTime.getText().toString()))) {
                    int maxPlayTimeMinutes = Integer.parseInt(maxPlayTimeStr);
                    TimeManager.getInstance().setMaxPlayTimeMinutes(maxPlayTimeMinutes);
                }

                String planRestTimeStr;
                if (etPlanRestTime.getText() != null
                        && !TextUtils.isEmpty((planRestTimeStr = etPlanRestTime.getText().toString()))) {
                    int planRestTime = Integer.parseInt(planRestTimeStr);
                    TimeManager.getInstance().setPlanRestTimeMinutes(planRestTime);
                }

                dialog.dismiss();
            }
        });

        //使得点击对话框外部不消失对话框
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void showPwdSettingDialog(Context context) {
        final Dialog dialog = new Dialog(context, R.style.NormalDialogStyle);
        View view = View.inflate(context, R.layout.dialog_pwd_setting, null);
        final EditText etPwd = view.findViewById(R.id.et_set_new_pwd);
        Button cancel = view.findViewById(R.id.btn_cancel);
        Button confirm = view.findViewById(R.id.btn_confirm);
        dialog.setContentView(view);

        int[] screenSize = ScreenUtils.getScreenPixels(context);
        view.setMinimumHeight((int) (screenSize[1] * 0.23f));
        Window dialogWindow = dialog.getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        lp.width = (int) (screenSize[0] * 0.80f);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        dialogWindow.setAttributes(lp);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Editable pwdEdit = etPwd.getText();
                if (pwdEdit == null) {
                    Toast.makeText(v.getContext(), "请输入密码", Toast.LENGTH_SHORT).show();
                    return;
                }
                String pwdStr = pwdEdit.toString();
                if (TextUtils.isEmpty(pwdStr)) {
                    Toast.makeText(v.getContext(), "请输入密码", Toast.LENGTH_SHORT).show();
                    return;
                }

                String pwdMd5 = Md5Util.getStringMD5(pwdStr);
                SharedPreferences.Editor editor = mSp.edit();
                editor.putString(Preferences.KEY_PASS_WORD, pwdMd5);
                editor.apply();
                dialog.dismiss();
            }
        });

        //使得点击对话框外部不消失对话框
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void stopPlay() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(App.ACTION_STOP_PLAY));
    }

    @Override
    protected void onPause() {
        LogUtils.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onStop() {
        LogUtils.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        LogUtils.d(TAG, "onDestroy");
        saveVideoPath(mLastAudioPath);
        if (mPermissionHandler != null) {
            mPermissionHandler.destroy();
            mPermissionHandler = null;
        }
        mHandler.removeCallbacksAndMessages(null);
        mTotalList.clear();
        mAudioList.clear();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            LogUtils.d(TAG, "onReceive: " + action);
            if (App.ACTION_PLAY_STOPPED.equals(action)) {
                mStopPlayMenu.setVisible(false);
            }
        }
    };

}
