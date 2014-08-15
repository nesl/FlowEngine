package edu.ucla.nesl.flowengine.aidl;

interface DeviceAPI {
	void start();
	void stop();
	void kill();
	void startSensor(int sensor);
	void stopSensor(int sensor);
}