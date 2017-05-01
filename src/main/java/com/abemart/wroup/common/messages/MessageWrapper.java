package com.abemart.wroup.common.messages;


import com.abemart.wroup.common.WiFiP2PDevice;

public class MessageWrapper {

    public enum MessageType {
        NORMAL, CONNECTION_MESSAGE, DISCONNECTION_MESSAGE, REGISTERED_DEVICES;
    }

    private String message;
    private MessageType messageType;
    private WiFiP2PDevice wiFiP2PDevice;


    public void setWiFiP2PDevice(WiFiP2PDevice wiFiP2PDevice) {
        this.wiFiP2PDevice = wiFiP2PDevice;
    }

    public WiFiP2PDevice getWiFiP2PDevice() {
        return wiFiP2PDevice;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

}
