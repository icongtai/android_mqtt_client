package com.zebra.module.mqtt;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import com.zebra.module.mqtt.client.MqttClient;
import com.zebra.module.mqtt.client.StoreMqttConnectOptions;
import com.zebra.module.mqtt.msg.MqttMessageWrap;
import com.zebra.module.mqtt.service.MqttService;
import com.zebra.module.mqtt.service.connection.MqttConntion;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ServiceTestRule;

import static com.zebra.module.mqtt.service.connection.MqttConntion.Conntioned;
import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.CONNECTION_TIMEOUT_DEFAULT;

/**
 * @author xining
 * @date 2019/3/4
 */
@RunWith(AndroidJUnit4.class)
public class ZebraMqttClientTest {
    public static final String broker = "tcp://iot.eclipse.org:1883";
    public static final String clientId = "99000873202032";
    public MqttClient mqttClient;
    public boolean tag;
    @Rule
    public final ServiceTestRule serviceRule = new ServiceTestRule();
    CountDownLatch countDownLatch = new CountDownLatch(1);
    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();
    public MqttService service;
    public StoreMqttConnectOptions mStoreMqttConnectOptions;

    @Before
    public void setup() throws TimeoutException {
        Context appContext = ApplicationProvider.getApplicationContext();
        mStoreMqttConnectOptions = getZebraStoreMqttConnectOptions(broker);
        mqttClient = new MqttClient(appContext, clientId, broker, mStoreMqttConnectOptions);
        Intent intent = new Intent(appContext, MqttService.class);
        IBinder binder = serviceRule.bindService(intent);
        service = mqttClient.testInit(binder);
        Assert.assertTrue(service != null);
    }


    @Test
    public void testConnect() {
        service.createOrReconnectAsync(broker, clientId, broker, mStoreMqttConnectOptions, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Assert.assertTrue(true);
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Assert.assertTrue(false);
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        MqttConntion conntion =   service.getZebraMqttConntion(broker);
        Assert.assertTrue(conntion!=null);
        Assert.assertTrue(conntion.getStatus()==Conntioned);
        conntion.close();
    }

    @Test
    public void test() throws MqttException {
        // Context of the app under test.
        MqttMessageWrap message = getZebraMqttMessage();
        message.callback = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                tag = true;
                countDownLatch.countDown();
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Assert.assertTrue(exception == null);
                countDownLatch.countDown();
            }
        };
        IMqttToken token = mqttClient.publishAsync(message);
        Assert.assertTrue(token != null);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Assert.assertTrue(tag);
    }

    public MqttMessageWrap getZebraMqttMessage() {
        MqttMessageWrap message = new MqttMessageWrap();
        message.mqttMessage = getMqttMessage();
        message.topic = "zebra_test";
        return message;
    }

    private MqttMessage getMqttMessage() {
        MqttMessage mqttMessage = new MqttMessage("".getBytes());
        mqttMessage.setQos(1);
        mqttMessage.setRetained(false);
        return mqttMessage;
    }


    public static StoreMqttConnectOptions getZebraStoreMqttConnectOptions(String broker) {
        final StoreMqttConnectOptions options = StoreMqttConnectOptions.get(broker,
                clientId);
        // 配置MQTT连接
        options.setAutomaticReconnect(true);
        // 设置是否清空session,false表示服务器会保留客户端的连接记录，true表示每次连接到服务器都以新的身份连接
        options.setCleanSession(true);
        options.setUserName(clientId);
        options.setPassword("");
        options.setConnectionTimeout(CONNECTION_TIMEOUT_DEFAULT);  //超时时间
        //设置会话心跳时间 单位为秒 服务器会每隔(1.5*keepTime)秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
        options.setKeepAliveInterval(7 * 60); //心跳时间,单位秒
        options.setMaxInflight(10);//允许同时发送几条消息（未收到broker确认信息）
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);//选择MQTT版本
        return options;
    }
}
