package edu.ucla.nesl.flowengine.node;

import android.os.RemoteException;
import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.Device;
import edu.ucla.nesl.flowengine.Sensor;


public class SeedNode extends DataFlowNode {
	private static final String TAG = SeedNode.class.getSimpleName();

	private int mSensorType;
	private Device mAttachedDevice;

	public SeedNode(int sensorType, Device attachedDevice) {
		mSensorType = sensorType;
		mAttachedDevice = attachedDevice;
	}

	public Sensor getSensor() {
		return mAttachedDevice.getSensor(mSensorType);
	}
	
	public Device getAttachedDevice() {
		return mAttachedDevice;
	}
	
	public void initializeGraph() {
		super.initializeGraph(null);
	}

	@Override
	public void startSensor() {
		DebugHelper.log(TAG, "Starting mSensorType: " + mSensorType + ", mAttachedDevice: " + mAttachedDevice);
		try {
			mAttachedDevice.getInterface().startSensor(mSensorType);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void stopSensor() {
		DebugHelper.log(TAG, "Stopping mSensorType: " + mSensorType + ", mAttachedDevice: " + mAttachedDevice);
		try {
			mAttachedDevice.getInterface().stopSensor(mSensorType);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void processInput(String name, String type, Object inputData, int length, long timestamp) {
		DebugHelper.log(TAG, "name: " + name + ", type: " + type + ", length: " + length + ", timestamp: " + timestamp);
		output(name, type, inputData, length, timestamp);
	}
}
