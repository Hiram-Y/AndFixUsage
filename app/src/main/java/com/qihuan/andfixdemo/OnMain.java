package com.qihuan.andfixdemo;

import android.util.Log;

/**
 * Created by qihuan on 16/5/31.
 */
public class OnMain {

    public String changeButton() {
       Log.d("Main", "has changed");
        return "this is a test 变化了的" + talk();
    }

    public String talk(){
        Log.d("Main", "has changed changed");
        return "normal 变化了的 ";
    }
}
