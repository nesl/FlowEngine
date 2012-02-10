package edu.ucla.nesl.flowengine.aidl;

import edu.ucla.nesl.flowengine.aidl.DeviceInterface;

interface FlowEngineAPI {

	void pushIntData(int deviceID, int sensor, int data, int length, long timestamp);
	void pushIntArrayData(int deviceID, int sensor, in int[] data, int length, long timestamp);
	void pushDoubleArrayData(int deviceID, int sensor, in double[] data, int length, long timestamp);

	int addDevice(DeviceInterface deviceInterface);
	void addSensor(int deviceID, int sensorType, int sampleInterval);		
	void removeDevice(int deviceID);
} 