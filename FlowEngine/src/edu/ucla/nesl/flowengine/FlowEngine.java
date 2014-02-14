package edu.ucla.nesl.flowengine;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import edu.ucla.nesl.flowengine.aidl.ApplicationInterface;
import edu.ucla.nesl.flowengine.aidl.DeviceAPI;
import edu.ucla.nesl.flowengine.aidl.FlowEngineAPI;
import edu.ucla.nesl.flowengine.aidl.FlowEngineAppAPI;
import edu.ucla.nesl.flowengine.node.DataFlowNode;
import edu.ucla.nesl.flowengine.node.SeedNode;
import edu.ucla.nesl.util.NotificationHelper;

public class FlowEngine extends Service {

	private static final String TAG = FlowEngine.class.getSimpleName();
	private static FlowEngine INSTANCE;

	private static final String BUNDLE_FLOWENGINE_TEST = "flowengine_test";
	
	private static final String BUNDLE_TYPE = "type";
	private static final String BUNDLE_SENSOR = "sensor";
	private static final String BUNDLE_DATA = "data";
	private static final String BUNDLE_LENGTH = "length";
	private static final String BUNDLE_DEVICE_ID = "deviceID";
	private static final String BUNDLE_TIMESTAMP = "timestamp";
	private static final String BUNDLE_CONTEXT_NAME = "context_name";
	private static final String BUNDLE_GRAPH = "graph";

	private static final int MSG_PUSH_DATA = 1;
	private static final int MSG_SUBSCRIBE = 2;
	private static final int MSG_UNSUBSCRIBE = 3;
	private static final int MSG_UNREGISTER_APP = 4;
	private static final int MSG_ADD_SENSOR = 5;
	private static final int MSG_REMOVE_DEVICE = 6;
	private static final int MSG_SUBMIT_GRAPH = 7;

	private static final String BUNDLE_APP_ID = "app_id";
	private static final String BUNDLE_NODE_NAME = "node_name";
	private static final String BUNDLE_SENSOR_ID = "sensor_id";
	private static final String BUNDLE_SAMPLE_INTERVAL = "sample_interval";

	private static final int LED_NOTIFICATION_ID = 1;

	private NotificationHelper mNotification;

	int mNextDeviceID = 1;
	int mNextApplicationID = 1;

	private Map<Integer, Device> mDeviceMap = new HashMap<Integer, Device>();
	private Map<Integer, Application> mApplicationMap = new HashMap<Integer, Application>();
	private Map<Integer, SeedNode> mSeedNodeMap = new HashMap<Integer, SeedNode>();
	private Map<String, DataFlowNode> mNodeNameMap = new HashMap<String, DataFlowNode>();

	private GraphConfiguration mGraphConfig = new GraphConfiguration(mSeedNodeMap, mNodeNameMap);

	private Map<Integer, Timer> mCancelTimerMap = new HashMap<Integer, Timer>();

	class CancelNotificationTimerTask extends TimerTask {
		int mSensorID;

		public CancelNotificationTimerTask(int sensorID) {
			mSensorID = sensorID;
		}

		@Override
		public void run() {
			mNotification.cancel(mSensorID);
			Timer timer;
			timer = mCancelTimerMap.remove(mSensorID);
			timer.cancel();
		}
	}

	private void showNotification(int sensor, String name) {
		Timer timer = mCancelTimerMap.get(sensor);
		if (timer == null) {
			mNotification.showNotificationNowOngoing(sensor, "Receiving " + name + "..");
			timer = new Timer();
			timer.schedule(new CancelNotificationTimerTask(sensor), 5000);
			mCancelTimerMap.put(sensor, timer);
		} else {
			timer.cancel();
			timer = new Timer();
			timer.schedule(new CancelNotificationTimerTask(sensor), 5000);
			mCancelTimerMap.put(sensor, timer);
		}
	}

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_SUBMIT_GRAPH:
				handleSubmitGraph((Bundle)msg.obj);
				break;
			case MSG_REMOVE_DEVICE:
				handleRemoveDevice((Bundle)msg.obj);
				break;
			case MSG_ADD_SENSOR:
				handleAddSensor((Bundle)msg.obj);
				break;
			case MSG_UNREGISTER_APP:
				handleUnregisterApplication((Bundle)msg.obj);
				break;
			case MSG_UNSUBSCRIBE:
				handleUnsubscribe((Bundle)msg.obj);
				break;
			case MSG_SUBSCRIBE:
				handleSubscribe((Bundle)msg.obj);
				break;
			case MSG_PUSH_DATA:
				handlePushData((Bundle)msg.obj);
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	};
	
	private void handleSubmitGraph(Bundle bundle) {
		String contextName = bundle.getString(BUNDLE_CONTEXT_NAME);
		String graph = bundle.getString(BUNDLE_GRAPH);
		mGraphConfig.submitGraph(contextName, graph);
	}

	private void handleRemoveDevice(Bundle bundle) {
		int deviceID = bundle.getInt(BUNDLE_DEVICE_ID);
		
		synchronized(mDeviceMap) {
			Device removedDevice = mDeviceMap.remove(deviceID);
			if (removedDevice != null) {
				Sensor[] sensors = removedDevice.getSensorList();
				for (Sensor sensor: sensors) {
					SeedNode seed = mSeedNodeMap.get(sensor.getSensorID());
					if (seed.isConnected()) {
						seed.detachDevice();
					} else {
						mSeedNodeMap.remove(sensor.getSensorID());
						mNodeNameMap.remove("|" + SensorType.getSensorName(sensor.getSensorID()));
					}
				}
				//mNotification.showNotificationNow("Removed device ID " + deviceID);
			}
		}
	}

	private void printSeedNodeMap() {
		// print seed map
		Log.d(TAG, "Printing mSeedNodeMap..");
		for (Map.Entry<Integer, SeedNode> entry: mSeedNodeMap.entrySet()) {
			int sensor = entry.getKey();
			SeedNode node1 = entry.getValue();
			Log.d(TAG, node1.getClass().getName() + ": " + sensor + " device: " + node1.getAttachedDevice());
		}
		Log.d(TAG, "Done.");
	}

	private void printNodeNameMap() {
		// print node name map
		Log.d(TAG, "Printing mNodeNameMap..");
		for (Map.Entry<String, DataFlowNode> entry: mNodeNameMap.entrySet()) {
			String nodeName = entry.getKey();
			DataFlowNode node2 = entry.getValue();
			//Log.d(TAG, nodeName + ": " + node2.getClass().getName());
			Log.d(TAG, nodeName + ": " + node2);
		}
		Log.d(TAG, "Done.");

	}
	
	private void handleAddSensor(Bundle bundle) {
		int deviceID = bundle.getInt(BUNDLE_DEVICE_ID);
		int sensor = bundle.getInt(BUNDLE_SENSOR_ID);
		int sampleInterval = bundle.getInt(BUNDLE_SAMPLE_INTERVAL);

		synchronized(mDeviceMap) {
			Device device = mDeviceMap.get(deviceID);
			device.addSensor(sensor, sampleInterval);
			SeedNode seedNode = mSeedNodeMap.get(sensor);
			if (seedNode != null) {
				seedNode.attachDevice(device);
			} else {
				seedNode = new SeedNode(SensorType.getSensorName(sensor), sensor, device);
				mSeedNodeMap.put(sensor, seedNode);
				mNodeNameMap.put("|" + SensorType.getSensorName(sensor), seedNode);
			}
			DebugHelper.log(TAG, "Added sensor type: " + sensor);

			if (seedNode.isEnabled()) {
				seedNode.startSensor();
			}

			/*for (Map.Entry<Integer, SeedNode> entry: mSeedNodeMap.entrySet()) {
					sensor = entry.getKey();
					SeedNode node = entry.getValue();
					DebugHelper.log(TAG, node.getClass().getName() + ": " + sensor + " device: " + node.getAttachedDevice());
				}*/
		}
	}

	private void handleUnregisterApplication(Bundle bundle) {
		int appId = bundle.getInt(BUNDLE_APP_ID);
		unregisterApplication(appId);
	}

	private void handleSubscribe(Bundle bundle) {
		int appId = bundle.getInt(BUNDLE_APP_ID);
		String nodeName = bundle.getString(BUNDLE_NODE_NAME);
		mApplicationMap.get(appId).addSubscribedNodeNames(nodeName);
		mGraphConfig.subscribe(mApplicationMap.get(appId), nodeName);
		
		//mNotification.showNotificationNow("Subscribed " + nodeName);
	}

	private void handleUnsubscribe(Bundle bundle) {
		int appId = bundle.getInt(BUNDLE_APP_ID);
		String nodeName = bundle.getString(BUNDLE_NODE_NAME);
		mApplicationMap.get(appId).removeSubscribedNodeNames(nodeName);
		mGraphConfig.unsubscribe(mApplicationMap.get(appId), nodeName);
		
		//mNotification.showNotificationNow("Unsubscribed " + nodeName);
	}

	private void handlePushData(Bundle bundle) {
		int sensor = bundle.getInt(BUNDLE_SENSOR);
		SeedNode seed = mSeedNodeMap.get(sensor);

		if (seed == null) {
			//throw new UnsupportedOperationException("No SeedNode for SensorType: " + sensor);
			DebugHelper.log(TAG, "No SeedNode for SensorType: " + sensor);
			return;
		}

		int deviceID = bundle.getInt(BUNDLE_DEVICE_ID);

		if (seed.getAttachedDevice() != mDeviceMap.get(deviceID)) {
			DebugHelper.log(TAG, "Unmatched seed node and attached device(sensor: " + sensor + ", attempted device ID: " + deviceID);
			return;
		}

		String type = bundle.getString(BUNDLE_TYPE);
		int length = bundle.getInt(BUNDLE_LENGTH);
		String name = SensorType.getSensorName(sensor);
		long timestamp = bundle.getLong(BUNDLE_TIMESTAMP);

		// show notification
		//showNotification(sensor, name);

		if (type.equals(DataType.DOUBLE_ARRAY)) {
			seed.input(name, type, bundle.getDoubleArray(BUNDLE_DATA), length, timestamp);
		} else if (type.equals(DataType.DOUBLE)) {
			seed.input(name, type, bundle.getDouble(BUNDLE_DATA), length, timestamp);
		} else if (type.equals(DataType.INTEGER_ARRAY)) {
			seed.input(name, type, bundle.getIntArray(BUNDLE_DATA), length, timestamp);
		} else if (type.equals(DataType.INTEGER)) {
			seed.input(name, type, bundle.getInt(BUNDLE_DATA), length, timestamp);
		} else if (type.equals(DataType.STRING)) {
			seed.input(name, type, bundle.getString(BUNDLE_DATA), length, timestamp);
		} else {
			throw new IllegalArgumentException("Unknown data_type: " + type);
		}
	}

	private FlowEngineAppAPI.Stub mApplicationAPI = new FlowEngineAppAPI.Stub() {
		// This override is very useful for debugging.
		@Override
		public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
			try {
				return super.onTransact(code, data, reply, flags);
			} catch (RuntimeException e) {
				Log.e(TAG, "Unexpected exception", e);
				throw e;
			}
		}

		@Override
		public int register(ApplicationInterface appInterface) throws RemoteException {
			synchronized(mApplicationMap) {
				int appID = mNextApplicationID;
				Application app = new Application(appID, appInterface);
				mApplicationMap.put(appID, app);
				mNextApplicationID += 1;

				//mNotification.showNotificationNow("Registered application ID: " + appID);

				return appID;
			}
		}

		@Override
		public void subscribe(int appId, String nodeName) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_APP_ID, appId);
			bundle.putString(BUNDLE_NODE_NAME, nodeName);			
			mHandler.sendMessage(mHandler.obtainMessage(MSG_SUBSCRIBE, bundle));
		}

		@Override
		public void unregister(int appId) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_APP_ID, appId);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_UNREGISTER_APP, bundle));
		}

		@Override
		public void unsubscribe(int appId, String nodeName) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_APP_ID, appId);
			bundle.putString(BUNDLE_NODE_NAME, nodeName);			
			mHandler.sendMessage(mHandler.obtainMessage(MSG_UNSUBSCRIBE, bundle));
		}

		@Override
		public String[] getSubscribedNodeNames(int appId) throws RemoteException {
			Set<String> sensorList = mApplicationMap.get(appId).getSubscribedNodeNames();
			if (sensorList.size() > 0) {
				return sensorList.toArray(new String[sensorList.size()]);
			} else {
				return null;
			}
		}

		@Override
		public void submitGraph(String contextName, String graph) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putString(BUNDLE_CONTEXT_NAME, contextName);
			bundle.putString(BUNDLE_GRAPH, graph);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_SUBMIT_GRAPH, bundle));
		}
	};

	private FlowEngineAPI.Stub mDeviceAPI = new FlowEngineAPI.Stub() {
		// This override is very useful for debugging.
		@Override
		public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
			try {
				return super.onTransact(code, data, reply, flags);
			} catch (RuntimeException e) {
				Log.e(TAG, "Unexpected exception", e);
				throw e;
			}
		}

		@Override
		public int addDevice(DeviceAPI deviceAPI) throws RemoteException {
			synchronized(mDeviceMap) {
				Device device = new Device(deviceAPI);
				int deviceID = mNextDeviceID;
				mNextDeviceID += 1;
				mDeviceMap.put(deviceID, device);

				//mNotification.showNotificationNow("Added device ID " + deviceID);

				return deviceID;
			}
		}

		@Override
		public void addSensor(int deviceID, int sensor, int sampleInterval) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_DEVICE_ID, deviceID);
			bundle.putInt(BUNDLE_SENSOR_ID, sensor);
			bundle.putInt(BUNDLE_SAMPLE_INTERVAL, sampleInterval);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_ADD_SENSOR, bundle));
		}

		@Override
		public void removeDevice(int deviceID) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_DEVICE_ID, deviceID);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_REMOVE_DEVICE, bundle));
		}

		@Override
		public void pushDoubleArray(int deviceID, int sensor, double[] data, int length, long timestamp) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_DEVICE_ID, deviceID);
			bundle.putInt(BUNDLE_SENSOR, sensor);
			bundle.putString(BUNDLE_TYPE, DataType.DOUBLE_ARRAY);
			bundle.putDoubleArray(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, length);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUSH_DATA, bundle));
		}

		@Override
		public void pushDouble(int deviceID, int sensor, double data, long timestamp) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_DEVICE_ID, deviceID);
			bundle.putInt(BUNDLE_SENSOR, sensor);
			bundle.putString(BUNDLE_TYPE, DataType.DOUBLE);
			bundle.putDouble(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, 1);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUSH_DATA, bundle));
		}

		@Override
		public void pushIntArray(int deviceID, int sensor, int[] data, int length, long timestamp) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_DEVICE_ID, deviceID);
			bundle.putInt(BUNDLE_SENSOR, sensor);
			bundle.putString(BUNDLE_TYPE, DataType.INTEGER_ARRAY);
			bundle.putIntArray(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, length);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUSH_DATA, bundle));
		}

		@Override
		public void pushInt(int deviceID, int sensor, int data, long timestamp) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_DEVICE_ID, deviceID);
			bundle.putInt(BUNDLE_SENSOR, sensor);
			bundle.putString(BUNDLE_TYPE, DataType.INTEGER);
			bundle.putInt(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, 1);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUSH_DATA, bundle));
		}

		@Override
		public void pushString(int deviceID, int sensor, String data, int length, long timestamp) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_DEVICE_ID, deviceID);
			bundle.putInt(BUNDLE_SENSOR, sensor);
			bundle.putString(BUNDLE_TYPE, DataType.STRING);
			bundle.putString(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, length);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUSH_DATA, bundle));
		}
	};

	public static FlowEngine getInstance() {
		return INSTANCE;
	}

	@Override
	public IBinder onBind(Intent intent) {
		DebugHelper.log(TAG, "Bound by intent " + intent);
		if (FlowEngine.class.getName().equals(intent.getAction())) {
			return mDeviceAPI;
		} else if (intent.getAction().equals("edu.ucla.nesl.flowengine.FlowEngine.application")) {
			return mApplicationAPI; 
		} else {	
			return null;
		}
	}

	@Override
	public void onCreate() {
		
		INSTANCE = this;

		mNotification = new NotificationHelper(this, TAG, this.getClass().getName(), R.drawable.ic_launcher);

		DebugHelper.startTrace();
		
		super.onCreate();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		
		CharSequence text = getText(R.string.foreground_service_started);
		Notification notification = new Notification(R.drawable.ic_launcher, text, System.currentTimeMillis());
		PendingIntent contentIntent = PendingIntent.getBroadcast(this, 0, new Intent(), 0);
		notification.setLatestEventInfo(this, text, text, contentIntent);
		startForeground(R.string.foreground_service_started, notification);
		
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			boolean isTest = bundle.getBoolean(BUNDLE_FLOWENGINE_TEST, false);
			if (isTest) {
				runTest();
			}
		}
		
		return START_STICKY;
	}

	private void runTest() {
		//ActivityGraphControl agc = (ActivityGraphControl)mNodeNameMap.get("|ActivityGraphControl(gps,motion)");
		//Log.d(TAG, agc.mMotionNode + "\n" + agc.mGpsSeed);
		
		printSeedNodeMap();
		printNodeNameMap();
		
		/*SeedNode gpsSeed = mSeedNodeMap.get(SensorType.PHONE_GPS);
		
		if (gpsSeed != null) {
			List<DataFlowNode> nodeList = gpsSeed.getOutPortMap().get("default");
			for (DataFlowNode node: nodeList) {
				Log.d(TAG, "nodeName: " + node.getName() + ": " + node.toString());
			}
		}*/
	}

	@Override
	public void onDestroy() {
		DebugHelper.stopTrace();

		// Kill device services
		//for (Map.Entry<Integer, Device> entry : mDeviceMap.entrySet()) {
		//	try {
		//		entry.getValue().getInterface().kill();
		//	} catch (RemoteException e) {
		//		e.printStackTrace();
		//	}
		//}

		stopForeground(true);
		
		super.onDestroy();
	}

	public void unregisterApplication(int appId) {
		synchronized(mApplicationMap) {
			Application removedApp = mApplicationMap.remove(appId);
			mGraphConfig.removeApplication(removedApp);
			//mNotification.showNotificationNow("Unregistering application ID " + appId);
		}
	}
}
