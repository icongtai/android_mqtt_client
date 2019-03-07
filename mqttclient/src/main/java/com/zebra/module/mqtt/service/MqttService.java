package com.zebra.module.mqtt.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

import com.zebra.module.mqtt.client.StoreMqttConnectOptions;
import com.zebra.module.mqtt.exception.MqttNotConnectedException;
import com.zebra.module.mqtt.exception.NetworkNotConnectedException;
import com.zebra.module.mqtt.job.JobManager;
import com.zebra.module.mqtt.log.Logger;
import com.zebra.module.mqtt.msg.MqttMessageWrap;
import com.zebra.module.mqtt.net.ConnectivityManager;
import com.zebra.module.mqtt.service.connection.MqttConntion;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.zebra.module.mqtt.client.OptStore.Publish;
import static com.zebra.module.mqtt.client.OptStore.Subscribe;
import static com.zebra.module.mqtt.client.OptStore.UnSubscribe;

/**
 * @author xining
 * @date 2019/3/4
 */
public class MqttService extends Service {
    public static final String TAG = MqttService.class.getSimpleName();
    public Map<String, MqttConntion> collections = new ConcurrentHashMap<>();
    /**
     * 任务管理
     */
    public JobManager jobManager = new JobManager(this);
    /**
     * 网络管理
     */
    public ConnectivityManager connectivityManager = new ConnectivityManager(this);

    public ZebraMqttServiceBinder iBinder = new ZebraMqttServiceBinder() {
        public MqttService getZebraMqttService() {
            return MqttService.this;
        }
    };

    public static void startService(Context context) {
        Intent intent = new Intent(context, MqttService.class);
        context.startService(intent);
    }

    public static void bindService(Context context, ServiceConnection serviceConnection) {
        Intent intent = new Intent(context, MqttService.class);
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        jobManager.refresheCycleJob(this, "MqttService start");
        connectivityManager.start(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (collections != null && collections.size() > 0) {
            Iterator<Map.Entry<String, MqttConntion>> it = collections.entrySet().iterator();
            while (it.hasNext()) {
                MqttConntion conntion = it.next().getValue();
                conntion.close();
                it.remove();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Logger.d(TAG, "onBind");
        return iBinder;
    }

    public MqttConntion getZebraMqttConntion(String uniqueId) {
        MqttConntion conntion = collections.get(uniqueId);
        return conntion;
    }

    public IMqttToken excute(int type, String uniconId, MqttMessageWrap message) {
        Logger.d(TAG, "excute " + type);
        IMqttToken token = null;
        if (!connectivityManager.isConnected()) {
            message.callback.onFailure(null, new NetworkNotConnectedException("excute " + type + " " + uniconId));
            return null;
        }
        MqttConntion conntion = getZebraMqttConntion(uniconId);
        if (conntion != null) {
            switch (type) {
                case Publish:
                    token = conntion.publishAsync(message);
                    break;
                case Subscribe:
                    token = conntion.subscribe(message);
                    break;
                case UnSubscribe:
                    token = conntion.unSubscribe(message);
                    break;
            }
        }
        return token;
    }


    public IMqttToken createOrReconnectAsync(String uniconId, String clientId, String broker, StoreMqttConnectOptions options, IMqttActionListener callback) {
        Logger.d(TAG, "createOrReconnectAsync");
        IMqttToken token = null;
        if (!connectivityManager.isConnected()) {
            Logger.d(TAG, "no network");
            callback.onFailure(null, new NetworkNotConnectedException("connect"));
            return null;
        }
        MqttConntion conntion = getZebraMqttConntion(uniconId);
        if (conntion != null) {
            Logger.d(TAG, "find exist - reconnectAsync");
            conntion.reconnectAsync(options);
            callback.onFailure(null, new MqttNotConnectedException());
            return null;
        }
        try {
            Logger.d(TAG, "try connectAsync ");
            conntion = MqttConntion.create(this, uniconId,clientId, broker);
            addZebraMqttConntion(conntion);
            token = conntion.connectAsync(options, callback);
        } catch (MqttException e) {
            Logger.e(TAG, "createOrReconnectAsync ", e);
            callback.onFailure(null, e);
        }
        return token;
    }

    public void addZebraMqttConntion(MqttConntion conntion) {
        Logger.d(TAG, "addZebraMqttConntion");
        collections.put(conntion.uniconId, conntion);
    }

    public void removeZebraMqttConntion(MqttConntion mqttConntion, String reason) {
        Logger.d(TAG, "removeZebraMqttConntion " + reason);
        collections.remove(mqttConntion.uniconId);
        mqttConntion.close();
    }

    public void refresheCycleJob(String reason) {
        Logger.d(TAG, "refresheCycleJob");
        jobManager.refresheCycleJob(this, reason);
    }

    /**
     * 一般情况是密码修改
     *
     * @param options
     */
    public void updateConnectionOption(StoreMqttConnectOptions options) {
        Logger.d(TAG, "updateConnectionOption");
        MqttConntion conntion = getZebraMqttConntion(options.conntionUniqueId);
        if (conntion != null) {
            conntion.connectAsync(options, null);
        }
        jobManager.updateConnectionOption(options, this);
    }

    public abstract class ZebraMqttServiceBinder extends Binder {
        public abstract MqttService getZebraMqttService();
    }
}
