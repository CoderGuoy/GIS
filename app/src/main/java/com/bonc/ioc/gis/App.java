package com.bonc.ioc.gis;


import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;

public class App extends Application {
    private static App sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //Dex分包初始化
        MultiDex.install(base);
    }

    public static App getInstance() {
        return sInstance;
    }
}
