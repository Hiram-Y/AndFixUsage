package com.qihuan.andfixdemo;

import android.app.Application;
import android.content.Context;

import com.qihuan.andfixdemo.base.ApkUtil;

/**
 * Created by qihuan on 16/5/31.
 */
public class WApplication extends Application {
    private static final String TAG = WApplication.class.getSimpleName();
    private static Context context;

    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;

        Thread.currentThread().setUncaughtExceptionHandler(new MyCrashHandler());

        ApkUtil.initPatch(context, "1.3");

        ApkUtil.checkApatchUpdate("1.3");
    }
}
