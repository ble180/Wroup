package com.abemart.wroup.common;


import android.content.Context;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.abemart.wroup.common.listeners.PeerConnectedListener;
import com.abemart.wroup.common.listeners.ServiceDisconnectedListener;

import java.util.ArrayList;
import java.util.List;


public class WiFiP2PInstance implements WifiP2pManager.ConnectionInfoListener {

    private static final String TAG = WiFiP2PInstance.class.getSimpleName();

    private static WiFiP2PInstance instance;

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private WiFiDirectBroadcastReceiver broadcastReceiver;

    private WiFiP2PDevice thisDevice;

    private List<PeerConnectedListener> peerConnectedListeners = new ArrayList<>();
    private List<ServiceDisconnectedListener> serviceDisconnectedListeners = new ArrayList<>();

    private WiFiP2PInstance() {
    }

    private WiFiP2PInstance(Context context) {
        this();

        wifiP2pManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
        channel = wifiP2pManager.initialize(context, context.getMainLooper(), null);
        broadcastReceiver = new WiFiDirectBroadcastReceiver(this);
    }


    public static WiFiP2PInstance getInstance(Context context) {
        if (instance == null) {
            instance = new WiFiP2PInstance(context);
        }

        return instance;
    }

    public WifiP2pManager getWifiP2pManager() {
        return wifiP2pManager;
    }

    public WifiP2pManager.Channel getChannel() {
        return channel;
    }

    public WiFiDirectBroadcastReceiver getBroadcastReceiver() {
        return broadcastReceiver;
    }

    public void setThisDevice(WiFiP2PDevice thisDevice) {
        this.thisDevice = thisDevice;
    }

    public WiFiP2PDevice getThisDevice() {
        return thisDevice;
    }

    public void addPeerConnectedListener(PeerConnectedListener peerConnectedListener) {
        peerConnectedListeners.add(peerConnectedListener);
    }

    public void addServerDisconnectedListener(ServiceDisconnectedListener serviceDisconnectedListener) {
        serviceDisconnectedListeners.add(serviceDisconnectedListener);
    }

    public void startPeerDiscovering() {
        wifiP2pManager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Peers discovering initialized");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Error initiating peer disconvering. Reason: " + reason);
            }
        });
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        for (PeerConnectedListener peerConnectedListener : peerConnectedListeners) {
            peerConnectedListener.onPeerConnected(info);
        }
    }

    public void onServerDeviceDisconnected() {
        for (ServiceDisconnectedListener serviceDisconnectedListener : serviceDisconnectedListeners) {
            serviceDisconnectedListener.onServerDisconnectedListener();
        }
    }

}
