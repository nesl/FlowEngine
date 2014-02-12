package edu.ucla.nesl.flowengine;

import java.util.HashMap;
import java.util.Map;

import edu.ucla.nesl.flowengine.aidl.DeviceAPI;

public class Device {
	private DeviceAPI mInterface;
	private Map<Integer, Sensor> mSensorMap = new HashMap<Integer, Sensor>();
	
	public Device(DeviceAPI deviceAPI) {
		mInterface = deviceAPI;
	}
	
	public DeviceAPI getInterface() {
		return mInterface;
	}
	
	public void addSensor(int sensorType, int sampleInterval) {
		mSensorMap.put(sensorType, new Sensor(sensorType, sampleInterval));
	}
	
	public Sensor getSensor(int sensorType) {
		return mSensorMap.get(sensorType);
	}
	
	public Sensor[] getSensorList() {
		Sensor[] sensors = new Sensor[mSensorMap.size()];
		int sensorsIndex = 0;
		for (Map.Entry<Integer, Sensor> entry: mSensorMap.entrySet()) {
			Sensor sensor = entry.getValue();
			sensors[sensorsIndex++] = sensor;
		}
		return sensors;
	}
}
