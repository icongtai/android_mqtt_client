package com.zebra.module.mqtt.net;

import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import com.zebra.module.mqtt.log.Logger;
import com.zebra.module.mqtt.service.MqttService;

import static android.content.Context.CONNECTIVITY_SERVICE;
/**
 * 网络状态管理
 * @author xining
 * @date 2019/3/4
 */
public class ConnectivityManager {
    public static final String TAG= ConnectivityManager.class.getSimpleName();
    public MqttService mMqttService;

    public ConnectivityManager(MqttService mMqttService) {
        this.mMqttService = mMqttService;
    }

    public void start(MqttService mqttService) {
        android.net.ConnectivityManager connManager = (android.net.ConnectivityManager) mqttService
                .getSystemService(CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connManager.registerDefaultNetworkCallback(new android.net.ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    super.onAvailable(network);
                }

                @Override
                public void onLosing(Network network, int maxMsToLive) {
                    super.onLosing(network, maxMsToLive);
                }

                @Override
                public void onLost(Network network) {
                    super.onLost(network);
                }

                @Override
                public void onUnavailable() {
                    super.onUnavailable();
                }

                @Override
                public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities);
                }

                @Override
                public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
                    super.onLinkPropertiesChanged(network, linkProperties);
                }
            });
        }
    }

    public boolean isConnected() {
        android.net.ConnectivityManager connManager = (android.net.ConnectivityManager) mMqttService
                .getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
        boolean connect = networkInfo!=null&&networkInfo.isConnected();
        Logger.d(TAG, "network "+connect);
        return connect;
    }
}
