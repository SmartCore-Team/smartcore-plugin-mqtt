# smartcore-plugin-mqtt
SmartCore的MQTT插件，可以将支持MQTT协议的设备接入到SmartCore平台。

## 配置说明
### 配置例(窗帘)
```
connection: 'mqttConnection'
mqtt:
  host: 'tcp://6.0.1.1:1883'
  username: 'admin'
  password: '141564'
  clientId: 'SmartCore_mqtt_plugin'
device:
  type: 'curtain'
  friendlyName: '书房的窗帘'
  properties:
    position:
      friendlyName: '位置'
      valueType: 'int'
      readOnly: false
      isNotify: true
      getTopic: '/HassSmart/curtain_shufang/position'
      setTopic: '/HassSmart/curtain_shufang/cmnd/set_position'
      getQos: 2
      setQos: 2
  info:
    manufacturer: 'HassSmart'
    model: 'curtain.1'
  operation:
    open:
      friendlyName: '打开'
      topic: '/HassSmart/curtain_shufang/cmnd/set'
      message: 'OPEN'
      qos: 2
    stop:
      friendlyName: '停止'
      topic: '/HassSmart/curtain_shufang/cmnd/set'
      message: 'STOP'
      qos: 2
    close:
      friendlyName: '关闭'
      topic: '/HassSmart/curtain_shufang/cmnd/set'
      message: 'CLOSE'
      qos: 2
```