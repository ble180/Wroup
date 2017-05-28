package com.abemart.wroup.common.listeners;


import com.abemart.wroup.common.WroupDevice;
import com.abemart.wroup.common.WiFiP2PError;
import com.abemart.wroup.common.WroupServiceDevice;
import com.abemart.wroup.service.WroupService;

import java.util.List;

public interface ServiceDiscoveredListener {

    void onNewServiceDeviceDiscovered(WroupServiceDevice serviceDevice);

    void onFinishServiceDeviceDiscovered(List<WroupServiceDevice> serviceDevices);

    void onError(WiFiP2PError wiFiP2PError);

}
