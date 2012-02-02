package edu.ucla.nesl.flowengine;

import java.util.ArrayList;
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
import edu.ucla.nesl.flowengine.aidl.AbstractDeviceInterface;
import edu.ucla.nesl.flowengine.aidl.FlowEngineDeviceAPI;
import edu.ucla.nesl.flowengine.node.BufferNode;
import edu.ucla.nesl.flowengine.node.SeedNode;
import edu.ucla.nesl.flowengine.node.classifier.ActivityClassifier;
import edu.ucla.nesl.flowengine.node.feature.AverageVariance;
import edu.ucla.nesl.flowengine.node.feature.Goertzel;
import edu.ucla.nesl.flowengine.node.feature.IERatio;
import edu.ucla.nesl.flowengine.node.feature.RootMeanSquare;
import edu.ucla.nesl.util.NotificationHelper;

public class FlowEngine extends Service {

	private static final String TAG = FlowEngine.class.getSimpleName();
	private static final String BUNDLE_TYPE = "type";
	private static final String BUNDLE_DATA = "data";
	private static final String BUNDLE_LENGTH = "length";
	/*private static final String DATA_TYPE_INT = "int";
	private static final String DATA_TYPE_INT_ARRAY = "int[]";
	private static final String DATA_TYPE_DOUBLE = "double";
	private static final String DATA_TYPE_DOUBLE_ARRAY = "double[]";
	private static final String DATA_TYPE_ARRAY = "[]";*/
	
	int mNextDeviceID = 1;
	
	private NotificationHelper notify;

	private Map<Integer, AbstractDevice> deviceMap = new HashMap<Integer, AbstractDevice>();
	private ArrayList<SeedNode> mSeedNodeList = new ArrayList<SeedNode>();
	
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			for (SeedNode seed: mSeedNodeList) {
				if (seed.mSeedName == msg.what) {
					Bundle bundle = (Bundle)msg.obj;
					String type = bundle.getString(BUNDLE_TYPE);
					int length = bundle.getInt(BUNDLE_LENGTH);
					String name = SensorName.getSensorNameString(msg.what);
					if (type.equals("double[]")) {
						seed.inputData(name, type, bundle.getDoubleArray(BUNDLE_DATA), length);	
					} else if (type.equals("int[]")) {
						seed.inputData(name, type, bundle.getIntArray(BUNDLE_DATA), length);
					} else if (type.equals("int")) {
						seed.inputData(name, type, bundle.getInt(BUNDLE_DATA), length);
					} else {
						Log.e(TAG, "Unknown data_type: " + type);
					}
				}
			}
		}
	};
	
	private FlowEngineDeviceAPI.Stub deviceApiEndpoint = new FlowEngineDeviceAPI.Stub() {
		// This override is very useful for debugging.
		@Override
		public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
			try {
				return super.onTransact(code, data, reply, flags);
			} catch (RuntimeException e) {
				Log.w(TAG, "Unexpected remote exception", e);
				throw e;
			}
		}
		
		@Override
		public int addAbstractDevice(AbstractDeviceInterface adi) throws RemoteException {
			synchronized(deviceMap) {
				AbstractDevice device = new AbstractDevice(adi);
				int deviceID = mNextDeviceID;
				mNextDeviceID += 1;
				deviceMap.put(deviceID, device);
				Log.d(TAG, "Added device ID: " + Integer.toString(deviceID));
				return deviceID;
			}
		}

		@Override
		public void removeAbstractDevice(int deviceID) throws RemoteException {
			synchronized(deviceMap) {
				deviceMap.remove(deviceID);
			}
		}

		@Override
		public void pushDoubleArrayData(int seed_name, double[] data, int length) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putString(BUNDLE_TYPE, "double[]");
			bundle.putDoubleArray(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, length);
			mHandler.sendMessage(mHandler.obtainMessage(seed_name, bundle));
		}

		@Override
		public void pushIntData(int seed_name, int data, int length) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putString(BUNDLE_TYPE, "int");
			bundle.putInt(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, length);
			mHandler.sendMessage(mHandler.obtainMessage(seed_name, bundle));
		}

		@Override
		public void pushIntArrayData(int seed_name, int[] data, int length) throws RemoteException {
			Bundle bundle = new Bundle();
			bundle.putString(BUNDLE_TYPE, "int[]");
			bundle.putIntArray(BUNDLE_DATA, data);
			bundle.putInt(BUNDLE_LENGTH, length);
			mHandler.sendMessage(mHandler.obtainMessage(seed_name, bundle));
		}
	};

	
	/*private String dumpWaveSegment(final WaveSegment ws) {
		String wsDump = "timestamp:" + Long.toString(ws.timestamp) 
				+ ", interval:" + Long.toString(ws.interval) 
				+ ", location:";
		wsDump += "(" + Double.toString(ws.latitude) + "," + Double.toString(ws.longitude) + ")";
		wsDump += ", format:";
		if (ws.format == null) {
			wsDump += "null";
		} else {
			wsDump += "{";
			for (int i = 0; i < ws.numChannel; i++) {
				wsDump += ws.format[i] + ",";
			}
			wsDump += "}";
		}
		wsDump += ", data:";
		if (ws.data == null) {
			wsDump += "null";
		} else {
			wsDump += "{";
			for (int i = 0; i < ws.numData; i++) {
				wsDump += Double.toString(ws.data[i]) + ",";
			}
			wsDump += "}";
		}
		return wsDump;
	}*/
	
	@Override
	public IBinder onBind(Intent intent) {
		if (FlowEngine.class.getName().equals(intent.getAction())) {
			Log.d(TAG, "Bound by intent " + intent);
			return deviceApiEndpoint;
		} else {
			return null;
		}
	}

	private void configureGraph() {
		//SeedNode accelerometer = new SeedNode(SensorName.ACCELEROMETER);
		//SeedNode ecg = new SeedNode(SensorName.ECG);
		SeedNode rip = new SeedNode(SensorName.RIP);
		//SeedNode skintemp = new SeedNode(SensorName.SKIN_TEMPERATURE);
		//mSeedNodeList.add(accelerometer);
		//mSeedNodeList.add(ecg);
		mSeedNodeList.add(rip);
		//mSeedNodeList.add(skintemp);

		// Activity classifier
		/*RootMeanSquare rms = new RootMeanSquare();
		BufferNode rmsBuffer = new BufferNode(50);
		AverageVariance avgVar = new AverageVariance();
		Goertzel goertzel = new Goertzel(1.0, 10.0, 1.0);
		ActivityClassifier activity= new ActivityClassifier();
		goertzel.addOutputNode(activity);
		avgVar.addOutputNode(activity);
		rmsBuffer.addOutputNode(goertzel);
		rmsBuffer.addOutputNode(avgVar);
		rms.addOutputNode(rmsBuffer);
		accelerometer.addOutputNode(rms);*/
		
		//Stress classifier
		IERatio ieratio = new IERatio();
		BufferNode ripBuffer = new BufferNode(18 * 60);
		ripBuffer.addOutputNode(ieratio);
		rip.addOutputNode(ripBuffer);
	}
	
	@Override
	public void onCreate() {
		//Debug.startMethodTracing("FlowEngine");
		
		super.onCreate();
		
		notify = new NotificationHelper(this, this.getClass().getSimpleName(), this.getClass().getName(), R.drawable.ic_launcher);
		
		configureGraph();
		
		Log.i(TAG, "Service creating");
	}
	
	@Override
	public void onDestroy() {
		//Debug.stopMethodTracing();
		
		Log.i(TAG, "Service destroying");
		
		for (Map.Entry<Integer, AbstractDevice> entry : deviceMap.entrySet()) {
			try {
				entry.getValue().adi.kill();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		
		super.onDestroy();
	}
}
