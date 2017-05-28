# Wroup
Wroup is a WiFi P2P Android Library, or also know as WiFi Direct. This library adds a layer to the official WiFi Direct API to ease the use of this. The name of the library come from join Group + WiFi with the result of Wroup since the purpose of this library is creating WiFi P2P groups and allow the communication between the peers connected.


### Table of contents
* [Installation](#installation)
* [Usage](#usage)
    * [Getting Started](#getting-started)
    * [Introduction](#introduction)
    * [Server device](#server-device)
    * [Client device](#client-device)
    * [Sending messages](#sending-messages)
* [Example App](#example-app)
* [Contributing](#contributing)
* [License](#license)

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
wroupService.setClientConnectedListener(new ClientConnectedListener() {
    @Override
    public void onClientConnected(WroupDevice wroupDevice) {
        // New client connected to the group
    });
});
    
wroupService.setClientDisconnected(new ClientDisconnectedListener() {
    @Override
    public void onClientDisconnected(WroupDevice wroupDevice) {
        // Client disconnected from the group
    }    
})

```


### Client Device
Multiple client devices can be connected to the same group. The client device can discover new groups registered in the same local network and connecting to them, to find those nearby groups you have to do the following:
```java
wroupClient = WroupClient.getInstance(getApplicationContext());
wroupClient.discoverServices(5000L, new ServiceDiscoveredListener() {
    
    @Override
    public void onNewServiceDeviceDiscovered(WroupServiceDevice serviceDevice) {
        // New service discover
    }
    
    @Override
    public void onFinishServiceDeviceDiscovered(List<WroupServiceDevice> serviceDevices) {
    // The list of services discovered in the time indicated
    }
    
    @Override
    public void onError(WiFiP2PError wiFiP2PError) {
        // An error occurred during the searching
    }
}); 
```

At the same as ```WroupService``` you can registered the listeners: ```ClientConnectedListener``` and ```ClientDisconnectedListener```.

### Sending Messages
Both ```WroupService``` and ```WroupClient``` can send messages to all the clients connected to the group. The object to send is a ```MessageWrapper``` that contains the sender device, a type and the message in String format. There are four types of messages:
* NORMAL: The normal type is which you must to use. The rest of them are message types to manage the state of group between clients and server.
* CONNECTION_MESSAGE: A connection message is sent when a new client is connected to the group.
* DISCONNECTION_MESSAGE: A disconnection message is sent when a client is disconnected from the group.
* REGISTERED_DEVICES: This message is sent by the server to the client when it has been connected to the group. The content of the message is the clients already connected to the group, with this approach the client can know in every moment the devices connected.

You can send a message as follow:
```java
MessageWrapper message = new MessageWrapper();
message.setMessage("This is a message to all clients");
message.setMessageType(MessageWrapper.MessageType.NORMAL);
   
wroupClient.sendMessageToAllClients(message);                  
```

To receive the messages you have to implement the ```DataReceivedListener``` and set to the ```WroupClient``` or ```WroupServer``` instance:
```java
@Override
public void onDataReceived(MessageWrapper messageWrapper) {
    // New message received
}
```

Messages that are not of ```NORMAL``` type are excluded from this listener.

### Cleaning the instances
To disconnect from a group (client) or delete a group (server) you must call to:
#### Server
```java
wroupService.disconnect();
```
#### Client
```java
wroupClient.disconnect();
```


## Example App
If you have see the library in action you can dowload the [Wroup-Example](https://github.com/ble180/Wroup-Example) project. It's a chat application with you can create a group (server) and other devices (clients) can be join to the group and have a conversation.

## Contributing
Feel free to submit issues, requests, or fork the project.

## License
(MIT)

```
Copyright (c) 2015 Peak Digital LLC

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWA