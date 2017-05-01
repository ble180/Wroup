package com.abemart.wroup.common.messages;


import com.abemart.wroup.common.WiFiP2PDevice;

import java.util.List;

public class RegisteredDevicesMessageContent {

    private List<WiFiP2PDevice> devicesRegistered;

    public List<WiFiP2PDevice> getDevicesRegistered() {
        return devicesRegistered;
    }

    public void setDevicesRegistered(List<WiFiP2PDevice> devicesRegistered) {
        this.devicesRegistered = devicesRegistered;
    }

}
