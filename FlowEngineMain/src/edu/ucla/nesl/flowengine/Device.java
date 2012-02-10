package edu.ucla.nesl.flowengine;

import java.util.HashMap;
import java.util.Map;

import edu.ucla.nesl.flowengine.aidl.DeviceInterface;

public class Device {
	private DeviceInterface mInterface;
	private Map<Integer, Sensor> mSensorMap = new HashMap<Integer, Sensor>();
	
	public Device(DeviceInterface deviceInterface) {
		mInterface = deviceInterface;
	}
	
	public DeviceInterface getInterface() {
		return mInterface;
	}
	
	public void addSensor(int sensorType, int sampleInterval) {
		mSensorMap.put(sensorType, new Sensor(sensorType, sampleInterval));
	}
	
	public Sensor getSensor(int sensorType) {
		return mSensorMap.get(sensorType);
	}
}
