package com.zebra.module.mqtt.msg;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;
/**
 * @author xining
 * @date 2019/3/4
 */
public class MqttMessageWrap {
    public String topic;
    public int qos;
    public MqttMessage mqttMessage;
    public IMqttActionListener callback;
    public IMqttMessageListener messageListeners;

    public static MqttMessageWrap getZebraMqttMessage(String topic, byte[] payload){
        MqttMessageWrap message = new MqttMessageWrap();
        MqttMessage mqttMessage = new MqttMessage(payload);
        mqttMessage.setQos(1);
        mqttMessage.setRetained(false);
        message.mqttMessage = mqttMessage;
        message.topic = topic;
        return message;
    }
}
