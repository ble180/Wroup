# Wroup
Wroup is a WiFi P2P Android Library, or also know as WiFi Direct. This library adds a layer to the official WiFi Direct API to ease the use of this. The name of the library come from join Group + WiFi with the result of Wroup since the purpose of this library is creating WiFi P2P groups and allow the communication between the peers connected.


### Table of contents
* [Installation](#installation)
* [Usage](#usage)
    * [Getting Started](#getting-started)
    * [Introduction](#introduction)
    * [Server device](#server-device)
    * [Client device](#client-device)


## Installation
You only need to add the Wroup Library as dependency in your gradle file. The library is located in JCenter repositories, so you maybe need to add the JCenter repository to your gradle configuration:
```groovy
    repositories {
        jcenter()
    }

    compile 'com.abemart.wroup:wroup:0.9'
```

## Usage
### Getting started
Once that you have imported the Wroup dependency in your app, the first step is register the ```WiFiDirectBroadcastReceiver``` in your main activity as follows:
```java
    ...
    
    private WiFiDirectBroadcastReceiver wiFiDirectBroadcastReceiver;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
            
        wiFiDirectBroadcastReceiver = WiFiP2PInstance.getInstance(this).getBroadcastReceiver();
        ...
    }
    
    @Override
    protected void onResume() {
        super.onResume();
    
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
        registerReceiver(wiFiDirectBroadcastReceiver, intentFilter);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(wiFiDirectBroadcastReceiver);
    }
    
    ...
```

Many events in the official WiFi Direct API are received by this BroadcastReceiver because all the methods in the API are asynchronous.

### Introduction
We distinguish two types of devices:
* Server devices: When a group is created in WiFi Direct, there is a device that it's the group owner. In the Wroup library the Server Device is who creates the group and the group owner. Also it manages the interaction in the group. For example, if a new client has been connected to the group the server device sends to all the clients already connected this new connection. With this approach all the clients can know all the peers connected.
* Client devices: Clients devices can discover nearby groups and connecting to them. After a client is connected to the group, it can send messages to any of the peers connected.

### Server Device
To create a Service device you must obtain the ```WroupService``` instance and register a group in the local WiFi network.
```java
    ...
    
    WroupService wroupService = WroupService.getInstance(getApplicationContext());
    wroupService.registerService("Group Name", new ServiceRegisteredListener() {
    
        @Override
        public void onSuccessServiceRegistered() {
            ...
        }
    
        @Override
        public void onErrorServiceRegistered(WiFiP2PError wiFiP2PError) {
            Toast.makeText(getApplicationContext(), "Error creating group", Toast.LENGTH_SHORT).show();
        }
    
    });
    
```

Then you can implement a series of listener to know group changes (connections and disconnections):

```java


```



### Client Device