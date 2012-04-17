package edu.ucla.nesl.datalogger;

public class PhoneGPSData {
	private long mTimestamp;
	private double mLatitude;
	private double mLongitude;
	private double mAltitude;
	private double mSpeed;
	
	public PhoneGPSData(long timestamp, double latitude, double longitude, double altitude, double speed) {
		this.mTimestamp = timestamp;
		this.mLatitude = latitude;
		this.mLongitude = longitude;
		this.mAltitude = altitude;
		this.mSpeed = speed;
	}
	
	public long getTimestamp() {
		return mTimestamp;
	}
	
	public void setTimestamp(long timestamp) {
		this.mTimestamp = timestamp;
	}
	
	public double getLatitude() {
		return mLatitude;
	}
	
	public void setLatitude(double latitude) {
		this.mLatitude = latitude;
	}
	
	public double getLongitude() {
		return mLongitude;
	}
	
	public void setLongitude(double longitude) {
		this.mLongitude = longitude;
	}
	
	public double getAltitude() {
		return mAltitude;
	}
	
	public void setAltitude(double altitude) {
		this.mAltitude = altitude;
	}
	
	public double getSpeed() {
		return mSpeed;
	}

	public void setSpeed(double speed) {
		this.mSpeed = speed;
	}
}
