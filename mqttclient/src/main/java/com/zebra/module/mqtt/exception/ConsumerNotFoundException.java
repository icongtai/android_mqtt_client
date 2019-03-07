package com.zebra.module.mqtt.exception;

import com.zebra.module.mqtt.job.CycleJob;

/**
 * @author xining
 * @date 2019/3/5
 */
public class ConsumerNotFoundException extends Exception {
    public CycleJob cycleJob;
    public ConsumerNotFoundException(CycleJob cycleJob) {
        super();
        this.cycleJob = cycleJob;
    }
}
