package edu.ucla.nesl.flowengine.aidl;

import edu.ucla.nesl.flowengine.aidl.ApplicationInterface;

interface FlowEngineAppAPI {

	int register(ApplicationInterface appInterface);
	void unregister(int appId);
	
	void submitGraph(String contextName, String graph);
	void subscribe(int appId, String nodeName);
	void unsubscribe(int appId, String nodeName);
	
	String[] getSubscribedNodeNames(int appId);
} 