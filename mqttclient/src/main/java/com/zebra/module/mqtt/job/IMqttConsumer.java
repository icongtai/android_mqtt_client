package com.zebra.module.mqtt.job;

import com.zebra.module.mqtt.service.connection.MqttConntion;

/**
 * @author xining
 * @date 2019/3/4
 */
public interface IMqttConsumer {
    /**
     *
     * @param cycleJob
     * @param conntion
     * @return breakGroup
     */
    boolean comsume(CycleJob cycleJob, MqttConntion conntion) throws Exception;

    void onThrowable(Throwable e);
}
