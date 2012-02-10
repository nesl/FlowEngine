package edu.ucla.nesl.flowengine.abstractdevice.zephyr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import edu.ucla.nesl.flowengine.SensorType;
import edu.ucla.nesl.flowengine.aidl.DeviceInterface;
import edu.ucla.nesl.flowengine.aidl.FlowEngineAPI;

public class ZephyrService extends Service implements Runnable {
	private static final String TAG = ZephyrService.class.getSimpleName();
	private static final String FlowEngineServiceName = "edu.ucla.nesl.flowengine.FlowEngine";
	private static final String BLUETOOTH_SERVICE_UUID = "00001101-0000-1000-8000-00805f9b34fb";

	private static final int MSG_STOP = 1;
	private static final int MSG_START = 2;
	private static final int MSG_KILL = 3;
	
	private FlowEngineAPI mAPI;
	private int	mDeviceID;
	private ZephyrService mThisService = this;

	private BluetoothSocket mSocket;
	private String mDeviceAddress = "00:07:80:99:9E:6C";
	private Thread mReceiveThread;
	private boolean mIsStopRequest = false;
	private OutputStream mOutputStream;
	private InputStream mInputStream;

	private boolean connect(String deviceAddress) {
		BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
		BluetoothDevice device = btAdapter.getRemoteDevice(deviceAddress);

		// Get a BluetoothSocket to connect with the given BluetoothDevice
		try {        	
			// the UUID of the bridge's service
			UUID uuid = UUID.fromString(BLUETOOTH_SERVICE_UUID);
			mSocket = device.createRfcommSocketToServiceRecord(uuid);
		} 
		catch (IOException e) { 
			Log.e(TAG, "Exception from createRfcommSocketToServiceRecord()..");
			e.printStackTrace();
			return false;
		}

		// just in case, always cancel discovery before trying to connect to a socket.  
		// discovery will slow down or prevent connections from being made
		btAdapter.cancelDiscovery();

		try {
			// Connect the device through the socket. This will block
			// until it succeeds or throws an exception
			mSocket.connect();
		} catch (IOException e) {
			Log.d(TAG, "Failed to connect to " + deviceAddress);
			e.printStackTrace();
			try {
				mSocket.close();
			} catch (IOException e1) {
				Log.e(TAG, "IOException from mSocket.close()..");
				e1.printStackTrace();
			}
			mSocket = null;
			return false;
		}
		
		//Zephyr initialization.
		Log.d(TAG, "Initializing Zephyr...");
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			Log.d(TAG, "Thread sleep interrupted.");
		}
		
		try {
			mOutputStream = mSocket.getOutputStream();
			mInputStream = mSocket.getInputStream();
		} 
		catch (IOException e)
		{
			Log.d(TAG, "IOException from getting input and output stream..");
			e.printStackTrace();
			mSocket = null;
			return false;
		}

		byte setECG[] = { 0x02, 0x16, 0x01, 0x01, 0x5e, 0x03 };
		byte setBreath[] = { 0x02, 0x15, 0x01, 0x01, 0x5e, 0x03};
		byte setAcc[] = { 0x02, 0x1e, 0x01, 0x01, 0x5e, 0x03 };
		byte setGeneral[] = { 0x02, 0x14, 0x01, 0x01, 0x5e, 0x03 };
		
		try {
			mOutputStream.write(setECG);
			mOutputStream.write(setBreath);
			mOutputStream.write(setAcc);
			mOutputStream.write(setGeneral);
		} 
		catch (IOException e)
		{
			Log.d(TAG, "IOException from OutputStream..");
			e.printStackTrace();
			mSocket = null;
			return false;
		}
		Log.d(TAG, "Init successful.");

		return true;
	}

	// source: http://rgagnon.com/javadetails/java-0596.html
	/*static final String HEXES = "0123456789ABCDEF";
	public String getHex(byte[] raw, int num) {
		if (raw == null) 
			return null;
		final StringBuilder hex = new StringBuilder(2 * num);
		for (int i=0; i<num; i++) {
			hex.append(HEXES.charAt((raw[i] & 0xF0) >> 4)).append(HEXES.charAt((raw[i] & 0x0F)));
		}
		return hex.toString();
	}*/

	private double convertADCtoG(int sample) {
		// 10bits ADC 0 ~ 1023 = -16g ~ 16g
		return (sample / 1023.0) * 32.0 - 16.0;
	}
	
	@Override
	public void run() {
		final byte STX = 2;
		long lastTime = System.currentTimeMillis();
		byte[] receivedBytes = new byte[128];
    	
		while (!mIsStopRequest) {
    		try {
    			// Receiving STX
    			do {
    				mInputStream.read(receivedBytes, 0, 1);
    			} while (receivedBytes[0] != STX);
    			
    			// Receiving Msg ID and DLC
    			mInputStream.read(receivedBytes, 1, 2);
    			int msgID = receivedBytes[1] & 0xFF;
    			
    			// Receiving payload, CRC, and ACK
    			mInputStream.read(receivedBytes, 3, receivedBytes[2]+2);

    			//int numBytes = receivedBytes[2]+5;
	    		//Log.d(TAG, "Received " + Integer.toString(numBytes) + " bytes: " + getHex(receivedBytes, receivedBytes[2]+5));
	    		
	    		// parsing receivedBytes
	    		if (msgID == 0x20) {
	    			//Log.d(TAG, "Received General Data Packet");
	    			long timestamp = 0;
	    			for (int i=8, j=0; i<12; i++, j+=8) {
	    				timestamp |= (receivedBytes[i]&0xFF) << j;
	    			}
	    			//Log.d(TAG, "timestamp: " + timestamp);
	    			//int heartRate = (receivedBytes[12]&0xFF) | ((receivedBytes[13]&0xFF)<<8);
	    			//int respirationRate = (receivedBytes[14]&0xFF) | ((receivedBytes[15]&0xFF)<<8);
	    			int skinTemp = (receivedBytes[16]&0xFF) | ((receivedBytes[17]&0xFF)<<8);
	    			try {
	    				//sample interval: 1s
						mAPI.pushIntData(mDeviceID, SensorType.SKIN_TEMPERATURE, skinTemp, 0, timestamp);
					} catch (RemoteException e) {
						e.printStackTrace();
						startFlowEngineService();
					}
	    		} else if (msgID == 0x21) {
	    			//Log.d(TAG, "Received Breathing Waveform Packet");
	    			int[] breathingData = new int[18];
	    			long timestamp = 0;
	    			for (int i=8, j=0; i<12; i++, j+=8) {
	    				timestamp |= (receivedBytes[i]&0xFF) << j;
	    			}
	    			for (int i=12, j=0; i<35; i+=5)	{
	    				breathingData[j++] = (receivedBytes[i]&0xFF) | (((receivedBytes[i+1]&0xFF) & 0x03) << 8);
	    				if (i+2 < 35)
	    					breathingData[j++] = ((receivedBytes[i+1]&0xFF)>>2) | (((receivedBytes[i+2]&0xFF)&0x0F) << 6);
	    				if (i+3 < 35)
	    					breathingData[j++] = ((receivedBytes[i+2]&0xFF)>>4) | (((receivedBytes[i+3]&0xFF)&0x3F) << 4);
	    				if (i+4 < 35)
	    					breathingData[j++] = ((receivedBytes[i+3]&0xFF)>>6) | ((receivedBytes[i+4]&0xFF) << 2);
	    			}
	    			try {
	    				// sample interval: 56ms
						mAPI.pushIntArrayData(mDeviceID, SensorType.RIP, breathingData, breathingData.length, timestamp);
					} catch (RemoteException e) {
						e.printStackTrace();
						startFlowEngineService();
					}
	    		} else if (msgID == 0x22) {
	    			//Log.d(TAG, "Received ECG Waveform Packet");
	    			long timestamp = 0;
	    			for (int i=8, j=0; i<12; i++, j+=8) {
	    				timestamp |= (receivedBytes[i]&0xFF) << j;
	    			}
	    			//Log.d(TAG, "timestamp: " + timestamp);
	    			int[] ecgData = new int[63];
	    			for (int i=12, j=0; i<91; i+=5) {
	    				ecgData[j++] = (receivedBytes[i]&0xFF) | (((receivedBytes[i+1]&0xFF) & 0x03) << 8);
	    				if (i+2 < 91)
	    					ecgData[j++] = ((receivedBytes[i+1]&0xFF)>>2) | (((receivedBytes[i+2]&0xFF)&0x0F) << 6);
	    				if (i+3 < 91)
	    					ecgData[j++] = ((receivedBytes[i+2]&0xFF)>>4) | (((receivedBytes[i+3]&0xFF)&0x3F) << 4);
	    				if (i+4 < 91)
	    					ecgData[j++] = ((receivedBytes[i+3]&0xFF)>>6) | ((receivedBytes[i+4]&0xFF) << 2);
	    			}
	    			
	    			//String dump = "";
	    			//for (int i=0; i<63; i++)
	    			//	dump += Integer.toString(ecgData[i]) + ", ";
	    			//Log.d(TAG, "ECG Data: " + dump);
	    			
	    			try {
	    				// sample iterval: 4ms
						mAPI.pushIntArrayData(mDeviceID, SensorType.ECG, ecgData, ecgData.length, timestamp);
					} catch (RemoteException e) {
						e.printStackTrace();
						startFlowEngineService();
					}
	    		} else if (msgID == 0x25) {
	    			//Log.d(TAG, "Received Accelerometer Packet");
	    			int[] accData = new int[60];
	    			long timestamp = 0;
	    			for (int i=8, j=0; i<12; i++, j+=8) {
	    				timestamp |= (receivedBytes[i]&0xFF) << j;
	    			}
	    			for (int i=12, j=0; i<87; i+=5) {
	    				accData[j++] = (receivedBytes[i]&0xFF) | (((receivedBytes[i+1]&0xFF) & 0x03) << 8);
	    				if (i+2 < 87)
	    					accData[j++] = ((receivedBytes[i+1]&0xFF)>>2) | (((receivedBytes[i+2]&0xFF)&0x0F) << 6);
	    				if (i+3 < 87)
	    					accData[j++] = ((receivedBytes[i+2]&0xFF)>>4) | (((receivedBytes[i+3]&0xFF)&0x3F) << 4);
	    				if (i+4 < 87)
	    					accData[j++] = ((receivedBytes[i+3]&0xFF)>>6) | ((receivedBytes[i+4]&0xFF) << 2);
	    			}
	    			
	    			//int[] accX = new int[20], accY = new int[20], accZ = new int[20];
	    			/*for (int i=0, j=0; i<60; i+=3) {
	    				accX[j] = accData[i];
	    				accY[j] = accData[i+1];
	    				accZ[j] = accData[i+2];
	    				j+=1;
	    			}*/
	    			
	    			//Log.d(TAG, "timestamp: " + timestamp);
	    			double[] accSample = new double[3];
	    			for (int i = 0, j = 0; i < accData.length; i += 3, j++) {
	    				accSample[0] = convertADCtoG(accData[i]);
	    				accSample[1] = convertADCtoG(accData[i+1]);
	    				accSample[2] = convertADCtoG(accData[i+2]);
	    				//Log.d(TAG, "Acc: " + accSample[0] + ", " + accSample[1] + ", " + accSample[2]);
		    			try {
		    				// sample interval: 20ms
							mAPI.pushDoubleArrayData(mDeviceID, SensorType.ACCELEROMETER, accSample, accSample.length, timestamp + (j * 20));
						} catch (RemoteException e) {
							e.printStackTrace();
							startFlowEngineService();
						}
	    			}
	    		} else if (msgID == 0x23 ) {
	    			//Log.d(TAG, "Recevied lifesign from Zephyr.");
	    		} else {
	    			Log.d(TAG, "Received something else.. msgID: 0x" + Integer.toHexString(msgID));
	    		}

	    		long currTime = System.currentTimeMillis();
	    		if (currTime - lastTime > 8000)
	    		{
	    			// Sending lifesign. (Zephyr requires this at least every 10 seconds)
	    	        if (mOutputStream != null)
	    	        {
	    		        byte lifesign[] = { 0x02, 0x23, 0x00, 0x00, 0x03 };
    		        	mOutputStream.write(lifesign);
	    		        //Log.d(TAG, "Sent Lifesign");
	    	        } else {
	    	        	throw new NullPointerException("mOutputStream is null");
	    	        }
	    	        lastTime = System.currentTimeMillis();
	    		}
    		}
    		catch(IOException e) {
    			Log.d(TAG, "IOException while run()..");
    			e.printStackTrace();
    			
				try {
					Log.d(TAG, "Closing socket");
					mSocket.close();
					Log.d(TAG, "Socket closed");
				} catch (IOException e1) {
					Log.d(TAG, "Failed to close the bt socket");			
					e1.printStackTrace();
					mSocket = null;
					break;
				}
				Log.d(TAG, "Trying to reconnect(1)..");
				int numRetries = 2;
				while (!mIsStopRequest && !connect(mDeviceAddress)) {
					Log.d(TAG, "Trying to reconnect(" + numRetries + ")..");
					numRetries += 1;
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
    		}
		} // end while
		
		if (mSocket != null) {
			try {
				Log.d(TAG, "Closing socket");
				mSocket.close();
				Log.d(TAG, "Socket closed");
			} catch (IOException e) {
				Log.d(TAG, "Failed to close the bt socket");			
				e.printStackTrace();
			}
		}
		mSocket = null;
		mIsStopRequest = false;
		Log.d(TAG, "Receive thrad stopped.");
	}

	private void stop() {
		if (mReceiveThread != null)
		{
			mIsStopRequest = true;
			Log.i(TAG, "Stop receiving requested.");
		}
	}
	
	private void start() {
		if (mReceiveThread == null) {
			mReceiveThread = new Thread(this);
		}
		
		Log.d(TAG, "Trying to connect..");
		if (mSocket == null) {
			while (!connect(mDeviceAddress)) {
				Log.d(TAG, "Retrying..");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			Log.d(TAG, "Start receiving thread..");
			mReceiveThread.start();
		} else {
			Log.d(TAG, "Already connected to Zephyr.");
		}
	}
	
	private void kill() {
		
	}
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_STOP:
				stop();
				break;
			case MSG_START:
				start();
				break;
			case MSG_KILL:
				kill();
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};
	
	private DeviceInterface.Stub mZephyrDeviceInterface = new DeviceInterface.Stub() {
		@Override
		public void start() throws RemoteException {
			mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP));
		}
		@Override
		public void stop() throws RemoteException {
			mHandler.sendMessage(mHandler.obtainMessage(MSG_START));
		}
		@Override
		public void kill() throws RemoteException {
			mHandler.sendMessage(mHandler.obtainMessage(MSG_KILL));
		}
	};
	

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(TAG, "Service connection established.");
			mAPI = FlowEngineAPI.Stub.asInterface(service);
			try {
				mDeviceID = mAPI.addDevice(mZephyrDeviceInterface);
				mAPI.addSensor(mDeviceID, SensorType.ACCELEROMETER, 20);
				mAPI.addSensor(mDeviceID, SensorType.ECG, 4);
				mAPI.addSensor(mDeviceID, SensorType.RIP, 56);
				mAPI.addSensor(mDeviceID, SensorType.SKIN_TEMPERATURE, 1000);
				mThisService.start();
			} catch (RemoteException e) {
				Log.e(TAG, "Failed to add AbstractDevice..", e);
				startFlowEngineService();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i(TAG, "Service connection closed.");
		}
	};
	

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	private void startFlowEngineService() {
        // Start FlowEngine service if it's not running.
		Log.d(TAG, "Starting FlowEngineService..");
		Intent intent = new Intent(FlowEngineServiceName);

		int numRetries = 1;
		while (startService(intent) == null) {
			Log.d(TAG, "Retrying to start FlowEngineService.. (" + numRetries + ")");
			numRetries++;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		// Bind to the FlowEngine service.
		Log.d(TAG, "Binding to FlowEngineService..");
		numRetries = 1;
		while (!bindService(intent, mServiceConnection, 0)) {
			Log.d(TAG, "Retrying to bind to FlowEngineService.. (" + numRetries + ")");
			numRetries++;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "Service creating");
		startFlowEngineService();
	}
	
	@Override
	public void onDestroy() {
		Log.i(TAG, "Service destroying");
		
		stop();
		
		try {
			mAPI.removeDevice(mDeviceID);
			unbindService(mServiceConnection);
		} catch (Throwable t) {
			Log.w(TAG, "Failed to unbind from the service", t);
		}

		super.onDestroy();
	}
}
