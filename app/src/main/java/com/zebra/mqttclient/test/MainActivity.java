package com.zebra.mqttclient.test;

import android.os.Bundle;
import android.view.View;

import com.zebra.module.mqtt.client.MqttClient;
import com.zebra.module.mqtt.client.StoreMqttConnectOptions;
import com.zebra.module.mqtt.job.CycleJob;
import com.zebra.module.mqtt.job.CycleJobGroup;
import com.zebra.module.mqtt.log.Logger;
import com.zebra.module.mqtt.msg.MqttMessageWrap;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import androidx.appcompat.app.AppCompatActivity;
/**
 * @author xining
 * @date 2019/3/4
 */
public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";
    MqttClient mMqttClient;
    public static final String broker = "tcp://iot.eclipse.org:1883";
    public String clientId="clientId";
    MqttMessageWrap message = getZebraMqttMessage("sendMessage button click");
    public IMqttMessageListener listener = new IMqttMessageListener() {

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Logger.d(TAG, "messageArrived " + topic + "  " + new String(message.getPayload()));
        }
    };

    public IMqttActionListener callback = new IMqttActionListener() {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            Logger.d(TAG, "sendMessage onSuccess " + asyncActionToken);
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Logger.d(TAG, "sendMessage onFailure " + asyncActionToken);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StoreMqttConnectOptions options = getMqttConnectOptions("", "");
        mMqttClient = new MqttClient(this.getApplicationContext(),"clientId",broker, options);
        mMqttClient.updateConnectionOption(options);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    public void sub(View view) {
        message.messageListeners = listener;
        message.callback = callback;
        mMqttClient.subscribeAsync(message);
    }

    public void unSub(View view) {
        message.callback = callback;
        mMqttClient.unSubscribeAsync(message);
    }

    public void sendMessage(View view) {
        message.callback = callback;
        message.callback = callback;
        mMqttClient.publishAsync(message);
    }

    public static MqttMessageWrap getZebraMqttMessage(String msg) {
        MqttMessageWrap message = new MqttMessageWrap();
        message.mqttMessage = getMqttMessage(msg);
        message.topic = "zebra_test";
        return message;
    }

    private static MqttMessage getMqttMessage(String message) {
        MqttMessage mqttMessage = new MqttMessage(message.getBytes());
        mqttMessage.setQos(1);
        mqttMessage.setRetained(false);
        return mqttMessage;
    }


    public StoreMqttConnectOptions getMqttConnectOptions(String userName, String password) {
        final StoreMqttConnectOptions mqttConnectOptions = StoreMqttConnectOptions.get(broker,clientId);
        // 配置MQTT连接
        mqttConnectOptions.setAutomaticReconnect(true);
        // 设置是否清空session,false表示服务器会保留客户端的连接记录，true表示每次连接到服务器都以新的身份连接
        mqttConnectOptions.setCleanSession(true);
//        mqttConnectOptions.setUserName(userName);
//        mqttConnectOptions.setPassword(password.toCharArray());
        mqttConnectOptions.setConnectionTimeout(0);  //超时时间
        //设置会话心跳时间 单位为秒 服务器会每隔(1.5*keepTime)秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
        mqttConnectOptions.setKeepAliveInterval(7 * 60); //心跳时间,单位秒
        mqttConnectOptions.setMaxInflight(10);//允许同时发送几条消息（未收到broker确认信息）
        mqttConnectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);//选择MQTT版本
        return mqttConnectOptions;
    }

    public void removeGroupTask(View view) {
        mMqttClient.addOrRemoveJobGroup(getCycleJobGroup(), false);
    }

    public void addGroupTask(View view) {
        mMqttClient.addOrRemoveJobGroup(getCycleJobGroup(), true);
    }

    public CycleJobGroup getCycleJobGroup() {
        CycleJobGroup jobGroup = CycleJobGroup.getCycleJobGroup("111", 1, 10);
        CycleJob cycleJob = CycleJob.create(TestIMqttConsumer.class, getMqttConnectOptions(null, null));
        jobGroup.addJob(cycleJob);
        return jobGroup;
    }
}
