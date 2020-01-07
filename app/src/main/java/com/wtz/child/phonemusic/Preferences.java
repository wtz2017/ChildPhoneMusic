package com.wtz.child.phonemusic;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {

    private static Preferences INSTANCE;
    private SharedPreferences mSp;

    private static final String SP_NAME = "config";

    public static final String KEY_PASS_WORD = "pass_word";
    public static final String DEFAULT_PASS_WORD = "123456";

    public static final String KEY_LAST_AUDIO_PATH = "last_audio_path";
    public static final String KEY_AUDIO_PLAY_MODE = "audio_play_mode";

    public static final String KEY_MAX_PLAY_TIME_MILL = "max_play_time_mill";
    public static final String KEY_PLAN_REST_TIME_MILL = "plan_rest_time_mill";
    public static final String KEY_PLAYED_TIME_MILL = "played_time_mill";
    public static final String KEY_REST_TIME_STAMP = "rest_time_stamp";

    public static Preferences getInstance() {
        if (INSTANCE == null) {
            synchronized (Preferences.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Preferences();
                }
            }
        }
        return INSTANCE;
    }

    private Preferences() {}

    public SharedPreferences getSP() {
        if (mSp == null) {
            mSp = App.getApp().getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
        }
        return mSp;
    }

}
