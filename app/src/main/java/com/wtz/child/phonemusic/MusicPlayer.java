package com.wtz.child.phonemusic;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.squareup.picasso.Picasso;
import com.wtz.child.phonemusic.data.Item;
import com.wtz.child.phonemusic.utils.AsyncTaskExecutor;
import com.wtz.child.phonemusic.utils.BitmapUtils;
import com.wtz.child.phonemusic.utils.DateTimeUtil;
import com.wtz.child.phonemusic.utils.LogUtils;
import com.wtz.child.phonemusic.utils.MusicIcon;
import com.wtz.child.phonemusic.utils.PicassoRoundTransformation;
import com.wtz.child.phonemusic.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

public class MusicPlayer extends AppCompatActivity implements View.OnClickListener,
        View.OnTouchListener, SeekBar.OnSeekBarChangeListener {
    private static final String TAG = MusicPlayer.class.getSimpleName();

    public static final String KEY_MUSIC_LIST = "key_music_list";
    public static final String KEY_MUSIC_INDEX = "key_music_index";
    private List<Item> mMusicList = new ArrayList<>();
    private int mSize;
    private int mIndex;
    private Item mCurrentItem;
    private boolean isBackward = true;// 向后切换音频

    private ImageView ivAlbum;
    private int mAlbumWidth;
    private int mAlbumHeight;
    private Drawable mAlbumDrawable;
    private int mRoundPx;

    private TextView tvName;

    private TextView mPlayTimeView;
    private int mDurationMsec;
    private int mPlayPositionMsec;
    private String mDurationText;

    private SeekBar mPlaySeekBar;
    private boolean isSeeking;

    private ImageView ivPre;
    private ImageView ivPlay;
    private ImageView ivNext;
    private ImageView ivPlayMode;
    private static final int PLAY_MODE_REPEAT = 0;
    private static final int PLAY_MODE_ORDER = 1;
    private int mPlayMode = PLAY_MODE_REPEAT;
    private int mInitPlayMode;

    private SharedPreferences mSp;

    private static final int UPDATE_PLAY_TIME_INTERVAL = 300;
    private static final int DELAY_UPDATE_ALBUM_TIME = 700;
    private static final int MSG_UPDATE_PLAY_TIME = 1;
    private static final int MSG_UPDATE_ALBUM = 2;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_PLAY_TIME:
                    updatePlayTime();
                    removeMessages(MSG_UPDATE_PLAY_TIME);
                    sendEmptyMessageDelayed(MSG_UPDATE_PLAY_TIME, UPDATE_PLAY_TIME_INTERVAL);
                    break;
                case MSG_UPDATE_ALBUM:
                    updateAlbum();
                    break;
            }
        }
    };

    private MusicService mService;
    private boolean mBound = false;

    private AsyncTaskExecutor mAsyncTaskExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LogUtils.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (!initData(getIntent())) return;

        initPlayMode();
        configView();

        Intent playService = new Intent(this, MusicService.class);
        bindService(playService, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // 由于设置了 singleInstance 模式，按了 Home 键隐藏后再次进入时会调用此方法
        LogUtils.d(TAG, "onNewIntent " + intent);
        super.onNewIntent(intent);

        if (!initData(intent)) return;

        startNewAudio();
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

        if (ivAlbum != null) {
            mAlbumDrawable = ivAlbum.getDrawable();
        }
        configView();
    }

    private void configView() {
        if (ScreenUtils.isPortrait(this)) {
            // 竖屏
            setContentView(R.layout.activity_music_player_portrait);
        } else {
            // 横屏
            setContentView(R.layout.activity_music_player_landscape);
        }
        initViews();
        updatePlayTime();// 考虑播放半途中切屏场景
    }

    private boolean initData(Intent intent) {
        List<Item> list = intent.getParcelableArrayListExtra(KEY_MUSIC_LIST);
        if (list == null || list.isEmpty()) {
            finish();
            LogUtils.e(TAG, "initData failed: list is null or empty");
            return false;
        }

        mMusicList.clear();
        mMusicList.addAll(list);
        mSize = mMusicList.size();
        mIndex = intent.getIntExtra(KEY_MUSIC_INDEX, 0);
        if (mIndex < 0 || mIndex >= mSize) {
            mIndex = 0;
        }
        mCurrentItem = mMusicList.get(mIndex);
        LogUtils.d(TAG, "initData " + mCurrentItem);
        return true;
    }

    private void initViews() {
        ivAlbum = findViewById(R.id.iv_album);
        setAlbumLayout(ivAlbum);
        if (mAlbumDrawable != null) {
            ivAlbum.setImageDrawable(mAlbumDrawable);
        } else {
            ivAlbum.setImageResource(R.drawable.icon_music);
        }
        mRoundPx = (int) getResources().getDimension(R.dimen.dp_10);

        tvName = findViewById(R.id.tv_name);
        tvName.setText(mMusicList.get(mIndex).name);

        mPlayTimeView = findViewById(R.id.tv_play_time);

        mPlaySeekBar = findViewById(R.id.seek_bar_play);
        setSeekbarWith(mPlaySeekBar);
        mPlaySeekBar.setMax(mDurationMsec);// 在重新设置 seekbar 宽度后，需要重新设置其最大值
        mPlaySeekBar.setOnSeekBarChangeListener(this);

        ivPre = (ImageView) this.findViewById(R.id.iv_pre);
        ivPre.setOnClickListener(this);
        ivPre.setOnTouchListener(this);

        ivPlay = (ImageView) this.findViewById(R.id.iv_play);
        ivPlay.setOnClickListener(this);
        ivPlay.setOnTouchListener(this);

        ivNext = (ImageView) this.findViewById(R.id.iv_next);
        ivNext.setOnClickListener(this);
        ivNext.setOnTouchListener(this);

        ivPlayMode = (ImageView) this.findViewById(R.id.iv_play_mode);
        ivPlayMode.setOnClickListener(this);
        ivPlayMode.setOnTouchListener(this);
        switch (mPlayMode) {
            case PLAY_MODE_REPEAT:
                ivPlayMode.setImageResource(R.drawable.repeat_play);
                break;
            case PLAY_MODE_ORDER:
                ivPlayMode.setImageResource(R.drawable.order_play);
                break;
        }
    }

    private void setAlbumLayout(ImageView album) {
        int[] wh = ScreenUtils.getScreenPixels(this);
        int albumWidth;
        if (ScreenUtils.isPortrait(this)) {
            albumWidth = (int) Math.round(wh[0] * 0.85);
        } else {
            albumWidth = (int) Math.round(wh[0] * 0.5 * 0.85);
        }
        if (albumWidth > wh[1]) {
            albumWidth = (int) Math.round(wh[1] * 0.85);
        }

        mAlbumHeight = mAlbumWidth = albumWidth;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) album.getLayoutParams();
        lp.width = albumWidth;
        lp.height = albumWidth;
        album.setLayoutParams(lp);
    }

    private void setSeekbarWith(SeekBar seekbar) {
        int[] wh = ScreenUtils.getScreenPixels(this);
        int seekWidth;
        if (ScreenUtils.isPortrait(this)) {
            seekWidth = (int) Math.round(wh[0] * 0.75);
        } else {
            seekWidth = (int) Math.round(wh[0] * 0.5 * 0.75);
        }

        ViewGroup.LayoutParams lp = seekbar.getLayoutParams();
        lp.width = seekWidth;
        seekbar.setLayoutParams(lp);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
        if (isSeeking) {
            // 因为主动 seek 导致的 seekbar 变化，此时只需要更新时间
            updatePlayTime();
        } else {
            // 因为实际播放时间变化而设置 seekbar 导致变化，什么都不用做
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        LogUtils.d(TAG, "mPlaySeekBar onStartTrackingTouch");
        isSeeking = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        LogUtils.d(TAG, "mPlaySeekBar onStopTrackingTouch");
        seekTo(seekBar.getProgress());
        isSeeking = false;
    }

    @Override
    public void onClick(View view) {
        LogUtils.d(TAG, "onClick " + view);
        switch (view.getId()) {
            case R.id.iv_pre:
                pre();
                break;
            case R.id.iv_next:
                next();
                break;
            case R.id.iv_play:
                if (isPlaying()) {
                    pause();
                } else {
                    play();
                }
                break;
            case R.id.iv_play_mode:
                if (mPlayMode == PLAY_MODE_REPEAT) {
                    ivPlayMode.setImageResource(R.drawable.order_play);
                    mPlayMode = PLAY_MODE_ORDER;
                } else if (mPlayMode == PLAY_MODE_ORDER) {
                    ivPlayMode.setImageResource(R.drawable.repeat_play);
                    mPlayMode = PLAY_MODE_REPEAT;
                }
                break;
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        switch (view.getId()) {
            case R.id.iv_pre:
                ivPre.requestFocus();
                break;
            case R.id.iv_next:
                ivNext.requestFocus();
                break;
            case R.id.iv_play:
                ivPlay.requestFocus();
                break;
            case R.id.iv_play_mode:
                ivPlayMode.requestFocus();
                break;
        }
        return false;
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            LogUtils.d(TAG, "onServiceConnected");
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            initPlayService();
            startNewAudio();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            LogUtils.e(TAG, "onServiceDisconnected");
            mBound = false;
        }
    };

    private boolean isServiceOK() {
        return mService != null && mBound;
    }

    private boolean isPlaying() {
        if (isServiceOK()) {
            return mService.isPlaying();
        }
        return false;
    }

    private void initPlayService() {
        if (isServiceOK()) {
            mService.init(mOnPreparedListener, mOnCompletionListener, mOnErrorListener);
        }
    }

    private MediaPlayer.OnPreparedListener mOnPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mDurationMsec = getDuration();
            LogUtils.d(TAG, "onPrepared mDurationMsec=" + mDurationMsec);
            mPlaySeekBar.setMax(mDurationMsec);
            mDurationText = DateTimeUtil.changeRemainTimeToHms(mDurationMsec);
            play();
        }
    };

    private MediaPlayer.OnCompletionListener mOnCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {
            LogUtils.d(TAG, "onCompletion");
            if (mPlayMode == PLAY_MODE_ORDER) {
                next();
            } else if (mPlayMode == PLAY_MODE_REPEAT) {
                openAudio();
            }
        }
    };

    private MediaPlayer.OnErrorListener mOnErrorListener = new MediaPlayer.OnErrorListener() {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            LogUtils.e(TAG, "onError what=" + what + ";extra=" + extra);
            if (isBackward) {
                next();
            } else {
                pre();
            }
            return false;
        }
    };

    private void openAudio() {
        if (isServiceOK()) {
            mService.openMusic(mCurrentItem.path);
        }
    }

    private void pre() {
        isBackward = false;
        if (mIndex == 0) {
            mIndex = mMusicList.size() - 1;
        } else {
            mIndex--;
        }
        mCurrentItem = mMusicList.get(mIndex);

        startNewAudio();
    }

    private void next() {
        isBackward = true;
        if (mIndex == mMusicList.size() - 1) {
            mIndex = 0;
        } else {
            mIndex++;
        }
        mCurrentItem = mMusicList.get(mIndex);

        startNewAudio();
    }

    private void startNewAudio() {
        resetPlayUI();
        stop();

        tvName.setText(mCurrentItem.name);
        startUpdateAlbum();
        openAudio();
    }

    private void play() {
        if (isServiceOK()) {
            mService.start();
            ivPlay.setImageResource(R.drawable.pause_image_selector);
            startTimeUpdate();
        }
    }

    private void pause() {
        if (isServiceOK()) {
            mService.pause();
            ivPlay.setImageResource(R.drawable.play_image_selector);
            stopTimeUpdate();
        }
    }

    private void seekTo(int msec) {
        if (isServiceOK()) {
            mService.seekTo(msec);
        }
    }

    private int getDuration() {
        if (isServiceOK()) {
            return mService.getDuration();
        }
        return 0;
    }

    private int getCurrentPosition() {
        if (isServiceOK()) {
            return mService.getCurrentPosition();
        }
        return 0;
    }

    private void stop() {
        if (isServiceOK()) {
            mService.stop();
            stopTimeUpdate();
        }
    }

    private void releasePlayService() {
        if (isServiceOK()) {
            mService.release();
            stopTimeUpdate();
        }
    }

    private void resetPlayUI() {
        stopUpdateAlbum();
        ivAlbum.setImageResource(R.drawable.icon_music);
        mPlayTimeView.setText("00:00:00/" + mDurationText);
        mPlaySeekBar.setProgress(0);
    }

    private void startTimeUpdate() {
        mHandler.sendEmptyMessage(MSG_UPDATE_PLAY_TIME);
    }

    private void stopTimeUpdate() {
        mHandler.removeMessages(MSG_UPDATE_PLAY_TIME);
    }

    private void updatePlayTime() {
        if (isSeeking) {
            // seek 时 seekbar 会自动更新位置，只需要根据 seek 位置更新时间
            mPlayPositionMsec = mPlaySeekBar.getProgress();
            String currentPosition = DateTimeUtil.changeRemainTimeToHms(mPlayPositionMsec);
            mPlayTimeView.setText(currentPosition + "/" + mDurationText);
        } else {
            // 没有 seek 时，如果还在播放中，就正常按实际播放时间更新时间和 seekbar
            mPlayPositionMsec = getCurrentPosition();
            String currentPosition = DateTimeUtil.changeRemainTimeToHms(mPlayPositionMsec);
            mPlayTimeView.setText(currentPosition + "/" + mDurationText);
            mPlaySeekBar.setProgress(mPlayPositionMsec);
        }
    }

    private void startUpdateAlbum() {
        mHandler.removeMessages(MSG_UPDATE_ALBUM);
        mHandler.sendEmptyMessageDelayed(MSG_UPDATE_ALBUM, DELAY_UPDATE_ALBUM_TIME);
    }

    private void stopUpdateAlbum() {
        mHandler.removeMessages(MSG_UPDATE_ALBUM);
    }

    private void updateAlbum() {
        if (mAsyncTaskExecutor == null) {
            mAsyncTaskExecutor = AsyncTaskExecutor.getInstance();
        }
        mAsyncTaskExecutor.submit(new Runnable() {
            @Override
            public void run() {
                String path = mCurrentItem.path;
                final Bitmap bitmap = MusicIcon.getArtworkFromFile(path, mAlbumWidth, mAlbumHeight);
                LogUtils.d(TAG, "getArtworkFromFile bitmap=" + bitmap);
                if (bitmap != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Bitmap result = BitmapUtils.roundImageCorner(bitmap, BitmapUtils.RoundCornerType.ALL, mRoundPx);
                            if (result != bitmap) {
                                bitmap.recycle();
                            }
                            ivAlbum.setImageBitmap(result);
                        }
                    });
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            // Picasso call should happen from the main thread
                            Picasso.get()
                                    .load(MusicIcon.getRandomIconUrl(mCurrentItem.name))
                                    // 解决 OOM 问题
                                    .resize(mAlbumWidth, mAlbumHeight)
                                    .transform(new PicassoRoundTransformation(mRoundPx))
                                    .centerCrop()// 需要先调用fit或resize设置目标大小，否则会报错：Center crop requires calling resize with positive width and height
                                    .placeholder(R.drawable.icon_music)
                                    .noFade()
                                    .into(ivAlbum);
                        }
                    });
                }
            }
        });
    }

    private void initPlayMode() {
        mSp = Preferences.getSP(this);
        mInitPlayMode = mSp.getInt(Preferences.KEY_AUDIO_PLAY_MODE, PLAY_MODE_REPEAT);
        mPlayMode = mInitPlayMode;
    }

    private void savePlayMode(int mode) {
        if (mSp == null) {
            return;
        }
        SharedPreferences.Editor editor = mSp.edit();
        editor.putInt(Preferences.KEY_AUDIO_PLAY_MODE, mode);
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
        if (mInitPlayMode != mPlayMode) {
            savePlayMode(mPlayMode);
        }
        if (mAsyncTaskExecutor != null) {
            mAsyncTaskExecutor.shutdown();
            mAsyncTaskExecutor = null;
        }
        mAlbumDrawable = null;
        stop();
        releasePlayService();
        if (mBound) {
            unbindService(mConnection);
            mService = null;
            mBound = false;
        }
        if (mMusicList != null) {
            mMusicList.clear();
            mMusicList = null;
        }
        mHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

}
