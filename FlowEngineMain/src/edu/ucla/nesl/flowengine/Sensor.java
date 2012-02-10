package edu.ucla.nesl.flowengine;

public class Sensor {
	private int mSensorType;
	private int mSampleInterval; //ms
	
	public Sensor(int sensorType, int sampleInterval) {
		mSensorType = sensorType;
		mSampleInterval = sampleInterval;
	}
	
	public int getSensorType() {
		return mSensorType;
	}
	
	public int getSampleInterval() {
		return mSampleInterval;
	}
}