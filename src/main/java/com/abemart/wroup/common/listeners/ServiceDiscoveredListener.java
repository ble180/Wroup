package com.abemart.wroup.common.listeners;


import com.abemart.wroup.common.WiFiP2PDevice;
import com.abemart.wroup.common.WiFiP2PError;

import java.util.List;

public interface ServiceDiscoveredListener {

    void onNewServiceDeviceDiscovered(WiFiP2PDevice serviceDevice);

    void onFinishServiceDeviceDiscovered(List<WiFiP2PDevice> serviceDevices);

    void onError(WiFiP2PError wiFiP2PError);

}
