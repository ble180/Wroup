package com.abemart.wroup.common.messages;


import com.abemart.wroup.common.WroupDevice;

public class DisconnectionMessageContent {

    private WroupDevice wroupDevice;


    public void setWroupDevice(WroupDevice wroupDevice) {
        this.wroupDevice = wroupDevice;
    }

    public WroupDevice getWroupDevice() {
        return wroupDevice;
    }

}
