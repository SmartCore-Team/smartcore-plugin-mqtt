package com.hangyin.smart.smartcore.plugin.mqtt.model;

import org.fusesource.mqtt.client.QoS;

/**
 * @author hang.yin
 * @date 2020-05-18 22:01
 */
public class OperationModel {
    private String friendlyName;
    private String topic;
    private String message;
    private QoS qos;
    private boolean retain;

    public OperationModel(String topic, String message, QoS qos, boolean retain) {
        this.topic = topic;
        this.message = message;
        this.qos = qos;
        this.retain = retain;
    }

    public OperationModel(String topic, String message) {
        this.topic = topic;
        this.message = message;
        this.qos = QoS.AT_MOST_ONCE;
        this.retain = false;
    }

    public String getTopic() {
        return topic;
    }

    public OperationModel setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public OperationModel setMessage(String message) {
        this.message = message;
        return this;
    }

    public QoS getQos() {
        return qos;
    }

    public OperationModel setQos(QoS qos) {
        this.qos = qos;
        return this;
    }

    public boolean isRetain() {
        return retain;
    }

    public OperationModel setRetain(boolean retain) {
        this.retain = retain;
        return this;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public OperationModel setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
        return this;
    }
}
