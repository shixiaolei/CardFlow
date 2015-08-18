package com.demo.card;

import android.app.Application;
import android.content.res.Resources;

public class App extends Application {

    private static App sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    public static App get() {
        return sInstance;
    }

    public static Resources res() {
        return sInstance.getResources();
    }
}
