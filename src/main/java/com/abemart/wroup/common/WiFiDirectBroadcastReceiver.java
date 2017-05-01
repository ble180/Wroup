package com.abemart.wroup.common;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;


public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = WiFiDirectBroadcastReceiver.class.getName();

    private WiFiP2PInstance wiFiP2PInstance;

    public WiFiDirectBroadcastReceiver(WiFiP2PInstance wiFiP2PInstance) {
        this.wiFiP2PInstance = wiFiP2PInstance;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {

            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.i(TAG, "WiFi P2P is active");
            } else {
                Log.i(TAG, "WiFi P2P isn't active");
            }

        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {

            Log.d(TAG, "New peers detected. Requesting peers list...");

            if (wiFiP2PInstance != null) {
                wiFiP2PInstance.getWifiP2pManager().requestPeers(wiFiP2PInstance.getChannel(), new WifiP2pManager.PeerListListener() {

                    @Override
                    public void onPeersAvailable(WifiP2pDeviceList peers) {
                        if (!peers.getDeviceList().isEmpty()) {
                            Log.d(TAG, "Peers detected:");

                            for (WifiP2pDevice device : peers.getDeviceList()) {
                                Log.d(TAG, "\tDevice Name: " + device.deviceName);
                                Log.d(TAG, "\tDevice Address: " + device.deviceAddress);
                            }
                        } else {
                            Log.d(TAG, "No peers detected");
                        }
                    }
                });
            }

        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if (networkInfo.isConnected()) {
                Log.d(TAG, "New device is connected");
                wiFiP2PInstance.getWifiP2pManager().requestConnectionInfo(wiFiP2PInstance.getChannel(), wiFiP2PInstance);
            } else {
                Log.d(TAG, "The server device has been disconnected");
                wiFiP2PInstance.onServerDeviceDisconnected();
            }

        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {

            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Log.d(TAG, "This device name: " + device.deviceName);
            Log.d(TAG, "This device address: " + device.deviceAddress);

            if (wiFiP2PInstance.getThisDevice() == null) {
                wiFiP2PInstance.setThisDevice(new WiFiP2PDevice(device));
            }
        }
    }

}
