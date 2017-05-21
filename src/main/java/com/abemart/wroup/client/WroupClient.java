package com.abemart.wroup.client;


import android.content.Context;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.DnsSdServiceResponseListener;
import android.net.wifi.p2p.WifiP2pManager.DnsSdTxtRecordListener;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.net.wifi.p2p.nsd.WifiP2pServiceRequest;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import com.abemart.wroup.common.WiFiP2PError;
import com.abemart.wroup.common.WiFiP2PInstance;
import com.abemart.wroup.common.WroupDevice;
import com.abemart.wroup.common.WroupServiceDevice;
import com.abemart.wroup.common.direct.WiFiDirectUtils;
import com.abemart.wroup.common.listeners.ClientDisconnectedListener;
import com.abemart.wroup.common.listeners.ClientRegisteredListener;
import com.abemart.wroup.common.listeners.DataReceivedListener;
import com.abemart.wroup.common.listeners.PeerConnectedListener;
import com.abemart.wroup.common.listeners.ServiceConnectedListener;
import com.abemart.wroup.common.listeners.ServiceDisconnectedListener;
import com.abemart.wroup.common.listeners.ServiceDiscoveredListener;
import com.abemart.wroup.common.messages.DisconnectionMessageContent;
import com.abemart.wroup.common.messages.MessageWrapper;
import com.abemart.wroup.common.messages.RegisteredDevicesMessageContent;
import com.abemart.wroup.common.messages.RegistrationMessageContent;
import com.abemart.wroup.service.WroupService;
import com.google.gson.Gson;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton class acting as a client device.
 * <p>
 * Wroup Library will allow you to create a "Server" device and multiple "Client" devices. The
 * {@link WroupService} can register a service which could be discover by the multiple client
 * devices. The client will search the Wroup services registered in the local network and could
 * connect to any ot them.
 * <p>
 * WroupClient only discover Wroup services, can exist multiple services registered with WiFi-P2P in
 * the same local network but only a WroupClient instance will found services registered by a
 * WroupService device.
 * <p>
 * To discover the Wroup services registered you only need to do the following:
 * <pre>
 * {@code
 *
 * wiFiP2PClient = WroupClient.getInstance(getApplicationContext());
 * wiFiP2PClient.discoverServices(5000L, new ServiceDiscoveredListener() {
 *
 *  @Override
 *  public void onNewServiceDeviceDiscovered(WroupServiceDevice serviceDevice) {
 *      Log.i(TAG, "New service found:");
 *      Log.i(TAG, "\tName: " + serviceDevice.getDeviceName());
 *  }
 *
 *  @Override
 *  public void onFinishServiceDeviceDiscovered(List<WroupServiceDevice> serviceDevices) {
 *      Log.i(TAG, "Found '" + serviceDevices.size() + "' services");
 *  }
 *
 *  @Override
 *  public void onError(WiFiP2PError wiFiP2PError) {
 *      Toast.makeText(getApplicationContext(), "Error searching groups: " + wiFiP2PError, Toast.LENGTH_LONG).show();
 *  }
 * });
 * }
 * </pre>
 * Once that you have the desired service to which connect you must call to
 * {@link #connectToService(WroupServiceDevice, ServiceConnectedListener)} passing as argument the
 * appropiate {@link WroupServiceDevice} obtained in the <code>discoverServices()</code> call.
 */
public class WroupClient implements PeerConnectedListener, ServiceDisconnectedListener {

    private static final String TAG = WroupClient.class.getSimpleName();

    private static WroupClient instance;

    private List<WroupServiceDevice> serviceDevices = new ArrayList<>();

    private DnsSdTxtRecordListener dnsSdTxtRecordListener;
    private DnsSdServiceResponseListener dnsSdServiceResponseListener;
    private ServiceConnectedListener serviceConnectedListener;
    private DataReceivedListener dataReceivedListener;
    private ServiceDisconnectedListener serviceDisconnectedListener;
    private ClientRegisteredListener clientRegisteredListener;
    private ClientDisconnectedListener clientDisconnectedListener;

    private ServerSocket serverSocket;

    private WiFiP2PInstance wiFiP2PInstance;
    private WroupDevice serviceDevice;
    private Map<String, WroupDevice> clientsConnected;
    private Boolean isRegistered = false;

    private WroupClient(Context context) {
        wiFiP2PInstance = WiFiP2PInstance.getInstance(context);
        wiFiP2PInstance.setPeerConnectedListener(this);
        wiFiP2PInstance.setServerDisconnectedListener(this);
        this.clientsConnected = new HashMap<>();
    }

    /**
     * Return the WroupClient instance. If the instance doesn't exist yet, it's created and returned.
     *
     * @param context The application context.
     * @return The actual WroupClient instance.
     */
    public static WroupClient getInstance(Context context) {
        if (instance == null && context != null) {
            instance = new WroupClient(context);
        }
        return instance;
    }

    /**
     * Start to discover Wroup services registered in the current local network.
     * <p>
     * Before you start to discover services you must to register the <code>WiFiDirectBroadcastReceiver</code>
     * in the <code>onResume()</code> method of your activity.
     *
     * @param discoveringTimeInMillis   The time in milliseconds to search for registered Wroup services.
     * @param serviceDiscoveredListener The listener to notify changes of the services found by the client.
     * @see com.abemart.wroup.common.WiFiDirectBroadcastReceiver
     */
    public void discoverServices(Long discoveringTimeInMillis, final ServiceDiscoveredListener serviceDiscoveredListener) {
        serviceDevices.clear();

        // We need to start discovering peers to activate the service search
        wiFiP2PInstance.startPeerDiscovering();

        setupDnsListeners(wiFiP2PInstance, serviceDiscoveredListener);
        WiFiDirectUtils.clearServiceRequest(wiFiP2PInstance);

        WifiP2pServiceRequest serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
        wiFiP2PInstance.getWifiP2pManager().addServiceRequest(wiFiP2PInstance.getChannel(), serviceRequest, new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Success adding service request");
            }

            @Override
            public void onFailure(int reason) {
                WiFiP2PError wiFiP2PError = WiFiP2PError.fromReason(reason);
                Log.e(TAG, "Error adding service request. Reason: " + WiFiP2PError.fromReason(reason));
                serviceDiscoveredListener.onError(wiFiP2PError);
            }

        });

        wiFiP2PInstance.getWifiP2pManager().discoverServices(wiFiP2PInstance.getChannel(), new WifiP2pManager.ActionListener() {

            @Override
            public void onSuccess() {
                Log.d(TAG, "Success initiating disconvering services");
            }

            @Override
            public void onFailure(int reason) {
                WiFiP2PError wiFiP2PError = WiFiP2PError.fromReason(reason);
                if (wiFiP2PError != null) {
                    Log.e(TAG, "Error discovering services. Reason: " + wiFiP2PError.name());
                    serviceDiscoveredListener.onError(wiFiP2PError);
                }
            }

        });

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                serviceDiscoveredListener.onFinishServiceDeviceDiscovered(serviceDevices);
            }
        }, discoveringTimeInMillis);
    }

    /**
     * Start the connection with the <code>WroupServiceDevice</code> passed by argument. When the
     * connection is stablished with the device service the {@link ServiceConnectedListener#onServiceConnected(WroupDevice)}
     * method is called.
     * <p>
     * When the client is connected to the service, it's connected to the WiFi Direct Group created
     * by the service device. Once the client belongs to the "Wroup" (group), it can know when a new
     * client is connected or disconnected from it.
     *
     * @param serviceDevice            The WroupServiceDevice with you want to connect.
     * @param serviceConnectedListener The listener to know when the client device is connected to
     *                                 the desired service.
     */
    public void connectToService(final WroupServiceDevice serviceDevice, ServiceConnectedListener serviceConnectedListener) {
        this.serviceDevice = serviceDevice;
        this.serviceConnectedListener = serviceConnectedListener;

        WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
        wifiP2pConfig.deviceAddress = serviceDevice.getDeviceMac();

        wiFiP2PInstance.getWifiP2pManager().connect(wiFiP2PInstance.getChannel(), wifiP2pConfig, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, "Initiated connection to device: ");
                Log.i(TAG, "\tDevice name: " + serviceDevice.getDeviceName());
                Log.i(TAG, "\tDevice address: " + serviceDevice.getDeviceMac());
            }

            @Override
            public void onFailure(int reason) {
                Log.e(TAG, "Fail initiation connection. Reason: " + WiFiP2PError.fromReason(reason));
            }
        });
    }

    /**
     * Set the listener to know when data is received from the service device or other client devices
     * connected to the same group.
     *
     * @param dataReceivedListener The <code>DataReceivedListener</code> to notify data entries.
     */
    public void setDataReceivedListener(DataReceivedListener dataReceivedListener) {
        this.dataReceivedListener = dataReceivedListener;
    }

    /**
     * Set the listener to notify when the service device has been disconnected.
     *
     * @param serviceDisconnectedListener The <code>ServiceDisconnectedListener</code> to notify
     *                                    service device disconnections.
     */
    public void setServerDisconnetedListener(ServiceDisconnectedListener serviceDisconnectedListener) {
        this.serviceDisconnectedListener = serviceDisconnectedListener;
    }

    /**
     * Set the listener to know when a new client is registered in the actual group.
     *
     * @param clientRegisteredListener The <code>ClientRegisteredListener</code> to notify new
     *                                 connections in the group.
     */
    public void setClientRegisteredListener(ClientRegisteredListener clientRegisteredListener) {
        this.clientRegisteredListener = clientRegisteredListener;
    }

    /**
     * Set the listener to know when a client has been disconnected from the group.
     *
     * @param clientDisconnectedListener The <code>ClientDisconnectedListener</code> to notify
     *                                   client disconnections.
     */
    public void setClientDisconnectedListener(ClientDisconnectedListener clientDisconnectedListener) {
        this.clientDisconnectedListener = clientDisconnectedListener;
    }

    @Override
    public void onPeerConnected(WifiP2pInfo wifiP2pInfo) {
        Log.i(TAG, "OnPeerConnected...");

        if (wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner) {
            Log.e(TAG, "I shouldn't be the group owner, I'am a client!");
        }

        if (wifiP2pInfo.groupFormed && serviceDevice != null && !isRegistered) {
            serviceDevice.setDeviceServerSocketIP(wifiP2pInfo.groupOwnerAddress.getHostAddress());
            Log.i(TAG, "The Server Address is: " + wifiP2pInfo.groupOwnerAddress.getHostAddress());

            // We are connected to the server. Create a server socket to receive messages
            createServerSocket();

            // FIXME - Change this into a server socket creation listener or similar
            // Wait 2 seconds for the server socket creation
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    // We send the negotiation message to the server
                    sendServerRegistrationMessage();
                    if (serviceConnectedListener != null) {
                        serviceConnectedListener.onServiceConnected(serviceDevice);
                    }

                    isRegistered = true;
                }
            }, 2000);
        }
    }

    @Override
    public void onServerDisconnectedListener() {
        // If the server is disconnected the client is cleared
        disconnect();

        if (serviceDisconnectedListener != null) {
            serviceDisconnectedListener.onServerDisconnectedListener();
        }
    }

    /**
     * Send a message to the service device.
     *
     * @param message The message to be sent.
     */
    public void sendMessageToServer(MessageWrapper message) {
        sendMessage(serviceDevice, message);
    }

    /**
     * Send a message to all the devices connected to the group, including the service device.
     *
     * @param message The message to be sent.
     */
    public void sendMessageToAllClients(MessageWrapper message) {
        sendMessageToServer(message);

        for (WroupDevice device : clientsConnected.values()) {
            if (!device.getDeviceMac().equals(wiFiP2PInstance.getThisDevice().getDeviceMac())) {
                sendMessage(device, message);
            }
        }
    }

    /**
     * Send a message to the desired device who it's connected in the group.
     *
     * @param device  The receiver of the message.
     * @param message The message to be sent.
     */
    public void sendMessage(final WroupDevice device, MessageWrapper message) {
        // Set the actual device to the message
        message.setWroupDevice(wiFiP2PInstance.getThisDevice());

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

    /**
     * Disconnect from the actual group connected. Before the disconnection, the client sends a
     * message to the service device to notify the disconnection.
     */
    public void disconnect() {
        if (serverSocket != null) {
            try {
                serverSocket.close();
                Log.i(TAG, "ServerSocket closed");
            } catch (IOException e) {
                Log.e(TAG, "Error closing the serverSocket");
            }
        }

        sendDisconnectionMessage();

        // FIXME - Change this into a message sent it listener
        // Wait 2 seconds to disconnection message was sent
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                WiFiDirectUtils.clearServiceRequest(wiFiP2PInstance);
                WiFiDirectUtils.stopPeerDiscovering(wiFiP2PInstance);
                WiFiDirectUtils.removeGroup(wiFiP2PInstance);

                serverSocket = null;
                isRegistered = false;
                clientsConnected.clear();
            }
        }, 2000);
    }

    /**
     * Obtain the devices connected to the actual group.
     *
     * @return the devices connected to the actual group.
     */
    public Collection<WroupDevice> getClientsConnected() {
        return clientsConnected.values();
    }

    private void setupDnsListeners(WiFiP2PInstance wiFiP2PInstance, ServiceDiscoveredListener serviceDiscoveredListener) {
        if (dnsSdTxtRecordListener == null || dnsSdServiceResponseListener == null) {
            dnsSdTxtRecordListener = getTxtRecordListener(serviceDiscoveredListener);
            dnsSdServiceResponseListener = getServiceResponseListener();

            wiFiP2PInstance.getWifiP2pManager().setDnsSdResponseListeners(wiFiP2PInstance.getChannel(), dnsSdServiceResponseListener, dnsSdTxtRecordListener);
        }
    }

    private DnsSdTxtRecordListener getTxtRecordListener(final ServiceDiscoveredListener serviceDiscoveredListener) {
        return new DnsSdTxtRecordListener() {

            @Override
            public void onDnsSdTxtRecordAvailable(String fullDomainName, Map<String, String> txtRecordMap, WifiP2pDevice device) {

                if (txtRecordMap.containsKey(WroupService.SERVICE_NAME_PROPERTY) && txtRecordMap.get(WroupService.SERVICE_NAME_PROPERTY).equalsIgnoreCase(WroupService.SERVICE_NAME_VALUE)) {
                    Integer servicePort = Integer.valueOf(txtRecordMap.get(WroupService.SERVICE_PORT_PROPERTY));
                    WroupServiceDevice serviceDevice = new WroupServiceDevice(device);
                    serviceDevice.setDeviceServerSocketPort(servicePort);
                    serviceDevice.setTxtRecordMap(txtRecordMap);

                    if (!serviceDevices.contains(serviceDevice)) {
                        Log.i(TAG, "Found a new Wroup service: ");
                        Log.i(TAG, "\tDomain Name: " + fullDomainName);
                        Log.i(TAG, "\tDevice Name: " + device.deviceName);
                        Log.i(TAG, "\tDevice Address: " + device.deviceAddress);
                        Log.i(TAG, "\tServer socket Port: " + serviceDevice.getDeviceServerSocketPort());

                        serviceDevices.add(serviceDevice);
                        serviceDiscoveredListener.onNewServiceDeviceDiscovered(serviceDevice);
                    }
                } else {
                    Log.d(TAG, "Found a new service: ");
                    Log.d(TAG, "\tDomain Name: " + fullDomainName);
                    Log.d(TAG, "\tDevice Name: " + device.deviceName);
                    Log.d(TAG, "\tDevice Address: " + device.deviceAddress);
                }
            }
        };
    }

    private DnsSdServiceResponseListener getServiceResponseListener() {
        return new DnsSdServiceResponseListener() {

            @Override
            public void onDnsSdServiceAvailable(String instanceName, String registrationType, WifiP2pDevice srcDevice) {

            }
        };
    }

    private void createServerSocket() {
        if (serverSocket == null) {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {

                    try {
                        serverSocket = new ServerSocket(0);

                        int port = serverSocket.getLocalPort();
                        wiFiP2PInstance.getThisDevice().setDeviceServerSocketPort(port);

                        Log.i(TAG, "Client ServerSocket created. Accepting requests...");
                        Log.i(TAG, "\tPort: " + port);

                        while (true) {
                            Socket socket = serverSocket.accept();

                            String dataReceived = IOUtils.toString(socket.getInputStream());
                            Log.i(TAG, "Data received: " + dataReceived);
                            Log.i(TAG, "From IP: " + socket.getInetAddress().getHostAddress());

                            Gson gson = new Gson();
                            MessageWrapper messageWrapper = gson.fromJson(dataReceived, MessageWrapper.class);
                            onMessageReceived(messageWrapper);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error creating/closing client ServerSocket: " + e.getMessage());
                    }

                    return null;
                }

            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void onMessageReceived(MessageWrapper messageWrapper) {
        if (MessageWrapper.MessageType.CONNECTION_MESSAGE.equals(messageWrapper.getMessageType())) {
            Gson gson = new Gson();

            String messageContentStr = messageWrapper.getMessage();
            RegistrationMessageContent registrationMessageContent = gson.fromJson(messageContentStr, RegistrationMessageContent.class);
            WroupDevice device = registrationMessageContent.getWroupDevice();
            clientsConnected.put(device.getDeviceMac(), device);

            if (clientRegisteredListener != null) {
                clientRegisteredListener.onClientRegistered(device);
            }

            Log.d(TAG, "New client connected to the group:");
            Log.d(TAG, "\tDevice name: " + device.getDeviceName());
            Log.d(TAG, "\tDecive mac: " + device.getDeviceMac());
            Log.d(TAG, "\tDevice IP: " + device.getDeviceServerSocketIP());
            Log.d(TAG, "\tDevice ServerSocket port: " + device.getDeviceServerSocketPort());
        } else if (MessageWrapper.MessageType.DISCONNECTION_MESSAGE.equals(messageWrapper.getMessageType())) {
            Gson gson = new Gson();

            String messageContentStr = messageWrapper.getMessage();
            DisconnectionMessageContent disconnectionMessageContent = gson.fromJson(messageContentStr, DisconnectionMessageContent.class);
            WroupDevice device = disconnectionMessageContent.getWroupDevice();
            clientsConnected.remove(device.getDeviceMac());

            if (clientDisconnectedListener != null) {
                clientDisconnectedListener.onClientDisconnected(device);
            }

            Log.d(TAG, "Client disconnected from the group:");
            Log.d(TAG, "\tDevice name: " + device.getDeviceName());
            Log.d(TAG, "\tDecive mac: " + device.getDeviceMac());
            Log.d(TAG, "\tDevice IP: " + device.getDeviceServerSocketIP());
            Log.d(TAG, "\tDevice ServerSocket port: " + device.getDeviceServerSocketPort());
        } else if (MessageWrapper.MessageType.REGISTERED_DEVICES.equals(messageWrapper.getMessageType())) {
            Gson gson = new Gson();

            String messageContentStr = messageWrapper.getMessage();
            RegisteredDevicesMessageContent registeredDevicesMessageContent = gson.fromJson(messageContentStr, RegisteredDevicesMessageContent.class);
            List<WroupDevice> devicesConnected = registeredDevicesMessageContent.getDevicesRegistered();

            for (WroupDevice device : devicesConnected) {
                clientsConnected.put(device.getDeviceMac(), device);
                Log.d(TAG, "Client already connected to the group:");
                Log.d(TAG, "\tDevice name: " + device.getDeviceName());
                Log.d(TAG, "\tDecive mac: " + device.getDeviceMac());
                Log.d(TAG, "\tDevice IP: " + device.getDeviceServerSocketIP());
                Log.d(TAG, "\tDevice ServerSocket port: " + device.getDeviceServerSocketPort());
            }
        } else {
            if (dataReceivedListener != null) {
                dataReceivedListener.onDataReceived(messageWrapper);
            }
        }
    }

    private void sendServerRegistrationMessage() {
        RegistrationMessageContent content = new RegistrationMessageContent();
        content.setWroupDevice(wiFiP2PInstance.getThisDevice());

        Gson gson = new Gson();

        MessageWrapper negotiationMessage = new MessageWrapper();
        negotiationMessage.setMessageType(MessageWrapper.MessageType.CONNECTION_MESSAGE);
        negotiationMessage.setMessage(gson.toJson(content));

        sendMessageToServer(negotiationMessage);
    }

    private void sendDisconnectionMessage() {
        DisconnectionMessageContent content = new DisconnectionMessageContent();
        content.setWroupDevice(wiFiP2PInstance.getThisDevice());

        Gson gson = new Gson();

        MessageWrapper disconnectionMessage = new MessageWrapper();
        disconnectionMessage.setMessageType(MessageWrapper.MessageType.DISCONNECTION_MESSAGE);
        disconnectionMessage.setMessage(gson.toJson(content));

        sendMessageToServer(disconnectionMessage);
    }

}
