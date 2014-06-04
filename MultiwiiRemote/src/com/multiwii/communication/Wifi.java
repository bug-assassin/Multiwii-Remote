package com.multiwii.communication;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

public class Wifi extends Communication {
	private Socket mySocket;
	private WifiManager wifiManager;

	public Wifi(Context context) {
		super(context);
		wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		Enable();
	}

	// TODO connectToNetwork not working
	public boolean connectToNetwork(String ssid, String key) {
		WifiConfiguration wifiConfig = new WifiConfiguration();
		wifiConfig.SSID = String.format("\"%s\"", ssid);
		wifiConfig.preSharedKey = String.format("\"%s\"", key);
		wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);

		int netId;
		WifiConfiguration currentConf = getConfiguredNetwork(wifiManager, ssid);
		if (currentConf == null)
			netId = wifiManager.addNetwork(wifiConfig);
		else
			netId = currentConf.networkId;

		if (wifiManager.getConnectionInfo().getSSID().equalsIgnoreCase(ssid))
			return true;

		wifiManager.disconnect();
		wifiManager.enableNetwork(netId, true);
		return wifiManager.reconnect();
	}

	private WifiConfiguration getConfiguredNetwork(WifiManager wifiManager,
			String ssid) {
		List<WifiConfiguration> myList = wifiManager.getConfiguredNetworks();
		for (WifiConfiguration current : myList) {
			if (current.SSID.equalsIgnoreCase(ssid))
				return current;
		}
		return null;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getConnectionState() {
		return state;
	}

	public boolean Write(byte[] data) {
		super.Write(data);
		try {
			outStream.write(data);
			Connected = true;
		} catch (IOException e) {
			e.printStackTrace();
			Connected = false;
		}
		return Connected;
	}

	public boolean flush() {
		try {
			outStream.flush();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void Enable() {
		if (!wifiManager.isWifiEnabled())
			wifiManager.setWifiEnabled(true);
	}

	@Override
	public boolean Connect(String ip, int port) {
		address = ip + ":" + port;
		setState(STATE_CONNECTING);
		try {
			mySocket = new Socket(ip, port);
			mySocket.setKeepAlive(true);
			inStream = mySocket.getInputStream();
			outStream = mySocket.getOutputStream();
			setState(STATE_CONNECTED);
			Connected = true;
		} catch (Exception e) {
			e.printStackTrace();
			setState(STATE_NONE);
			setState(e.getMessage());
			Connected = false;
		}
		return Connected;
	}

	@Override
	public boolean dataAvailable() {
		try {
			if (inStream == null)
				return false;
			return inStream.available() != 0;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public byte Read() {
		BytesReceived += 1;
		byte a = 0;
		try {
			a = (byte) inStream.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return (byte) (a);
	}

	@Override
	public void Close() {
		Connected = false;
		if (mySocket != null && mySocket.isConnected())
			try {
				mySocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void Disable() {
		if (wifiManager.isWifiEnabled())
			wifiManager.setWifiEnabled(false);
	}

	@Override
	public int getStrength() {
	      try {
	         int rssi = wifiManager.getConnectionInfo().getRssi();
	         int level = WifiManager.calculateSignalLevel(rssi, 10);
	         int percentage = (int) ((level / 10.0) * 100);
	         Log.d("aaa", "WiFI RSSI=" + String.valueOf(percentage));
	         return percentage;

	      } catch (Exception e) {
	         return 0;
	      }
	   }
	@Override
	public CommunicationMode getMode() 
	{
	return CommunicationMode.WIFI;
	}
}
