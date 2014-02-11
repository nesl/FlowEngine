package edu.ucla.nesl.datacollector;

public class Device {

	public static final String LOCATION = "Location";
	public static final String ACCELEROMETER = "Accelerometer";
	public static final String ECG = "ECG";
	public static final String RESPIRATION = "Respiration";
	public static final String ACTIVITY = "Activity";
	public static final String STRESS = "Stress";
	public static final String CONVERSATION = "Conversation";
	
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
