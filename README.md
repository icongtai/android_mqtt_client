
### 功能
- 1隐藏处理连接逻辑
- 2后台服务，链接重用
- 3支持添加定时任务
- 4支持任务优先级



发送消息


```
mMqttClient.publishAsync(MqttMessageWrap);
```

添加任务组,任务会周期执行

```
mMqttClient.addOrRemoveJobGroup(CycleJobGroup, true);
```


构造mqtt client

```
MqttClient getMqttClient(){
    //配置
    final StoreMqttConnectOptions options = StoreMqttConnectOptions.get(broker);
    
    //以下配置和MqttConnectOptions保持一致
    options.userName="userName";
    options.password="password";
    options.connectionTimeout=connectionTimeout;
    
    //client
    MqttClient mMqttClient = new MqttClient(context,clientId, broker, options);
}

```

构造CycleJobGroup

```
CycleJobGroup getCycleJobGroup(){
    CycleJobGroup jobGroup = CycleJobGroup.getCycleJobGroup("唯一标识", (int)优先级, 周期-秒);
    //options同ZebraMqttClient配置
    //TestIMqttConsumer 周期任务实现
    CycleJob cycleJob = CycleJob.create(TestIMqttConsumer.class,options);
    //添加至少一个任务
    jobGroup.addJob(cycleJob);
    return jobGroup;
}

```

任务实现

```
public class TestIMqttConsumer implements IMqttConsumer {

    @Override
    public boolean comsume(CycleJob cycleJob, MqttConntion conntion) throws Exception {
        //周期发送 mqttMessageWrap
        MqttMessageWrap mqttMessageWrap = new MqttMessageWrap();
        conntion.publish(mqttMessageWrap);
        return true;
    }

    @Override
    public void onThrowable(Throwable e) {
      //异常
    }
}
```

构造MqttMessageWrap

```
MqttMessageWrap getMqttMessageWrap(){
   //mqtt orgin message
    MqttMessage mqttMessage = new MqttMessage("hello zebra");
    mqttMessage.setQos(1);
    mqttMessage.setRetained(false);
    //MqttMessageWrap
    MqttMessageWrap zebraMessage = new MqttMessageWrap();
    zebraMessage.mqttMessage = mqttMessage
    zebraMessage.topic = "zebra_test"; 
}

```


发送消息详细

```
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
 
消息订阅

```
message.topic=topic;
message.qos=qos;
message.messageListeners = listener;
message.callback = callback;
mMqttClient.subscribeAsync(message);
```
 
取消订阅

```
message.topic=topic;
message.callback = callback;
mMqttClient.unSubscribeAsync(message);
```

 
添加任务组说明

```
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
