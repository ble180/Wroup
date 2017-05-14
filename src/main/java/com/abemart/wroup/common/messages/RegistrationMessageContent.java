package com.abemart.wroup.common.messages;


import com.abemart.wroup.common.WroupDevice;

public class RegistrationMessageContent {

    private WroupDevice wroupDevice;

    public WroupDevice getWroupDevice() {
        return wroupDevice;
    }

    public void setWroupDevice(WroupDevice wroupDevice) {
        this.wroupDevice = wroupDevice;
    }

}
