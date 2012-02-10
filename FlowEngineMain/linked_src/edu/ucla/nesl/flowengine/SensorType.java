package edu.ucla.nesl.flowengine;

public class SensorType {
	public static final int ACCELEROMETER = 1;
	public static final int ECG = 2;
	public static final int RIP = 3;
	public static final int SKIN_TEMPERATURE = 4;

	private SensorType() {}

	public static String getSensorName(int sensorName) {
		switch (sensorName) {
		case ACCELEROMETER:
			return "Accelerometer";
		case ECG:
			return "ECG";
		case RIP:
			return "RIP";
		case SKIN_TEMPERATURE:
			return "Skin Temperature";
		}
		return "Unkown Sensor";
	}
}
