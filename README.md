paho android  service client
- 1隐藏处理连接逻辑
- 2后台服务，链接重用
- 3支持添加定时任务
- 4支持任务优先级
```


//发送消息
mMqttClient.publishAsync(message);
//添加任务组
mMqttClient.addOrRemoveJobGroup(getCycleJobGroup(), true);
```

```
//发送消息详细

//配置
final StoreMqttConnectOptions options = StoreMqttConnectOptions.get(broker);
//client
MqttClient mMqttClient = new MqttClient(context,clientId, broker, options);
//mqtt message
MqttMessage mqttMessage = new MqttMessage("hello zebra");
mqttMessage.setQos(1);
mqttMessage.setRetained(false);
//ZebraMqttMessage
MqttMessageWrap zebraMessage = new MqttMessageWrap();
zebraMessage.mqttMessage = mqttMessage
zebraMessage.topic = "zebra_test";
//设置回调
zebraMessage.callback = new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                Logger.d(TAG, "sendMessage onSuccess " + asyncActionToken);
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                Logger.e(TAG, "sendMessage onFailure", exception);
            }
};
//发送消息
mMqttClient.publishAsync(message);
```

```
//消息订阅
message.topic=topic;
message.qos=qos;
message.messageListeners = listener;
message.callback = callback;
mMqttClient.subscribeAsync(message);


//取消订阅
message.topic=topic;
message.callback = callback;
mMqttClient.unSubscribeAsync(message);
```


```
//添加任务组详细

CycleJobGroup jobGroup = CycleJobGroup.getCycleJobGroup("唯一标识", (int)优先级, 周期-秒);
//options同ZebraMqttClient配置
CycleJob cycleJob = CycleJob.create(TestJobMqttSourceProvider.class,options);
//添加至少一个任务
jobGroup.addJob(cycleJob);
//添加任务组
mMqttClient.addOrRemoveJobGroup(getCycleJobGroup(), true);
//移除任务组
mMqttClient.addOrRemoveJobGroup(getCycleJobGroup(), false);


JobGroup 1{
    j0b1,job2,jobn
}

JobGroup 2{
   j0b1,job2,jobn
}
JobGroup n{
    j0b1,job2,jobn
}

//添加删除以任务组为单位
//job n执行依赖job( n-1)是否成功
//JobGroup n执行不依赖JobGroup(n-1)是否成功
//JobGroup 按优先级顺序执行
//job按添加顺序执行
```

```
多次失败重连

Token失效

注册接口
```
