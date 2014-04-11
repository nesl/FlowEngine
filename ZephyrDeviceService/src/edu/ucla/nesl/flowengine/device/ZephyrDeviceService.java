package edu.ucla.nesl.flowengine.device;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.util.Log;
import edu.ucla.nesl.flowengine.SensorType;
import edu.ucla.nesl.flowengine.aidl.DeviceAPI;
import edu.ucla.nesl.flowengine.aidl.FlowEngineAPI;
import edu.ucla.nesl.flowengine.device.zephyr.R;
import edu.ucla.nesl.util.NotificationHelper;

public class ZephyrDeviceService extends Service {
	private static final String TAG = ZephyrDeviceService.class.getSimpleName();

	private static final int RETRY_INTERVAL = 5000; // ms
	private static final int LIFE_SIGN_SEND_INTERVAL = 8000; //ms
	private static final int VALID_WORN_DURATION = 3000; // ms
	private static final int VALID_UNWORN_DURATION = 3000; // ms
	private static final int ECG_PACKET_TIMEOUT = 500; // ms (ECG packet period = 252 ms)
	private static final int RIP_PACKET_TIMEOUT = 1500; // ms (RIP packet period = 1.008 sec);

	private static final String NOTI_TITLE = "Zephyr Device Service";
	private static final String FLOW_ENGINE_SERVICE_NAME = "edu.ucla.nesl.flowengine.FlowEngine";
	private static final String BLUETOOTH_SERVICE_UUID = "00001101-0000-1000-8000-00805f9b34fb";

	private static final String PROPERTY_FILE_NAME = "flowengine.properties";
	private static final String PROP_NAME_ZEPHYR_ADDR = "zephyr_bluetooth_addr";

	private static final int MSG_STOP = 1;
	private static final int MSG_START = 2;
	private static final int MSG_KILL = 3;
	private static final int MSG_START_SENSOR = 4;
	private static final int MSG_STOP_SENSOR = 5;
	private static final int MSG_READ_PROPERTY_FILE = 6;
	private static final int MSG_TRY_BINDING_FLOWENGINE = 7;

	private static final byte START_ECG_PACKET[] = { 0x02, 0x16, 0x01, 0x01, 0x5e, 0x03 };
	private static final byte START_RIP_PACKET[] = { 0x02, 0x15, 0x01, 0x01, 0x5e, 0x03};
	private static final byte START_ACCELEROMETER_PACKET[] = { 0x02, 0x1e, 0x01, 0x01, 0x5e, 0x03 };
	private static final byte START_GENERAL_PACKET[] = { 0x02, 0x14, 0x01, 0x01, 0x5e, 0x03 };

	private static final byte STOP_ECG_PACKET[] = { 0x02, 0x16, 0x01, 0x00, 0x00, 0x03 };
	private static final byte STOP_RIP_PACKET[] = { 0x02, 0x15, 0x01, 0x00, 0x00, 0x03};
	private static final byte STOP_ACCELEROMETER_PACKET[] = { 0x02, 0x1e, 0x01, 0x00, 0x00, 0x03 };
	private static final byte STOP_GENERAL_PACKET[] = { 0x02, 0x14, 0x01, 0x00, 0x00, 0x03 };

	private static final String BUNDLE_SENSOR_ID = "sensor_id";

	//private byte SET_RTC_PACKET[];

	private String bluetoothAddr = null; 

	private FlowEngineAPI mAPI;
	private int	mDeviceID;
	private ZephyrDeviceService mThisService = this;

	private BluetoothSocket mSocket;
	private Thread mReceiveThread;
	private boolean mIsStopRequest = false;
	private OutputStream mOutputStream;
	private InputStream mInputStream;

	private PowerManager.WakeLock mWakeLock; 

	private NotificationHelper mNotification;

	private int mFakeRIPData[][] = {
			{450, 489, 528, 566, 603, 640, 675, 708, 739, 768, 794, 818, 839, 857, 872, 884, 893, 898}, {900, 898, 893, 884, 872, 857, 839, 818, 794, 768, 739, 708, 675, 640, 603, 566, 528, 489}, {450, 410, 371, 333, 296, 259, 225, 191, 160, 131, 105, 81, 60, 42, 27, 15, 6, 1}, {0, 1, 6, 15, 27, 42, 60, 81, 105, 131, 160, 191, 224, 259, 296, 333, 371, 410}, {449, 489, 528, 566, 603, 640, 675, 708, 739, 768, 794, 818, 839, 857, 872, 884, 893, 898}, {900, 898, 893, 884, 872, 857, 839, 818, 794, 768, 739, 708, 674, 640, 603, 566, 528, 489}, {450, 410, 371, 333, 296, 259, 225, 191, 160, 131, 105, 81, 60, 42, 27, 15, 6, 1}, {0, 1, 6, 15, 27, 42, 60, 81, 105, 131, 160, 191, 225, 259, 296, 333, 371, 410}, {449, 489, 528, 566, 603, 640, 674, 708, 739, 768, 794, 818, 839, 857, 872, 884, 893, 898}, {900, 898, 893, 884, 872, 857, 839, 818, 794, 768, 739, 708, 675, 640, 603, 566, 528, 489}, {450, 410, 371, 333, 296, 259, 225, 191, 160, 131, 105, 81, 60, 42, 27, 15, 6, 1}, {0, 1, 6, 15, 27, 42, 60, 81, 105, 131, 160, 191, 224, 259, 296, 333, 371, 410}, {449, 489, 528, 566, 603, 640, 674, 708, 739, 768, 794, 818, 839, 857, 872, 884, 893, 898}, {900, 898, 893, 884, 872, 857, 839, 818, 794, 768, 739, 708, 675, 640, 603, 566, 528, 489}, {450, 410, 371, 333, 296, 259, 225, 191, 160, 131, 105, 81, 60, 42, 27, 15, 6, 1}, {0, 1, 6, 15, 27, 42, 60, 81, 105, 131, 160, 191, 224, 259, 296, 333, 371, 410}, {449, 489, 528, 566, 603, 640, 674, 708, 739, 768, 794, 818, 839, 857, 872, 884, 893, 898}, {900, 898, 893, 884, 872, 857, 839, 818, 794, 768, 739, 708, 675, 640, 603, 566, 528, 489}, {450, 410, 371, 333, 296, 259, 225, 191, 160, 131, 105, 81, 60, 42, 27, 15, 6, 1}, {0, 1, 6, 15, 27, 42, 60, 81, 105, 131, 160, 191, 224, 259, 296, 333, 371, 410}, {449, 489, 528, 566, 603, 640, 674, 708, 739, 768, 794, 818, 839, 857, 872, 884, 893, 898}, {900, 898, 893, 884, 872, 857, 839, 818, 794, 768, 739, 708, 675, 640, 603, 566, 528, 489}, {450, 410, 371, 333, 296, 259, 225, 191, 160, 131, 105, 81, 60, 42, 27, 15, 6, 1}, {0, 1, 6, 15, 27, 42, 60, 81, 105, 131, 160, 191, 224, 259, 296, 333, 371, 410}, {449, 489, 528, 566, 603, 640, 674, 708, 739, 768, 794, 818, 839, 857, 872, 884, 893, 898}, {900, 898, 893, 884, 872, 857, 839, 818, 794, 768, 739, 708, 675, 640, 603, 566, 528, 489}, {450, 410, 371, 333, 296, 259, 225, 191, 160, 131, 105, 81, 60, 42, 27, 15, 6, 1}, {0, 1, 6, 15, 27, 42, 60, 81, 105, 131, 160, 191, 224, 259, 296, 333, 371, 410}, {449, 489, 528, 566, 603, 640, 674, 708, 739, 768, 794, 818, 839, 857, 872, 884, 893, 898}, {900, 898, 893, 884, 872, 857, 839, 818, 794, 768, 739, 708, 675, 640, 603, 566, 528, 489}, {450, 410, 371, 333, 296, 259, 225, 191, 160, 131, 105, 81, 60, 42, 27, 15, 6, 1}, {0, 1, 6, 15, 27, 42, 60, 81, 105, 131, 160, 191, 224, 259, 296, 333, 371, 410}, {449, 489, 528, 566, 603, 640, 674, 708, 739, 768, 794, 818, 839, 857, 872, 884, 893, 898}, {900, 898, 893, 884, 872, 857, 839, 818, 794, 768, 739, 708, 675, 640, 603, 566, 528, 489}, {450, 410, 371, 333, 296, 259, 225, 191, 160, 131, 105, 81, 60, 42, 27, 15, 6, 1}, {0, 1, 6, 15, 27, 42, 60, 81, 105, 131, 160, 191, 224, 259, 296, 333, 371, 410}, {449, 489, 528, 566, 603, 640, 674, 708, 739, 768, 794, 818, 839, 857, 872, 884, 893, 898}, {900, 898, 893, 884, 872, 857, 839, 818, 794, 768, 739, 708, 675, 640, 603, 566, 528, 489}, {450, 410, 371, 333, 296, 259, 225, 191, 160, 131, 105, 81, 60, 42, 27, 15, 6, 1}, {0, 1, 6, 15, 27, 42, 60, 81, 105, 131, 160, 191, 224, 259, 296, 333, 371, 410}, {449, 489, 528, 566, 603, 640, 674, 708, 739, 768, 794, 818, 839, 857, 872, 884, 893, 898}, {900, 898, 893, 884, 872, 857, 839, 818, 794, 768, 739, 708, 675, 640, 603, 566, 528, 489}, {449, 410, 371, 333, 296, 259, 225, 191, 160, 131, 105, 81, 60, 42, 27, 15, 6, 1}, {0, 1, 6, 15, 27, 42, 60, 81, 105, 131, 160, 191, 225, 259, 296, 333, 371, 410}, {449, 489, 528, 566, 603, 640, 674, 708, 739, 768, 794, 818, 839, 857, 872, 884, 893, 898}, {900, 898, 893, 884, 872, 857, 839, 818, 794, 768, 739, 708, 675, 640, 603, 566, 528, 489}, {450, 410, 371, 333, 296, 259, 225, 191, 160, 131, 105, 81, 60, 42, 27, 15, 6, 1}, {0, 1, 6, 15, 27, 42, 60, 81, 105, 131, 160, 191, 224, 259, 296, 333, 371, 410}, {449, 489, 528, 566, 603, 640, 675, 708, 739, 768, 794, 818, 839, 857, 872, 884, 893, 898}, {900, 898, 893, 884, 872, 857, 839, 818, 794, 768, 739, 708, 675, 640, 603, 566, 528, 489}, {449, 410, 371, 333, 296, 259, 225, 191, 160, 131, 105, 81, 60, 42, 27, 15, 6, 1}, {0, 1, 6, 15, 27, 42, 60, 81, 105, 131, 160, 191, 225, 259, 296, 333, 371, 410}, {449, 489, 528, 566, 603, 640, 674, 708, 739, 768, 794, 818, 839, 857, 872, 884, 893, 898}, {900, 898, 893, 884, 872, 857, 839, 818, 794, 768, 739, 708, 675, 640, 603, 566, 528, 489}, {450, 410, 371, 333, 296, 259, 225, 191, 160, 131, 105, 81, 60, 42, 27, 15, 6, 1}, {0, 1, 6, 15, 27, 42, 60, 81, 105, 131, 160, 191, 224, 259, 296, 333, 371, 410}, {449, 489, 528, 566, 603, 640, 674, 708, 739, 768, 794, 818, 839, 857, 872, 884, 893, 898}, {900, 898, 893, 884, 872, 857, 839, 818, 794, 768, 739, 708, 675, 640, 603, 566, 528, 489}, {450, 410, 371, 333, 296, 259, 225, 191, 160, 131, 105, 81, 60, 42, 27, 15, 6, 1}, {0, 1, 6, 15, 27, 42, 60, 81, 105, 131, 160, 191, 225, 259, 296, 333, 371, 410}			
	};
	private int mFakeRIPDataIndex = 0;

	private boolean mIsSkinTemp = false;
	private boolean mIsBattery = false;
	private boolean mIsButtonWorn = false;
	private boolean mIsECG = false;
	private boolean mIsRIP = false;
	private boolean mIsAccelerometer = false;

	private long lastWornTime = -1;
	private long lastUnwornTime = -1;
	private long lastECGRecvTime = -1;
	private long lastRIPRecvTime = -1;

	int[] breathingData = new int[18];
	int[] ecgData = new int[63];
	int[] accData = new int[60];
	
	@Override
	public void onCreate() {
		mNotification = new NotificationHelper(this, TAG, this.getClass().getName(), R.drawable.ic_launcher);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
		mWakeLock.setReferenceCounted(false);

		super.onCreate();
	}

	private void unflagAllSensor() {
		mIsSkinTemp = false;
		mIsBattery = false;
		mIsButtonWorn = false;
		mIsECG = false;
		mIsRIP = false;
		mIsAccelerometer = false;
	}

	private void flagAllSensor() {
		mIsSkinTemp = true;
		mIsBattery = true;
		mIsButtonWorn = true;
		mIsECG = true;
		mIsRIP = true;
		mIsAccelerometer = true;
	}

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

		sendStopAllSensorsPacket();
		
		sendSetRTCPacket();
		//sendGetRTCPacket();

		if (mIsAccelerometer) {
			sendStartAccPacket();
		}
		if (mIsECG || mIsRIP || mIsSkinTemp || mIsBattery || mIsButtonWorn) {
			sendStartGeneralPacket();
		}

		return true;
	}

	// source: http://rgagnon.com/javadetails/java-0596.html
	static final String HEXES = "0123456789ABCDEF";
	public String getHex(byte[] raw, int num) {
		if (raw == null) 
			return null;
		final StringBuilder hex = new StringBuilder(2 * num);
		for (int i=0; i<num; i++) {
			hex.append(HEXES.charAt((raw[i] & 0xF0) >> 4)).append(HEXES.charAt((raw[i] & 0x0F)));
		}
		return hex.toString();
	}

	private String dumpMsg(byte[] msg) {
		String str = "";
		for (byte b : msg) {
			str += String.format("0x%02x ", b);
		}
		return str;
	}

	private void putCRC(byte[] msg) {
		int payloadLength = msg.length - 5;
		byte[] payload = new byte[payloadLength];
		System.arraycopy(msg, 3, payload, 0, payloadLength);
		msg[msg.length - 2] = crc8PushBlock(payload);
	}

	private byte crc8PushBlock(byte[] payload) {
		byte crc = 0;
		for (int i = 0; i < payload.length; i++) {
			crc = crc8PushByte(crc, payload[i]);
		}
		return crc;
	}

	private byte crc8PushByte(byte crc, byte b) {
		crc = (byte) (crc ^ b);
		for (int i = 0; i < 8; i++) {
			if ((crc & 1) == 1) {
				crc = (byte) ((crc >> 1) ^ 0x8C);
			} else {
				crc = (byte) (crc >> 1);
			}
		}
		return crc;
	}

	private double convertADCtoG(int sample) {
		// 10bits ADC 0 ~ 1023 = -16g ~ 16g
		return (sample / 1023.0) * 32.0 - 16.0;
	}

	private Runnable receiveRunnable = new Runnable() {

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

					try {
						// Receiving payload, CRC, and ACK
						mInputStream.read(receivedBytes, 3, receivedBytes[2]+2);
					} catch (ArrayIndexOutOfBoundsException e) {
						e.printStackTrace();
						continue;
					}
					
					int year = (receivedBytes[4] & 0xFF) | ((receivedBytes[5] & 0xFF) << 8);
					int month = receivedBytes[6];
					int day = receivedBytes[7];

					// TODO: add CRC check?
					
          //int numBytes = receivedBytes[2]+5;
					//Log.d(TAG, "Received " + Integer.toString(numBytes) + " bytes: " + getHex(receivedBytes, receivedBytes[2]+5));

					if (msgID == 0x20) {
						//sample interval: 1s
						//Log.d(TAG, "Received General Data Packet");
						long timestamp = parseTimestamp(receivedBytes);
						//int heartRate = (receivedBytes[12]&0xFF) | ((receivedBytes[13]&0xFF)<<8);
						//int respirationRate = (receivedBytes[14]&0xFF) | ((receivedBytes[15]&0xFF)<<8);
						int skinTemp = (receivedBytes[16]&0xFF) | ((receivedBytes[17]&0xFF)<<8);
						int battery = receivedBytes[54] & 0x7F;
						int buttonWorn = receivedBytes[55] & 0xF0; 

						boolean isWorn = (buttonWorn & 0x80) > 0;
						//boolean isHRSignalLow =(buttonWorn & 0x20) > 0; 
						//handleChestbandStatus(isWorn);
						handleChestbandStatus(true);

						if (mIsSkinTemp) {
							mAPI.pushInt(mDeviceID, SensorType.SKIN_TEMPERATURE, skinTemp, timestamp);
						}
						if (mIsBattery) {
							mAPI.pushInt(mDeviceID, SensorType.ZEPHYR_BATTERY, battery, timestamp);
						}
						if (mIsButtonWorn) {
							mAPI.pushInt(mDeviceID, SensorType.ZEPHYR_BUTTON_WORN, buttonWorn, timestamp);
						}
					} else if (msgID == 0x21) {
						//Log.d(TAG, "Received Breathing Waveform Packet");
						long timestamp = parseTimestamp(receivedBytes);
						lastRIPRecvTime = timestamp;
						for (int i=12, j=0; i<35; i+=5)	{
							breathingData[j++] = (receivedBytes[i]&0xFF) | (((receivedBytes[i+1]&0xFF) & 0x03) << 8);
							if (i+2 < 35)
								breathingData[j++] = ((receivedBytes[i+1]&0xFF)>>2) | (((receivedBytes[i+2]&0xFF)&0x0F) << 6);
							if (i+3 < 35)
								breathingData[j++] = ((receivedBytes[i+2]&0xFF)>>4) | (((receivedBytes[i+3]&0xFF)&0x3F) << 4);
							if (i+4 < 35)
								breathingData[j++] = ((receivedBytes[i+3]&0xFF)>>6) | ((receivedBytes[i+4]&0xFF) << 2);
						}

						//breathingData = mFakeRIPData[mFakeRIPDataIndex];

						// sample interval: 56ms
						if (mIsRIP) {
							mAPI.pushIntArray(mDeviceID, SensorType.RIP, breathingData, breathingData.length, timestamp);
						}

						//mFakeRIPDataIndex++;
						//if (mFakeRIPDataIndex >= mFakeRIPData.length) {
						//	mFakeRIPDataIndex = 0;
						//}

					} else if (msgID == 0x22) {
						//Log.d(TAG, "Received ECG Waveform Packet");
						long timestamp = parseTimestamp(receivedBytes);
						lastECGRecvTime = timestamp;
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

						// sample iterval: 4ms
						if (mIsECG) {
							mAPI.pushIntArray(mDeviceID, SensorType.ECG, ecgData, ecgData.length, timestamp);
						}
					} else if (msgID == 0x25) {
						//Log.d(TAG, "Received Accelerometer Packet");
						long timestamp = parseTimestamp(receivedBytes);
						for (int i=12, j=0; i<87; i+=5) {
							accData[j++] = (receivedBytes[i]&0xFF) | (((receivedBytes[i+1]&0xFF) & 0x03) << 8);
							if (i+2 < 87)
								accData[j++] = ((receivedBytes[i+1]&0xFF)>>2) | (((receivedBytes[i+2]&0xFF)&0x0F) << 6);
							if (i+3 < 87)
								accData[j++] = ((receivedBytes[i+2]&0xFF)>>4) | (((receivedBytes[i+3]&0xFF)&0x3F) << 4);
							if (i+4 < 87)
								accData[j++] = ((receivedBytes[i+3]&0xFF)>>6) | ((receivedBytes[i+4]&0xFF) << 2);
						}

						double[] accSample = new double[3];
						for (int i = 0, j = 0; i < accData.length; i += 3, j++) {
							accSample[0] = convertADCtoG(accData[i]);
							accSample[1] = convertADCtoG(accData[i+1]);
							accSample[2] = convertADCtoG(accData[i+2]);

							// sample interval: 20ms
							if (mIsAccelerometer) {
								mAPI.pushDoubleArray(mDeviceID, SensorType.CHEST_ACCELEROMETER, accSample, accSample.length, timestamp + (j * 20));
							}
						}
					} else if (msgID == 0x23 ) {
						//Log.d(TAG, "Recevied lifesign from Zephyr.");
					} else {
						//Log.d(TAG, "Received something else.. msgID: 0x" + Integer.toHexString(msgID));
					}

					long currTime = System.currentTimeMillis();
					if (currTime - lastTime > LIFE_SIGN_SEND_INTERVAL)
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
				} catch (IOException e) {
					e.printStackTrace();
					try {
						mSocket.close();
					} catch (IOException e1) {}
					mSocket = null;
					Log.d(TAG, "Trying to reconnect(1)..");
					int numRetries = 2;
					while (!mIsStopRequest && !connect(bluetoothAddr)) {
						Log.d(TAG, "Trying to reconnect(" + numRetries + ")..");
						numRetries += 1;
						try {
							Thread.sleep(RETRY_INTERVAL);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			} // end while

			if (mSocket != null) {
				try {
					mSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			mSocket = null;
			mInputStream = null;
			mOutputStream = null;
			mIsStopRequest = false;
			mReceiveThread = null;
			mNotification.showNotificationNow("Receiving stopped.");
			releaseWakeLock();
		}
	};

	private long parseTimestamp(byte[] receivedBytes) {
		
		int year = (receivedBytes[4] & 0xFF) | ((receivedBytes[5] & 0xFF) << 8);
		int month = receivedBytes[6];
		int day = receivedBytes[7];
		long millisDay = 0;
		for (int i=8, j=0; i<12; i++, j+=8) {
			millisDay |= (receivedBytes[i] & 0xFF) << j;
		}
		long timestamp = new Date(year - 1900, month - 1, day).getTime() + millisDay;
		
		return timestamp;
	}

	private void handleChestbandStatus(boolean isGood) {
		//Log.d(TAG, "isGood: " + isGood);
		long curTime = System.currentTimeMillis();
		if (isGood) {
			lastUnwornTime = -1;
			if (lastWornTime < 0) {
				lastWornTime = curTime;
			} else {
				if (curTime - lastWornTime >= VALID_WORN_DURATION) {
					startWornRelatedSensors();
				}
			}
		} else {
			lastWornTime = -1;
			if (lastUnwornTime < 0) {
				lastUnwornTime = curTime;
			} else {
				if (curTime - lastUnwornTime >= VALID_UNWORN_DURATION) {
					stopWornRelatedSensors();
				}
			}
		}
	}

	private void startWornRelatedSensors() {
		long curTime = System.currentTimeMillis();
		if (mIsECG) {
			if (curTime - lastECGRecvTime > ECG_PACKET_TIMEOUT) {
				sendStartECGPacket();
			}
		}
		if (mIsRIP) {
			if (curTime - lastRIPRecvTime > RIP_PACKET_TIMEOUT) {
				sendStartRIPPacket();
			}
		}
	}

	private void stopWornRelatedSensors() {
		long curTime = System.currentTimeMillis();
		if (mIsECG) {
			if (curTime - lastECGRecvTime < ECG_PACKET_TIMEOUT) {
				sendStopECGPacket();
			}
		}
		if (mIsRIP) {
			if (curTime - lastRIPRecvTime < RIP_PACKET_TIMEOUT) {
				sendStopRIPPacket();
			}
		}
	}

	private void stopReceiveThread() {
		if (mReceiveThread != null && !mIsStopRequest)
		{
			mIsStopRequest = true;
			mNotification.showNotificationNow(NOTI_TITLE, "Stop receiving..");
		}
	}

	private boolean tryToConnect() {
		acquireWakeLock();

		if (bluetoothAddr == null) {
			mNotification.showNotificationNow(NOTI_TITLE, "Please setup Bluetooth Address.");
			return false;
		}

		if (mReceiveThread == null) {
			mReceiveThread = new Thread(receiveRunnable);
		}

		if (mSocket == null) {
			mNotification.showNotificationNow(NOTI_TITLE, "Connecting to Zephyr..");
			Log.d(TAG, "Connecting..");
			while (!connect(bluetoothAddr)) {
				Log.d(TAG, "Reconnecting..");
				mNotification.showNotificationNow(NOTI_TITLE, "Retrying to connect to Zephyr...");
				try {
					Thread.sleep(RETRY_INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			mNotification.showNotificationNow(NOTI_TITLE, "Connected to Zephyr!");
			mReceiveThread.start();
		}
		return true;
	}

	private void sendStartAllSensorsPacket() {
		if (mOutputStream != null) {
			try {
				mOutputStream.write(START_ECG_PACKET);
				mOutputStream.write(START_RIP_PACKET);
				mOutputStream.write(START_ACCELEROMETER_PACKET);
				mOutputStream.write(START_GENERAL_PACKET);
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void sendStartECGPacket() {
		//Log.d(TAG, "sendStartECGPacket()");
		if (mOutputStream != null) {
			try {
				mOutputStream.write(START_ECG_PACKET);
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void sendStartRIPPacket() {
		//Log.d(TAG, "sendStartRIPPacket()");
		if (mOutputStream != null) {
			try {
				mOutputStream.write(START_RIP_PACKET);
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void sendStartAccPacket() {
		if (mOutputStream != null) {
			try {
				mOutputStream.write(START_ACCELEROMETER_PACKET);
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void sendStartGeneralPacket() {
		if (mOutputStream != null) {
			try {
				mOutputStream.write(START_GENERAL_PACKET);
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void sendStopAllSensorsPacket() {
		if (mOutputStream != null) {
			try {
				mOutputStream.write(STOP_ECG_PACKET);
				mOutputStream.write(STOP_RIP_PACKET);
				mOutputStream.write(STOP_ACCELEROMETER_PACKET);
				mOutputStream.write(STOP_GENERAL_PACKET);
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void sendStopECGPacket() {
		//Log.d(TAG, "sendStopECGPacket()");
		if (mOutputStream != null) {
			try {
				mOutputStream.write(STOP_ECG_PACKET);
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void sendStopRIPPacket() {
		//Log.d(TAG, "sendStopRIPPacket()");
		if (mOutputStream != null) {
			try {
				mOutputStream.write(STOP_RIP_PACKET);
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void sendStopAccPacket() {
		if (mOutputStream != null) {
			try {
				mOutputStream.write(STOP_ACCELEROMETER_PACKET);
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void sendStopGeneralPacket() {
		if (mOutputStream != null) {
			try {
				mOutputStream.write(STOP_GENERAL_PACKET);
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_READ_PROPERTY_FILE:
				handleReadPropertyFile();
				break;
			case MSG_TRY_BINDING_FLOWENGINE:
				handleTryBindingFlowEngine();
				break;
			case MSG_STOP:
				unflagAllSensor();
				handleStopAll();
				break;
			case MSG_START:
				flagAllSensor();
				handleStartAll();
				break;
			case MSG_KILL:
				unflagAllSensor();
				handleStopAll();
				mThisService.stopSelf();
				break;
			case MSG_START_SENSOR:
				handleStartSensor(((Bundle)msg.obj).getInt(BUNDLE_SENSOR_ID));
				break;
			case MSG_STOP_SENSOR:
				handleStopSensor(((Bundle)msg.obj).getInt(BUNDLE_SENSOR_ID));
				break;
			default:
				super.handleMessage(msg);
			}
		}
	};

	private void handleTryBindingFlowEngine() {
		if (mAPI == null) {
			tryBindToFlowEngineService();
		}
	}

	private void handleReadPropertyFile() {
		while (bluetoothAddr == null) {
			Log.d(TAG, "Trying to read properies..");
			readPropertyFile();
			if (bluetoothAddr == null) {
				try {
					Thread.sleep(RETRY_INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} 
		mHandler.sendMessage(mHandler.obtainMessage(MSG_TRY_BINDING_FLOWENGINE));
	}

	private void handleStopAll() {
		sendStopAllSensorsPacket();
		stopReceiveThread();
	}

	private void handleStopSensor(int sensorId) {
		switch (sensorId) {
		case SensorType.ECG:
			mIsECG = false;
			sendStopECGPacket();
			break;
		case SensorType.RIP:
			mIsRIP = false;
			sendStopRIPPacket();
			break;
		case SensorType.CHEST_ACCELEROMETER:
			mIsAccelerometer = false;
			sendStopAccPacket();
			break;
		case SensorType.SKIN_TEMPERATURE:
			mIsSkinTemp = false;
			break;
		case SensorType.ZEPHYR_BATTERY:
			mIsBattery = false;
			break;
		case SensorType.ZEPHYR_BUTTON_WORN:
			mIsButtonWorn = false;
			break;
		}
		if (isAllSensorExceptAccUnflagged()) {
			sendStopGeneralPacket();
		}
		if (isAllSensorUnflagged()) {
			stopReceiveThread();
		}
	}

	private boolean isAllSensorUnflagged() {
		return !mIsECG && !mIsRIP && !mIsAccelerometer && !mIsSkinTemp && !mIsBattery && !mIsButtonWorn;
	}

	private boolean isAllSensorExceptAccUnflagged() {
		return !mIsECG && !mIsRIP && !mIsSkinTemp && !mIsBattery && !mIsButtonWorn;
	}

	private void handleStartAll() {
		if (tryToConnect()) {
			sendStartGeneralPacket();
			//sendStartAllSensorsPacket();
		}
	}

	private void handleStartSensor(int sensorId) {
		if (tryToConnect()) {
			switch (sensorId) {
			case SensorType.ECG:
				mIsECG = true;
				sendStartGeneralPacket();
				//sendStartECGPacket();
				break;
			case SensorType.RIP:
				mIsRIP = true;
				sendStartGeneralPacket();
				//sendStartRIPPacket();
				break;
			case SensorType.CHEST_ACCELEROMETER:
				mIsAccelerometer = true;
				sendStartAccPacket();
				break;
			case SensorType.SKIN_TEMPERATURE:
				mIsSkinTemp = true;
				break;
			case SensorType.ZEPHYR_BATTERY:
				mIsBattery = true;
				break;
			case SensorType.ZEPHYR_BUTTON_WORN:
				mIsButtonWorn = true;
				break;
			}
			if (mIsSkinTemp || mIsBattery || mIsButtonWorn) {
				sendStartGeneralPacket();
			}
		}
	}

	private DeviceAPI.Stub mZephyrDeviceInterface = new DeviceAPI.Stub() {
		@Override
		public void start() throws RemoteException {
			mHandler.sendMessage(mHandler.obtainMessage(MSG_START));
		}
		@Override
		public void stop() throws RemoteException {
			mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP));
		}
		@Override
		public void kill() throws RemoteException {
			mHandler.sendMessage(mHandler.obtainMessage(MSG_KILL));
		}
		@Override
		public void startSensor(int sensor) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_SENSOR_ID, sensor);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_START_SENSOR, bundle));
		}
		@Override
		public void stopSensor(int sensor) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_SENSOR_ID, sensor);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_STOP_SENSOR, bundle));
		}
	};

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(TAG, "Service connection established.");
			mAPI = FlowEngineAPI.Stub.asInterface(service);
			try {
				mDeviceID = mAPI.addDevice(mZephyrDeviceInterface);
				mAPI.addSensor(mDeviceID, SensorType.CHEST_ACCELEROMETER, 20);
				mAPI.addSensor(mDeviceID, SensorType.ECG, 4);
				mAPI.addSensor(mDeviceID, SensorType.RIP, 56);
				mAPI.addSensor(mDeviceID, SensorType.SKIN_TEMPERATURE, 1000);
				mAPI.addSensor(mDeviceID, SensorType.ZEPHYR_BATTERY, -1);
				mAPI.addSensor(mDeviceID, SensorType.ZEPHYR_BUTTON_WORN, -1);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i(TAG, "Service connection closed.");
			stopReceiveThread();
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			mAPI = null;
			mHandler.sendMessage(mHandler.obtainMessage(MSG_TRY_BINDING_FLOWENGINE));
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void tryBindToFlowEngineService() {
		Intent intent = new Intent(FLOW_ENGINE_SERVICE_NAME);

		int numRetries = 1;
		while (startService(intent) == null) {
			Log.d(TAG, "Retrying to start FlowEngineService.. (" + numRetries + ")");
			numRetries++;
			try {
				Thread.sleep(RETRY_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		// Bind to the FlowEngine service.
		numRetries = 1;
		while (!bindService(intent, mServiceConnection, BIND_AUTO_CREATE)) {
			Log.d(TAG, "Retrying to bind to FlowEngineService.. (" + numRetries + ")");
			numRetries++;
			try {
				Thread.sleep(RETRY_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		CharSequence text = getText(R.string.foreground_service_started);
		Notification notification = new Notification(R.drawable.ic_launcher, text, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getBroadcast(this, 0, new Intent(), 0);
		notification.setLatestEventInfo(this, text, text, contentIntent);
		startForeground(R.string.foreground_service_started, notification);
		
		if (bluetoothAddr == null) {
			mHandler.sendMessage(mHandler.obtainMessage(MSG_READ_PROPERTY_FILE));
		} else {
			if (mAPI == null) {
				mHandler.sendMessage(mHandler.obtainMessage(MSG_TRY_BINDING_FLOWENGINE));
			}
		}
		
		return START_STICKY;
	}

	private void readPropertyFile() {
		String path = Environment.getExternalStorageDirectory().getPath() + "/" + PROPERTY_FILE_NAME;
		File file = new File(path);
		if (file.exists()) {
			try {
				FileInputStream fin = new FileInputStream(file);
				Properties prop = new Properties();
				prop.load(fin);
				bluetoothAddr = (String)prop.get(PROP_NAME_ZEPHYR_ADDR);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onDestroy() {
		stopReceiveThread();

		try {
			if (mAPI != null) {
				mAPI.removeDevice(mDeviceID);
				unbindService(mServiceConnection);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		stopForeground(true);
		
		super.onDestroy();
	}

	private void acquireWakeLock() {
		if (!mWakeLock.isHeld()) {
			mWakeLock.acquire();
		}
	}

	private void releaseWakeLock() {
		if (mWakeLock.isHeld()) {
			mWakeLock.release();
		}
	}
	
	private void sendSetRTCPacket() {
		if (mOutputStream != null) {
			byte[] msg = new byte[] { 0x02, 0x07, 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03 };

			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
			String curDateTime = sdf.format(new Date(Calendar.getInstance().getTimeInMillis()));
			
			Log.d(TAG, "curDateTime: " + curDateTime);
			
			String split[] = curDateTime.split("-");
			int year = Integer.parseInt(split[0]);
			int month = Integer.parseInt(split[1]);
			int day = Integer.parseInt(split[2]);
			int hour= Integer.parseInt(split[3]);
			int minute = Integer.parseInt(split[4]);
			int sec = Integer.parseInt(split[5]);

			msg[3] = (byte)day;
			msg[4] = (byte)month;
			msg[5] = (byte)(year & 0xFF);
			msg[6] = (byte)((year>>8) & 0xFF);
			msg[7] = (byte)hour;
			msg[8] = (byte)minute;
			msg[9] = (byte)sec;
			
			putCRC(msg);
			
			Log.d(TAG, "here sent: " + getHex(msg, msg.length));
			
			try {
				mOutputStream.write(msg);
			} 
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	void sendGetRTCPacket() {
		byte[] msg = new byte[] { 0x02, 0x08, 0x00, 0x00, 0x03 };
		putCRC(msg);
		
		try {
			mOutputStream.write(msg);
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
