package com.zebra.module.mqtt.client;


import com.zebra.module.mqtt.msg.MqttMessageWrap;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;


/**
 * @author xining
 * @date 2019/3/4
 */
public class OptStore {
    public static final int Publish = 1;
    public static final int RefreshCycleJob = 2;
    public static final int Subscribe = 3;
    public static final int UnSubscribe = 4;

    public OptStore(int optType) {
        this.optType = optType;
    }

    public int optType;
    public MqttMessageWrap message;

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof OptStore)) {
            return false;
        }
        OptStore store = (OptStore) obj;
        if (this.optType != store.optType) {
            return false;
        }
        if (optType == Publish) {
            return message.equals(store.message);
        }
        return true;
    }

    @IntDef({Publish, RefreshCycleJob})
    @Retention(RetentionPolicy.SOURCE)
    public @interface OptStoreType {
    }
}
