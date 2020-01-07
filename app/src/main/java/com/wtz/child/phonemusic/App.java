package com.wtz.child.phonemusic;

import android.app.Application;

public class App extends Application {

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
