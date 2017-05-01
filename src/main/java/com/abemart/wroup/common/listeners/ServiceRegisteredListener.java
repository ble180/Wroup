package com.abemart.wroup.common.listeners;


import com.abemart.wroup.common.WiFiP2PError;

public interface ServiceRegisteredListener {

    void onSuccessServiceRegistered();

    void onErrorServiceRegistered(WiFiP2PError wiFiP2PError);

}
