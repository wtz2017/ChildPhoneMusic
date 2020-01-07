package com.wtz.child.phonemusic;

import android.content.SharedPreferences;

import com.wtz.child.phonemusic.utils.LogUtils;
import com.wtz.child.phonemusic.utils.MusicIcon;

public class TimeManager {
    private static final String TAG = "TimeManager";

    private static TimeManager INSTANCE;
    private SharedPreferences mSp;

    private long DEFAULT_MAX_PLAY_TIME_LENGTH = 15 * 60 * 1000;// 默认最大播放时长，毫秒
    private long mMaxPlayTimeLength = DEFAULT_MAX_PLAY_TIME_LENGTH;// 最大播放时长，毫秒
    private long DEFAULT_PLAN_REST_TIME_LENGTH = 10 * 60 * 1000;// 默认计划休息时长，毫秒
    private long mPlanRestTimeLength = DEFAULT_PLAN_REST_TIME_LENGTH;// 计划休息时长，毫秒

    private boolean isPlayStarted;
    private long mStartPlayTimeStamp;//开始播放时间戳，毫秒
    private long mPlayedTimeMill;//已播放时长，毫秒
    private long mRestTimeStamp;//休息时间戳，毫秒

    private boolean canPlay = true;

    public static TimeManager getInstance() {
        if (INSTANCE == null) {
            synchronized (TimeManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TimeManager();
                }
            }
        }
        return INSTANCE;
    }

    private TimeManager() {
        mSp = Preferences.getInstance().getSP();
        mMaxPlayTimeLength = mSp.getLong(Preferences.KEY_MAX_PLAY_TIME_MILL, DEFAULT_MAX_PLAY_TIME_LENGTH);
        mPlanRestTimeLength = mSp.getLong(Preferences.KEY_PLAN_REST_TIME_MILL, DEFAULT_PLAN_REST_TIME_LENGTH);
        mPlayedTimeMill = mSp.getLong(Preferences.KEY_PLAYED_TIME_MILL, 0);
        mRestTimeStamp = mSp.getLong(Preferences.KEY_REST_TIME_STAMP, 0);
    }

    public void setMaxPlayTimeMinutes(int minutes) {
        LogUtils.d(TAG, "setMaxPlayTimeMinutes: " + minutes);
        mMaxPlayTimeLength = minutes * 60 * 1000;
        SharedPreferences.Editor editor = mSp.edit();
        editor.putLong(Preferences.KEY_MAX_PLAY_TIME_MILL, mMaxPlayTimeLength);
        editor.apply();
    }

    public int getMaxPlayTimeMinutes() {
        return (int) (mMaxPlayTimeLength / 1000 / 60);
    }

    public void setPlanRestTimeMinutes(int minutes) {
        LogUtils.d(TAG, "setPlanRestTimeMinutes: " + minutes);
        mPlanRestTimeLength = minutes * 60 * 1000;
        SharedPreferences.Editor editor = mSp.edit();
        editor.putLong(Preferences.KEY_PLAN_REST_TIME_MILL, mPlanRestTimeLength);
        editor.apply();
    }

    public int getPlanRestTimeMinutes() {
        return (int) (mPlanRestTimeLength / 1000 / 60);
    }

    public void saveTime() {
        SharedPreferences.Editor editor = mSp.edit();
        editor.putLong(Preferences.KEY_PLAYED_TIME_MILL, mPlayedTimeMill);
        editor.putLong(Preferences.KEY_REST_TIME_STAMP, mRestTimeStamp);
        editor.apply();
    }

    public void startPlayTiming() {
        if (!isPlayStarted) {
            mStartPlayTimeStamp = System.currentTimeMillis();
            isPlayStarted = true;
        }
    }

    public void stopPlayTiming() {
        if (isPlayStarted) {
            mPlayedTimeMill += System.currentTimeMillis() - mStartPlayTimeStamp;
            isPlayStarted = false;
        }
    }

    public boolean canPlay() {
        if (getRestTimeLength() > 0) {
            canPlay = false;
        } else if (mPlayedTimeMill > mMaxPlayTimeLength) {
            canPlay = false;
            mRestTimeStamp = System.currentTimeMillis();
            mPlayedTimeMill = 0;
        } else {
            canPlay = true;
        }
        return canPlay;
    }

    public long getRestTimeLength() {
        long rest = mPlanRestTimeLength - (System.currentTimeMillis() - mRestTimeStamp);
        return rest;
    }

}
