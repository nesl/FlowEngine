package edu.ucla.nesl.flowengine;

import edu.ucla.nesl.flowengine.aidl.AbstractDeviceInterface;

public class AbstractDevice {
	AbstractDeviceInterface adi;
	
	AbstractDevice(AbstractDeviceInterface adi) {
		this.adi = adi;
	}
}
