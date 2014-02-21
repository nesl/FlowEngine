package edu.ucla.nesl.flowengine;

public class SensorType {
	public static final int ECG_SAMPLE_INTERVAL = 4; //ms
	public static final int RIP_SAMPLE_INTERVAL = 56; //ms
	
	public static final int CHEST_ACCELEROMETER = 1;
	public static final int ECG = 2;
	public static final int RIP = 3;
	public static final int SKIN_TEMPERATURE = 4;
	public static final int PHONE_ACCELEROMETER = 5;
	public static final int PHONE_GPS = 6;
	public static final int EXTERNAL_ACCELEROMETER = 7;
	public static final int PHONE_BATTERY = 8;
	public static final int ZEPHYR_BATTERY = 9;
	public static final int ZEPHYR_BUTTON_WORN = 10;

	public static final String CHEST_ACCELEROMETER_NAME = "ChestAccelerometer";
	public static final String ECG_NAME = "ECG";
	public static final String RIP_NAME = "RIP";
	public static final String SKIN_TEMPERATURE_NAME = "SkinTemperature";
	public static final String PHONE_ACCELEROMETER_NAME = "PhoneAccelerometer";
	public static final String PHONE_GPS_NAME = "PhoneGPS";
	public static final String EXTERNAL_ACCELEROMETER_NAME = "ExternalAccelerometer";
	public static final String PHONE_BATTERY_NAME = "PhoneBattery";
	public static final String ZEPHYR_BATTERY_NAME = "ZephyrBattery";
	public static final String ZEPHYR_BUTTON_WORN_NAME = "ZephyrButtonWorn";

	public static final String ACTIVITY_CONTEXT_NAME = "Activity";
	public static final String STRESS_CONTEXT_NAME = "Stress";
	public static final String CONVERSATION_CONTEXT_NAME = "Conversation";
	public static final String MOTION_CONTEXT_NAME = "Motion";
	public static final String OUTDOOR_CONTEXT_NAME = "Outdoor";
		
	public static final int ACTIVITY_STILL = 0;
	public static final int ACTIVITY_WALK = 1;
	public static final int ACTIVITY_RUN = 2;
	public static final int ACTIVITY_BIKE = 3;
	public static final int ACTIVITY_DRIVE = 4;
	
	public static final int NO_STRESS = 0;
	public static final int STRESS = 1;
	
	public static final int QUIET = 0;
	public static final int SPEAKING = 1;
	public static final int SMOKING = 2;
	
	private SensorType() {}

	public static String getSensorName(int sensor) {
		switch (sensor) {
		case CHEST_ACCELEROMETER:
			return CHEST_ACCELEROMETER_NAME;
		case ECG:
			return ECG_NAME;
		case RIP:
			return RIP_NAME;
		case SKIN_TEMPERATURE:
			return SKIN_TEMPERATURE_NAME;
		case PHONE_ACCELEROMETER:
			return PHONE_ACCELEROMETER_NAME;
		case PHONE_GPS:
			return PHONE_GPS_NAME;
		case EXTERNAL_ACCELEROMETER:
			return EXTERNAL_ACCELEROMETER_NAME;
		case PHONE_BATTERY:
			return PHONE_BATTERY_NAME;
		case ZEPHYR_BATTERY:
			return ZEPHYR_BATTERY_NAME;
		case ZEPHYR_BUTTON_WORN:
			return ZEPHYR_BUTTON_WORN_NAME;
		}
		return "UnkownSensor";
	}
	
	public static int getSensorId(String name) {
		if (name.equals(CHEST_ACCELEROMETER_NAME)) {
			return CHEST_ACCELEROMETER;
		} else if (name.equals(ECG_NAME)) {
			return ECG;
		} else if (name.equals(RIP_NAME)) {
			return RIP;
		} else if (name.equals(SKIN_TEMPERATURE_NAME)) {
			return SKIN_TEMPERATURE;
		} else if (name.equals(PHONE_ACCELEROMETER_NAME)) {
			return PHONE_ACCELEROMETER;
		} else if (name.equals(PHONE_GPS_NAME)) {
			return PHONE_GPS;
		} else if (name.equals(EXTERNAL_ACCELEROMETER_NAME)) {
			return EXTERNAL_ACCELEROMETER;
		} else if (name.equals(PHONE_BATTERY_NAME)) {
			return PHONE_BATTERY;
		} else if (name.equals(ZEPHYR_BATTERY_NAME)) {
			return ZEPHYR_BATTERY;
		} else if (name.equals(ZEPHYR_BUTTON_WORN_NAME)) {
			return ZEPHYR_BUTTON_WORN;
		} 
		return -1;
	}
}
