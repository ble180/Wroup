package com.abemart.wroup.common;


import android.net.wifi.p2p.WifiP2pDevice;


public class WroupDevice {

    private String deviceName;
    private String deviceMac;
    private String deviceServerSocketIP;
    private int deviceServerSocketPort;

    private String customName;

    public WroupDevice() {

    }

    public WroupDevice(WifiP2pDevice device) {
        this.deviceName = device.deviceName;
        this.deviceMac = device.deviceAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceMac() {
        return deviceMac;
    }

    public void setDeviceMac(String deviceMac) {
        this.deviceMac = deviceMac;
    }

    public String getDeviceServerSocketIP() {
        return deviceServerSocketIP;
    }

    public void setDeviceServerSocketIP(String deviceServerSocketIP) {
        this.deviceServerSocketIP = deviceServerSocketIP;
    }

    public int getDeviceServerSocketPort() {
        return deviceServerSocketPort;
    }

    public void setDeviceServerSocketPort(int deviceServerSocketPort) {
        this.deviceServerSocketPort = deviceServerSocketPort;
    }

    public String getCustomName() {
        return customName;
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    @Override
    public String toString() {
        return new StringBuilder().append("WroupDevice[deviceName=").append(deviceName).append("][deviceMac=").append(deviceMac).append("][deviceServerSocketIP=").append(deviceServerSocketIP).append("][deviceServerSocketPort=").append(deviceServerSocketPort).append("]").toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WroupDevice that = (WroupDevice) o;

        if (deviceName != null ? !deviceName.equals(that.deviceName) : that.deviceName != null)
            return false;
        return deviceMac != null ? deviceMac.equals(that.deviceMac) : that.deviceMac == null;
    }

    @Override
    public int hashCode() {
        int result = deviceName != null ? deviceName.hashCode() : 0;
        result = 31 * result + (deviceMac != null ? deviceMac.hashCode() : 0);
        return result;
    }

}
