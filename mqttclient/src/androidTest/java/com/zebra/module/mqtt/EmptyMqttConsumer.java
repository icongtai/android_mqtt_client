package com.zebra.module.mqtt;

import com.zebra.module.mqtt.job.CycleJob;
import com.zebra.module.mqtt.job.IMqttConsumer;
import com.zebra.module.mqtt.service.connection.MqttConntion;

public class EmptyMqttConsumer implements IMqttConsumer {
    @Override
    public boolean comsume(CycleJob cycleJob, MqttConntion conntion) throws Exception {
        return false;
    }

    @Override
    public void onThrowable(Throwable e) {

    }
}
