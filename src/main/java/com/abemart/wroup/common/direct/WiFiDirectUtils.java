package com.abemart.wroup.common.direct;


import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.abemart.wroup.common.WiFiP2PError;
import com.abemart.wroup.common.WiFiP2PInstance;

public class WiFiDirectUtils {

    private static final String TAG = WiFiDirectUtils.class.getSimpleName();

    public static void clearServiceRequest(WiFiP2PInstance wiFiP2PInstance) {
        wiFiP2PInstance.getWifiP2pManager().clearServiceRequests(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Success clearing service request");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Error clearing service request: " + reason);
            }

        });
    }

    public static void clearLocalServices(WiFiP2PInstance wiFiP2PInstance) {
        wiFiP2PInstance.getWifiP2pManager().clearLocalServices(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Local services cleared");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Error clearing local services: " + WiFiP2PError.fromReason(reason));
            }

        });
    }

    public static void cancelConnect(WiFiP2PInstance wiFiP2PInstance) {
        wiFiP2PInstance.getWifiP2pManager().cancelConnect(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Connect canceled successfully");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Error canceling connect: " + WiFiP2PError.fromReason(reason));
            }

        });
    }

    public static void removeGroup(final WiFiP2PInstance wiFiP2PInstance) {
        wiFiP2PInstance.getWifiP2pManager().requestGroupInfo(wiFiP2PInstance.getChannel(), new WifiP2pManager.GroupInfoListener() {
            @Override
            public void onGroupInfoAvailable(final WifiP2pGroup group) {
                wiFiP2PInstance.getWifiP2pManager().removeGroup(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.i(TAG, "Group removed: " + group.getNetworkName());
                    }

                    @Override
                    public void onFailure(int reason) {
                        Log.e(TAG, "Fail disconnecting from group. Reason: " + WiFiP2PError.fromReason(reason));
                    }
                });
            }
        });
    }

    public static void stopPeerDiscovering(WiFiP2PInstance wiFiP2PInstance) {
        wiFiP2PInstance.getWifiP2pManager().stopPeerDiscovery(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Peer disconvering stopped");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Error stopping peer discovering: " + WiFiP2PError.fromReason(reason));
            }

        });
    }

}
