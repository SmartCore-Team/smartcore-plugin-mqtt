package com.hangyin.smart.smartcore.plugin.mqtt;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.hangyin.smart.smartcore.model.SmartCoreShareModel;
import com.hangyin.smart.smartcore.model.SmartCoreDeviceInfoModel;
import com.hangyin.smart.smartcore.model.SmartCoreDeviceModel;
import com.hangyin.smart.smartcore.model.SmartCorePropertyModel;
import com.hangyin.smart.smartcore.plugin.mqtt.model.OperationModel;
import com.hangyin.smart.smartcore.plugin.mqtt.util.ContentUtil;
import com.hangyin.smart.smartcore.service.ISmartCoreDevicePropertyValueChangeNotifyFunction;
import com.hangyin.smart.smartcore.service.ISmartCoreDeviceService;
import org.apache.commons.lang3.StringUtils;
import org.fusesource.mqtt.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author hang.yin
 * @date 2020-05-14 21:10
 */
public class MQTTService implements ISmartCoreDeviceService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MQTTService.class);

    private static final String CONFIG_ITEM_NAME_CONNECTIONSHARENAME = "connection";
    private static final String CONFIG_ITEM_NAME_MQTTCFG = "mqtt";
    private static final String CONFIG_ITEM_NAME_MQTTCFG_HOST = "host";
    private static final String CONFIG_ITEM_NAME_MQTTCFG_USERNAME = "username";
    private static final String CONFIG_ITEM_NAME_MQTTCFG_PASSWORD = "password";
    private static final String CONFIG_ITEM_NAME_MQTTCFG_CLIENTID = "clientId";
    private static final String CONFIG_ITEM_NAME_MQTTCFG_VERSION = "version";
    private static final String CONFIG_ITEM_NAME_DEVICECFG = "device";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_TYPE = "type";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_FRIENDLYNAME = "friendlyName";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_EXTRA = "extra";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES = "properties";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_FRIENDLYNAME = "friendlyName";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_VALUEPATTERN = "valuePattern";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_VALUETYPE = "valueType";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_READONLY = "readOnly";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_ISNOTIFY = "isNotify";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_GETTOPIC = "getTopic";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETTOPIC = "setTopic";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_GETVALUEFORMAT = "getValueFormat";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETVALUETEMPLATE = "setValueTemplate";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_GETVALUEMAP = "getValueMap";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETVALUEMAP = "setValueMap";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_GETQOS = "getQos";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETQOS = "setQos";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETRETAIN = "setRetain";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_EXTRA = "extra";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_INFO = "info";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_INFO_MANUFACTURER = "manufacturer";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_INFO_MODEL = "model";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_INFO_SERIALNUMBER = "serialNumber";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_INFO_EXTRA = "extra";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_OPERATION = "operation";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_OPERATION_TOPIC = "topic";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_OPERATION_MESSAGE = "message";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_OPERATION_QOS = "qos";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_OPERATION_RETAIN = "retain";
    private static final String CONFIG_ITEM_NAME_DEVICECFG_OPERATION_FRIENDLYNAME = "friendlyName";

    private static Map<String, SmartCoreDeviceModel> deviceMap = new ConcurrentHashMap<>();

    private Map<String, SmartCoreShareModel> shareMap;
    private ISmartCoreDevicePropertyValueChangeNotifyFunction notifyFunction;
    private String connectionShareName;

    private BlockingConnection connection;
    private SmartCoreDeviceModel device;

    @SuppressWarnings("unchecked")
    private static <T> T getValue(Map configs, String key, T defaultValue) {
        return Optional.ofNullable(configs).map(config -> (T) config.get(key)).orElse(defaultValue);
    }

    @SuppressWarnings({"unchecked", "JavaReflectionInvocation"})
    @Override
    public Boolean init(Map<String, SmartCoreShareModel> shareMap, ISmartCoreDevicePropertyValueChangeNotifyFunction notifyFunction, String deviceId, Map params) {
        if(StringUtils.isBlank(deviceId)) {
            LOGGER.error("init {} fail: deviceId is empty.", deviceId);
            return false;
        }

        // member variable
        this.shareMap = shareMap;
        this.notifyFunction = notifyFunction;
        this.connectionShareName = getValue(params, CONFIG_ITEM_NAME_CONNECTIONSHARENAME, "mqttConnect_" + deviceId);

        // init mqtt connection
        if(null == shareMap.get(connectionShareName)) {
            Map<String, Object> mqttCfg = getValue(params, CONFIG_ITEM_NAME_MQTTCFG, null);
            if(null == mqttCfg) {
                LOGGER.error("init {} fail: no mqtt config.", deviceId);
                return false;
            }

            String hostStr = getValue(mqttCfg, CONFIG_ITEM_NAME_MQTTCFG_HOST, null);
            if(StringUtils.isBlank(hostStr)) {
                LOGGER.error("init {} fail: host is empty.", deviceId);
                return false;
            }
            String usernameStr = getValue(mqttCfg, CONFIG_ITEM_NAME_MQTTCFG_USERNAME, null);
            String passwordStr = getValue(mqttCfg, CONFIG_ITEM_NAME_MQTTCFG_PASSWORD, null);
            String clientIdStr = getValue(mqttCfg, CONFIG_ITEM_NAME_MQTTCFG_CLIENTID, UUID.randomUUID().toString());
            String versionStr = getValue(mqttCfg, CONFIG_ITEM_NAME_MQTTCFG_VERSION, "3.1");
            MQTT mqtt = new MQTT();
            try {
                mqtt.setHost(hostStr);
                if(StringUtils.isNotBlank(usernameStr)) {
                    mqtt.setUserName(usernameStr);
                }
                if(StringUtils.isNotBlank(passwordStr)) {
                    mqtt.setPassword(passwordStr);
                }
                mqtt.setClientId(clientIdStr);
                mqtt.setVersion(versionStr);
                mqtt.setConnectAttemptsMax(-1);
                mqtt.setReconnectAttemptsMax(-1);

                this.connection = mqtt.blockingConnection();
                shareMap.put(connectionShareName, new SmartCoreShareModel(this.connection).use());
            } catch (Exception e) {
                LOGGER.error("init {} fail: mqtt init fail.", deviceId);
                return false;
            }
        } else {
            this.connection = (BlockingConnection) shareMap.get(connectionShareName).use().get();
        }

        if(!this.connection.isConnected()) {
            try {
                this.connection.connect();
            } catch (Exception e) {
                LOGGER.error("init {} fail: mqtt connect fail.", deviceId);
            }
        }

        // get device config
        Map<String, Object> deviceCfg = getValue(params, CONFIG_ITEM_NAME_DEVICECFG, null);
        if(null == deviceCfg) {
            LOGGER.error("init {} fail: no device config.", deviceId);
            return false;
        }

        // set device type
        String type = getValue(deviceCfg, CONFIG_ITEM_NAME_DEVICECFG_TYPE, null);
        if(StringUtils.isBlank(type)) {
            LOGGER.error("init {} fail: type is empty.", deviceId);
            return false;
        }
        // set device FriendlyName
        String dFriendlyName = getValue(deviceCfg, CONFIG_ITEM_NAME_DEVICECFG_FRIENDLYNAME, null);

        // init device
        this.device = new SmartCoreDeviceModel(deviceId).setType(type).setFriendlyName(dFriendlyName);

        // set device operation
        Map<String, OperationModel> operation = new ConcurrentHashMap<>();
        Map<String, Object> operationCfg = getValue(deviceCfg, CONFIG_ITEM_NAME_DEVICECFG_OPERATION, null);
        if(null != operationCfg) {
            operationCfg.forEach((operName, operDetail) -> {
                if (operDetail instanceof Map) {
                    Map<String, Object> operDetailMap = (Map<String, Object>) operDetail;
                    String topic = getValue(operDetailMap, CONFIG_ITEM_NAME_DEVICECFG_OPERATION_TOPIC, null);
                    if(null == topic) {
                        return;
                    }
                    String msg = getValue(operDetailMap, CONFIG_ITEM_NAME_DEVICECFG_OPERATION_MESSAGE, null);
                    if(null == msg) {
                        return;
                    }
                    int qosInt = getValue(operDetailMap, CONFIG_ITEM_NAME_DEVICECFG_OPERATION_QOS, 0);
                    QoS qos;
                    switch(qosInt) {
                        case 0:
                            qos = QoS.AT_MOST_ONCE;
                            break;
                        case 1:
                            qos = QoS.AT_LEAST_ONCE;
                            break;
                        case 2:
                            qos = QoS.EXACTLY_ONCE;
                            break;
                        default:
                            qos = QoS.AT_MOST_ONCE;
                            break;
                    }
                    String friendlyName = getValue(operDetailMap, CONFIG_ITEM_NAME_DEVICECFG_OPERATION_FRIENDLYNAME, null);
                    boolean retain = getValue(operDetailMap, CONFIG_ITEM_NAME_DEVICECFG_OPERATION_RETAIN, true);
                    operation.put(operName, new OperationModel(topic, msg, qos, retain).setFriendlyName(friendlyName));
                }
            });
        }
        this.device.addExtra(CONFIG_ITEM_NAME_DEVICECFG_OPERATION, operation);

        // set device extra
        Map<String, Object> dExtra = getValue(deviceCfg, CONFIG_ITEM_NAME_DEVICECFG_EXTRA, null);
        this.device.setExtra(dExtra);

        // set device properties
        Map<String, Object> deviceProperties = getValue(deviceCfg, CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES, null);
        if(null != deviceProperties) {
            deviceProperties.forEach((pid, pv) -> {
                if (pv instanceof Map) {
                    Map<String, Object> pvMap = (Map<String, Object>) pv;
                    String friendlyName = getValue(pvMap, CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_FRIENDLYNAME, null);
                    String valuePattern = getValue(pvMap, CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_VALUEPATTERN, null);
                    String valueType = getValue(pvMap, CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_VALUETYPE, null);
                    if (null == valueType) {
                        return;
                    }
                    boolean readOnly = getValue(pvMap, CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_READONLY, true);
                    boolean isNotify = getValue(pvMap, CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_ISNOTIFY, false);
                    String getTopic = getValue(pvMap, CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_GETTOPIC, "");
                    String setTopic = getValue(pvMap, CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETTOPIC, "");
                    String getValueFormat = getValue(pvMap, CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_GETVALUEFORMAT, "");
                    String setValueTemplate = getValue(pvMap, CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETVALUETEMPLATE, "");
                    HashMap<Object, Object> getValueMap = getValue(pvMap, CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_GETVALUEMAP, new HashMap<>());
                    HashMap<Object, Object> setValueMap = getValue(pvMap, CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETVALUEMAP, new HashMap<>());
                    int setQosInt = getValue(pvMap, CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETQOS, 0);
                    QoS setQos;
                    switch(setQosInt) {
                        case 0:
                            setQos = QoS.AT_MOST_ONCE;
                            break;
                        case 1:
                            setQos = QoS.AT_LEAST_ONCE;
                            break;
                        case 2:
                            setQos = QoS.EXACTLY_ONCE;
                            break;
                        default:
                            setQos = QoS.AT_MOST_ONCE;
                            break;
                    }
                    boolean setRetain = getValue(pvMap, CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETRETAIN, true);
                    Map<String, Object> pExtra = getValue(pvMap, CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_EXTRA, null);
                    this.device.addProperty(pid, new SmartCorePropertyModel(friendlyName, null, valueType, valuePattern, readOnly, isNotify)
                        .addExtra(CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_GETTOPIC, getTopic)
                        .addExtra(CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETTOPIC, setTopic)
                        .addExtra(CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETQOS, setQos)
                        .addExtra(CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETRETAIN, setRetain)
                        .addExtra(CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_GETVALUEMAP, getValueMap)
                        .addExtra(CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETVALUEMAP, setValueMap)
                        .addExtra(CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_GETVALUEFORMAT, getValueFormat)
                        .addExtra(CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETVALUETEMPLATE, setValueTemplate)
                        .setExtra(pExtra));

                    if(StringUtils.isNotBlank(getTopic)) {
                        int getQosInt = getValue(pvMap, CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_GETQOS, 0);
                        QoS getQos;
                        switch (getQosInt) {
                            case 0:
                                getQos = QoS.AT_MOST_ONCE;
                                break;
                            case 1:
                                getQos = QoS.AT_LEAST_ONCE;
                                break;
                            case 2:
                                getQos = QoS.EXACTLY_ONCE;
                                break;
                            default:
                                getQos = QoS.AT_MOST_ONCE;
                                break;
                        }

                        try {
                            connection.subscribe(new Topic[]{new Topic(getTopic, getQos)});
                        } catch (Exception e) {
                            LOGGER.error("mqtt subscribe fail.", e);
                        }
                    }
                }
            });
        }

        // set device info
        Map<String, Object> deviceInfo = getValue(deviceCfg, CONFIG_ITEM_NAME_DEVICECFG_INFO, null);
        if(null != deviceInfo) {
            String manufacturer = getValue(deviceInfo, CONFIG_ITEM_NAME_DEVICECFG_INFO_MANUFACTURER, null);
            String model = getValue(deviceInfo, CONFIG_ITEM_NAME_DEVICECFG_INFO_MODEL, null);
            String serialNumber = getValue(deviceInfo, CONFIG_ITEM_NAME_DEVICECFG_INFO_SERIALNUMBER, deviceId);
            Map<String, Object> extra = getValue(deviceInfo, CONFIG_ITEM_NAME_DEVICECFG_INFO_EXTRA, null);
            this.device.setInfo(new SmartCoreDeviceInfoModel(manufacturer, model, serialNumber).setExtra(extra));
        }

        // create mqtt receive thread
        String receiveThreadShareName = this.connectionShareName + "_receiveThread";
        if(null == shareMap.get(receiveThreadShareName)) {
            new Thread(() -> {
                while (shareMap.get(connectionShareName + "_receiveThread").useNum() > 0) {
                    try {
                        Message message = connection.receive();
                        String topic = message.getTopic();
                        String payload = new String(message.getPayload());
                        LOGGER.debug("mqtt receive. topic: {}, payload: {}", topic, payload);
                        deviceMap.forEach((did, targetDevice) -> {
                            targetDevice.getProperties().forEach((pid, p) -> {
                                if (topic.equals(p.getExtra(CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_GETTOPIC))) {
                                    try {
                                        Object newValue;
                                        String getValueFormat = (String) p.getExtra(CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_GETVALUEFORMAT);
                                        if (StringUtils.isNotBlank(getValueFormat)) {
                                            newValue = String.valueOf(getFormatValue(getValueFormat, payload));
                                        } else {
                                            newValue = payload;
                                        }

                                        Map<String, Object> getValueMap = (Map<String, Object>) p.getExtra(CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_GETVALUEMAP);
                                        if (null != getValueMap.get(String.valueOf(newValue))) {
                                            newValue = getValueMap.get(String.valueOf(newValue));
                                        }

                                        if(!p.getValueType().equals(newValue.getClass().getName())) {
                                            if (!p.getValueType().equals(String.class.getName())) {
                                                Class vClass = Class.forName(p.getValueType());
                                                Method valueofMethod = vClass.getMethod("valueOf", String.class);
                                                newValue = valueofMethod.invoke(null, String.valueOf(newValue));
                                            }
                                        }

                                        LOGGER.debug("device {} property {} value change to {}.", targetDevice.getDeviceId(), pid, newValue);
                                        targetDevice.setPropertyValue(pid, newValue, (i, v) -> true, this.notifyFunction, false);
                                    } catch (ClassNotFoundException e) {
                                        LOGGER.error("value type not found: {}", p.getValueType(), e);
                                    } catch (NoSuchMethodException e) {
                                        LOGGER.error("class {} not found method valueOf", p.getValueType(), e);
                                    } catch (IllegalAccessException | InvocationTargetException e) {
                                        LOGGER.error("class {} invoke method valueOf({}) fail.", p.getValueType(), payload, e);
                                    } catch (Exception e) {
                                        LOGGER.error("device {} property {} value change fail.", targetDevice.getDeviceId(), pid, e);
                                    }
                                }
                            });
                        });
                        message.ack();
                    } catch (Exception e) {
                        LOGGER.error("mqtt receive fail.", e);
                    }
                }
            }, "SmartCore_Plugin_MQTTService_receive").start();

            shareMap.put(receiveThreadShareName, new SmartCoreShareModel(null).use());
        } else {
            shareMap.get(receiveThreadShareName).use();
        }

        deviceMap.put(deviceId, this.device);
        return true;
    }

    @Override
    public Boolean close(Map params) {
        try {
            BlockingConnection connection = (BlockingConnection) shareMap.get(connectionShareName).revert();
            if(null != connection) {
                connection.disconnect();
                shareMap.remove(connectionShareName);
            }

            shareMap.get(connectionShareName + "_receiveThread").revert();

            return true;
        } catch (Exception e) {
            LOGGER.error("connection.disconnect fail.", e);
        }
        return false;
    }

    @Override
    public SmartCoreDeviceModel getDevice() {
        return this.device;
    }

    @Override
    public String getDeviceId() {
        return this.device.getDeviceId();
    }

    @Override
    public String getDeviceFriendlyName() {
        return this.device.getFriendlyName();
    }

    @Override
    public String getDeviceType() {
        return this.device.getType();
    }

    @Override
    public SmartCoreDeviceInfoModel getDeviceInfo() {
        return this.device.getInfo();
    }

    @Override
    public Map<String, SmartCorePropertyModel> getDeviceProperties() {
        return this.device.getProperties();
    }

    @Override
    public SmartCorePropertyModel getProperty(String propertyId) {
        return this.device.getProperty(propertyId);
    }

    @Override
    public Object getPropertyValue(String propertyId) {
        return this.device.getPropertyValue(propertyId);
    }

    @Override
    public Object getPropertyFriendlyName(String propertyId) {
        return this.device.getPropertyFriendlyName(propertyId);
    }

    @Override
    public Boolean setPropertyValue(String propertyId, Object propertyValue) {
        SmartCorePropertyModel prop = this.device.getProperty(propertyId);
        if(null == prop) {
            return false;
        }

        return this.device.setPropertyValue(propertyId, propertyValue, (id, value) -> {
            try {
                if(StringUtils.isNotBlank((String) prop.getExtra(CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETTOPIC))) {
                    Map<String, Object> setValueMap = (Map<String, Object>) prop.getExtra(CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETVALUEMAP);
                    if(null != setValueMap.get(String.valueOf(value))) {
                        value = setValueMap.get(String.valueOf(value));
                    }

                    String setValueTemplate = (String) prop.getExtra(CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETVALUETEMPLATE);
                    if(StringUtils.isNotBlank(setValueTemplate)) {
                        HashMap p = this.device.getProperties().entrySet().stream().collect(HashMap::new, (m, d) -> m.put(d.getKey(), d.getValue().getValue()), HashMap::putAll);
                        p.put(id, value);
                        value = ContentUtil.getContent(setValueTemplate, p);
                    }

                    this.connection.publish(
                        (String) prop.getExtra(CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETTOPIC),
                        String.valueOf(value).getBytes(),
                        (QoS) prop.getExtra(CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETQOS),
                        (Boolean) prop.getExtra(CONFIG_ITEM_NAME_DEVICECFG_PROPERTIES_SETRETAIN));
                }
                return true;
            } catch (Exception e) {
                LOGGER.error("mqtt publish fail.", e);
                return false;
            }
        }, this.notifyFunction, true);
    }

    @Override
    public Map<String, Object> getPropertyValueMap() {
        return this.device.getProperties().entrySet().stream().collect(HashMap::new, (m, d) -> m.put(d.getKey(), d.getValue().getValue()), HashMap::putAll);
    }

    @Override
    public Map<String, Boolean> setPropertyValueMap(Map<String, Object> values) {
        Map<String, Boolean> r = new HashMap<>();
        for(String propertyId: values.keySet()) {
            r.put(propertyId, this.setPropertyValue(propertyId, values.get(propertyId)));
        }
        return r;
    }

    @Override
    public Boolean operation(String operationType) {
        Map<String, OperationModel> deviceOperation = (Map<String, OperationModel>) this.device.getExtra(CONFIG_ITEM_NAME_DEVICECFG_OPERATION);
        if(null == deviceOperation) {
            return false;
        } else {
            OperationModel operation = deviceOperation.get(operationType);
            if(null == operation) {
                return false;
            } else {
                try {
                    this.connection.publish(operation.getTopic(), operation.getMessage().getBytes(), operation.getQos(), operation.isRetain());
                    return true;
                } catch (Exception e) {
                    LOGGER.error("mqtt publish fail.", e);
                    return false;
                }
            }
        }
    }

    private Object getFormatValue(String formatStr, String valueStr) throws Exception {
        try {
            int mIndex = formatStr.indexOf(":");
            String formatType = formatStr.substring(0, mIndex);
            String formatValues = formatStr.substring(mIndex + 1);
            if ("json".equalsIgnoreCase(formatType)) {
                return getJsonFormatValue(formatValues, valueStr);
            } else {
                return valueStr;
            }
        } catch (Exception e) {
            throw e;
        }
    }

    private Object getJsonFormatValue(String formatValues, String valueStr) throws Exception {
        try {
            Object value = JSON.parseObject(valueStr);
            String[] formatValueArr = StringUtils.splitPreserveAllTokens(formatValues, ".");
            for(String formatValue: formatValueArr) {
                String formatKey;
                if(formatValue.endsWith("]")) {
                    formatKey = formatValue.substring(0, formatValue.indexOf("["));
                } else {
                    formatKey = formatValue;
                }

                if(value instanceof JSONObject && ((JSONObject) value).containsKey(formatKey)) {
                    value = ((JSONObject) value).get(formatKey);

                    if(formatValue.endsWith("]")) {
                        if(value instanceof JSONArray) {
                            int sIndex = formatValue.indexOf("[");
                            int eIndex = formatValue.indexOf("]");
                            int index = Integer.valueOf(formatValue.substring(sIndex + 1, eIndex));
                            if(index > ((JSONArray) value).size()) {
                                throw new Exception(formatKey + " size less than " + index + ".");
                            } else {
                                value = ((JSONArray) value).get(index);
                            }
                        } else {
                            throw new Exception(formatKey + " is not an array.");
                        }
                    }
                } else {
                    throw new Exception(formatValue + " not exist.");
                }
            }
            return value;
        } catch (JSONException e) {
            LOGGER.error("JSON.parseObject fail. date: {}", valueStr, e);
            throw e;
        }
    }
}
