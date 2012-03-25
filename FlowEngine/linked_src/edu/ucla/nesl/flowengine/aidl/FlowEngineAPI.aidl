package edu.ucla.nesl.flowengine.aidl;

import edu.ucla.nesl.flowengine.aidl.DeviceAPI;

interface FlowEngineAPI {
	void pushInt(int deviceID, int sensor, int data, long timestamp);
	void pushIntArray(int deviceID, int sensor, in int[] data, int length, long timestamp);
	void pushDouble(int deviceID, int sensor, in double data, long timestamp);
	void pushDoubleArray(int deviceID, int sensor, in double[] data, int length, long timestamp);
	void pushString(int deviceID, int sensor, in String data, int length, long timestamp);

	int addDevice(DeviceAPI deviceInterface);
	void addSensor(int deviceID, int sensor, int sampleInterval);		
	void removeDevice(int deviceID);
} 