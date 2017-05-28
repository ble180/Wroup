package com.abemart.wroup.common;


import android.content.Context;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.abemart.wroup.common.listeners.PeerConnectedListener;
import com.abemart.wroup.common.listeners.ServiceDisconnectedListener;


public class WiFiP2PInstance implements WifiP2pManager.ConnectionInfoListener {

    private static final String TAG = WiFiP2PInstance.class.getSimpleName();

    private static WiFiP2PInstance instance;

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private WiFiDirectBroadcastReceiver broadcastReceiver;

    private WroupDevice thisDevice;

    private PeerConnectedListener peerConnectedListener;
    private ServiceDisconnectedListener serviceDisconnectedListener;

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

    public void setThisDevice(WroupDevice thisDevice) {
        this.thisDevice = thisDevice;
    }

    public WroupDevice getThisDevice() {
        return thisDevice;
    }

    public void setPeerConnectedListener(PeerConnectedListener peerConnectedListener) {
        this.peerConnectedListener = peerConnectedListener;
    }

    public void setServerDisconnectedListener(ServiceDisconnectedListener serviceDisconnectedListener) {
        this.serviceDisconnectedListener = serviceDisconnectedListener;
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
        if (peerConnectedListener != null) {
            peerConnectedListener.onPeerConnected(info);
        }
    }

    public void onServerDeviceDisconnected() {
        if (serviceDisconnectedListener != null) {
            serviceDisconnectedListener.onServerDisconnectedListener();
        }
    }

}
