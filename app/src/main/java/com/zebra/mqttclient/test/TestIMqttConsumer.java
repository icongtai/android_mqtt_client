package com.zebra.mqttclient.test;

import com.zebra.module.mqtt.job.CycleJob;
import com.zebra.module.mqtt.job.IMqttConsumer;
import com.zebra.module.mqtt.msg.MqttMessageWrap;
import com.zebra.module.mqtt.service.connection.MqttConntion;

/**
 * @author xining
 * @date 2019/3/4
 */
public class TestIMqttConsumer implements IMqttConsumer {

    @Override
    public boolean comsume(CycleJob cycleJob, MqttConntion conntion) throws Exception {
        MqttMessageWrap mqttMessageWrap = MainActivity.getZebraMqttMessage("source"+System.currentTimeMillis());
        conntion.publish(mqttMessageWrap);
        return false;
    }

    @Override
    public void onThrowable(Throwable e) {

    }
}
