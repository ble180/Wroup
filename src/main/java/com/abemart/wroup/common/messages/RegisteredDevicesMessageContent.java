package com.abemart.wroup.common.messages;


import com.abemart.wroup.common.WroupDevice;

import java.util.List;

public class RegisteredDevicesMessageContent {

    private List<WroupDevice> devicesRegistered;

    public List<WroupDevice> getDevicesRegistered() {
        return devicesRegistered;
    }

    public void setDevicesRegistered(List<WroupDevice> devicesRegistered) {
        this.devicesRegistered = devicesRegistered;
    }

}
