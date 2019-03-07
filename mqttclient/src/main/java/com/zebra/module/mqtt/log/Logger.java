package com.zebra.module.mqtt.log;

import android.util.Log;

/**
 * @author xining
 * @date 2019/3/4
 */
public class Logger {
    public static final String TAG="zebra_mqtt";
    public static void d(String tag, String msg) {
        Log.d(TAG, "["+tag+"] "+msg);
    }

    public static void e(String tag, String msg) {
        Log.e(TAG, "["+tag+"] "+msg);
    }

    public static void e(String tag, String msg, Throwable e) {
        Log.e(TAG, "["+tag+"] "+msg,e);
    }
}
