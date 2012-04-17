package edu.ucla.nesl.datalogger;

public class ZephyrBatteryData {
	private long mTimestamp;
	private int mZephyrBattery;
	
	public ZephyrBatteryData(long timestamp, int zephyrBatter) {
		this.mTimestamp = timestamp;
		this.mZephyrBattery = zephyrBatter;
	}

	public long getTimestamp() {
		return mTimestamp;
	}
	
	public void setTimestamp(long timestamp) {
		this.mTimestamp = timestamp;
	}
	
	public int getZephyrBattery() {
		return mZephyrBattery;
	}
	
	public void setZephyrBatter(int zephyrBatter) {
		this.mZephyrBattery = zephyrBatter;
	}
}
