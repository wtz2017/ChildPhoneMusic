package com.wtz.child.phonemusic;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.wtz.child.phonemusic.adapter.MusicGridAdapter;
import com.wtz.child.phonemusic.data.Item;
import com.wtz.child.phonemusic.utils.LogUtils;
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
    private static final String SP_NAME = "config";
    private static final String SP_KEY_LAST_AUDIO_PATH = "sp_key_last_audio_path";
    private String mLastAudioPath;
    private String mRootPath;

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private PermissionHandler mPermissionHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogUtils.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

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
        mSp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        mLastAudioPath = mSp.getString(SP_KEY_LAST_AUDIO_PATH, mRootPath);
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
        editor.putString(SP_KEY_LAST_AUDIO_PATH, path);
        editor.apply();
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
        super.onDestroy();
    }

}
