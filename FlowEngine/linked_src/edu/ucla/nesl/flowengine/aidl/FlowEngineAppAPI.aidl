package edu.ucla.nesl.flowengine.aidl;

import edu.ucla.nesl.flowengine.aidl.ApplicationInterface;

interface FlowEngineAppAPI {
	int registerApplication(ApplicationInterface appInterface);
	void addSubscription(int appId, String nodeName);
	void configure(int appId);
	void unregisterApplication(int appId);
} 