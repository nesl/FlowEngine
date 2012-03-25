package edu.ucla.nesl.flowengine;

public class Sensor {
	private int mSensorID;
	private int mSampleInterval; //ms
	
	public Sensor(int sensorID, int sampleInterval) {
		mSensorID = sensorID;
		mSampleInterval = sampleInterval;
	}
	
	public int getSensorID() {
		return mSensorID;
	}
	
	public int getSampleInterval() {
		return mSampleInterval;
	}
}