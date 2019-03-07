package com.zebra.module.mqtt.service.message;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;


/**
 * @author xining
 * @date 2019/3/5
 */
public class MessageDispatcher {
    public static final String MqttCallbackACTION = "com.zebra.module.mqtt.service.conntion";
    public static final String MqttCallback = "MqttCallback";
    public static final int MqttCallback_ConnectionComplete = 1;
    public static final String Reconnect = "Reconnect";
    public static final String ConnectBroker = "ConnectBroker";

    public static void registMqttCallback(Context context, BroadcastReceiver receiver) {
        IntentFilter intent = new IntentFilter(MqttCallbackACTION);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, intent);
    }

    public static void unRegist(Context context, BroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }

    public void connectComplete(Context context, boolean reconnect, String serverURI) {
        Intent intent = new Intent(MqttCallbackACTION);
        intent.putExtra(MqttCallback, MqttCallback_ConnectionComplete);
        intent.putExtra(Reconnect, reconnect);
        intent.putExtra(ConnectBroker, serverURI);
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }
}
