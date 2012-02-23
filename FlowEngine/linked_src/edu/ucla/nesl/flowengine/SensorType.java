package edu.ucla.nesl.flowengine;

public class SensorType {
	public static final int CHEST_ACCELEROMETER = 1;
	public static final int ECG = 2;
	public static final int RIP = 3;
	public static final int SKIN_TEMPERATURE = 4;
	public static final int PHONE_ACCELEROMETER = 5;
	public static final int PHONE_GPS = 6;
	public static final int EXTERNAL_ACCELEROMETER = 7;
	
	private SensorType() {}

	public static String getSensorName(int sensorName) {
		switch (sensorName) {
		case CHEST_ACCELEROMETER:
			return "Chest Accelerometer";
		case ECG:
			return "ECG";
		case RIP:
			return "RIP";
		case SKIN_TEMPERATURE:
			return "Skin Temperature";
		case PHONE_ACCELEROMETER:
			return "Phone Accelerometer";
		case PHONE_GPS:
			return "Phone GPS";
		case EXTERNAL_ACCELEROMETER:
			return "External Accelerometer";
		}
		return "Unkown Sensor";
	}
}
