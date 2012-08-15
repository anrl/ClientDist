package com.mcgill.clientdist;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import com.mcgill.clientdist.Network.DoBeep;
import com.mcgill.clientdist.Network.Signal;

public class KryoClient {
	private Handler handler; //handler to send data to the UI
	private ClientDistActivity activity; //object to connect to the main activity
	
	private Client client;
	public String ipAddress;
	private Signal lastSignal;

	public KryoClient(String ipAddr, ClientDistActivity parent) {
    	activity = parent;
        handler = new Handler();
        
		ipAddress = ipAddr;
		client = new Client();
		Log.TRACE();
		Network.register(client);


		client.addListener(new Listener() {
			public void connected (Connection connection) {
				postConnectionStatus(Network.CONNECTED);
				
//				Log.e("Client", "Connected!");
			}

			public void received (Connection connection, Object object) {
				if (object instanceof Signal) {
					lastSignal = (Signal)object;
					postWaitMessage();
				}
				if (object instanceof DoBeep) {
					postWaitMessage();
					new Thread(new Runnable() {
						public void run() {
							final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
							tg.startTone(ToneGenerator.TONE_PROP_BEEP);
						}
					}).start();
				}
			}

			public void disconnected (Connection connection) {
				postConnectionStatus(Network.DISCONNECTED);
//				Log.e("Client", "Disconnected");
			}
		});
		
		client.start();
	}

	public void sendResponse(String androidID, boolean heard, int volume) {
		if (client == null)
			return;
		lastSignal.id = androidID;
		lastSignal.heard = heard;
		lastSignal.volume = volume;
		client.sendTCP(lastSignal);
	}

	public void close() {
		client.close();
		client.stop();
	}
	
	public void connect() throws UnknownHostException {
		InetAddress addr;
//		if (ipAddress == "")
//			addr = client.discoverHost(Network.UDPPort, 5000);
//		else
			addr = InetAddress.getByName(ipAddress);

		try {
			client.connect(2000, addr, Network.TCPPort);
		} catch (IOException e) {
//			Log.e("Error", "Server down.");
			postConnectionStatus(Network.DISCONNECTED);
		}
	}

	public void reconnect() {
		try {
			client.reconnect();
		} catch (IOException e) {
//			Log.e("Error", "Server down.");
			postConnectionStatus(Network.DISCONNECTED);
		}
	}
	
	public boolean isConnected() {
		return client.isConnected();
	}

	private void postConnectionStatus(final int data) {
		handler.post(new Runnable() {
			public void run() {
				activity.receiveConnectionStatus(data);
			}
		});
    }

	private void postWaitMessage() {
		handler.post(new Runnable() {
			public void run() {
				activity.waitForData();
			}
		});
    }
}