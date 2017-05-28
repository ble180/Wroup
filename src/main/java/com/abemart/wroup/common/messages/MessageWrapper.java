package com.abemart.wroup.common.messages;


import com.abemart.wroup.common.WroupDevice;

public class MessageWrapper {

    public enum MessageType {
        NORMAL, CONNECTION_MESSAGE, DISCONNECTION_MESSAGE, REGISTERED_DEVICES;
    }

    private String message;
    private MessageType messageType;
    private WroupDevice wroupDevice;


    public void setWroupDevice(WroupDevice wroupDevice) {
        this.wroupDevice = wroupDevice;
    }

    public WroupDevice getWroupDevice() {
        return wroupDevice;
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

    @Override
    public String toString() {
        return "MessageWrapper{" +
                "message='" + message + '\'' +
                ", messageType=" + messageType +
                ", wroupDevice=" + wroupDevice +
                '}';
    }

}
