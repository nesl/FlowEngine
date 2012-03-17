package edu.ucla.nesl.flowengine.aidl;

interface ApplicationInterface {
	void publish(String name, String data, long timestamp);
}