package com.zebra.module.mqtt.client;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.text.TextUtils;

import com.zebra.module.mqtt.job.CycleJobGroup;
import com.zebra.module.mqtt.job.JobManager;
import com.zebra.module.mqtt.log.Logger;
import com.zebra.module.mqtt.msg.MqttMessageWrap;
import com.zebra.module.mqtt.service.MqttService;
import com.zebra.module.mqtt.service.connection.MqttConntion;
import com.zebra.module.mqtt.service.message.MessageDispatcher;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.util.concurrent.CopyOnWriteArrayList;

import static com.zebra.module.mqtt.client.OptStore.Publish;
import static com.zebra.module.mqtt.client.OptStore.RefreshCycleJob;
import static com.zebra.module.mqtt.client.OptStore.Subscribe;
import static com.zebra.module.mqtt.client.OptStore.UnSubscribe;
import static com.zebra.module.mqtt.service.connection.MqttConntion.Conntioned;
import static com.zebra.module.mqtt.service.connection.MqttConntion.Init;
import static com.zebra.module.mqtt.service.connection.MqttConntion.UnConntioned;

/**
 * mqtt client
 *
 * @author xining
 * @date 2019/3/4
 */
public class MqttClient {
    public static final String TAG = "MqttClient";
    private String broker;
    private String uniconId;
    public String clientId;

    public StoreMqttConnectOptions getZebraStoreMqttConnectOptions() {
        return mMqttConnectOptions;
    }

    private StoreMqttConnectOptions mMqttConnectOptions;

    private MqttService mMqttService;
    private Context context;
    private final CopyOnWriteArrayList<OptStore> optStores = new CopyOnWriteArrayList<>();

    public BroadcastReceiver connectionRecevier = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Logger.d(TAG, "MqttCall onReceive");
            redoOpt("onReceive");
        }
    };

    private IMqttActionListener connectMqttActionListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            redoOpt("on connection onSuccess");
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            synchronized (optStores) {
                if (optStores.size() > 0) {
                    for (OptStore opt : optStores) {
                        if (opt.optType == Publish) {
                            optStores.remove(opt);
                            opt.message.callback.onFailure(asyncActionToken, exception);
                        }
                    }
                }
            }
        }
    };

    public MqttClient(Context context, String clientId, String broker, StoreMqttConnectOptions mMqttConnectOptions) {
        this.context = context;
        checkContext();
        this.broker = broker;
        MqttConnectOptions.validateURI(broker);
        this.mMqttConnectOptions = mMqttConnectOptions;
        this.uniconId = getUniconId(broker);
        this.clientId = clientId;
        MqttService.startService(context);
    }

    /**
     * publish message
     *
     * @param message
     * @return
     */
    public IMqttToken publishAsync(MqttMessageWrap message) {
        Logger.d(TAG, "publishAsync ");
        if (message == null || TextUtils.isEmpty(message.topic) || message.callback == null) {
            throw new RuntimeException("require topic and callback");
        }
        if (message.mqttMessage == null) {
            throw new RuntimeException("require mqttMessage");
        }
        return autoCheckServiceOptSync(Publish, message);
    }

    /**
     * @param message
     * @return
     */
    public IMqttToken subscribeAsync(MqttMessageWrap message) {
        Logger.d(TAG, "subscribeAsync ");
        if (message == null || TextUtils.isEmpty(message.topic) || message.callback == null) {
            throw new RuntimeException("require topic and callback");
        }
        if (message.messageListeners == null) {
            throw new RuntimeException("require messageListeners");
        }
        return autoCheckServiceOptSync(Subscribe, message);
    }

    /**
     * @param message
     * @return
     */
    public IMqttToken unSubscribeAsync(MqttMessageWrap message) {
        Logger.d(TAG, "unSubscribeAsync ");
        if (message == null || TextUtils.isEmpty(message.topic) || message.callback == null) {
            throw new RuntimeException("require topic and callback");
        }
        return autoCheckServiceOptSync(UnSubscribe, message);
    }

    /**
     * 添加/删除任务
     *
     * @param jobGroup
     */
    public void addOrRemoveJobGroup(CycleJobGroup jobGroup, boolean add) {
        Logger.d(TAG, "addOrRemoveJobGroup ");
        if (jobGroup == null || jobGroup.jobsForWrite == null || jobGroup.jobsForWrite.size() == 0) {
            throw new RuntimeException("jobGroup not valid");
        }
        checkContext();
        if (add) {
            JobManager.createOrUpdateCycleJobGroupOffline(jobGroup, context);
        } else {
            JobManager.deleteCycleJobGroupOffline(jobGroup, context);
        }
        autoCheckServiceOptSync(RefreshCycleJob, null);
    }

    public IMqttToken autoCheckServiceOptSync(int type, MqttMessageWrap message) {
        Logger.d(TAG, "autoCheckServiceOptSync ");
        IMqttToken token = null;
        if (mMqttService == null) {
            createAndAddOpt(type, message);
            MqttService.bindService(context, serviceConnection);
        } else {
            token = doOptInServiceSync(null, type, message, "autoCheckServiceOptSync direct");
        }
        return token;
    }

    public void updateConnectionOption(StoreMqttConnectOptions options) {
        Logger.d(TAG, "updateConnectionOption");
        this.mMqttConnectOptions = options;
        if (mMqttService != null) {
            mMqttService.updateConnectionOption(options);
        }
    }

    public static String getUniconId(String broker) {
        return broker;
    }


    private void createAndAddOpt(int type, MqttMessageWrap message) {
        OptStore optStore = new OptStore(type);
        optStore.message = message;
        addOpt(optStore);

    }

    private void addOpt(OptStore optStore) {
        synchronized (optStores) {
            if (!optStores.contains(optStore)) {
                optStores.add(optStore);
            }
        }
    }

    private void redoOpt(String log) {
        Logger.d(TAG,"redoOpt "+log);
        synchronized (optStores) {
            for (OptStore opt : optStores) {
                doOptInServiceSync(opt, opt.optType, opt.message, "redoOpt " + log);
            }
        }
    }


    private void refresheCycleJob(String reason) {
        if (mMqttService != null) {
            mMqttService.refresheCycleJob(reason);
        }
    }

    private void checkContext() {
        if (context == null) {
            throw new RuntimeException("Context can not be null");
        }
    }

    private IMqttToken createOrReconnectAsync() {
        Logger.d(TAG, "createOrReconnectAsync");
        if (mMqttService != null) {
            IMqttToken token = mMqttService.createOrReconnectAsync(uniconId, clientId, broker, mMqttConnectOptions, connectMqttActionListener);
            return token;
        }
        return null;
    }


    public void doRemove(OptStore opt) {
        if (opt != null) {
            Logger.d(TAG,"doRemove "+opt);
            optStores.remove(opt);
        }
    }

    private IMqttToken doOptInServiceSync(OptStore opt, int type, MqttMessageWrap message, String log) {
        Logger.d(TAG, "doOptInServiceSync " + log);
        IMqttToken token = null;
        if (mMqttService == null) {
            //should not happen
            doRemove(opt);
            Logger.e(TAG, "mMqttService is null");
            return null;
        }
        if (type == RefreshCycleJob) {
            doRemove(opt);
            refresheCycleJob("type " + type);
            return null;
        }
        token = doOptInConntionSync(opt, type, message);
        return token;
    }

    public IMqttToken doOptInConntionSync(OptStore opt, int type, MqttMessageWrap message) {
        Logger.d(TAG, "doOptInConntionSync");
        IMqttToken token = null;
        MqttConntion conntion = getZebraMqttConntion();
        if (conntion == null || conntion.getStatus() == Init || conntion.getStatus() == UnConntioned) {
//            createAndAddOpt(type, message);
            return createOrReconnectAsync();
        }
        if (conntion != null && conntion.getStatus() == Conntioned) {
            doRemove(opt);
            Logger.d(TAG, "excute " + type);
            token = mMqttService.excute(type, uniconId, message);
        }
        return token;
    }

    private MqttConntion getZebraMqttConntion() {
        MqttConntion conntion = mMqttService.getZebraMqttConntion(uniconId);
        return conntion;
    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mMqttService = ((MqttService.ZebraMqttServiceBinder) binder).getZebraMqttService();
            redoOpt("redo after onServiceConnected");
            MessageDispatcher.registMqttCallback(context, connectionRecevier);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mMqttService = null;
            synchronized (optStores) {
                optStores.clear();
            }
            MessageDispatcher.unRegist(context, connectionRecevier);
        }
    };

    public MqttService testInit(IBinder binder) {
        mMqttService = ((MqttService.ZebraMqttServiceBinder) binder).getZebraMqttService();
        return mMqttService;
    }
}
