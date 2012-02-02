package edu.ucla.nesl.flowengine;

import edu.ucla.nesl.flowengine.aidl.AbstractDeviceInterface;
import edu.ucla.nesl.flowengine.node.DataFlowNode;

public class AbstractDevice {
	AbstractDeviceInterface adi;
	
	AbstractDevice(AbstractDeviceInterface adi) {
		this.adi = adi;
	}
}
