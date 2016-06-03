package com.qihuan.andfixdemo;

import android.content.Context;
import android.content.SharedPreferences;

import com.qihuan.andfixdemo.base.ApkUtil;
import com.qihuan.andfixdemo.base.LogUtil;

public class MyCrashHandler implements Thread.UncaughtExceptionHandler {

    private final Thread.UncaughtExceptionHandler mDefaultHandler;
    private final SharedPreferences sp;

    public MyCrashHandler() {
        sp = WApplication.getContext().getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        mDefaultHandler = Thread.currentThread().getUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, final Throwable throwable) {
        //捕捉到异常，对崩溃进行记录，如果出现连续崩溃的情况，则考虑是热插件的问题，进行清理工作
        long lastCrash = sp.getLong("last_crash", 0);
        long currentCrash = System.currentTimeMillis();
        LogUtil.d(" last " + lastCrash + " current " + currentCrash + " diff " + (currentCrash - lastCrash));
        if (currentCrash - lastCrash < 10000) {
            LogUtil.d("do clean");
            //10秒内崩溃两次，考虑清理插件
            ApkUtil.cleanApatch();
        }
        sp.edit().putLong("last_crash", currentCrash).apply();

        //交给默认处理handler处理，或者直接退出应用
        if (null != mDefaultHandler) {
            LogUtil.d("transfer to default " + mDefaultHandler);
            mDefaultHandler.uncaughtException(thread, throwable);
        } else {
            LogUtil.d("退出程序 ");
            // 退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }
}