package com.wtz.child.phonemusic;

import android.content.Context;
import android.content.SharedPreferences;

public class Preferences {

    private static final String SP_NAME = "config";

    public static final String KEY_LAST_AUDIO_PATH = "last_audio_path";
    public static final String KEY_AUDIO_PLAY_MODE = "audio_play_mode";

    public static SharedPreferences getSP(Context context) {
        if (context == null) {
            return null;
        }
        return context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);
    }

}
