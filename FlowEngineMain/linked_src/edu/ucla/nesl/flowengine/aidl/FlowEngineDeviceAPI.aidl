package edu.ucla.nesl.flowengine.aidl;

import edu.ucla.nesl.flowengine.aidl.WaveSegment;
import edu.ucla.nesl.flowengine.aidl.AbstractDeviceInterface;

interface FlowEngineDeviceAPI {
	void pushData(int deviceID, in double[] data);
	int addAbstractDevice(AbstractDeviceInterface adi);		
	void removeAbstractDevice(int deviceID);
} 