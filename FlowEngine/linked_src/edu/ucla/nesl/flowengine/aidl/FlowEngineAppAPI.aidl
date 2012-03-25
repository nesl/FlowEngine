package edu.ucla.nesl.flowengine.aidl;

import edu.ucla.nesl.flowengine.aidl.ApplicationInterface;

interface FlowEngineAppAPI {
	int register(ApplicationInterface appInterface);
	void subscribe(int appId, String nodeName);
	void unregister(int appId);
} 