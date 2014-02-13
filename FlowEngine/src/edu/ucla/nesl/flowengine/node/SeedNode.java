package edu.ucla.nesl.flowengine.node;

import java.util.Map;

import android.os.RemoteException;
import edu.ucla.nesl.flowengine.DebugHelper;
import edu.ucla.nesl.flowengine.Device;
import edu.ucla.nesl.flowengine.Sensor;


public class SeedNode extends DataFlowNode {
	private static final String TAG = SeedNode.class.getSimpleName();

	private int mSensorID;
	private Device mAttachedDevice;

	public SeedNode(String simpleNodeName, int sensorID, Device attachedDevice) {
		super(simpleNodeName);
		mSensorID = sensorID;
		mAttachedDevice = attachedDevice;
	}

	public void configureNodeName(Map<String, DataFlowNode> nodeNameMap) {
		configurePushNodeName(nodeNameMap, null);
	}

	public int getSensorID() {
		return mSensorID;
	}
	
	public Sensor getSensor() {
		return mAttachedDevice.getSensor(mSensorID);
	}
	
	public Device getAttachedDevice() {
		return mAttachedDevice;
	}
	
	/*public void initializeGraph() {
		super.initializeGraph(null);
	}*/
	
	public void attachDevice(Device device) {
		mAttachedDevice = device;
	}
	
	public void detachDevice() {
		mAttachedDevice = null;
	}

	@Override
	public void startSensor() {
		DebugHelper.log(TAG, "Starting mSensorType: " + mSensorID + ", mAttachedDevice: " + mAttachedDevice);
		if (mAttachedDevice == null) {
			DebugHelper.log(TAG, "Device not attached yet.");
			return;
		}
		try {
			mAttachedDevice.getInterface().startSensor(mSensorID);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void stopSensor() {
		DebugHelper.log(TAG, "Stopping mSensorType: " + mSensorID + ", mAttachedDevice: " + mAttachedDevice);
		if (mAttachedDevice == null) {
			DebugHelper.log(TAG, "Device not attached yet.");
			return;
		}
		try {
			mAttachedDevice.getInterface().stopSensor(mSensorID);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	protected void processInput(String name, String type, Object data, int length, long timestamp) {
		//DebugHelper.log(TAG, "name: " + name + ", type: " + type + ", length: " + length + ", timestamp: " + timestamp);
		/*if (type.equals(DataType.STRING)) {
			DebugHelper.log(TAG, (String)data);
		}*/
		/*
		} else if (name.equals(SensorType.getSensorName(SensorType.ZEPHYR_BUTTON_WORN))) {
			DebugHelper.log(TAG, name + ": " + (Integer)data);
		}*/
		/*if (name.equals(SensorType.getSensorName(SensorType.ECG))){
			DebugHelper.log(TAG, name + " " + length + " samples");
		} else if (name.equals(SensorType.getSensorName(SensorType.RIP))){
			DebugHelper.log(TAG, name + " " + length + " samples");
		} else if (name.equals(SensorType.getSensorName(SensorType.ZEPHYR_BATTERY))) {
			DebugHelper.log(TAG, name + ": " + (Integer)data);
		} else if (name.equals(SensorType.getSensorName(SensorType.PHONE_GPS))) {
			double[] gpsdata = (double[])data;
			DebugHelper.log(TAG, name + ": " + gpsdata[0] + ", " + gpsdata[1] + ", " + gpsdata[2] + ", " + gpsdata[3]);
		}*/
		
		output(name, type, data, length, timestamp);
	}
}
