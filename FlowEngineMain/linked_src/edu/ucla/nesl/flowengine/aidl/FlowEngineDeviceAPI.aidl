package edu.ucla.nesl.flowengine.aidl;

import edu.ucla.nesl.flowengine.aidl.WaveSegment;
import edu.ucla.nesl.flowengine.aidl.AbstractDeviceInterface;

interface FlowEngineDeviceAPI {

	void pushWaveSegment(in WaveSegment ws);
	
	void addAbstractDevice(AbstractDeviceInterface adi);
	
	void removeAbstractDevice(AbstractDeviceInterface adi);
	
} 