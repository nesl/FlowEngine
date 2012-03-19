package edu.ucla.nesl.flowengine;

import java.util.HashMap;
import java.util.Map;

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
import edu.ucla.nesl.flowengine.node.Publish;
import edu.ucla.nesl.flowengine.node.SeedNode;
import edu.ucla.nesl.util.NotificationHelper;

public class FlowEngine extends Service {

	private static final String TAG = FlowEngine.class.getSimpleName();
	private static FlowEngine INSTANCE;
	
	private static final String BUNDLE_TYPE = "type";
	private static final String BUNDLE_SENSOR = "sensor";
	private static final String BUNDLE_DATA = "data";
	private static final String BUNDLE_LENGTH = "length";
	private static final String BUNDLE_DEVICE_ID = "deviceID";
	private static final String BUNDLE_TIMESTAMP = "timestamp";
	
	private static final int MSG_PUSH_DATA = 1;
	
	private NotificationHelper mNotify;
	
	int mNextDeviceID = 1;
	int mNextApplicationID = 1;
	
	private Map<Integer, Device> mDeviceMap = new HashMap<Integer, Device>();
	private Map<Integer, Application> mApplicationMap = new HashMap<Integer, Application>();
	private Map<Integer, SeedNode> mSeedNodeMap = new HashMap<Integer, SeedNode>();

	private GraphConfiguration mGraphConfig = new GraphConfiguration(mSeedNodeMap);
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_PUSH_DATA:
					Bundle bundle = (Bundle)msg.obj;
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
					
					if (type.equals("double[]")) {
						seed.input(name, type, bundle.getDoubleArray(BUNDLE_DATA), length, timestamp);	
					} else if (type.equals("int[]")) {
						seed.input(name, type, bundle.getIntArray(BUNDLE_DATA), length, timestamp);
					} else if (type.equals("int")) {
						seed.input(name, type, bundle.getInt(BUNDLE_DATA), length, timestamp);
					} else if (type.equals("String")) {
						seed.input(name, type, bundle.getString(BUNDLE_DATA), length, timestamp);
					} else {
						throw new IllegalArgumentException("Unknown data_type: " + type);
					}
					break;
					
				default:
					super.handleMessage(msg);
					break;
			}
		}
	};
	
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
		public int registerApplication(ApplicationInterface appInterface) throws RemoteException {
			synchronized(mApplicationMap) {
				Application app = new Application(appInterface);
				int appID = mNextApplicationID;
				mNextApplicationID += 1;
				mApplicationMap.put(appID, app);
				
				DebugHelper.log(TAG, "Added application ID: " + appID);
				
				return appID;
			}
		}

		@Override
		public void addSubscription(int appId, String nodeName) throws RemoteException {
			Log.d(TAG, "subscribe for " + nodeName);
			mApplicationMap.get(appId).addSubscribedNodeNames(nodeName);
		}

		@Override
		public void configure(int appId) throws RemoteException {
			mGraphConfig.configureApplication(mApplicationMap.get(appId));
		}

		@Override
		public void unregisterApplication(int appId) throws RemoteException {
			synchronized(mApplicationMap) {
				Application removedApplication = mApplicationMap.remove(appId);
				for (Publish node: removedApplication.getPublishNodes()) {
					for (DataFlowNode parent: node.getParents()) {
						parent.removeChild(node);
					}
				}
			}
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
				
				DebugHelper.log(TAG, "Added device ID: " + deviceID);
				
				return deviceID;
			}
		}

		@Override
		public void addSensor(int deviceID, int sensor, int sampleInterval) throws RemoteException {
			synchronized(mDeviceMap) {
				synchronized(mSeedNodeMap) {
					Device device = mDeviceMap.get(deviceID);
					device.addSensor(sensor, sampleInterval);
					SeedNode existingSeed = mSeedNodeMap.get(sensor);
					if (existingSeed != null) {
						existingSeed.attachDevice(device);
					} else {
						mSeedNodeMap.put(sensor, new SeedNode(SensorType.getSensorName(sensor), sensor, device));
					}
					mGraphConfig.configureStressGraph();
					mGraphConfig.configureConversationGraph();
					DebugHelper.log(TAG, "Added sensor type: " + sensor);
					
					for (Map.Entry<Integer, SeedNode> entry: mSeedNodeMap.entrySet()) {
						sensor = entry.getKey();
						SeedNode node = entry.getValue();
						DebugHelper.log(TAG, node.getClass().getName() + ": " + sensor + " device: " + node.getAttachedDevice());
					}
				}
			}
		}

		@Override
		public void removeDevice(int deviceID) throws RemoteException {
			synchronized(mDeviceMap) {
				synchronized(mSeedNodeMap) {
					Device removedDevice = mDeviceMap.remove(deviceID);
					if (removedDevice != null) {
						Sensor[] sensors = removedDevice.getSensorList();
						for (Sensor sensor: sensors) {
							mSeedNodeMap.remove(sensor.getSensorType());
						}
					}
				}
			}
		}

		@Override
		public void pushDoubleArray(int deviceID, int sensor, double[] data, int length, long timestamp) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_DEVICE_ID, deviceID);
			bundle.putInt(BUNDLE_SENSOR, sensor);
			bundle.putString(BUNDLE_TYPE, "double[]");
			bundle.putDoubleArray(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, length);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUSH_DATA, bundle));
		}

		@Override
		public void pushInt(int deviceID, int sensor, int data, int length, long timestamp) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_DEVICE_ID, deviceID);
			bundle.putInt(BUNDLE_SENSOR, sensor);
			bundle.putString(BUNDLE_TYPE, "int");
			bundle.putInt(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, length);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUSH_DATA, bundle));
		}

		@Override
		public void pushIntArray(int deviceID, int sensor, int[] data, int length, long timestamp) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_DEVICE_ID, deviceID);
			bundle.putInt(BUNDLE_SENSOR, sensor);
			bundle.putString(BUNDLE_TYPE, "int[]");
			bundle.putIntArray(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, length);
			bundle.putLong(BUNDLE_TIMESTAMP, timestamp);
			mHandler.sendMessage(mHandler.obtainMessage(MSG_PUSH_DATA, bundle));
		}

		@Override
		public void pushString(int deviceID, int sensor, String data, int length, long timestamp) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putInt(BUNDLE_DEVICE_ID, deviceID);
			bundle.putInt(BUNDLE_SENSOR, sensor);
			bundle.putString(BUNDLE_TYPE, "String");
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
		super.onCreate();
		DebugHelper.logi(TAG, "Service creating..");
		
		INSTANCE = this;
		
		mNotify = new NotificationHelper(this, this.getClass().getSimpleName(), this.getClass().getName(), R.drawable.ic_launcher);
		
		DebugHelper.startTrace();
	}
	
	@Override
	public void onDestroy() {
		DebugHelper.logi(TAG, "Service destroying..");

		DebugHelper.stopTrace();
		
		for (Map.Entry<Integer, Device> entry : mDeviceMap.entrySet()) {
			try {
				entry.getValue().getInterface().kill();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
		super.onDestroy();
	}
}
