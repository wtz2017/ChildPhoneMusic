package com.wtz.child.phonemusic;

import android.app.Application;

public class App extends Application {

    public static final String ACTION_STOP_PLAY = "com.wtz.child.phonemusic.STOP_PLAY";
    public static final String ACTION_PLAY_STOPPED = "com.wtz.child.phonemusic.PLAY_STOPPED";

    private static App INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;
    }

    public static App getApp() {
        return INSTANCE;
    }

}
