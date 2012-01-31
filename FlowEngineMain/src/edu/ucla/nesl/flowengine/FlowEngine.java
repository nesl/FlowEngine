package edu.ucla.nesl.flowengine;

import java.util.HashMap;
import java.util.Map;

import android.app.Service;
import android.content.Intent;
import android.os.Debug;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import edu.ucla.nesl.flowengine.aidl.AbstractDeviceInterface;
import edu.ucla.nesl.flowengine.aidl.FlowEngineDeviceAPI;
import edu.ucla.nesl.flowengine.node.BufferNode;
import edu.ucla.nesl.flowengine.node.classifier.ActivityClassifier;
import edu.ucla.nesl.flowengine.node.feature.AverageVariance;
import edu.ucla.nesl.flowengine.node.feature.Goertzel;
import edu.ucla.nesl.flowengine.node.feature.RootMeanSquare;
import edu.ucla.nesl.util.NotificationHelper;

public class FlowEngine extends Service {

	private static final String TAG = FlowEngine.class.getSimpleName();

	int mNextDeviceID = 1;
	
	private NotificationHelper notify;

	private Map<Integer, AbstractDevice> deviceMap = new HashMap<Integer, AbstractDevice>();

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			synchronized (deviceMap) {
				AbstractDevice device = deviceMap.get(msg.what);
				device.node.inputData("Accelerometer", msg.obj);	
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
				
				// TODO: refine how to configure a graph.
				RootMeanSquare rms = new RootMeanSquare();
				BufferNode bufferNode = new BufferNode(50);
				AverageVariance avgVar = new AverageVariance();
				Goertzel goertzel = new Goertzel(1.0, 10.0, 1.0);
				ActivityClassifier activity= new ActivityClassifier();
				goertzel.addOutputNode(activity);
				avgVar.addOutputNode(activity);
				bufferNode.addOutputNode(goertzel);
				bufferNode.addOutputNode(avgVar);
				rms.addOutputNode(bufferNode);
				device.node.addOutputNode(rms);
				
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
		public void pushData(int deviceID, double[] data) throws RemoteException {
			mHandler.sendMessage(mHandler.obtainMessage(deviceID, data));
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

	@Override
	public void onCreate() {
		Debug.startMethodTracing("FlowEngine");
		
		super.onCreate();
		
		notify = new NotificationHelper(this, this.getClass().getSimpleName(), this.getClass().getName(), R.drawable.ic_launcher);
		
		Log.i(TAG, "Service creating");
	}
	
	@Override
	public void onDestroy() {
		Debug.stopMethodTracing();
		
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
