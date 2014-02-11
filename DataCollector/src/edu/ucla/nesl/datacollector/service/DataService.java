package edu.ucla.nesl.datacollector.service;

import java.text.DateFormat;
import java.util.Date;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import edu.ucla.nesl.datacollector.Const;
import edu.ucla.nesl.datacollector.Device;
import edu.ucla.nesl.flowengine.SensorType;
import edu.ucla.nesl.flowengine.aidl.ApplicationInterface;
import edu.ucla.nesl.flowengine.aidl.FlowEngineAppAPI;

public class DataService extends Service {

	public static final String BROADCAST_INTENT_MESSAGE = "edu.ucla.nesl.datacollector.service.DataService";
	
	private static final String BUNDLE_NAME = "name";
	private static final String BUNDLE_TYPE = "type";
	private static final String BUNDLE_DATA = "data";
	private static final String BUNDLE_LENGTH = "length";
	private static final String BUNDLE_TIMESTAMP = "timestamp";

	private static final int MSG_PUBLISH = 1;

	public static final String REQUEST_TYPE = "request_type";
	public static final String GET_SUBSCRIBED_SENSORS = "get_subscribed_sensors";
	public static final String CHANGE_SUBSCRIPTION = "change_subscription";
	public static final String EXTRA_SENSOR_NAME = "sensor_name";
	public static final String EXTRA_IS_ENABLED = "is_enabled";

	public static final String BUNDLE_SUBSCRIBED_SENSORS = "subscribed_sensors";
	
	private FlowEngineAppAPI mAPI;
	private int mAppID;

	@Override
	public void onCreate() {
		Log.d(Const.TAG, "Trying to bind to flowengine service");
		Intent intent = new Intent(Const.FLOW_ENGINE_APPLICATION_SERVICE);
		startService(intent);
		bindService(intent, mServiceConnection, 0);
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		try {
			if (mAPI != null) {
				mAPI.unregister(mAppID);
				unbindService(mServiceConnection);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Bundle bundle = intent.getExtras();
		if (bundle != null && mAPI != null) {
			String requestType = bundle.getString(REQUEST_TYPE);
			try {
				if (requestType.equals(GET_SUBSCRIBED_SENSORS)) {
					String[] sensors = mAPI.getSubscribedNodeNames(mAppID);
					Intent i = new Intent(BROADCAST_INTENT_MESSAGE);
					i.putExtra(BUNDLE_SUBSCRIBED_SENSORS, sensors);
					sendBroadcast(i);
				} else if (requestType.equals(CHANGE_SUBSCRIPTION)) {
					String sensor = bundle.getString(EXTRA_SENSOR_NAME);
					boolean isEnabled = bundle.getBoolean(EXTRA_IS_ENABLED);
					if (sensor != null) {
						if (sensor.equals(Device.LOCATION)) {
							if (isEnabled) {
								mAPI.subscribe(mAppID, SensorType.PHONE_GPS_NAME);
							} else {
								mAPI.unsubscribe(mAppID, SensorType.PHONE_GPS_NAME);
							}
						} else if (sensor.equals(Device.ACTIVITY)) {
							if (isEnabled) {
								mAPI.subscribe(mAppID, SensorType.ACTIVITY_CONTEXT_NAME);
							} else {
								mAPI.unsubscribe(mAppID, SensorType.ACTIVITY_CONTEXT_NAME);
							}
						} else if (sensor.equals(Device.STRESS)){
							if (isEnabled) {
								mAPI.subscribe(mAppID, SensorType.STRESS_CONTEXT_NAME);		
							} else {
								mAPI.unsubscribe(mAppID, SensorType.STRESS_CONTEXT_NAME);
							}
						} else if (sensor.equals(Device.CONVERSATION)) {
							if (isEnabled) {
								mAPI.subscribe(mAppID, SensorType.CONVERSATION_CONTEXT_NAME);
							} else {
								mAPI.unsubscribe(mAppID, SensorType.CONVERSATION_CONTEXT_NAME);
							}
						} else if (sensor.equals(Device.ACCELEROMETER)) {
							if (isEnabled) {
								mAPI.subscribe(mAppID, SensorType.PHONE_ACCELEROMETER_NAME);
							} else {
								mAPI.unsubscribe(mAppID, SensorType.PHONE_ACCELEROMETER_NAME);
							}
						} else if (sensor.equals(Device.ECG)) {
							if (isEnabled) {
								mAPI.subscribe(mAppID, SensorType.ECG_NAME);
							} else {
								mAPI.unsubscribe(mAppID, SensorType.ECG_NAME);
							}
						} else if (sensor.equals(Device.RESPIRATION)) {
							if (isEnabled) {
								mAPI.subscribe(mAppID, SensorType.RIP_NAME);
							} else {
								mAPI.unsubscribe(mAppID, SensorType.RIP_NAME);
							}
						}
					}
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}		
		}

		return super.onStartCommand(intent, flags, startId);
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(Const.TAG, "Service connection established.");
			mAPI = FlowEngineAppAPI.Stub.asInterface(service);
			try {
				mAppID = mAPI.register(mAppInterface);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i(Const.TAG, "Service connection closed.");
			mAPI = null;
			mAppID = -1;
		}
	};

	private ApplicationInterface.Stub mAppInterface = new ApplicationInterface.Stub() {
		@Override
		public void publishString(String name, String data, long timestamp) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putString(BUNDLE_NAME, name);
			bundle.putString(BUNDLE_TYPE, "String");
			bundle.putString(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, 1);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUBLISH, bundle));
		}

		@Override
		public void publishDouble(String name, double data, long timestamp)
				throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putString(BUNDLE_NAME, name);
			bundle.putString(BUNDLE_TYPE, "double");
			bundle.putDouble(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, 1);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUBLISH, bundle));
		}

		@Override
		public void publishDoubleArray(String name, double[] data, int length,
				long timestamp) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putString(BUNDLE_NAME, name);
			bundle.putString(BUNDLE_TYPE, "double[]");
			bundle.putDoubleArray(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, length);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUBLISH, bundle));
		}

		@Override
		public void publishInt(String name, int data, long timestamp)
				throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putString(BUNDLE_NAME, name);
			bundle.putString(BUNDLE_TYPE, "int");
			bundle.putInt(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, 1);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUBLISH, bundle));
		}

		@Override
		public void publishIntArray(String name, int[] data, int length,
				long timestamp) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putString(BUNDLE_NAME, name);
			bundle.putString(BUNDLE_TYPE, "int[]");
			bundle.putIntArray(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, length);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUBLISH, bundle));
		}
	};

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_PUBLISH:
				Bundle bundle = (Bundle)msg.obj;
				String name = bundle.getString(BUNDLE_NAME);
				long timestamp = bundle.getLong(BUNDLE_TIMESTAMP);
				int length = bundle.getInt(BUNDLE_LENGTH);
				String type = bundle.getString(BUNDLE_TYPE);

				dumpReceivedData(timestamp, type, bundle, name);

				break;
			}
			super.handleMessage(msg);
		}

		private void dumpReceivedData(long timestamp, String type, Bundle bundle, String name) {
			Date date = new Date(timestamp);
			String localDate = DateFormat.getDateInstance().format(date);
			String localTime = DateFormat.getTimeInstance().format(date);
			String lastUpdateString = "\nLast Updated: " + localDate + ' ' + localTime;

			if (type.equals("String")) {
				String data = bundle.getString(BUNDLE_DATA);
				String str = data + lastUpdateString;
				if (name.equals(SensorType.PHONE_BATTERY_NAME)) {
					Log.d(Const.TAG, str);
				} else if (name.equals(SensorType.ACTIVITY_CONTEXT_NAME)) {
					Log.d(Const.TAG, str);
				} else if (name.equals(SensorType.STRESS_CONTEXT_NAME)) {
					Log.d(Const.TAG, str);
				} else if (name.equals(SensorType.CONVERSATION_CONTEXT_NAME)) {
					Log.d(Const.TAG, str);
				}
			} else if (type.equals("double[]")) {
				double[] data = bundle.getDoubleArray(BUNDLE_DATA);
				String str = name + ": { ";
				for (double v : data) {
					str += v + ", ";
				}
				str = str.substring(0, str.length() - 2) + " }";
				Log.d(Const.TAG, str + lastUpdateString);
			} else if (type.equals("int[]")) {
				int[] data = bundle.getIntArray(BUNDLE_DATA);
				String str = name + ": { ";
				for (int v : data) {
					str += v + ", ";
				}
				str = str.substring(0, str.length() - 2) + " }";
				Log.d(Const.TAG, str + lastUpdateString);
			} else if (type.equals("int")) {
				int data = bundle.getInt(BUNDLE_DATA);
				Log.d(Const.TAG, name + ": " + data + lastUpdateString);
			} else if (type.equals("double")) {
				double data = bundle.getDouble(BUNDLE_DATA);
				Log.d(Const.TAG, name + ": " + data + lastUpdateString);
			} else {
				Log.d(Const.TAG, "Unknown type: " + type);
			}
		}
	};
}
