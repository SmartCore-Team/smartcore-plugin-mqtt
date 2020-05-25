# smartcore-plugin-mqtt
SmartCore的MQTT插件，可以将支持MQTT协议的设备接入到SmartCore平台。

## 配置说明
### 配置例(窗帘)
```
connection: 'mqttConnection'
mqtt:
  host: 'tcp://6.0.1.1:1883'
  username: 'mqtt'
  password: 'mqtt'
  clientId: 'SmartCore_mqtt_plugin'
device:
  type: 'curtain'
  friendlyName: '书房的窗帘'
  properties:
    position:
      friendlyName: '位置'
      valueType: 'int'
      valuePattern: '([1-9]?\d|100)$'
      readOnly: false
      isNotify: true
      getTopic: '/HassSmart/curtain_shufang/position'
      setTopic: '/HassSmart/curtain_shufang/cmnd/set_position'
      getQos: 2
      setQos: 2
      extra:
        valueMin: 0
        valueMax: 100
        valueStep: 1
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
    openHalf:
      friendlyName: '打开一半'
      topic: '/HassSmart/curtain_shufang/cmnd/set_position'
      message: '50'
      qos: 2
```

### 配置例(z2m开关)
```
connection: 'mqttConnection'
mqtt:
  host: 'tcp://6.0.1.1:1883'
  username: 'mqtt'
  password: 'mqtt'
  clientId: 'SmartCore_mqtt_plugin'
device:
  type: 'lamp'
  friendlyName: '玄关的吸顶灯'
  properties:
    power:
      friendlyName: '开关'
      valueType: 'boolean'
      readOnly: false
      isNotify: true
      getTopic: '/z2m/shufang/0x00158d00014ddbb0'
      getValueFormat: 'json:state'
      getValueMap:
        "ON": true
        "OFF": false
      setTopic: '/z2m/shufang/0x00158d00014ddbb0/set'
      setValueMap:
        "true": "ON"
        "false": "OFF"
      getQos: 2
      setQos: 2
      extra:
        valueFriendlyName:
          "true": 开
          "false": 关
  info:
    manufacturer: 'HassSmart'
    model: 'lamp.1'

```