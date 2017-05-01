package com.abemart.wroup.service;


import android.content.Context;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.os.AsyncTask;
import android.util.Log;

import com.abemart.wroup.common.WiFiP2PDevice;
import com.abemart.wroup.common.WiFiP2PError;
import com.abemart.wroup.common.WiFiP2PInstance;
import com.abemart.wroup.common.direct.WiFiDirectUtils;
import com.abemart.wroup.common.listeners.ClientDisconnectedListener;
import com.abemart.wroup.common.listeners.ClientRegisteredListener;
import com.abemart.wroup.common.listeners.DataReceivedListener;
import com.abemart.wroup.common.listeners.PeerConnectedListener;
import com.abemart.wroup.common.listeners.ServiceRegisteredListener;
import com.abemart.wroup.common.messages.DisconnectionMessageContent;
import com.abemart.wroup.common.messages.MessageWrapper;
import com.abemart.wroup.common.messages.RegisteredDevicesMessageContent;
import com.abemart.wroup.common.messages.RegistrationMessageContent;
import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WiFiP2PService implements PeerConnectedListener {


    private static final String TAG = WiFiP2PService.class.getSimpleName();

    private static final String SERVICE_TYPE = "_pocha._tcp";
    public static final String SERVICE_PORT_PROPERTY = "SERVICE_PORT";
    public static final Integer SERVICE_PORT_VALUE = 9999;
    public static final String SERVICE_NAME_PROPERTY = "SERVICE_NAME";
    public static final String SERVICE_NAME_VALUE = "POCHA";

    private DataReceivedListener dataReceivedListener;
    private ClientRegisteredListener clientRegisteredListener;
    private ClientDisconnectedListener clientDisconnectedListener;
    private Map<String, WiFiP2PDevice> clientsConnected = new HashMap<>();
    private WiFiP2PInstance wiFiP2PInstance;

    private ServerSocket serverSocket;
    private Boolean groupAlreadyCreated = false;

    public WiFiP2PService(Context context) {
        wiFiP2PInstance = WiFiP2PInstance.getInstance(context);
        wiFiP2PInstance.setPeerConnectedListener(this);
    }

    public void registerService(String instanceName, final ServiceRegisteredListener serviceRegisteredListener) {

        // We need to start peer discovering because otherwise the clients cannot found the service
        wiFiP2PInstance.startPeerDiscovering();

        Map<String, String> record = new HashMap<>();
        record.put(SERVICE_PORT_PROPERTY, SERVICE_PORT_VALUE.toString());
        record.put(SERVICE_NAME_PROPERTY, SERVICE_NAME_VALUE);

        WifiP2pDnsSdServiceInfo serviceInfo = WifiP2pDnsSdServiceInfo.newInstance(instanceName, SERVICE_TYPE, record);

        wiFiP2PInstance.getWifiP2pManager().clearLocalServices(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Success clearing local services");
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Error clearing local services: " + reason);
            }
        });

        wiFiP2PInstance.getWifiP2pManager().addLocalService(wiFiP2PInstance.getChannel(), serviceInfo, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Service registered");
                serviceRegisteredListener.onSuccessServiceRegistered();

                // Create the group to the clients can connect to it
                removeAndCreateGroup();

                // Create the socket that will accept request
                createServerSocket();
            }

            @Override
            public void onFailure(int reason) {
                WiFiP2PError wiFiP2PError = WiFiP2PError.fromReason(reason);
                if (wiFiP2PError != null) {
                    Log.e(TAG, "Failure registering the service. Reason: " + wiFiP2PError.name());
                    serviceRegisteredListener.onErrorServiceRegistered(wiFiP2PError);
                }
            }

        });
    }

    public void disconnect() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
                Log.i(TAG, "ServerSocket closed");
            } catch (IOException e) {
                Log.e(TAG, "Error closing the serverSocket");
            }
        }

        groupAlreadyCreated = false;
        serverSocket = null;
        clientsConnected.clear();

        WiFiDirectUtils.removeGroup(wiFiP2PInstance);
        WiFiDirectUtils.clearLocalServices(wiFiP2PInstance);
        WiFiDirectUtils.stopPeerDiscovering(wiFiP2PInstance);
    }

    public void setDataReceivedListener(DataReceivedListener dataReceivedListener) {
        this.dataReceivedListener = dataReceivedListener;
    }

    public void setClientDisconnectedListener(ClientDisconnectedListener clientDisconnectedListener) {
        this.clientDisconnectedListener = clientDisconnectedListener;
    }

    public void setClientRegisteredListener(ClientRegisteredListener clientRegisteredListener) {
        this.clientRegisteredListener = clientRegisteredListener;
    }

    @Override
    public void onPeerConnected(WifiP2pInfo wifiP2pInfo) {
        Log.i(TAG, "OnPeerConnected...");

        if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
            Log.i(TAG, "I am the group owner");
            Log.i(TAG, "My addess is: " + wifiP2pInfo.groupOwnerAddress.getHostAddress());
        }
    }

    public void sendMessageToAllClients(final MessageWrapper message) {
        for (WiFiP2PDevice clientDevice : clientsConnected.values()) {
            sendMessage(clientDevice, message);
        }
    }


    public void sendMessage(final WiFiP2PDevice device, MessageWrapper message) {
        // Set the actual device to the message
        message.setWiFiP2PDevice(wiFiP2PInstance.getThisDevice());

        new AsyncTask<MessageWrapper, Void, Void>() {
            @Override
            protected Void doInBackground(MessageWrapper... params) {
                if (device != null && device.getDeviceServerSocketIP() != null) {
                    try {
                        Socket socket = new Socket();
                        socket.bind(null);

                        InetSocketAddress hostAddres = new InetSocketAddress(device.getDeviceServerSocketIP(), device.getDeviceServerSocketPort());
                        socket.connect(hostAddres, 2000);

                        Gson gson = new Gson();
                        String messageJson = gson.toJson(params[0]);

                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(messageJson.getBytes(), 0, messageJson.getBytes().length);

                        Log.d(TAG, "Sending data: " + params[0]);

                        socket.close();
                        outputStream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error creating client socket: " + e.getMessage());
                    }
                }

                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);
    }

    private void createServerSocket() {
        if (serverSocket == null) {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {

                    try {
                        serverSocket = new ServerSocket(SERVICE_PORT_VALUE);
                        Log.i(TAG, "Server socket created. Accepting requests...");

                        while (true) {
                            Socket socket = serverSocket.accept();

                            String dataReceived = IOUtils.toString(socket.getInputStream());
                            Log.i(TAG, "Data received: " + dataReceived);
                            Log.i(TAG, "From IP: " + socket.getInetAddress().getHostAddress());

                            Gson gson = new Gson();
                            MessageWrapper messageWrapper = gson.fromJson(dataReceived, MessageWrapper.class);
                            onMessageReceived(messageWrapper, socket.getInetAddress());
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error creating/closing server socket: " + e.getMessage());
                    }

                    return null;
                }

            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }


    private void removeAndCreateGroup() {
        wiFiP2PInstance.getWifiP2pManager().requestGroupInfo(wiFiP2PInstance.getChannel(), new WifiP2pManager.GroupInfoListener() {

            @Override
            public void onGroupInfoAvailable(final WifiP2pGroup group) {
                if (group != null) {
                    wiFiP2PInstance.getWifiP2pManager().removeGroup(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Group deleted");
                            Log.d(TAG, "\tNetwordk Name: " + group.getNetworkName());
                            Log.d(TAG, "\tInterface: " + group.getInterface());
                            Log.d(TAG, "\tPassword: " + group.getPassphrase());
                            Log.d(TAG, "\tOwner Name: " + group.getOwner().deviceName);
                            Log.d(TAG, "\tOwner Address: " + group.getOwner().deviceAddress);
                            Log.d(TAG, "\tClient list size: " + group.getClientList().size());

                            groupAlreadyCreated = false;

                            // Now we can create the group
                            createGroup();
                        }

                        @Override
                        public void onFailure(int reason) {
                            Log.e(TAG, "Error deleting group");
                        }
                    });
                } else {
                    createGroup();
                }
            }
        });
    }

    private void createGroup() {
        if (!groupAlreadyCreated) {
            wiFiP2PInstance.getWifiP2pManager().createGroup(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {

                @Override
                public void onSuccess() {
                    Log.i(TAG, "Group created!");
                    groupAlreadyCreated = true;
                }

                @Override
                public void onFailure(int reason) {
                    Log.e(TAG, "Error creating group. Reason: " + WiFiP2PError.fromReason(reason).name());
                }
            });
        }
    }

    private void onMessageReceived(MessageWrapper messageWrapper, InetAddress fromAddress) {
        if (messageWrapper.getMessageType().equals(MessageWrapper.MessageType.CONNECTION_MESSAGE)) {
            Gson gson = new Gson();

            String messageContentStr = messageWrapper.getMessage();
            RegistrationMessageContent registrationMessageContent = gson.fromJson(messageContentStr, RegistrationMessageContent.class);
            WiFiP2PDevice client = registrationMessageContent.getWiFiP2PDevice();
            client.setDeviceServerSocketIP(fromAddress.getHostAddress());
            clientsConnected.put(client.getDeviceMac(), client);

            Log.d(TAG, "New client registered:");
            Log.d(TAG, "\tDevice name: " + client.getDeviceName());
            Log.d(TAG, "\tDecive mac: " + client.getDeviceMac());
            Log.d(TAG, "\tDevice IP: " + client.getDeviceServerSocketIP());
            Log.d(TAG, "\tDevice ServerSocket port: " + client.getDeviceServerSocketPort());

            // Sending to all clients that new client is connected
            for (WiFiP2PDevice device : clientsConnected.values()) {
                if (!client.getDeviceMac().equals(device.getDeviceMac())) {
                    sendConnectionMessage(device, client);
                } else {
                    sendRegisteredDevicesMessage(device);
                }
            }

            if (clientRegisteredListener != null) {
                clientRegisteredListener.onClientRegistered(client);
            }
        } else if (messageWrapper.getMessageType().equals(MessageWrapper.MessageType.DISCONNECTION_MESSAGE)) {
            Gson gson = new Gson();

            String messageContentStr = messageWrapper.getMessage();
            DisconnectionMessageContent disconnectionMessageContent = gson.fromJson(messageContentStr, DisconnectionMessageContent.class);
            WiFiP2PDevice client = disconnectionMessageContent.getWiFiP2PDevice();
            clientsConnected.remove(client.getDeviceMac());

            Log.d(TAG, "Client disconnected:");
            Log.d(TAG, "\tDevice name: " + client.getDeviceName());
            Log.d(TAG, "\tDecive mac: " + client.getDeviceMac());
            Log.d(TAG, "\tDevice IP: " + client.getDeviceServerSocketIP());
            Log.d(TAG, "\tDevice ServerSocket port: " + client.getDeviceServerSocketPort());

            // Sending to all clients that a client is disconnected now
            for (WiFiP2PDevice device : clientsConnected.values()) {
                if (!client.getDeviceMac().equals(device.getDeviceMac())) {
                    sendDisconnectionMessage(device, client);
                }
            }

            if (clientDisconnectedListener != null) {
                clientDisconnectedListener.onClientDisconnected(client);
            }
        } else {
            if (dataReceivedListener != null) {
                dataReceivedListener.onDataReceived(messageWrapper);
            }
        }
    }

    private void sendConnectionMessage(WiFiP2PDevice deviceToSend, WiFiP2PDevice deviceConnected) {
        RegistrationMessageContent content = new RegistrationMessageContent();
        content.setWiFiP2PDevice(deviceConnected);

        Gson gson = new Gson();

        MessageWrapper messageWrapper = new MessageWrapper();
        messageWrapper.setMessageType(MessageWrapper.MessageType.CONNECTION_MESSAGE);
        messageWrapper.setMessage(gson.toJson(content));

        sendMessage(deviceToSend, messageWrapper);
    }

    private void sendDisconnectionMessage(WiFiP2PDevice deviceToSend, WiFiP2PDevice deviceDisconnected) {
        DisconnectionMessageContent content = new DisconnectionMessageContent();
        content.setWiFiP2PDevice(deviceDisconnected);

        Gson gson = new Gson();

        MessageWrapper disconnectionMessage = new MessageWrapper();
        disconnectionMessage.setMessageType(MessageWrapper.MessageType.DISCONNECTION_MESSAGE);
        disconnectionMessage.setMessage(gson.toJson(content));

        sendMessage(deviceToSend, disconnectionMessage);
    }

    private void sendRegisteredDevicesMessage(WiFiP2PDevice deviceToSend) {
        List<WiFiP2PDevice> devicesConnected = new ArrayList<>();
        for (WiFiP2PDevice device : clientsConnected.values()) {
            if (!device.getDeviceMac().equals(deviceToSend.getDeviceMac())) {
                devicesConnected.add(device);
            }
        }

        RegisteredDevicesMessageContent content = new RegisteredDevicesMessageContent();
        content.setDevicesRegistered(devicesConnected);

        Gson gson = new Gson();

        MessageWrapper messageWrapper = new MessageWrapper();
        messageWrapper.setMessageType(MessageWrapper.MessageType.REGISTERED_DEVICES);
        messageWrapper.setMessage(gson.toJson(content));

        sendMessage(deviceToSend, messageWrapper);
    }

}
