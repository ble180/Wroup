package com.abemart.wroup.common.messages;


import com.abemart.wroup.common.WroupDevice;

public class DisconnectionMessageContent {

    private String clientName;
    private WroupDevice wroupDevice;

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }


    public void setWroupDevice(WroupDevice wroupDevice) {
        this.wroupDevice = wroupDevice;
    }

    public WroupDevice getWroupDevice() {
        return wroupDevice;
    }
}
