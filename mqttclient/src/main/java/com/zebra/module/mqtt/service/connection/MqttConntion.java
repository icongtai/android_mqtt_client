package com.zebra.module.mqtt.service.connection;


import android.content.Context;

import com.zebra.module.mqtt.client.StoreMqttConnectOptions;
import com.zebra.module.mqtt.log.Logger;
import com.zebra.module.mqtt.msg.MqttMessageWrap;
import com.zebra.module.mqtt.service.MqttService;
import com.zebra.module.mqtt.service.message.MessageDispatcher;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;


/**
 * @author xining
 * @date 2019/3/4
 */
public class MqttConntion {
    public static final String TAG = MqttConntion.class.getSimpleName();
    public static final long TimeOut = 10000;
    @ZebraMqttConntionStatus
    public int status;
    public MqttService service;
    public String uniconId;
    public String clientId;
    public MqttAsyncClient mMqttClient;
    private MqttClientPersistence persistence = null;
    /**
     * 消息分发
     */
    public MessageDispatcher dispatcher = new MessageDispatcher();

    public IMqttActionListener connectLister = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Logger.d(TAG, "connectLister onSuccess");
            setStatus(Conntioned);
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Logger.d(TAG, "connectLister onFailure");
            checkMqttException(exception, "connectLister");
        }
    };

    public MqttCallbackExtended mqttCallbackExtended = new MqttCallbackExtended() {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            setStatus(Conntioned);
            dispatcher.connectComplete(service, reconnect, serverURI);
            Logger.d(TAG, "MqttCall connectComplete");
        }

        @Override
        public void connectionLost(Throwable cause) {
            setStatus(UnConntioned);
            Logger.d(TAG, "MqttCall connectionLost");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Logger.d(TAG, "MqttCall messageArrived");
            setStatus(Conntioned);
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Logger.d(TAG, "MqttCall deliveryComplete");
            setStatus(Conntioned);
        }
    };


    public MqttConntion(MqttService service, String uniconId, String clientId, String serverURI) throws MqttException {
        this.service = service;
        setStatus(Init);
        this.uniconId = uniconId;
        this.clientId = clientId;
        checkMqttClientPersistence();
        this.mMqttClient = new MqttAsyncClient(serverURI, clientId, persistence);
        this.mMqttClient.setCallback(mqttCallbackExtended);
        Logger.d(TAG, "new MqttConntion(" + uniconId + "," + serverURI + ")");
    }

    private void checkMqttClientPersistence() {
        if (persistence == null) {
            File myDir = service.getExternalFilesDir(TAG);
            if (myDir == null) {
                // No external storage, use internal storage instead.
                myDir = service.getDir(TAG, Context.MODE_PRIVATE);
                if (myDir == null) {
                    //Shouldn't happen.
                    return;
                }
            }
            persistence = new MqttDefaultFilePersistence(
                    myDir.getAbsolutePath());
        }
    }


    public static MqttConntion create(MqttService service, String uniconId, String clientId, String serverURI) throws MqttException {
        MqttConntion conntion = new MqttConntion(service, uniconId, clientId, serverURI);
        return conntion;
    }


    public IMqttToken subscribe(MqttMessageWrap message) {
        IMqttToken token = null;
        try {
            token = mMqttClient.subscribe(message.topic, message.qos, null, message.callback, message.messageListeners);
        } catch (MqttException e) {
            Logger.e(TAG, "subscribe error ", e);
            checkMqttException(e, "subscribe");
            message.callback.onFailure(null, e);
        }
        return token;
    }

    public IMqttToken unSubscribe(MqttMessageWrap message) {
        IMqttToken token = null;
        try {
            token = mMqttClient.unsubscribe(message.topic, null, message.callback);
        } catch (MqttException e) {
            Logger.e(TAG, "unSubscribe error ", e);
            checkMqttException(e, "unSubscribe");
            message.callback.onFailure(null, e);
        }
        return token;
    }

    public void reconnectAsync(StoreMqttConnectOptions options) {
        try {
            mMqttClient.reconnect();
        } catch (MqttException e) {
            checkMqttException(e, "reconnectAsync");
            Logger.e(TAG, "reconnectAsync error ", e);
        }
    }

//    public void closedAsync() {
//        try {
//            mMqttClient.close();
//        } catch (MqttException e) {
//            e.printStackTrace();
//        }
//    }

    public void connect(MqttConnectOptions options) throws MqttException {
        try {
            Logger.d(TAG, "connect ");
            if (status != Conntioning && status != Conntioned) {
                setStatus(Conntioning);
                mMqttClient.connect(options, null, connectLister).waitForCompletion(TimeOut);
            }
        } catch (MqttException e) {
            Logger.e(TAG, "connect error ", e);
            checkMqttException(e, "connect");
            throw e;
        }
    }

    public IMqttToken connectAsync(StoreMqttConnectOptions options, @Nullable final IMqttActionListener callback) {
        Logger.d(TAG, "connectAsync");
        IMqttToken token = null;
        try {
            if (status != Conntioning && status != Conntioned) {
                setStatus(Conntioning);
                token = mMqttClient.connect(options.getMqttConnectOptions(), null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        if (callback != null) {
                            callback.onSuccess(asyncActionToken);
                        }
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        if (callback != null) {
                            callback.onFailure(asyncActionToken, exception);
                        }
                        checkMqttException(exception, "connectAsync callback ");
//                        callback.onFailure(asyncActionToken, exception);
                    }
                });
            }
        } catch (MqttException e) {
            Logger.e(TAG, "connectAsync error ", e);
            callback.onFailure(null, e);
            checkMqttException(e, "connectAsync");
        }
        return token;
    }

    public void checkMqttException(Throwable e, String from) {
        Logger.d(TAG, "checkMqttException from " + from);
        if (e == null) {
            return;
        }
        if (e instanceof MqttException) {
            int code = ((MqttException) e).getReasonCode();
            Logger.d(TAG, "checkMqttException " + e.toString() + " from " + from);
            switch (code) {
                case MqttException.REASON_CODE_CLIENT_CONNECTED:
                    setStatus(Conntioned);
                    break;
                case MqttException.REASON_CODE_CLIENT_CLOSED:
                    setStatus(Closed);
                    service.removeZebraMqttConntion(this, "REASON_CODE_CLIENT_CLOSED");
                    break;
                case MqttException.REASON_CODE_CONNECTION_LOST:
                    setStatus(Closed);
                    service.removeZebraMqttConntion(this, "REASON_CODE_CONNECTION_LOST");
                    break;
                case MqttException.REASON_CODE_CLIENT_TIMEOUT:
                    setStatus(Closed);
                    service.removeZebraMqttConntion(this, "REASON_CODE_CLIENT_TIMEOUT");
                    break;
                case MqttException.REASON_CODE_FAILED_AUTHENTICATION:
                    setStatus(Closed);
                    service.removeZebraMqttConntion(this, "REASON_CODE_FAILED_AUTHENTICATION");
                    break;
                default:
                    setStatus(UnConntioned);
                    break;
            }
        }
    }

    public void publish(MqttMessageWrap mqttMessageWrap) throws MqttException {
        Logger.d(TAG, "publish ");
        try {
            mMqttClient.publish(mqttMessageWrap.topic, mqttMessageWrap.mqttMessage).waitForCompletion(TimeOut);
        } catch (MqttException e) {
            Logger.e(TAG, "publish error ", e);
            checkMqttException(e, "publish");
            throw e;
        }
    }

    public IMqttDeliveryToken publishAsync(final MqttMessageWrap message) {
        Logger.d(TAG, "publishAsync");
        if (message == null) {
            return null;
        }
        IMqttDeliveryToken token = null;
        try {
            token = mMqttClient.publish(message.topic, message.mqttMessage, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (message.callback != null) {
                        message.callback.onSuccess(asyncActionToken);
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    if (message.callback != null) {
                        message.callback.onFailure(asyncActionToken, exception);
                    }
                    checkMqttException(exception, "publishAsync callback");
                }
            });
        } catch (MqttException e) {
            Logger.e(TAG, "publishAsync error ", e);
            checkMqttException(e, "publish");
            message.callback.onFailure(null, e);
        }
        return token;
    }

    public void close() {
        Logger.d(TAG, "close()");
        try {
            mMqttClient.setCallback(null);
        } catch (Exception e) {
            Logger.e(TAG, "close error ", e);
        }
        try {
            mMqttClient.disconnectForcibly();
        } catch (MqttException e) {
            Logger.e(TAG, "close error ", e);
        }
        try {
            mMqttClient.close();
        } catch (MqttException e) {
            Logger.e(TAG, "close error ", e);
        }
    }

    public int getStatus() {
        if (mMqttClient.isConnected()) {
            setStatus(Conntioned);
        } else {
            if (status == Conntioned) {
                setStatus(UnConntioned);
            }
        }
        return status;
    }

    public void cleanError() {
    }

    public void setStatus(int newStatus) {
        this.status = newStatus;
        if (status == Conntioned && mMqttClient.isConnected()) {
            cleanError();
        }
    }

    public static final int Init = 1;
    public static final int Conntioning = 2;
    public static final int Conntioned = 3;
    public static final int UnConntioned = 4;
    public static final int Closed = 5;

    @IntDef({Init, Conntioning, Conntioned, UnConntioned})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ZebraMqttConntionStatus {
    }
}
