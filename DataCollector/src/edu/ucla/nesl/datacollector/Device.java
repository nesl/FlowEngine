package edu.ucla.nesl.datacollector;

public class Device {

	private String serviceName;
	private String deviceName;
	private boolean isEnabled;
	
	public Device(String serviceName, String deviceName, boolean isEnabled) {
		this.serviceName = serviceName;
		this.deviceName = deviceName;
		this.isEnabled = isEnabled;
	}

	public Device(String deviceName, boolean isEnabled) {
		this.deviceName = deviceName;
		this.isEnabled = isEnabled;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	public boolean isEnabled() {
		return isEnabled;
	}

	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}
}
