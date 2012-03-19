package edu.ucla.nesl.flowengine.node;

import java.util.Map;

import android.os.RemoteException;
import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.Device;
import edu.ucla.nesl.flowengine.Sensor;
import edu.ucla.nesl.flowengine.SensorType;


public class SeedNode extends DataFlowNode {
	private static final String TAG = SeedNode.class.getSimpleName();

	private int mSensorType;
	private Device mAttachedDevice;

	public SeedNode(String simpleNodeName, int sensorType, Device attachedDevice) {
		super(simpleNodeName);
		mSensorType = sensorType;
		mAttachedDevice = attachedDevice;
	}

	@Override
	protected String processParentNodeName(String parentNodeName) {
		return "";
	}
	
	public void configureNodeName(Map<String, DataFlowNode> nodeNameMap) {
		configurePushNodeName(nodeNameMap, null);
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
	
	public void attachDevice(Device device) {
		mAttachedDevice = device;
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
	protected void processInput(String name, String type, Object data, int length, long timestamp) {
		//DebugHelper.log(TAG, "name: " + name + ", type: " + type + ", length: " + length + ", timestamp: " + timestamp);
		if (type.equals("String")) {
			DebugHelper.log(TAG, (String)data);
		}
		/*if (name.equals(SensorType.getSensorName(SensorType.ZEPHYR_BATTERY))) {
			DebugHelper.log(TAG, name + ": " + (Integer)data);
		} else if (name.equals(SensorType.getSensorName(SensorType.ZEPHYR_BUTTON_WORN))) {
			DebugHelper.log(TAG, name + ": " + (Integer)data);
		}*/
		
		output(name, type, data, length, timestamp);
	}
}
