package com.wtz.child.phonemusic;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;

public class MusicService extends Service implements MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private static final String TAG = MusicService.class.getSimpleName();

    // NOTIFICATION_ID must not be 0.
    // startForeground 只要传的 id 相同，不管是不是一个进程，不管是不是同一个 notification，
    // 都会用最新的 notification 覆盖旧的，只显示一个。
    private static final int NOTIFICATION_ID = 1;
    private static final String NOTIFICATION_CHANNEL_ID = "channel_child_phone_music";
    private static final String NOTIFICATION_CHANNEL_NAME = "Child Phone Music is playing.";

    private final IBinder mBinder = new LocalBinder();

    // all possible internal states
    private static final int STATE_ERROR = -1;
    private static final int STATE_IDLE = 0;
    private static final int STATE_INITIALIZED = 1;
    private static final int STATE_PREPARING = 2;
    private static final int STATE_PREPARED = 3;
    private static final int STATE_PLAYING = 4;
    private static final int STATE_PAUSED = 5;
    private static final int STATE_STOPED = 6;
    private static final int STATE_PLAYBACK_COMPLETED = 7;
    private static final int STATE_RELEASED = 8;

    private int mCurrentState = STATE_ERROR;

    private String mCurrentSource;

    private MediaPlayer mMediaPlayer = null;

    private MediaPlayer.OnPreparedListener mOnPreparedListener;
    private MediaPlayer.OnCompletionListener mOnCompletionListener;
    private MediaPlayer.OnErrorListener mOnErrorListener;

    public MusicService() {
    }

    public class LocalBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                this, NOTIFICATION_CHANNEL_ID);
        builder.setSmallIcon(R.mipmap.ic_launcher);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager)getSystemService(
                    NOTIFICATION_SERVICE);
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW);
            notificationManager.createNotificationChannel(channel);
        }

        startForeground(NOTIFICATION_ID, builder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    public void init(MediaPlayer.OnPreparedListener onPreparedListener,
                     MediaPlayer.OnCompletionListener onCompletionListener,
                     MediaPlayer.OnErrorListener onErrorListener) {
        mOnPreparedListener = onPreparedListener;
        mOnCompletionListener = onCompletionListener;
        mOnErrorListener = onErrorListener;

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);

        /*
         * Must call this method before prepare() or prepareAsync() in order
         * for the target stream type to become effective thereafter.
         */
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mCurrentState = STATE_IDLE;
    }

    public void openMusic(String path) {
        Log.d(TAG, "openMusic...path = " + path);
        mCurrentSource = path;
        mMediaPlayer.reset();
        mCurrentState = STATE_IDLE;
        try {
            if (path.startsWith("content://")) {
                mMediaPlayer.setDataSource(MusicService.this, Uri.parse(path));
            } else {
                mMediaPlayer.setDataSource(path);
            }
            mCurrentState = STATE_INITIALIZED;

			/*
             * After setting the datasource and the display surface, you need to
			 * either call prepare() or prepareAsync(). For streams, you should
			 * call prepareAsync(), which returns immediately, rather than
			 * blocking until enough data has been buffered.
			 */
            mMediaPlayer.prepareAsync();
            mCurrentState = STATE_PREPARING;
        } catch (IOException ex) {
            Log.w(TAG, "Unable to open content: " + path, ex);
            this.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        } catch (IllegalArgumentException ex) {
            Log.w(TAG, "Unable to open content: " + path, ex);
            this.onError(mMediaPlayer, MediaPlayer.MEDIA_ERROR_UNKNOWN, 0);
        }
    }

    public String getCurrentSource() {
        return mCurrentSource;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "onPrepared");
        mCurrentState = STATE_PREPARED;
        if (mOnPreparedListener != null) {
            mOnPreparedListener.onPrepared(null);
        }
    }

    public void start() {
        Log.d(TAG, "start...");
        if (mMediaPlayer != null) {
            if (mCurrentState == STATE_PREPARED || mCurrentState == STATE_PAUSED
                    || mCurrentState == STATE_PLAYBACK_COMPLETED) {
                mMediaPlayer.start();
                mCurrentState = STATE_PLAYING;
                Log.d(TAG, "started");
            }
        }
    }

    public void pause() {
        Log.d(TAG, "pause...");
        if (mMediaPlayer != null) {
            if (mCurrentState == STATE_PLAYING) {
                mMediaPlayer.pause();
                mCurrentState = STATE_PAUSED;
                Log.d(TAG, "paused");
            }
        }
    }

    public void seekTo(int msec) {
        Log.d(TAG, "seekTo...msec = " + msec);
        if (mMediaPlayer != null) {
            if (mCurrentState == STATE_PREPARED || mCurrentState == STATE_PLAYING
                    || mCurrentState == STATE_PAUSED || mCurrentState == STATE_PLAYBACK_COMPLETED) {
                mMediaPlayer.seekTo(msec);
                Log.d(TAG, "seeked");
            }
        }
    }

    public void stop() {
        Log.d(TAG, "stop...");
        if (mMediaPlayer != null) {
            if (mCurrentState == STATE_PREPARED || mCurrentState == STATE_PLAYING
                    || mCurrentState == STATE_PAUSED || mCurrentState == STATE_PLAYBACK_COMPLETED) {
                mMediaPlayer.stop();
                mCurrentState = STATE_STOPED;
                Log.d(TAG, "stoped");
            }
        }
    }

    public void release() {
        Log.d(TAG, "release...");
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mCurrentState = STATE_RELEASED;
            mMediaPlayer = null;
            Log.d(TAG, "released");
        }
    }

    public int getDuration() {
        int duration = 0;
        if (mMediaPlayer != null) {
            if (mCurrentState == STATE_PREPARED || mCurrentState == STATE_PLAYING
                    || mCurrentState == STATE_PAUSED || mCurrentState == STATE_PLAYBACK_COMPLETED) {
                duration = mMediaPlayer.getDuration();
            }
        }
        return duration;
    }

    public int getCurrentPosition() {
        int currentPosition = 0;
        if (mMediaPlayer != null) {
            if (mCurrentState == STATE_PREPARED || mCurrentState == STATE_PLAYING
                    || mCurrentState == STATE_PAUSED || mCurrentState == STATE_PLAYBACK_COMPLETED) {
                currentPosition = mMediaPlayer.getCurrentPosition();
            }
        }
        return currentPosition;
    }

    public boolean isPlaying() {
        boolean isPlaying = false;
        try {
            isPlaying = mMediaPlayer.isPlaying();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isPlaying;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion");
        mCurrentState = STATE_PLAYBACK_COMPLETED;
        if (mOnCompletionListener != null) {
            mOnCompletionListener.onCompletion(null);
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        mCurrentState = STATE_ERROR;
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.e(TAG, "Error: MEDIA_ERROR_SERVER_DIED what=" + what + ", extra="
                        + extra + "; release MediaPlayer and reset MediaPlayer");
            default:
                Log.e(TAG, "Error: Song format is not correct! default what=" + what
                        + ",extra=" + extra + "; release MediaPlayer and reset MediaPlayer");
                break;
        }
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(null, what, extra);
        }
        return false;
    }
}
