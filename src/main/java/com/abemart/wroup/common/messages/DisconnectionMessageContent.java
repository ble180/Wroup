package com.abemart.wroup.common.messages;


import com.abemart.wroup.common.WiFiP2PDevice;

public class DisconnectionMessageContent {

    private String clientName;
    private WiFiP2PDevice wiFiP2PDevice;

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }


    public void setWiFiP2PDevice(WiFiP2PDevice wiFiP2PDevice) {
        this.wiFiP2PDevice = wiFiP2PDevice;
    }

    public WiFiP2PDevice getWiFiP2PDevice() {
        return wiFiP2PDevice;
    }
}
