package com.zebra.module.mqtt.client;

import android.content.Context;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import com.zebra.module.mqtt.db.MqttSqliteOpenHelper;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;

import java.sql.SQLException;

import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.CLEAN_SESSION_DEFAULT;
import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.CONNECTION_TIMEOUT_DEFAULT;
import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.KEEP_ALIVE_INTERVAL_DEFAULT;
import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.MAX_INFLIGHT_DEFAULT;
import static org.eclipse.paho.client.mqttv3.MqttConnectOptions.MQTT_VERSION_DEFAULT;

/**
 * 配置保存,用于后台任务
 *
 * @author xining
 * @date 2019/3/4
 */
@DatabaseTable(tableName = "mqtt_connect_option")
public class StoreMqttConnectOptions {
    /**
     * 唯一Id
     */
    @DatabaseField(id = true)
    public String conntionUniqueId;
    @DatabaseField
    public String clientId;
    @DatabaseField
    public String broker;
    @DatabaseField
    public String userName;
    @DatabaseField
    public String password;
    @DatabaseField
    public int MqttVersion = MQTT_VERSION_DEFAULT;
    @DatabaseField
    public int keepAliveInterval = KEEP_ALIVE_INTERVAL_DEFAULT;
    @DatabaseField
    public int maxInflight = MAX_INFLIGHT_DEFAULT;
    @DatabaseField
    public boolean cleanSession = CLEAN_SESSION_DEFAULT;
    @DatabaseField
    public int connectionTimeout = CONNECTION_TIMEOUT_DEFAULT;
    @DatabaseField
    public boolean automaticReconnect = false;

    public MqttConnectOptions getMqttConnectOptions() {
        final MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        // 配置MQTT连接
        mqttConnectOptions.setAutomaticReconnect(true);
        // 设置是否清空session,false表示服务器会保留客户端的连接记录，true表示每次连接到服务器都以新的身份连接
        mqttConnectOptions.setCleanSession(true);
//        mqttConnectOptions.setUserName(userName);
//        mqttConnectOptions.setPassword(password.toCharArray());
        mqttConnectOptions.setConnectionTimeout(connectionTimeout);  //超时时间
        //设置会话心跳时间 单位为秒 服务器会每隔(1.5*keepTime)秒的时间向客户端发送个消息判断客户端是否在线，但这个方法并没有重连的机制
        mqttConnectOptions.setKeepAliveInterval(keepAliveInterval); //心跳时间,单位秒
        mqttConnectOptions.setMaxInflight(maxInflight);//允许同时发送几条消息（未收到broker确认信息）
        mqttConnectOptions.setMqttVersion(MqttVersion);//选择MQTT版本
        return mqttConnectOptions;
    }

    public static StoreMqttConnectOptions get(String broker, String clientId) {
        StoreMqttConnectOptions connectOptions = new StoreMqttConnectOptions();
        connectOptions.broker = broker;
        connectOptions.clientId = clientId;
        connectOptions.conntionUniqueId = broker;
        return connectOptions;
    }

    public void setBroker(String broker) {
        this.broker = broker;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setMqttVersion(int mqttVersion) {
        MqttVersion = mqttVersion;
    }

    public void setKeepAliveInterval(int keepAliveInterval) {
        this.keepAliveInterval = keepAliveInterval;
    }

    public void setMaxInflight(int maxInflight) {
        this.maxInflight = maxInflight;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setAutomaticReconnect(boolean automaticReconnect) {
        this.automaticReconnect = automaticReconnect;
    }

    public void createOrUpdate(Context context) throws SQLException {
        Dao<StoreMqttConnectOptions, String> daoZebraStoreMqttConnectOptions = MqttSqliteOpenHelper.getHelper(context).getDao(StoreMqttConnectOptions.class);
        daoZebraStoreMqttConnectOptions.createOrUpdate(this);
    }

    public void refresh(Context context) throws SQLException {
        Dao<StoreMqttConnectOptions, String> daoZebraStoreMqttConnectOptions = MqttSqliteOpenHelper.getHelper(context).getDao(StoreMqttConnectOptions.class);
        daoZebraStoreMqttConnectOptions.refresh(this);
    }

    public void update(Context context) throws SQLException {
        Dao<StoreMqttConnectOptions, String> daoZebraStoreMqttConnectOptions = MqttSqliteOpenHelper.getHelper(context).getDao(StoreMqttConnectOptions.class);
        daoZebraStoreMqttConnectOptions.update(this);
    }
}
