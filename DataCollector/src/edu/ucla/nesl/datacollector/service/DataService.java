package edu.ucla.nesl.datacollector.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.util.Date;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import edu.ucla.nesl.datacollector.Const;
import edu.ucla.nesl.datacollector.DataArchive;
import edu.ucla.nesl.datacollector.R;
import edu.ucla.nesl.datacollector.activity.TabSensorsActivity;
import edu.ucla.nesl.flowengine.SensorType;
import edu.ucla.nesl.flowengine.aidl.ApplicationInterface;
import edu.ucla.nesl.flowengine.aidl.FlowEngineAppAPI;

public class DataService extends Service {

	public static final String BROADCAST_INTENT_MESSAGE = "edu.ucla.nesl.datacollector.service.DataService";
	public static final String BUNDLE_NAME = "name";
	public static final String BUNDLE_TYPE = "type";
	public static final String BUNDLE_DATA = "data";
	public static final String BUNDLE_LENGTH = "length";
	public static final String BUNDLE_TIMESTAMP = "timestamp";

	public static final String REQUEST_TYPE = "request_type";

	public static final String GET_SUBSCRIBED_SENSORS = "get_subscribed_sensors";

	public static final String START_BROADCAST_SENSOR_DATA = "start_sensor_data";
	public static final String STOP_BROADCAST_SENSOR_DATA = "stop_sensor_data";

	public static final String CHANGE_SUBSCRIPTION = "change_subscription";	
	public static final String EXTRA_SENSOR_NAME = "sensor_name";
	public static final String EXTRA_IS_ENABLED = "is_enabled";

	public static final String BUNDLE_SUBSCRIBED_SENSORS = "subscribed_sensors";

	private static final int RETRY_INTERVAL = 5000; // ms
	private static final int MSG_PUBLISH = 1;
	private static final int MSG_TRY_BINDING_FLOWENGINE = 2;
	private static final int MSG_HANDLE_START_SERVICE_INTENT = 3;
	
	private static final String ACTIVITY_GRAPH_FILENAME = "activity.graph";
	private static final String STRESS_GRAPH_FILENAME = "stress.graph";
	private static final String CONVERSATION_GRAPH_FILENAME = "conversation.graph";

	private static final int LED_NOTIFICATION_ID = 1000;

	private Context context = this;
	private FlowEngineAppAPI mAPI;
	private int mAppID;

	private boolean isBroadcastData = false; 

	private NotificationManager notiManager;

	private DataArchive dataArchive;
	
	private ServiceHandler mHandler; 
	private Looper mServiceLooper;
	
	final class ServiceHandler extends Handler {
		public ServiceHandler(Looper looper) {
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_HANDLE_START_SERVICE_INTENT:
				handleStartServiceIntent((Bundle)msg.obj);
				break;
			case MSG_TRY_BINDING_FLOWENGINE:
				handleTryBindingFlowEngine();
				break;
			case MSG_PUBLISH:
				Bundle bundle = (Bundle)msg.obj;

				handleLEDNotification(bundle);

				if (isBroadcastData) {
					Intent i = new Intent(BROADCAST_INTENT_MESSAGE);
					i.putExtras(bundle);
					sendBroadcast(i);
				}

				dataArchive.storeData(bundle);
				
				/*String name = bundle.getString(BUNDLE_NAME);
				long timestamp = bundle.getLong(BUNDLE_TIMESTAMP);
				int length = bundle.getInt(BUNDLE_LENGTH);
				String type = bundle.getString(BUNDLE_TYPE);

				dumpReceivedData(timestamp, type, bundle, name);*/

				break;
			}
			super.handleMessage(msg);
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		notiManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		HandlerThread thread = new HandlerThread(this.getClass().getName());
		thread.start();
		mServiceLooper = thread.getLooper();
		mHandler = new ServiceHandler(mServiceLooper);
	}

	private void tryBindToFlowEngineService() {
		Intent intent = new Intent(Const.FLOW_ENGINE_APPLICATION_SERVICE);

		int numRetries = 1;
		while (startService(intent) == null) {
			Log.d(Const.TAG, "Retrying to start FlowEngineService.. (" + numRetries + ")");
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
			Log.d(Const.TAG, "Retrying to bind to FlowEngineService.. (" + numRetries + ")");
			numRetries++;
			try {
				Thread.sleep(RETRY_INTERVAL);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void processStoredSubscriptionStatus() {
		SharedPreferences settings = getSharedPreferences(Const.PREFS_NAME, 0);

		for (String sensorName : TabSensorsActivity.sensorNames) {
			boolean isEnabled = settings.getBoolean(sensorName, false);	
			Log.d(Const.TAG, sensorName + ": " + isEnabled);
			Intent intent = new Intent(getApplicationContext(), DataService.class);
			intent.putExtra(DataService.REQUEST_TYPE, DataService.CHANGE_SUBSCRIPTION);
			intent.putExtra(DataService.EXTRA_SENSOR_NAME, sensorName);
			intent.putExtra(DataService.EXTRA_IS_ENABLED, isEnabled);
			startService(intent);
		}
	}

	@Override
	public void onDestroy() {
		mServiceLooper.quit();
		
		try {
			if (mAPI != null) {
				mAPI.unregister(mAppID);
				unbindService(mServiceConnection);
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		
		if (dataArchive != null) {
			dataArchive.close();
		}
		
		stopForeground(true);
		
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		CharSequence text = getText(R.string.foreground_service_started);
		Notification notification = new Notification(R.drawable.ic_launcher, text, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getBroadcast(this, 0, new Intent(), 0);
		notification.setLatestEventInfo(this, text, text, contentIntent);
		startForeground(R.string.foreground_service_started, notification);
		
		if (dataArchive == null) {
			dataArchive = new DataArchive(this);
		}
		
		Bundle bundle = intent.getExtras();

		if (bundle == null && mAPI == null) {
			mHandler.sendMessage(mHandler.obtainMessage(MSG_TRY_BINDING_FLOWENGINE));
		}

		if (bundle != null && mAPI != null) {
			mHandler.sendMessage(mHandler.obtainMessage(MSG_HANDLE_START_SERVICE_INTENT, bundle));
		}

		return START_STICKY;
	}

	private static String getStringFromInputStream(InputStream is) {

		BufferedReader br = null;
		StringBuilder sb = new StringBuilder();

		String line;
		try {

			br = new BufferedReader(new InputStreamReader(is));
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return sb.toString();
	}

	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.i(Const.TAG, "Service connection established.");
			mAPI = FlowEngineAppAPI.Stub.asInterface(service);
			try {
				mAppID = mAPI.register(mAppInterface);
				try {
					mAPI.submitGraph(SensorType.ACTIVITY_CONTEXT_NAME
							, getStringFromInputStream(context.getAssets().open(ACTIVITY_GRAPH_FILENAME)));
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					mAPI.submitGraph(SensorType.STRESS_CONTEXT_NAME
							, getStringFromInputStream(context.getAssets().open(STRESS_GRAPH_FILENAME)));
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					mAPI.submitGraph(SensorType.CONVERSATION_CONTEXT_NAME
							, getStringFromInputStream(context.getAssets().open(CONVERSATION_GRAPH_FILENAME)));
				} catch (IOException e) {
					e.printStackTrace();
				}
				processStoredSubscriptionStatus();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.i(Const.TAG, "Service connection closed.");
			mAPI = null;
			mAppID = -1;
			isBroadcastData = false;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			mHandler.sendMessage(mHandler.obtainMessage(MSG_TRY_BINDING_FLOWENGINE));
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

	private void handleTryBindingFlowEngine() {
		if (mAPI == null) {
			tryBindToFlowEngineService();
		}
	}

	private void handleStartServiceIntent(Bundle bundle) {
		String requestType = bundle.getString(REQUEST_TYPE);
		try {
			if (requestType.equals(START_BROADCAST_SENSOR_DATA)) {
				isBroadcastData = true;
			} else if (requestType.equals(STOP_BROADCAST_SENSOR_DATA)) {
				isBroadcastData = false;
			} else if (requestType.equals(GET_SUBSCRIBED_SENSORS)) {
				String[] sensors = mAPI.getSubscribedNodeNames(mAppID);
				Intent i = new Intent(BROADCAST_INTENT_MESSAGE);
				i.putExtra(BUNDLE_SUBSCRIBED_SENSORS, sensors);
				sendBroadcast(i);
			} else if (requestType.equals(CHANGE_SUBSCRIPTION)) {
				String sensor = bundle.getString(EXTRA_SENSOR_NAME);
				boolean isEnabled = bundle.getBoolean(EXTRA_IS_ENABLED);
				if (sensor != null) {
					if (isEnabled) {
						mAPI.subscribe(mAppID, sensor);
					} else {
						mAPI.unsubscribe(mAppID, sensor);
						dataArchive.finish(sensor);
					}
				}
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}		
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

	private boolean isRedOn = false;
	private boolean isGreenOn = false;

	private void handleLEDNotification(Bundle bundle) {
		String name = bundle.getString(DataService.BUNDLE_NAME);
		if (name.equals(SensorType.ACTIVITY_CONTEXT_NAME)) {
			flipActivityFlag();
			updateLed();
		} else if (name.equals(SensorType.ZEPHYR_BATTERY_NAME)) {
			flipZephyrBatteryFlag();
			updateLed();
		}
	}

	private void flipActivityFlag() {
		if (isRedOn) {
			isRedOn = false;
		} else {
			isRedOn = true;
		}
	}
	
	private void flipZephyrBatteryFlag() {
		if (isGreenOn) {
			isGreenOn = false;
		} else {
			isGreenOn = true;
		}
	}
	
	private void updateLed()
	{
		notiManager.cancel(LED_NOTIFICATION_ID);
		Notification notif = new Notification();
		notif.flags = Notification.FLAG_SHOW_LIGHTS;
		if (isRedOn && isGreenOn) {
			notif.ledARGB = 0xFF0000ff;
			notiManager.notify(LED_NOTIFICATION_ID, notif);
		} else if (isRedOn && !isGreenOn) {
			notif.ledARGB = 0xFFff0000;
			notiManager.notify(LED_NOTIFICATION_ID, notif);
		} else if (!isRedOn && isGreenOn) {
			notif.ledARGB = 0xFF00ff00;
			notiManager.notify(LED_NOTIFICATION_ID, notif);
		}
	}
}
