package edu.ucla.nesl.datalogger;

public class StressData {
	private long mTimestamp;
	private String mStress;
	
	public StressData(long timestamp, String stress) {
		this.mTimestamp = timestamp;
		this.mStress = stress;
	}
	
	public long getTimestamp() {
		return mTimestamp;
	}
	
	public void setTimestamp(long timestamp) {
		this.mTimestamp = timestamp;
	}
	
	public String getStress() {
		return mStress;
	}
	
	public void setStress(String stress) {
		this.mStress = stress;
	}
}
