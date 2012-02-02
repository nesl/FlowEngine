package edu.ucla.nesl.flowengine.aidl;

import edu.ucla.nesl.flowengine.aidl.AbstractDeviceInterface;

interface FlowEngineDeviceAPI {

	void pushIntData(int seed_name, int data, int length);
	void pushIntArrayData(int seed_name, in int[] data, int length);
	void pushDoubleArrayData(int seed_name, in double[] data, int length);

	int addAbstractDevice(AbstractDeviceInterface adi);		
	void removeAbstractDevice(int deviceID);
} 