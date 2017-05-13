package com.abemart.wroup.common.listeners;


import com.abemart.wroup.common.WroupDevice;
import com.abemart.wroup.common.WiFiP2PError;

import java.util.List;

public interface ServiceDiscoveredListener {

    void onNewServiceDeviceDiscovered(WroupDevice serviceDevice);

    void onFinishServiceDeviceDiscovered(List<WroupDevice> serviceDevices);

    void onError(WiFiP2PError wiFiP2PError);

}
