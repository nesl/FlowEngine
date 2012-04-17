package edu.ucla.nesl.datalogger;

public class PhoneBatteryData {
	private long mTimestamp;
	private String mBatteryInfo;

	public PhoneBatteryData(long timestamp, String batteryInfo) {
		this.mTimestamp = timestamp;
		this.mBatteryInfo = batteryInfo;
	}
	
	public long getTimestamp() {
		return mTimestamp;
	}
	
	public void setTimestamp(long timestamp) {
		this.mTimestamp = timestamp;
	}
	
	public String getBatteryInfo() {
		return mBatteryInfo;
	}
	
	public void setBatteryInfo(String batteryInfo) {
		this.mBatteryInfo = batteryInfo;
	}
}
